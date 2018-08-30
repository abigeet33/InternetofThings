package com.amazonaws.corejava;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.amazonaws.bean.ServiceReq;
import com.amazonaws.dao.NonLinearSolvers;
import com.amazonaws.utils.LambdaInvoker;
import com.amazonaws.request.SQSQueueServices;
import com.amazonaws.service.RestService;
import com.amazonaws.utils.Constants;
import com.amazonaws.utils.Utility;

/**
 * Servlet implementation class HelloWorld
 */
@WebServlet("/SubmitRequest")
public class SubmitRequest extends HttpServlet {

	private static final long serialVersionUID = 1L;
	HttpSession sess = null;
	static int serviceReqId = 0;
	SQSQueueServices services = null;
	private ServletContext servletContext;
	private String rootPath;
	static List<ServiceReq> serviceReqList = new ArrayList<ServiceReq>();

	static List<ServiceReq> tobeProcessedReqs = null;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public SubmitRequest() {
		super();
	}

	public void init(ServletConfig config) throws ServletException {
		System.out.println("Servlet Instantiated");
		servletContext = config.getServletContext();
		rootPath = servletContext.getRealPath("/");
		System.out.println("root path--->" + rootPath);
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		System.out.println("inside doGet method");
		// start time of request
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		String currentTime = dateFormat.format(new Date());
		System.out.println("start time of request-->" + currentTime);
		String deviceResponse = null;

		Connection conn = null;

		// get an instance of DB connection
		conn = new Utility().getDBConnection();
		String postJsonData = null;
		try {
			if (request.getParameter("cmd").equals("Submit")) {
				response.sendRedirect("Result.jsp");
				ServiceReq serviceReq = new ServiceReq();

				// Initilaizing Service Request Parameter
				postJsonData = validateUserRequest(request, serviceReq);
				System.out.println("validated service request");

				new SQSQueueServices().sendMessageToServiceQueue(postJsonData);

				// inserting data to service requests table for queued status
				new Utility()
						.insertDataToServiceRequests(conn, "R" + serviceReqId,
								(postJsonData.substring(postJsonData.indexOf('[') + 1, postJsonData.indexOf(']')))
										.replaceAll("\"", "'"),
								null, Constants.Queue_Status_IN_QUEUE, 0, postJsonData, 0);

				System.out.println("before deleteProcessedRequests");
				// deleting processed requests
				List<String> deleteRequestIdLst = new Utility().deleteProcessedRequests(conn);

				if (deleteRequestIdLst.size() > 0)
					new RestService().sendingDeleteRequest(deleteRequestIdLst);

				System.out.println("before updateRunningRequests");
				// update running status
				new Utility().updateRunningRequests(conn);

				System.out.println("after updateRunningRequests");

				/*
				 * // Invoking Lambda Function new
				 * LambdaInvoker().runLambdaFunction(Constants.LAMBDA_FUNC1);
				 */
				serviceReqList = new RestService().sendingGetRequest();
				if (serviceReqList.size() > 0) {
					System.out.println("after calling get request");
					deviceResponse = new SubmitRequest().categorizeRequest(conn, serviceReqList, postJsonData);
				}

				sess = request.getSession();
				if (deviceResponse != null) {
					if (deviceResponse == Constants.SUCCESS)
						sess.setAttribute("deviceResponse", "Request is being processed!!");
					else if (deviceResponse == Constants.FAILURE)
						sess.setAttribute("deviceResponse", "Request is not processed!!");
				} else
					sess.setAttribute("deviceResponse", "No Request to process");

				/*
				 * } else if (request.getParameter("cmd").equals("Simsubmit")) {
				 * String finalJsonData = ""; postJsonData = ""; String textarea
				 * = request.getParameter("allrequests");
				 * System.out.println(textarea); deviceResponse = "";
				 * tobeProcessedResponse = "";
				 * response.sendRedirect("SimResult.jsp"); String
				 * batch_requests[] = textarea.split("#"); for (String req :
				 * batch_requests) { ServiceReq serviceReq = new ServiceReq();
				 * List<String> serviceNameLst = new ArrayList<String>(); String
				 * reqFields[] = req.split(":");
				 * serviceReq.setRequestId(serviceReqId++); String[]
				 * serviceNames = reqFields[0].split(","); for (String sName :
				 * serviceNames) serviceNameLst.add(sName);
				 * serviceReq.setRequestList(serviceNameLst);
				 * serviceReq.setRequestType(Integer.parseInt(reqFields[1]));
				 * serviceReq.setLatitude(Integer.parseInt(reqFields[2]));
				 * serviceReq.setLongitude(Integer.parseInt(reqFields[3]));
				 * serviceReq.setRadius(Integer.parseInt(reqFields[4]));
				 * postJsonData = new
				 * Utility().createJSONMessageReq(serviceReq); new
				 * SQSQueueServices().sendMessageToServiceQueue(postJsonData);
				 * 
				 * //inserting data to service requests table for queued status
				 * new Utility().insertDataToServiceRequests(conn,
				 * "R"+serviceReqId,
				 * (postJsonData.substring(postJsonData.indexOf('[')+1,
				 * postJsonData.indexOf(']'))),
				 * null,Constants.Queue_Status_IN_QUEUE, 0, postJsonData,0);
				 * 
				 * } //deleting request that are going to run List<String>
				 * deleteRequestIdLst=new
				 * Utility().deleteProcessedRequests(conn);
				 * if(deleteRequestIdLst.size()>0) new
				 * RestService().sendingDeleteRequest(deleteRequestIdLst);
				 * 
				 * serviceReqList = new RestService().sendingGetRequest();
				 * if(serviceReqList.size()>0){
				 * System.out.println("after calling get request"); new
				 * SubmitRequest().categorizeRequest(conn, serviceReqList,
				 * postJsonData);
				 * 
				 * 
				 * }
				 * 
				 * tobeProcessedReqs = new ArrayList<ServiceReq>(); respList =
				 * new SubmitRequest().categorizeRequest(conn, serviceReqList,
				 * postJsonData);
				 * 
				 * System.out.println("respList size-->" + respList.size());
				 * 
				 * for (int j = 0; j < respList.size(); j++) {
				 * 
				 * new SQSQueueResponse().sendMsgtoQueue(respList.get(j), sqs2);
				 * new
				 * LambdaInvoker().runLambdaFunction(Constants.LAMBDA_FUNC2); }
				 * 
				 * // process the unprocessed service reqs by adding it to sqs
				 * queue new RestService().sendingDeleteRequest(serviceReqList);
				 * 
				 * new SubmitRequest().processFailedRequest(sqs1,
				 * tobeProcessedReqs); for (String resp : respList)
				 * deviceResponse += resp + "<br>";
				 * 
				 * for (ServiceReq resp1 : tobeProcessedReqs)
				 * tobeProcessedResponse += resp1.getRequestId() + "<br>";
				 * 
				 * sess = request.getSession(); if (deviceResponse != null)
				 * sess.setAttribute("deviceResponse", deviceResponse);
				 * 
				 * if (tobeProcessedResponse != null)
				 * sess.setAttribute("tobeProcessedResponse",
				 * tobeProcessedResponse);
				 * 
				 * System.out.println("deviceResponse-->" + deviceResponse);
				 * deviceResponse = ""; tobeProcessedResponse = "";
				 */
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		System.out.println("Posted");
	}

	public void destroy() {

	}

	public String categorizeRequest(Connection conn, List<ServiceReq> serviceReqList, String postJsonData) {
		System.out.println("inside categorizeRequest");
		List<ServiceReq> requestType1 = new ArrayList<ServiceReq>();
		String response = null;
		List<ServiceReq> requestType2 = new ArrayList<ServiceReq>();
		// String output = null;
		List<ServiceReq> modifiedList = new NonLinearSolvers().modifyList(serviceReqList);

		for (int i = 0; i < modifiedList.size(); i++) {
			if (modifiedList.get(i).getRequestType() == Constants.SCENARIO_1)
				requestType1.add(modifiedList.get(i));
			else if (modifiedList.get(i).getRequestType() == Constants.SCENARIO_2)
				requestType2.add(modifiedList.get(i));
		}
		System.out.println("size of request-->" + requestType1.size() + "----" + requestType2.size());
		if (requestType1.size() > 0)
			response = new NonLinearSolvers().nonLinearSolvers(conn, requestType1, Constants.SCENARIO_1);
		if (requestType2.size() > 0)
			response = new NonLinearSolvers().nonLinearSolvers(conn, requestType2, Constants.SCENARIO_2);

		System.out.println("exiting categorizeRequest");

		return response;

	}

	/*
	 * public void addFailedRequesttoQueue(ServiceReq tobeProcessedReq) {
	 * tobeProcessedReqs.add(tobeProcessedReq);
	 * System.out.println("to be processed size-->" + tobeProcessedReqs.size());
	 * }
	 * 
	 * public void processFailedRequest(AmazonSQS sqs1, List<ServiceReq>
	 * tobeProcessedReqs) { String postJsonData = null;
	 * System.out.println("Inside processFailedRequest " +
	 * tobeProcessedReqs.size()); try { for (ServiceReq serviceReq :
	 * tobeProcessedReqs) { System.out.println("Service Request Name--->" +
	 * serviceReq.getRequestList().get(0)); postJsonData = new
	 * Utility().createJSONMessageReq(serviceReq);
	 * System.out.println("postJsonData -end--" + postJsonData); new
	 * SQSQueueServices().sendMsgtoQueue(postJsonData, sqs1); new
	 * LambdaInvoker().runLambdaFunction(Constants.LAMBDA_FUNC1); } } catch
	 * (Exception e) { System.out.println(e.getMessage()); }
	 * 
	 * }
	 */
	public String validateUserRequest(HttpServletRequest request, ServiceReq serviceReq) {

		List<String> serviceNameLst = new ArrayList<String>();
		String postJsonData = null;
		String[] serviceNames = request.getParameter("requestList").split(",");

		serviceReq.setRequestType(Integer.parseInt(request.getParameter("requestType")));
		serviceReq.setRequestId(serviceReqId++);
		for (String sName : serviceNames)
			serviceNameLst.add(sName);
		serviceReq.setRequestList(serviceNameLst);
		serviceReq.setLatitude(Integer.parseInt(request.getParameter("latitude")));
		serviceReq.setLongitude(Integer.parseInt(request.getParameter("longitude")));
		serviceReq.setRadius(Integer.parseInt(request.getParameter("radius")));
		serviceReq.setCompletionTime(Integer.parseInt(request.getParameter("completionTime")));

		postJsonData = new Utility().createJSONMessageReq(serviceReq);

		return postJsonData;

	}
}
