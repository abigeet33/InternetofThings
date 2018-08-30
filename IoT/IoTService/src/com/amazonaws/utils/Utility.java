package com.amazonaws.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Map.Entry;

import org.json.simple.JSONObject;
import com.amazonaws.bean.ServiceReq;

public class Utility {

	static List<String> completedRequestIds = new ArrayList<String>();

	// Function to create JSON Message of the response
	public String createJSONMessageResp(int serviceReqId, String serviceName, String deviceName) {
		System.out.println("createJSONMessageResp");
		// Create JSON message
		JSONObject obj = new JSONObject();
		obj.put(Constants.SERVICE_REQUEST_ID, serviceReqId);
		obj.put(Constants.SERVICE_NAME, serviceName);
		obj.put(Constants.DEVICE, deviceName);
		System.out.println("JSON Message to send to Queue-->" + obj.toString());
		return obj.toString();
	}

	public List<String> deleteProcessedRequests(Connection conn) {

		System.out.println("inside deleteProcessedRequests");
		List<String> requestLst = new ArrayList<>();
		HashMap<String, String> serviceDeviceLst = new HashMap<String, String>();

		PreparedStatement statement1 = null;
		ResultSet resultSet = null;
		String Query_Str1 = "select serviceReqId,serviceName,deviceId from ServiceRequests where endtime<current_timestamp";
		System.out.println(Query_Str1);
		try {

			statement1 = conn.prepareStatement(Query_Str1);
			resultSet = statement1.executeQuery();

			while (resultSet.next()) {
				if (!completedRequestIds.contains(resultSet.getString("serviceReqId"))) {
					requestLst.add(resultSet.getString("serviceReqId"));
					serviceDeviceLst.put(resultSet.getString("deviceId"), resultSet.getString("serviceName"));
					// update user limit -1
					updateUserLimit(conn, serviceDeviceLst, true);
					// updating energy value in device table
					updateDeviceEnergy(conn, serviceDeviceLst);
					serviceDeviceLst.clear();
				}
			}
			completedRequestIds.addAll(requestLst);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("calling updateRequestStatus completed-->" + requestLst.size());
		updateRequestStatus(conn, requestLst, Constants.Queue_Status_COMPLETED);
		System.out.println("exiting deleteProcessedRequests");
		return requestLst;

	}

	public void updateDeviceEnergy(Connection conn, HashMap<String, String> serviceDeviceLst) {

		PreparedStatement statement1 = null;
		ResultSet resultSet = null;

		String device_query = "";
		if (serviceDeviceLst.size() > 0) {
			for (Entry<String, String> entry : serviceDeviceLst.entrySet()) {
				device_query = "select sum(energyConsumption) energy from service where deviceId='" + entry.getKey()
						+ "' and servicename in (" + entry.getValue() + ")";
				System.out.println(device_query);
				try {
					statement1 = conn.prepareStatement(device_query);
					resultSet = statement1.executeQuery();

					if (resultSet.next())
						updateEnergyLevel(conn, entry.getKey(), resultSet.getInt("energy"));
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						if (statement1 != null)
							statement1.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}

			}
		}
	}

	public List<String> updateRunningRequests(Connection conn) {
		System.out.println("inside updateRunningRequests");
		List<String> requestLst = new ArrayList<>();
		Map<String, String> serviceDeviceList = new HashMap<>();
		PreparedStatement statement1 = null;
		ResultSet resultSet = null;
		String serviceName = "";
		String deviceId = "";
		String Query_Str1 = "select serviceReqId,serviceName,deviceId from ServiceRequests where starttime<current_timestamp and endtime>current_timestamp ";
		System.out.println(Query_Str1);
		try {
			statement1 = conn.prepareStatement(Query_Str1);
			resultSet = statement1.executeQuery();

			while (resultSet.next()) {
				requestLst.add(resultSet.getString("serviceReqId"));
				serviceName = resultSet.getString("serviceName");
				deviceId = resultSet.getString("deviceId");
				serviceDeviceList.put(deviceId, serviceName);
				// updating userlimitInUse column of Service table
				updateUserLimit(conn, serviceDeviceList, false);
				serviceDeviceList.clear();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("calling updateRequestStatus running-->" + requestLst.size());
		updateRequestStatus(conn, requestLst, Constants.Queue_Status_RUNNING);
		System.out.println("exiting updateRunningRequests");
		return requestLst;

	}

	public void updateUserLimit(Connection conn, Map<String, String> serviceDeviceList, boolean flag) {
		System.out.println("inside updateUserLimit");
		PreparedStatement statement2 = null;
		String Query_Str2 = "";
		for (Entry<String, String> entry : serviceDeviceList.entrySet()) {
			if (flag == true)
				Query_Str2 = "update service set userLimitInUse= case when userLimitInUse>0 then userLimitInUse=userLimitInUse-1 end where deviceId= '"
						+ entry.getKey() + "' and serviceName in (" + entry.getValue() + ")";
			else
				Query_Str2 = "update service set userLimitInUse=userLimitInUse+1 where deviceId= '" + entry.getKey()
						+ "' and serviceName in (" + entry.getValue() + ")";
			try {
				System.out.println(Query_Str2);
				statement2 = conn.prepareStatement(Query_Str2);
				statement2.executeUpdate();
				System.out.println("exiting updateUserLimit");
				
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					if (statement2 != null)
						statement2.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}

	}

	// Updating the energy level for device after assigning device to service
	// requests
	public void updateRequestStatus(Connection conn, List<String> requestLst, String status) {

		System.out.println("inside updateRequestStatus");
		PreparedStatement statement1 = null;
		String expr = "";
		if (requestLst.size() > 0) {
			if (requestLst.size() == 1)
				expr += "'" + requestLst.get(0) + "'";
			else {
				for (int i = 0; i < requestLst.size(); i++) {
					if (i == 0 || requestLst.size() == i)
						expr += "'" + requestLst.get(i) + "'";
					else
						expr += ",'" + requestLst.get(i) + "'";
				}
			}
			String Query_Str1 = "update ServiceRequests set status = '" + status + "' where serviceReqId in (" + expr + ")";
			try {
				statement1 = conn.prepareStatement(Query_Str1);
				System.out.println(statement1 + "-----" + expr);
				statement1.executeUpdate();
				System.out.println("after updating request status");
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					if (statement1 != null)
						statement1.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("exiting updateRequestStatus");
	}

	// Function to get Database Connection
	public Connection getDBConnection() {
		Connection con = null;
		try {
			System.out.println("inside getDBConnection");

			// connect to database
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection(
					"jdbc:mysql://iotsm.clwtry0wciq5.us-west-2.rds.amazonaws.com:3306/IoTRequestScheduler",
					"iotscheduler", "Qwerty_12");

			System.out.println("database connected");
		} catch (Exception e) {
			System.out.println("Connection Error:" + e.getMessage());
			e.printStackTrace();
		}
		return con;
	}

	// Updating the energy level for device after assigning device to service
	// requests
	public void updateEnergyLevel(Connection conn, String deviceName, int energy) {

		PreparedStatement statement1 = null;
		String Query_Str1 = "update device set energylevel=energylevel-?  where deviceId= ? ";
		try {
			statement1 = conn.prepareStatement(Query_Str1);

			statement1.setInt(1, energy);
			statement1.setString(2, deviceName);
			statement1.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (statement1 != null)
					statement1.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}

	// Function to create JSON Message of the requests

	public String createJSONMessageReq(ServiceReq serviceReq) {
		// Create JSON message
		String expr = "";
		int size = serviceReq.getRequestList().size();
		for (int i = 0; i < size; i++) {
			if ((size == 1) || (i == size - 1))
				expr += "\"" + serviceReq.getRequestList().get(i) + "\"";
			else
				expr += "\"" + serviceReq.getRequestList().get(i) + "\"" + ",";
		}

		System.out.println("expres-->" + expr);
		String jsonMessage = "";
		jsonMessage += "{\"requestId\":" + serviceReq.getRequestId() + "," + "\"requestType\":"
				+ serviceReq.getRequestType() + ",\"requestList\":[" + expr + "],\"latitude\":\""
				+ serviceReq.getLatitude() + "\"," + "\"longitude\":\"" + serviceReq.getLongitude() + "\","
				+ "\"radius\":\"" + serviceReq.getRadius() + "\"," + "\"completionTime\":\""
				+ serviceReq.getCompletionTime() + "\"}";

		System.out.println("JSON Message to send to Queue-->" + jsonMessage);

		return jsonMessage;
	}

	// Logging details to ServiceRequest table
	public void insertDataToServiceRequests(Connection conn, String serviceReqId, String serviceName, String deviceId,
			String status, int totalExecutionTime, String msg, int timeDelay) {

		System.out.println("inside insert Data to ServiceRequests");
		String insertTableSQL = "INSERT INTO ServiceRequests"
				+ "(serviceReqId,serviceName,deviceId,status,totalExecutionTime,JsonMsg,timeDelay) VALUES"
				+ "(?,?,?,?,?,?,?)";
		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = conn.prepareStatement(insertTableSQL);
			preparedStatement.setString(1, serviceReqId);
			preparedStatement.setString(2, serviceName);
			preparedStatement.setString(3, deviceId);
			preparedStatement.setString(4, status);
			preparedStatement.setInt(5, totalExecutionTime);

			preparedStatement.setString(6, msg);
			preparedStatement.setInt(7, timeDelay);
			// execute insert SQL statement
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Insert data to service requests table exception");
		} finally {
			try {
				if (preparedStatement != null)
					preparedStatement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Exiting insert Data to ServiceRequests");

	}

	// Updating status and deviceid in service requests table
	public void updateServiceRequestsTable(Connection conn, String serviceReqId, String deviceName, String status) {
		System.out.println("inside updateServiceRequestsTable-device");

		String Query_Str = null;
		PreparedStatement statement = null;

		Query_Str = "update ServiceRequests set status=?, " + " deviceId=? where serviceReqId=? ";

		try {
			statement = conn.prepareStatement(Query_Str);

			statement.setString(1, status);
			statement.setString(2, deviceName);
			statement.setString(3, serviceReqId);
			statement.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (statement != null)
					statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Exiting updateServiceRequestsTable-device");

	}

	// Updating starttime,endtime and total execution time in service requests
	// table
	public void updateServiceRequestsTable(Connection conn, String serviceReqId, int totalExecutionTime) {
		System.out.println("inside updateServiceRequestsTable-total execution time");

		String Query_Str = null;
		PreparedStatement statement = null;
		Query_Str = "update ServiceRequests set totalExecutionTime=?,"
				+ " startTime=?,endTime=?,timeDelay=? where serviceReqId=? ";

		System.out.println(Query_Str);
		int diff = 0;
		Timestamp starttime;
		PreparedStatement statement1 = null;
		ResultSet resultSet = null;

		String Query_Str1 = "select max(timestampdiff(second,current_timestamp,endtime)) timediff from ServiceRequests where deviceId"
				+ " = (select deviceId from ServiceRequests where serviceReqId='" + serviceReqId + "')";

		System.out.println(Query_Str1);
		try {

			statement1 = conn.prepareStatement(Query_Str1);
			resultSet = statement1.executeQuery();

			while (resultSet.next())
				diff = resultSet.getInt("timediff");
			System.out.println(diff);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			statement = conn.prepareStatement(Query_Str);

			statement.setInt(1, totalExecutionTime);
			if (diff > 0) {
				starttime = getCurrentTimeStamp(diff);
				statement.setTimestamp(3, getCurrentTimeStamp(totalExecutionTime + diff));
				statement.setInt(4, diff);
			} else {
				starttime = getCurrentTimeStamp(0);
				statement.setTimestamp(3, getCurrentTimeStamp(totalExecutionTime));
				statement.setInt(4, 0);
			}
			statement.setTimestamp(2, starttime);

			statement.setString(5, serviceReqId);
			statement.executeUpdate();
			System.out.println(starttime);

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (statement != null)
					statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Exiting updateServiceRequestsTable-total execution time");

	}

	// Function to get currentTimestamp
	public java.sql.Timestamp getCurrentTimeStamp(int serviceTime) {

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		long NowDate = System.currentTimeMillis();
		Timestamp original = new Timestamp(NowDate);
		cal.setTimeInMillis(original.getTime());
		cal.add(Calendar.HOUR_OF_DAY, 0);
		Timestamp startTime = new Timestamp(cal.getTime().getTime());
		cal.add(Calendar.SECOND, serviceTime);
		Timestamp endTime = new Timestamp(cal.getTime().getTime());
		if (serviceTime == 0)
			return startTime;
		else
			return endTime;

	}

}
