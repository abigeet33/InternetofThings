package com.amazonaws.bean;

import java.util.List;

public class ServiceReq {
	
	int requestId;
	private int requestType;
	private List<String> requestList;//R1-->[s1,s2]
	private int latitude;
	private int longitude;
	private int radius;
	private int completionTime;

	public ServiceReq() {
		super();
	}

	public ServiceReq(int uniqueID,String userId,int requestType,List<String> requestList,
			int latitude,int longitude,int radius,int completionTime) {
		super();
		this.requestId = uniqueID;
		this.requestType=requestType;
		this.requestList = requestList;
		this.latitude=latitude;
		this.longitude=longitude;
		this.radius=radius;
		this.completionTime=completionTime;
	}

	public int getRequestId() {
		return requestId;
	}

	public void setRequestId(int requestId) {
		this.requestId = requestId;
	}

	public int getRequestType() {
		return requestType;
	}

	public void setRequestType(int requestType) {
		this.requestType = requestType;
	}

	public List<String> getRequestList() {
		return requestList;
	}

	public void setRequestList(List<String> requestList) {
		this.requestList = requestList;
	}

	public int getRadius() {
		return radius;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}

	public int getLatitude() {
		return latitude;
	}

	public void setLatitude(int latitude) {
		this.latitude = latitude;
	}

	public int getLongitude() {
		return longitude;
	}

	public void setLongitude(int longitude) {
		this.longitude = longitude;
	}

	public int getCompletionTime() {
		return completionTime;
	}

	public void setCompletionTime(int completionTime) {
		this.completionTime = completionTime;
	}
	
}
