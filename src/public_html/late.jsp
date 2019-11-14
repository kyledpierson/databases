<%@ page language="java" import="cs5530.*" %>
<html>
<head>
<link href="style.css" rel="stylesheet" type="text/css">
</head>
<body>

<%
String searchAttribute = request.getParameter("searchAttribute");
if(searchAttribute == null)
{
%>

	Get a list of all late books for a certain date:<BR><BR>
	<form name="late" method=get onsubmit="return check_all_fields(this)" action="late.jsp">
		<input type=hidden name="searchAttribute" value="late">
		Please input the date in the format of mm/dd/yyyy
		<table id="no_border">
		<tr><td><label for "1">Date:&nbsp;</label></td><td><input type=text class="attributeValue" name="1" length=20 required></td></tr>
		</table><BR>
		<input type=submit>
	</form>
	<BR><BR>

<%

}
else
{
	String sdate = request.getParameter("1");
	
	cs5530.Connector connector = new Connector();
	cs5530.Order order = new Order();
%>  

  <p><b>Late book status:</b><BR><BR>

  <%=order.getLate(sdate, connector)%><BR><BR>

<%
 connector.closeStatement();
 connector.closeConnection();
}
%>

<BR><a href="index.html"><button>Homepage</button></a></p>

</body>