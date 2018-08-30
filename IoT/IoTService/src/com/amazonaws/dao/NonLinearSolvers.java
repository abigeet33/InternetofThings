package com.amazonaws.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.amazonaws.bean.ServiceReq;
import com.amazonaws.utils.Constants;
import com.amazonaws.utils.Utility;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;

public class NonLinearSolvers {

	static int requestId = 1000;
	int[][] EnergyMatrix;
	int[][] PTMatrix;
	int[][] UserMatrix;

	com.jcraft.jsch.Session session = null;

	JSch jsch = new JSch();
	String user = "ec2-user";
	String host = "ec2-18-237-97-195.us-west-2.compute.amazonaws.com";
	int port = 22;
	String privateKey = "/usr/share/tomcat7/webapps/ROOT/IoTserviceschedule.pem";
	// String privateKey = "IoTserviceschedule.pem";
	Channel channel = null;

	public static void main(String args[]) {
		Connection conn = null;
		conn = new Utility().getDBConnection();
		HashMap<String, String> serviceDeviceLst = new HashMap<>();
		serviceDeviceLst.put("D1", "'S1','S2'");
		serviceDeviceLst.put("D2", "'S2','S3'");
		serviceDeviceLst.put("D3", "'S3','S4'");
		boolean flag = true;
		new Utility().updateUserLimit(conn, serviceDeviceLst, flag);
	}

	// Function to modify the user input into the required format for processing

	public List<ServiceReq> modifyList(List<ServiceReq> serviceReqlist) {
		System.out.println("inside modifyList");

		int[] service_nums = new int[Constants.NO_OF_SERVICES];

		for (int i = 0; i < serviceReqlist.size(); i++) {

			int size = serviceReqlist.get(i).getRequestList().size();

			Arrays.fill(service_nums, 0);

			int num = 0;
			for (int j = 0; j < size; j++) {
				num = Integer.parseInt(serviceReqlist.get(i).getRequestList().get(j).substring(1));
				service_nums[num - 1] = num;
			}

			System.out.println("Services List-->" + i);
			for (int k = 0; k < service_nums.length; k++)
				System.out.println(service_nums[k]);
			serviceReqlist.get(i).getRequestList().clear();
			System.out.println("Size after deleting-->" + serviceReqlist.get(i).getRequestList().size());

			List<String> modifiedServiceList = new ArrayList<String>();
			for (int k = 0; k < service_nums.length; k++) {
				if (service_nums[k] != 0)
					modifiedServiceList.add("S" + service_nums[k]);
				else
					modifiedServiceList.add("0");

			}
			serviceReqlist.get(i).setRequestList(modifiedServiceList);
		}
		System.out.println("exiting modifyList");

		return serviceReqlist;
	}

	public String nonLinearSolvers(Connection conn, List<ServiceReq> serviceReqlist, int requestType) {
		String response = null;
		System.out.println("inside nonLinearSolvers");
		constructDataFile(conn, serviceReqlist, requestType);
		response = executeAMPLSolver(conn, serviceReqlist, requestType);
		EnergyMatrix = null;
		PTMatrix = null;
		UserMatrix = null;
		System.out.println("exiting nonLinearSolvers");
		return response;

	}

	public void constructDataFile(Connection conn, List<ServiceReq> serviceReqlist, int requestType) {
		System.out.println("inside constructDataFile");
		buildDataFile(conn, serviceReqlist, requestType);
		System.out.println("exiting constructDataFile");
	}

	public String executeAMPLSolver(Connection conn, List<ServiceReq> serviceReqlist, int requestType) {
		System.out.println("inside executeAMPLSolver");

		// start time of AMPL solver
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		String currentTime = dateFormat.format(new Date());
		System.out.println("start time of AMPL solver-->" + currentTime);

		String response = null;
		try {
			jsch.addIdentity(privateKey);
			System.out.println("identity added ");

			session = jsch.getSession(user, host, port);
			System.out.println("session created.");

			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.setPassword("");
			session.connect();
			channel = session.openChannel("exec");

			String CommandOutput = null;
			if (requestType == Constants.SCENARIO_1)
				((ChannelExec) channel).setCommand(
						"cd /home/ec2-user/Java_Files; java -cp /home/ec2-user/Java_Files/lib/ampl-1.4.0.0.jar:. amplsolver1 bonmin");
			else if (requestType == Constants.SCENARIO_2)
				((ChannelExec) channel).setCommand(
						"cd /home/ec2-user/Java_Files; java -cp /home/ec2-user/Java_Files/lib/ampl-1.4.0.0.jar:. amplsolver2 bonmin");

			channel.setInputStream(null);
			((ChannelExec) channel).setErrStream(System.err);

			InputStream in = channel.getInputStream();
			channel.connect();

			System.out.println("Channel Connected to machine ");
			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);

					if (i < 0)
						break;
					System.out.print(new String(tmp, 0, i));
					CommandOutput = new String(tmp, 0, i);
				}

				if (channel.isClosed()) {
					System.out.println("exit-status: " + channel.getExitStatus());
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		// End time of AMPL solver
		SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		String currentTime1 = dateFormat1.format(new Date());
		System.out.println("End time of AMPL solver-->" + currentTime1);

		// reading from output file
		response = StoreValuesFromFiletoDatabase(conn, requestType);

		channel.disconnect();
		session.disconnect();
		System.out.println("exiting executeAMPLSolver");
		return response;
	}

	public String StoreValuesFromFiletoDatabase(Connection conn, int requestType) {
		System.out.println("inside StoreValuesFromFiletoDatabase");

		InputStream inputStream = null;
		String response = null;

		try {

			jsch.addIdentity(privateKey);
			System.out.println("identity added ");
			session = jsch.getSession(user, host, port);
			System.out.println("session created.");

			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.setPassword("");
			session.connect();

			channel = session.openChannel("sftp");
			channel.connect();
			ChannelSftp channelSftp = (ChannelSftp) channel;
			channelSftp.cd("/home/ec2-user/Java_Files");
			if (requestType == Constants.SCENARIO_1)
				inputStream = channelSftp.get("/home/ec2-user/Java_Files/output1.txt");

			else if (requestType == Constants.SCENARIO_2)
				inputStream = channelSftp.get("/home/ec2-user/Java_Files/output2.txt");

			int totalCompletionTime = 0;
			int compTime = 0;
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
			HashMap<String, String> req_device = new HashMap<String, String>();
			HashMap<String, Integer> req_time = new HashMap<String, Integer>();
			System.out.println("Output from file================================");
			String st;
			int count1 = 0, count2 = 0;
			while ((st = br.readLine()) != null) {
				if (st.contains("Completion Time")) {
					String[] finalstr = st.split("\\|");
					totalCompletionTime = (int) Math.round(Double.parseDouble((finalstr[1])));
					System.out.println(totalCompletionTime);
					System.out.println(st);
				} else if (st.contains("Start of W")) {
					++count1;
				} else if (count1 > 0) {
					System.out.println(st.toString());
					if (st.contains(".")) {
						int index = st.indexOf(".");
						char val = st.charAt(index - 1);

						System.out.println(index + "---" + val);
						if (Integer.parseInt(String.valueOf(val)) > 0) {
							String[] finalstr1 = st.split("\\ ");
							req_device.put(finalstr1[0], finalstr1[2]);
						}
					} else if (st.contains("End of W")) {
						count1 = 0;
					}
				} else if (st.contains("Start of CT")) {
					++count2;
				} else if (count2 > 0) {
					if (st.contains(".")) {
						String[] finalstr2 = st.split("\\ ");
						compTime = (int) Math.round(Double.parseDouble((finalstr2[4])));
						req_time.put(finalstr2[0], compTime);
					} else if (st.contains("End of CT")) {
						count2 = 0;
					}
				}
			}
			br.close();

			System.out.println("Request device mapping");
			if ((req_device.size() > 0) && (req_time.size() > 0)) {
				System.out.println("hashmaps are not empty");
				for (Entry<String, String> entry : req_device.entrySet()) {
					System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());

					new Utility().updateServiceRequestsTable(conn, entry.getKey(), entry.getValue(),
							Constants.Queue_Status_SCHEDULED);
				}

				System.out.println("Request completion time mapping");
				for (Entry<String, Integer> entry : req_time.entrySet()) {
					System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
					new Utility().updateServiceRequestsTable(conn, entry.getKey(), entry.getValue());

				}

				response = Constants.SUCCESS;
			} else {
				response = Constants.FAILURE;
				System.out.println("response is set to failure");
			}
			req_device = null;
			req_time = null;
		} catch (Exception e) {
			System.out.println("inside catch block of storedatabase");
			System.out.println(e.getMessage());
		}
		channel.disconnect();
		session.disconnect();

		System.out.println("exiting StoreValuesFromFiletoDatabase");
		return response;
	}

	public void buildDataFile(Connection conn, List<ServiceReq> serviceReqlist, int requestType) {
		PrintStream console = null;
		PrintStream o = null;
		System.out.println("inside buildDataFile");
		System.out.println("Connecting to  EC2 Instance");
		try {
			jsch.addIdentity(privateKey);
			System.out.println("identity added ");

			session = jsch.getSession(user, host, port);
			System.out.println("session created.");

			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.setPassword("");
			session.connect();
			channel = session.openChannel("exec");

			((ChannelExec) channel).setCommand("chmod o+x /home/ec2-user; cd /home/ec2-user/ampl.linux64/models");
			channel.setInputStream(null);
			((ChannelExec) channel).setErrStream(System.err);
			channel.setInputStream(System.in);
			channel.setOutputStream(System.out);
			InputStream in = channel.getInputStream();
			channel.connect();
			String CommandOutput = null;
			System.out.println("Channel Connected to machine " + host);
			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);

					if (i < 0)
						break;
					System.out.print(new String(tmp, 0, i));
					CommandOutput = new String(tmp, 0, i);
				}

				if (channel.isClosed()) {
					System.out.println("exit-status: " + channel.getExitStatus());
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
				}
			}

			System.out.println("connected!!");

			int req = serviceReqlist.size();
			System.out.println("request size-->" + req);
			int device = Constants.NO_OF_DEVICES;// select count(*) from device
			int service = Constants.NO_OF_SERVICES;

			// Creating a File object that represents the disk file.
			if (requestType == Constants.SCENARIO_1)
				o = new PrintStream(new File("/usr/share/tomcat7/webapps/ROOT/nlp1.dat"));
			else if (requestType == Constants.SCENARIO_2)
				o = new PrintStream(new File("/usr/share/tomcat7/webapps/ROOT/nlp2.dat"));
			// Store current System.out before assigning a new value

			console = System.out;

			// Assign o to output stream
			System.setOut(o);

			System.out.println("################################################################");
			System.out.println("##Scheduling.dat content");

			System.out.print("\n set req := ");
			int count = 0;
			for (int i = 1; i <= req; i++) {
				if (req == i)
					System.out.print("R" + (serviceReqlist.get(count++).getRequestId() + 1) + " ;\n");
				else
					System.out.print("R" + (serviceReqlist.get(count++).getRequestId() + 1) + " ");
			}

			System.out.print("\n set device := ");
			for (int i = 1; i <= device; i++) {
				if (device == i)
					System.out.print("D" + i + " ;\n");
				else
					System.out.print("D" + i + " ");
			}

			System.out.print("\n set service := ");
			for (int i = 1; i <= service; i++) {
				if (service == i)
					System.out.print("S" + i + " ;\n");
				else
					System.out.print("S" + i + " ");
			}

			System.out.print("\n set loc := ");
			for (int i = 1; i <= 2; i++) {
				if (i == 1)
					System.out.print(" X  ");
				else if (i == 2)
					System.out.print(" Y  ;\n");
			}

			int[][] RSMatrix = getInputMatrix(req, service, serviceReqlist);

			System.out.print("\n param RS : \t");
			for (int i = 1; i <= service; i++) {
				if (service == i)
					System.out.print("S" + i + " :=\n");
				else
					System.out.print("S" + i + " \t \t");
			}
			printMatrix(RSMatrix, "R", serviceReqlist);

			int[][] RStransposeMatrix = transposeMatrix(RSMatrix);

			count = 0;
			System.out.print("\n param RST : \t");
			for (int i = 1; i <= req; i++) {
				if (req == i)
					System.out.print("R" + (serviceReqlist.get(count++).getRequestId() + 1) + " :=\n");
				else
					System.out.print("R" + (serviceReqlist.get(count++).getRequestId() + 1) + " \t \t");
			}
			printMatrix(RStransposeMatrix, "S", serviceReqlist);

			EnergyMatrix = getEnergyMatrix(conn, req, device, serviceReqlist);

			PTMatrix = getTimeMatrix(conn, req, device, serviceReqlist);

			System.out.print("\n  param Lambda : \t");
			for (int i = 1; i <= device; i++) {
				if (device == i)
					System.out.print("D" + i + " :=\n");
				else
					System.out.print("D" + i + " \t \t");
			}
			printMatrix(EnergyMatrix, "R", serviceReqlist);

			System.out.print("\n  param T : \t");
			for (int i = 1; i <= device; i++) {
				if (device == i)
					System.out.print("D" + i + " :=\n");
				else
					System.out.print("D" + i + " \t \t");
			}
			printMatrix(PTMatrix, "R", serviceReqlist);

			System.out.print("\n param E : \t");
			for (int i = 1; i <= device; i++) {
				if (device == i)
					System.out.print("D" + i + " :=\n");
				else
					System.out.print("D" + i + " \t \t");
			}

			getCurrentEnergyOfDevice(conn, device);

			if (requestType == Constants.SCENARIO_2) {
				System.out.print("\n param Delay : \t");
				for (int i = 1; i <= device; i++) {
					if (device == i)
						System.out.print("D" + i + " :=\n");
					else
						System.out.print("D" + i + " \t \t");
				}

				for (int i = 1; i <= device; i++) {
					if (device == i)
						System.out.print("\t \t  \t 1 ; \n");
					else if (i == 1)
						System.out.print("1 \t  \t \t 1");
					else
						System.out.print("\t 1  \t \t ");
				}

				System.out.print("\n param EC : \t");
				for (int i = 1; i <= device; i++) {
					if (device == i)
						System.out.print("D" + i + " :=\n");
					else
						System.out.print("D" + i + " \t \t");
				}

				for (int i = 1; i <= device; i++) {
					if (device == i)
						System.out.print("\t \t  \t 1 ; \n");
					else if (i == 1)
						System.out.print("1 \t  \t \t 1");
					else
						System.out.print("\t 1  \t \t ");
				}
			}

			System.out.print("\n param requestedloc :     X      Y   := \n");
			System.out.println("1 \t \t " + serviceReqlist.get(0).getLatitude() + "\t \t"
					+ serviceReqlist.get(0).getLongitude() + ";");

			UserMatrix = getUserMatrix(conn, service, device, serviceReqlist);

			System.out.print("\n param U : \t");
			for (int i = 1; i <= device; i++) {
				if (device == i)
					System.out.print("D" + i + " :=\n");
				else
					System.out.print("D" + i + " \t \t");
			}
			printMatrix(UserMatrix, "S", serviceReqlist);
			RSMatrix = null;
			RStransposeMatrix = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.setOut(console);
		o.close();
		System.gc();
		channel.disconnect();
		try {
			channel = session.openChannel("sftp");
			channel.connect();
			ChannelSftp channelSftp = (ChannelSftp) channel;
			channelSftp.cd("/home/ec2-user/ampl.linux64/models");
			if (requestType == Constants.SCENARIO_1) {
				InputStream inputStream = new FileInputStream("/usr/share/tomcat7/webapps/ROOT/nlp1.dat");
				channelSftp.put(inputStream, "/home/ec2-user/ampl.linux64/models/nlp1.dat");
			} else if (requestType == Constants.SCENARIO_2) {
				InputStream inputStream = new FileInputStream("/usr/share/tomcat7/webapps/ROOT/nlp2.dat");
				channelSftp.put(inputStream, "/home/ec2-user/ampl.linux64/models/nlp2.dat");
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		channel.disconnect();
		session.disconnect();

	}

	public int[][] getInputMatrix(int r, int c, List<ServiceReq> serviceReqlist) {
		int[][] matrix = new int[r][c];
		// int count=0;
		for (r = 0; r < matrix.length; r++) {
			for (c = 0; c < matrix[r].length; c++) {
				// System.out.println(r+"--"+c+"--"+serviceReqlist.get(r).getRequestList().get(c));
				if (!serviceReqlist.get(r).getRequestList().get(c).equals("0"))
					matrix[r][c] = 1;
				else
					matrix[r][c] = 0;
			}
		}
		return matrix;
	}

	public int[][] getEnergyMatrix(Connection conn, int r, int c, List<ServiceReq> serviceReqlist) {
		int[][] matrix = new int[r][c];
		String expr = "";
		int energy = 0;
		String deviceName = "";
		for (r = 0; r < matrix.length; r++) {
			for (c = 0; c < matrix[r].length; c++) {
				matrix[r][c] = 0;
			}
		}

		for (int i = 0; i < serviceReqlist.size(); i++) {
			HashSet<String> hset = new HashSet<>();
			int count = 0;
			expr = "";
			// System.out.println(expr);
			try {

				for (int j = 0; j < serviceReqlist.get(i).getRequestList().size(); j++)
					if (!serviceReqlist.get(i).getRequestList().get(j).equals("0"))
						hset.add(serviceReqlist.get(i).getRequestList().get(j));

				Iterator<String> it = hset.iterator();
				// System.out.println(hset.size());
				while (it.hasNext()) {
					count++;
					if (count == hset.size())
						expr += "'" + it.next() + "'";
					else
						expr += "'" + it.next() + "',";
				}

				PreparedStatement statement = null;
				ResultSet resultSet = null;
				String Query_Str = "select deviceid,sum(energyConsumption) as total from service where servicename in ("
						+ expr + ") group by deviceid order by deviceid";

				// System.out.println(Query_Str);
				statement = conn.prepareStatement(Query_Str);

				resultSet = statement.executeQuery();

				while (resultSet.next())
					matrix[i][Integer.parseInt(resultSet.getString("deviceid").substring(1)) - 1] = resultSet
							.getInt("total");

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return matrix;
	}

	public int[][] getTimeMatrix(Connection conn, int r, int c, List<ServiceReq> serviceReqlist) {
		int[][] matrix = new int[r][c];
		String expr = "";

		for (r = 0; r < matrix.length; r++) {
			for (c = 0; c < matrix[r].length; c++) {
				matrix[r][c] = 0;
			}
		}

		for (int i = 0; i < serviceReqlist.size(); i++) {
			HashSet<String> hset = new HashSet<>();
			int count = 0;
			expr = "";
			// System.out.println(expr);
			try {

				for (int j = 0; j < serviceReqlist.get(i).getRequestList().size(); j++)
					if (!serviceReqlist.get(i).getRequestList().get(j).equals("0"))
						hset.add(serviceReqlist.get(i).getRequestList().get(j));

				Iterator<String> it = hset.iterator();
				// System.out.println(hset.size());
				while (it.hasNext()) {
					count++;
					if (count == hset.size())
						expr += "'" + it.next() + "'";
					else
						expr += "'" + it.next() + "',";
				}

				PreparedStatement statement = null;
				ResultSet resultSet = null;
				String Query_Str = "select deviceid,sum(serviceTime) as total from service where servicename in ("
						+ expr + ") group by deviceid order by deviceid";

				// System.out.println(Query_Str);
				statement = conn.prepareStatement(Query_Str);

				resultSet = statement.executeQuery();

				while (resultSet.next())
					matrix[i][Integer.parseInt(resultSet.getString("deviceid").substring(1)) - 1] = resultSet
							.getInt("total");

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return matrix;
	}

	public int[][] getUserMatrix(Connection conn, int r, int c, List<ServiceReq> serviceReqlist) {
		int[][] matrix = new int[r][c];
		int count = 0;
		HashSet<String> hset = new HashSet<>();
		String expr = "";

		for (int i = 0; i < serviceReqlist.size(); i++)
			for (int j = 0; j < serviceReqlist.get(i).getRequestList().size(); j++)
				if (!serviceReqlist.get(i).getRequestList().get(j).equals("0"))
					hset.add(serviceReqlist.get(i).getRequestList().get(j));

		Iterator<String> it = hset.iterator();
		// System.out.println(hset.size());
		while (it.hasNext()) {
			count++;
			if (count == hset.size())
				expr += "'" + it.next() + "'";
			else
				expr += "'" + it.next() + "',";
		}

		// System.out.println(expr);

		try {
			PreparedStatement statement = null;
			ResultSet resultSet = null;
			String Query_Str = "select deviceid,servicename,userlimit-userLimitInUse as usercount from service where servicename in ("
					+ expr + ") order by deviceid,servicename";

			statement = conn.prepareStatement(Query_Str);

			// System.out.println(Query_Str);
			resultSet = statement.executeQuery();

			for (r = 0; r < matrix.length; r++) {
				for (c = 0; c < matrix[r].length; c++) {
					matrix[r][c] = 0;
				}
			}

			while (resultSet.next())
				matrix[Integer.parseInt(resultSet.getString("servicename").substring(1)) - 1][Integer
						.parseInt(resultSet.getString("deviceid").substring(1)) - 1] = resultSet.getInt("usercount");

		} catch (Exception e) {
			e.printStackTrace();
		}

		return matrix;
	}

	public static void printMatrix(int[][] matrix, String str, List<ServiceReq> serviceReqlist) {

		int r = 0, c = 0;
		for (r = 0; r < matrix.length; r++) {
			if (str == "R")
				System.out.print(str + (serviceReqlist.get(r).getRequestId() + 1) + "\t \t");
			else
				System.out.print(str + (r + 1) + "\t \t");
			for (c = 0; c < matrix[r].length; c++) {
				System.out.print(matrix[r][c] + "\t \t");
			}
			if (r == matrix.length - 1)
				System.out.println(";");
			else
				System.out.println(" ");
		}
	}

	public static int[][] transposeMatrix(int[][] m) {
		int[][] temp = new int[m[0].length][m.length];
		for (int i = 0; i < m.length; i++)
			for (int j = 0; j < m[0].length; j++)
				temp[j][i] = m[i][j];
		return temp;
	}

	public void getCurrentEnergyOfDevice(Connection conn, int device) {
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		int count = 0;
		String Query_Str = "select energylevel from device order by deviceId";

		// System.out.println(Query_Str);
		try {
			statement = conn.prepareStatement(Query_Str);
			resultSet = statement.executeQuery();

			while (resultSet.next()) {
				++count;
				if (count == 1)
					System.out.print("1 \t  " + resultSet.getInt("energylevel"));
				else if (count == device)
					System.out.print("\t  " + resultSet.getInt("energylevel") + " ; \n");
				else
					System.out.print("\t  " + resultSet.getInt("energylevel") + "  \t  ");
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (resultSet != null)
					resultSet.close();
				if (statement != null)
					statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}

}
