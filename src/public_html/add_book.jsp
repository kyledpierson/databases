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

	Add a new book to the library:<BR><BR>
	<form name="add_book" method=get onsubmit="return check_all_fields(this)" action="add_book.jsp">
		<input type=hidden name="searchAttribute" value="add_book">
		<table id="no_border">
		<tr><td><label for "1">ISBN:&nbsp;</label></td><td><input type=text class="attributeValue" name="1" length=20 required></td></tr>
		<tr><td><label for "2">Title:&nbsp;</label></td><td><input type=text class="attributeValue" name="2" length=20 required></td></tr>
		<tr><td><label for "3">Publisher:&nbsp;</label></td><td><input type=text class="attributeValue" name="3" length=20 required></td></tr>
		<tr><td><label for "4">Year:&nbsp;</label></td><td><input type=text class="attributeValue" name="4" length=20 required></td></tr>
		<tr><td><label for "5">Subject:&nbsp;</label></td><td><input type=text class="attributeValue" name="5" length=20 required></td></tr>
		<tr><td><label for "6">Summary:&nbsp;</label></td><td><textarea cols='20' rows='3' class="attributeValue" name="6" required></textarea></td></tr>
		<tr><td><label for "7">Format:&nbsp;</label></td><td><input type=text class="attributeValue" name="7" length=20 required></td></tr>
		<tr><td><label for "8">Author(s) (separated by ':'):&nbsp;</label></td><td><textarea cols='20' rows='3' class="attributeValue" name="8" required></textarea></td></tr>
		</table><BR>
		<input type=submit>
	</form>
	<BR><BR>

<%

}
else
{
	String[] info = new String[7];

	int i = 1;	
	String attributeValue = request.getParameter("" + i);
	while(attributeValue != null && i < 8)
	{
		info[i - 1] = attributeValue;
		
		i++;
		attributeValue = request.getParameter("" + i);
	}
	
	String authorString = request.getParameter("8");
	String[] authors = authorString.split(":");
	
	cs5530.Connector connector = new Connector();
	cs5530.Order order = new Order();
%>  

  <p><b>Book addition results:</b><BR><BR>

  <%=order.add_book(info, authors, connector)%><BR><BR>

<%
 connector.closeStatement();
 connector.closeConnection();
}
%>

<BR><a href="index.html"><button>Homepage</button></a></p>

</body>
