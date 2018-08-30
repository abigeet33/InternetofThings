package com.amazonaws.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.amazonaws.bean.ServiceReq;


/*
 * It is just a helper class which should be replaced by database implementation.
 * It is not very well written class, it is just used for demonstration.
 */
public class RequestService {

	static HashMap<Integer, ServiceReq> ReqMap = getReqMap();

	public RequestService() {
		super();		
		
		if (ReqMap == null) {
			ReqMap = new HashMap<Integer, ServiceReq>();
		}
	}

	public List getAllServiceReqs() {
		List servicereqs = new ArrayList(ReqMap.values());
		return servicereqs;
		
	}

	public ServiceReq getRequest(int id) {
		ServiceReq request = ReqMap.get(id);
		return request;
	}

	public ServiceReq addRequest(ServiceReq request) {
		ReqMap.put(request.getRequestId(), request);
		
		return request;
	}

	public void deleteRequest(int id) {
		ReqMap.remove(id);
	}

	public static HashMap<Integer, ServiceReq> getReqMap() {
		return ReqMap;
	}
	
}
