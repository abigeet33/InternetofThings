
package com.amazonaws.controller;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.amazonaws.bean.ServiceReq;
import com.amazonaws.service.RequestService;



@JsonIgnoreProperties(ignoreUnknown = true)
@Path("/iotservices")
public class RequestController {

	RequestService requestService = new RequestService();

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List getServiceReq() {
		List listReq = null;
		try{
		listReq=requestService.getAllServiceReqs();
		System.out.println("in list");
		}catch(Exception e){
			e.printStackTrace();
		}
		return listReq;
		
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public ServiceReq getRequestById(@PathParam("id") int id) {
		return requestService.getRequest(id);
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public ServiceReq addRequest(ServiceReq request) {
		return requestService.addRequest(request);
	}


	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public void deleteRequest(@PathParam("id") int id) {
		requestService.deleteRequest(id);

	}

}

