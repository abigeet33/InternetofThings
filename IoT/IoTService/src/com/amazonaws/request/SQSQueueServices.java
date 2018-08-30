package com.amazonaws.request;

import java.util.HashMap;
import java.util.Map;


import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.SQSActions;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;
import com.amazonaws.utils.Constants;

public class SQSQueueServices {

	static int counterReq=0;

	static String myQueueUrl = null;

	public AmazonSQS createServiceQueue() {

		AWSCredentials credentials = null;

		try {
			System.out.println("Before Credentials");
			credentials = new BasicAWSCredentials(Constants.AWS_ACCESS_KEY_ID, Constants.AWS_SECRET_ACCESS_KEY);
		} catch (Exception e) {
			System.out.println("Cannot load the credentials from the credential profiles file. "
					+ "Please make sure that your credentials file is at the correct "
					+ "location (~/.aws/credentials), and is in valid format.");
			System.out.println(e.getStackTrace().toString());
			System.out.println(e.getMessage());
		}

		AmazonSQS sqs = new AmazonSQSClient(credentials);
		Region usWest2 = Region.getRegion(Regions.US_WEST_2);
		sqs.setRegion(usWest2);

		try {
			// Create a queue
			System.out.println("Creating a new SQS queue called ServiceQueue.\n");

			// TODO check how to generate url
			Policy policy = new Policy().withId(Constants.ARN_SQS + Constants.REQ_QUEUE + Constants.SQSDefaultPolicy)
					.withStatements(
							new Statement(Effect.Allow).withId("Sid1494892084085").withPrincipals(Principal.AllUsers)
									.withActions(SQSActions.SendMessage, SQSActions.ReceiveMessage,
											SQSActions.AllSQSActions)
									.withResources(new Resource(Constants.ARN_SQS + Constants.REQ_QUEUE)));
				
			Map queueAttributes = new HashMap();

			Map<String, String> attributes = new HashMap<String, String>();

			// A FIFO queue must have the FifoQueue attribute set to True
			attributes.put(Constants.FIFO_QUEUE, Constants.TRUE);

			// Generate a MessageDeduplicationId based on the content, if the
			// user doesn't provide a MessageDeduplicationId
			attributes.put(Constants.CONTENT_BASED_DEDUPLICATION, Constants.TRUE);

			queueAttributes.put(QueueAttributeName.Policy.toString(), policy.toJson());

			CreateQueueRequest createQueueRequest = new CreateQueueRequest(Constants.REQ_QUEUE)
					.withAttributes(attributes).addAttributesEntry("ReceiveMessageWaitTimeSeconds", "20");
			myQueueUrl = sqs.createQueue(createQueueRequest).getQueueUrl();
			System.out.println("Queue is getting created");
			sqs.setQueueAttributes(new SetQueueAttributesRequest(myQueueUrl, queueAttributes));
			System.out.println("all attributes are set");

		}

		catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which means your request made it "
					+ "to Amazon SQS, but was rejected with an error response for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		}

		catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means the client encountered "
					+ "a serious internal problem while trying to communicate with SQS, such as not "
					+ "being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}

		return sqs;
	}

	public void sendMsgtoQueue(String sendMsg, AmazonSQS sqs) {

		counterReq++;
		// Send a message
		System.out.println("Sending a message to ServiceQueue for Service Operation.\n");
		System.out.println("QueueUrl for sendMsgforServiceOperation-->" + myQueueUrl);
		SendMessageRequest sendMessageRequest = new SendMessageRequest(myQueueUrl, sendMsg);
		sendMessageRequest.setMessageGroupId("messageGroup1");
		// Uncomment the following to provide the MessageDeduplicationId
		sendMessageRequest.setMessageDeduplicationId(Integer.toString(counterReq));
		SendMessageResult sendMessageResult = sqs.sendMessage(sendMessageRequest);
		String sequenceNumber = sendMessageResult.getSequenceNumber();
		String messageId = sendMessageResult.getMessageId();
		System.out.println(
				"SendMessage succeed with messageId " + messageId + ", sequence number " + sequenceNumber + "\n");

	}

	public void deleleServiceQueue(AmazonSQS sqs) {

		// Delete a queue
		System.out.println("Deleting the ServiceQueue.\n");
		sqs.deleteQueue(new DeleteQueueRequest(myQueueUrl));
	}
	
	public AmazonSQS sendMessageToServiceQueue(String postJsonData) {

		AWSCredentials credentials = null;

		try {
			System.out.println("Before Credentials");
			credentials = new BasicAWSCredentials(Constants.AWS_ACCESS_KEY_ID, Constants.AWS_SECRET_ACCESS_KEY);
		} catch (Exception e) {
			System.out.println("Cannot load the credentials from the credential profiles file. "
					+ "Please make sure that your credentials file is at the correct "
					+ "location (~/.aws/credentials), and is in valid format.");
			System.out.println(e.getStackTrace().toString());
			System.out.println(e.getMessage());
		}

		AmazonSQS sqs = new AmazonSQSClient(credentials);
		Region usWest2 = Region.getRegion(Regions.US_WEST_2);
		sqs.setRegion(usWest2);

		
		try {
			String queue_url = sqs.getQueueUrl(Constants.REQ_QUEUE).getQueueUrl();
			SendMessageRequest send_msg_request=null;
			send_msg_request = new SendMessageRequest()
			        .withQueueUrl(queue_url)
			        .withMessageBody(postJsonData);
			send_msg_request.setMessageGroupId("theMessageGroup");//to maintain uniqueness of message
			sqs.sendMessage(send_msg_request);

		}

		catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which means your request made it "
					+ "to Amazon SQS, but was rejected with an error response for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		}

		catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means the client encountered "
					+ "a serious internal problem while trying to communicate with SQS, such as not "
					+ "being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}

		return sqs;
	}



}
