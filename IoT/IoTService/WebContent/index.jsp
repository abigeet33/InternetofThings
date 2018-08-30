<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
	<%@ page import="com.amazonaws.utils.Utility"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<body>
	<center>
		<br>
	<p align="left">
		<%
			Utility utility = new Utility();
		%>
		<%=utility.getCurrentTimeStamp(0)%><br>
		<a href="index.jsp">Home</a>
	</p>
		<p align="right">
		<a href="javascript:close();">Exit</a>
		<h2>IoT Service Matching Framework</h2>
		<br> <br>
		<table style="margin: 0 auto;">
			<tr></tr>
			<tr>
				<td><a href="sequential.jsp">Sequential Services</a></td>
			</tr>
			<tr></tr>
			<tr>
				<td><a href="simultaneous.jsp">Simultaneous Services</a></td>
			</tr>
			<tr></tr>
			<tr>
				<td><a href="ServiceRequestsData.jsp">Check Service Requests Status</a></td>
			</tr>
		</table>
	</center>
</body>
</html>