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

	View User Statistics:<BR><BR>
	<form name="user_stats" method=get action="user_stats.jsp">
		<input type=hidden name="searchAttribute" value="user_stats">
		Types:<BR>
		<INPUT TYPE="radio" NAME="stats" VALUE="0" CHECKED>
        Users who check out the most books
        <BR>
        <INPUT TYPE="radio" NAME="stats" VALUE="1">
		Users who review the most books
        <BR>	
		<INPUT TYPE="radio" NAME="stats" VALUE="2">
        Users who lose the most books
        <BR>
		<table id="no_border">
		<tr><td><label for "1">Number to return:&nbsp;</label></td><td><input type=text class="attributeValue" name="1" length=20 required></td></tr>
		</table><BR>
		<input type=submit>
	</form>
	<BR><BR>

<%

}
else
{
	String method = request.getParameter("stats");
	int num = 0;
	cs5530.Connector connector = new Connector();
	
	try
	{
		num = Integer.parseInt(request.getParameter("1"));
	
		cs5530.Order order = new Order();
%>  

  <p><b>User Statistics results:</b><BR><BR>

  <% 	if(method.equals("0"))
		{
			out.print(order.userCheckStats(num, connector));
		}
		if(method.equals("1"))
		{
			out.print(order.userRevStats(num, connector));
		}
		if(method.equals("2"))
		{
			out.print(order.userLoseStats(num, connector));
		}
  %><BR><BR>

<%
	}
	catch(Exception e)
	{
		out.print("An error occured: " + e.getMessage());
	}
	finally
	{
		connector.closeStatement();
		connector.closeConnection();		
	}
}
%>

<BR><a href="index.html"><button>Homepage</button></a></p>

</body>
