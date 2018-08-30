<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@page import="java.sql.DriverManager"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.Statement"%>
<%@page import="java.sql.Connection"%>
<%@ page import="com.amazonaws.utils.Utility"%>

<%
	String driverName = "com.mysql.jdbc.Driver";
	try {
		Class.forName(driverName);
	} catch (ClassNotFoundException e) {
		e.printStackTrace();
	}

	Connection connection = null;
	Statement statement = null;
	ResultSet resultSet = null;
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<body>
	<p align="left">
		<%
			Utility utility = new Utility();
		%>
		<%=utility.getCurrentTimeStamp(0)%><br>
	</p>
<p align="right">
<a href="index.jsp">Home</a><br> <a href="javascript:close();">Exit</a>
	<center>
		<br>
		<h2 align="center">
			<strong>IoT Service Matching Framework</strong>
		</h2>
		<table align="center" cellpadding="5" cellspacing="5" border="1">
			<tr bgcolor="#A52A2A">
				<td><b>serviceReqId</b></td>
				<td><b>serviceName</b></td>
				<td><b>deviceId</b></td>
				<td><b>status</b></td>
				<td><b>startTime</b></td>
				<td><b>endTime</b></td>
				<td><b>totalExecutionTime</b></td>
				<td><b>JsonMsg</b></td>
				<td><b>timeDelay</b></td>
			</tr>
			<%
				try {
					connection=DriverManager.getConnection(  
							"jdbc:mysql://iotsm.clwtry0wciq5.us-west-2.rds.amazonaws.com:3306/IoTRequestScheduler","iotscheduler","Qwerty_12");  			
					statement = connection.createStatement();
					String sql = "SELECT * FROM ServiceRequests";

					resultSet = statement.executeQuery(sql);
					while (resultSet.next()) {
			%>
			<tr bgcolor="#DEB887">

				<td><%=resultSet.getString("serviceReqId")%></td>
				<td><%=resultSet.getString("serviceName")%></td>
				<td><%=resultSet.getString("deviceId")%></td>
				<td><%=resultSet.getString("status")%></td>
				<td><%=resultSet.getTimestamp("startTime")%></td>
				<td><%=resultSet.getTimestamp("endTime")%></td>
				<td><%=resultSet.getInt("totalExecutionTime")%></td>
				<td><%=resultSet.getString("JsonMsg")%></td>
				<td><%=resultSet.getInt("timeDelay")%></td>

			</tr>

			<%
				}

				} catch (Exception e) {
					e.printStackTrace();
				}
				finally{
					if (statement != null)
					statement.close();
					if (connection != null)
					connection.close();
				}
			%>
		</table>
	</center>
</body>
</html>


