package com.amazonaws.service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.amazonaws.bean.ServiceReq;

public class RestService {
	
	private final String USER_AGENT = "Mozilla/5.0";

	String urlString = "http://iotscheduler.wtkj2cpjzs.us-west-2.elasticbeanstalk.com/rest/iotservices";
		
	public List<ServiceReq> sendingGetRequest() throws Exception {
		
		List<ServiceReq> serviceReqList = new ArrayList<ServiceReq>();

		
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();

		// By default it is GET request
		con.setRequestMethod("GET");

		// add request header
		con.setRequestProperty("User-Agent", USER_AGENT);

		int responseCode = con.getResponseCode();
		System.out.println("Sending get request : " + url);
		System.out.println("Response code : " + responseCode);

		// Reading response from input Stream
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String output;
		StringBuffer response = new StringBuffer();

		while ((output = in.readLine()) != null) {
			response.append(output);
		}
		in.close();

		// printing result from response
		System.out.println(response.toString());

		ObjectMapper objectMapper = new ObjectMapper();
		serviceReqList = objectMapper.readValue(response.toString(), new TypeReference<List<ServiceReq>>() {
		});
		System.out.println("size-->" + serviceReqList.size());

		return serviceReqList;
	}

	// HTTP Post request
	public void sendingPostRequest(String postJsonData) throws Exception {

		URL obj = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// Setting basic post request
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		con.setRequestProperty("Content-Type", "application/json");

		// Send post request
		con.setDoOutput(true);

		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(postJsonData);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		System.out.println("nSending 'POST' request to URL : " + urlString);
		System.out.println("Post Data : " + postJsonData);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String output;
		StringBuffer response = new StringBuffer();

		while ((output = in.readLine()) != null) {
			response.append(output);
		}
		in.close();

		// printing result from response
		System.out.println(response.toString());
	}
	
	public void sendingDeleteRequest(List<String> reqIds) throws Exception {

		int postId = 0;
		for (String requestId: reqIds) {
			postId=Integer.parseInt(requestId.substring(1))-1;
			String urlStr = urlString+"/"+ postId;

			URL url = new URL(urlStr);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setDoOutput(true);
			// By default it is DELETE request
			con.setRequestMethod("DELETE");

			// add request header
			con.setRequestProperty("User-Agent", USER_AGENT);

			int responseCode = con.getResponseCode();
			System.out.println("Sending delete request : " + url);
			System.out.println("Response code : " + responseCode);
		}

	}

}
