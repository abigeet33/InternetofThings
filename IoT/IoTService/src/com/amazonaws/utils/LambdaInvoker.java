package com.amazonaws.utils;

import com.amazonaws.services.lambda.AWSLambdaAsyncClient;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.regions.Regions;

public class LambdaInvoker {


	public void runLambdaFunction(String functionName) {
		try {

			//Invoking Lambda Function
			AWSLambdaAsyncClient client = new AWSLambdaAsyncClient();
			client.withRegion(Regions.US_WEST_2);
			InvokeRequest request = new InvokeRequest();
			request.withFunctionName(functionName);
			InvokeResult invoke = client.invoke(request);
			System.out.println("Result invoking " + functionName + ": " + invoke);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

}
