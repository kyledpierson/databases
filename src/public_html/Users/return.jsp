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

	Return a book:<BR><BR>
	<form name="return" method=get action="return.jsp">
		<input type=hidden name="searchAttribute" value="return">
		<table id="no_border">
		<tr><td><label for "1">Username:&nbsp;</label></td><td><input type=text class="attributeValue" name="1" length=20 required></td></tr>
		<tr><td><label for "2">Book Title or ISBN:&nbsp;</label></td><td><input type=text class="attributeValue" name="2" length=20 required></td></tr>
		</table><BR>
		<input type=submit>
	</form>
	<BR><BR>

<%

}
else
{
	String user = request.getParameter("1");
	String book = request.getParameter("2");
	
	cs5530.Connector connector = new Connector();
	cs5530.Order order = new Order();
%>  

  <p><b>Return results:</b><BR><BR>

  <%=order.return_book(user, book, connector)%><BR><BR>

<%
 connector.closeStatement();
 connector.closeConnection();
}
%>

<BR><a href="../index.html"><button>Homepage</button></a></p>

</body>
