<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="com.amazonaws.utils.Utility"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; ;charset=UTF-8">
<title>IoT Service Response</title>
</head>
<body>
	<p align="left">
		<%
			Utility utility = new Utility();
		%>
		<%=utility.getCurrentTimeStamp(0)%><br>
		<a href="index.jsp">Home</a>
	</p>
	<p align="right">
		<a href="javascript:close();">Exit</a>
	</p>
	<h2 align="center">IoT Service Matching Framework</h2>
	<form id="myForm3" name="form3">
		<h3 align="center">Processing Requests</h3>
		<br>
		<center>${deviceResponse}</center>
		<br>
		<h3 align="center">To be Processed Requests</h3>
		<br>
		<center>${tobeProcessedResponse}</center>
	</form>
</body>
</html>
