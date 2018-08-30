<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page import="com.amazonaws.utils.Utility"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>IoT Service Matching Framework</title>
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
		<a href="index.jsp">Home</a><br> <a href="javascript:close();">Exit</a>
	<center>
		<h2>IoT Service Matching Framework</h2>
		<br>
		<h3>Sequential Services</h3>
		<br />
		<form id="myForm1" name="seqform" action="SubmitRequest">
			<table>
				<tr>
					<td>Request List</td>
					<td><input type="text" name="requestList" id="id2"
						style="width: 120px" /></td>

				</tr>
				<tr>
					<td>Request Type</td>
					<td><input type="text" name="requestType" id="id3"
						style="width: 120px" /></td>

				</tr>
				<tr>
					<td>Requested location-latitude</td>
					<td><input type="text" name="latitude" id="id4"
						style="width: 120px" /></td>
				</tr>
				<tr>
					<td>Requested location-longitude</td>
					<td><input type="text" name="longitude" id="id5"
						style="width: 120px" /></td>
				</tr>
				<tr>
					<td>Radius</td>
					<td><input type="text" name="radius" id="id6"
						style="width: 120px" /></td>
				</tr>
				<tr>
					<td>Expected Completion Time</td>
					<td><input type="text" name="completionTime" id="id7"
						style="width: 120px" /></td>
				</tr>
				<tr>
					<td></td>
					<td><input type="submit" style="width: 100px" value="Submit"
						name="cmd" id="reqButton" /> <input type="button" value="Cancel"
						name="cmd" style="width: 100px" onClick="this.form.reset()" /></td>
				</tr>
			</table>
			<br> <br>
		</form>
	</center>
</body>
</html>