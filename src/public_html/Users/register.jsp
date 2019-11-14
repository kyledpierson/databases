<%@ page language="java" import="cs5530.*" %>
<html>
<head>
<link href="../style.css" rel="stylesheet" type="text/css">
</head>
<body>

<%
String searchAttribute = request.getParameter("searchAttribute");
if(searchAttribute == null)
{
%>

	Register a new user:<BR><BR>
	<form name="register" method=get onsubmit="return check_all_fields(this)" action="register.jsp">
		<input type=hidden name="searchAttribute" value="register">
		<table id="no_border">
		<tr><td><label for "1">Username:&nbsp;</label></td><td><input type=text class="attributeValue" name="1" length=20 required></td></tr>
		<tr><td><label for "2">Full Name:&nbsp;</label></td><td><input type=text class="attributeValue" name="2" length=20 required></td></tr>
		<tr><td><label for "3">Address:&nbsp;</label></td><td><input type=text class="attributeValue" name="3" length=20 required></td></tr>
		<tr><td><label for "4">City:&nbsp;</label></td><td><input type=text class="attributeValue" name="4" length=20 required></td></tr>
		<tr><td><label for "5">State:&nbsp;</label></td><td><input type=text class="attributeValue" name="5" length=20 required></td></tr>
		<tr><td><label for "6">Zip:&nbsp;</label></td><td><input type=text class="attributeValue" name="6" length=20 required></td></tr>
		<tr><td><label for "7">Phone:&nbsp;</label></td><td><input type=text class="attributeValue" name="7" length=20 required></td></tr>
		<tr><td>	<label for "8">Email:&nbsp;</label></td><td><input type=text class="attributeValue" name="8" length=20 required></td></tr>
		</table><BR>
		<input type=submit>
	</form>
	<BR><BR>

<%

}
else
{
	String[] info = new String[8];

	int i = 1;	
	String attributeValue = request.getParameter("" + i);
	while(i < 9)
	{
		if(attributeValue == null)
			attributeValue = "";
		
		info[i - 1] = attributeValue;
		
		i++;
		attributeValue = request.getParameter("" + i);
	}
	
	cs5530.Connector connector = new Connector();
	cs5530.Order order = new Order();
%>  

  <p><b>Registration results:</b><BR><BR>

  <%=order.register(info, connector)%><BR><BR>

<%
 connector.closeStatement();
 connector.closeConnection();
}
%>

<BR><a href="../index.html"><button>Homepage</button></a></p>

</body>
