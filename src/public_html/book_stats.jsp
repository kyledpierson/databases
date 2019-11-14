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

	View Book Statistics:<BR><BR>
	<form name="book_stats" method=get action="book_stats.jsp">
		<input type=hidden name="searchAttribute" value="book_stats">
		Types:<BR>
		<INPUT TYPE="radio" NAME="stats" VALUE="0" CHECKED>
        Most Checked-Out
        <BR>
        <INPUT TYPE="radio" NAME="stats" VALUE="1">
		Most Requested
        <BR>	
		<INPUT TYPE="radio" NAME="stats" VALUE="2">
        Most Read Author
        <BR>
        <INPUT TYPE="radio" NAME="stats" VALUE="3">
		Most Lost Book
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
	cs5530.Connector connector = new Connector();
	
	try
	{
		int num = Integer.parseInt(request.getParameter("1"));
	
		cs5530.Order order = new Order();
%>  

  <p><b>Book Statistics results:</b><BR><BR>

  <% 	if(method.equals("0"))
		{
			out.print(order.checkStats(num, connector));
		}
		if(method.equals("1"))
		{
			out.print(order.reqStats(num, connector));
		}
		if(method.equals("2"))
		{
			out.print(order.authStats(num, connector));
		}
		if(method.equals("3"))
		{
			out.print(order.lostStats(num, connector));
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
