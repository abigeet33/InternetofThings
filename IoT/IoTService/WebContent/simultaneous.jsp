<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page import="com.amazonaws.utils.Utility"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>IoT Service Matching Framework</title>
</head>
<body>
	<p align="right">
		<%-- <%
			Utility utility = new Utility();
		%>
		<%=utility.getCurrentTimeStamp(0) %> --%>
		<br> <a href="index.jsp">Home</a><br> <a
			href="javascript:close();">Exit</a>
	<center>
		<h2>IoT Service Matching Framework</h2>
		<br>
		<h3>Simultaneous Services</h3>
		<br />
		<form id="myForm1" name="simform" action="SubmitRequest">
			<table>
				<tr>
					<td>Enter the Batch Requests</td>
					<td></td>
				</tr>
				<tr>
					<td></td>
					<td><textarea name="allrequests" rows="10" cols="100"></textarea>
					</td>
				</tr>
				<tr>
					<td></td>
					<td><input type="submit" style="width: 100px" value="Simsubmit"
						name="cmd" id="reqButton" /> <input type="button" value="cancel"
						name="cmd" style="width: 100px" onClick="this.form.reset()" /></td>
				</tr>
			</table>
			<br> <br>
		</form>
	</center>
</body>
</html>