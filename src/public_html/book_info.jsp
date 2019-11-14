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

	Get information on a book:<BR><BR>
	<form name="book_info" method=get action="book_info.jsp">
		<input type=hidden name="searchAttribute" value="book_info">
		<table id="no_border">
		<tr><td><label for "1">Book title or ISBN:&nbsp;</label></td><td><input type=text class="attributeValue" name="1" length=20 required></td></tr>
		</table><BR>
		<input type=submit>
	</form>
	<BR><BR>

<%

}
else
{
	String book = request.getParameter("1");
	
	cs5530.Connector connector = new Connector();
	cs5530.Order order = new Order();
%>  

  <p><b>Book information results:</b><BR><BR>

  <%=order.book_info(book, connector)%><BR><BR>

<%
 connector.closeStatement();
 connector.closeConnection();
}
%>

<BR><a href="index.html"><button>Homepage</button></a></p>

</body>
