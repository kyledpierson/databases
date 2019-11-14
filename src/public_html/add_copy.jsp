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

	Add a copies of a book to the library:<BR><BR>
	<form name="add_copy" method=get onsubmit="return check_all_fields(this)" action="add_copy.jsp">
		<input type=hidden name="searchAttribute" value="add_copy">
		<table id="no_border">
		<tr><td><label for "1">ISBN:&nbsp;</label></td><td><input type=text class="attributeValue" name="1" length=20 required></td></tr>
		<tr><td><label for "2">Location:&nbsp;</label></td><td><input type=text class="attributeValue" name="2" length=20 required></td></tr>
		<tr><td><label for "3">Number of copies:&nbsp;</label></td><td><input type=text class="attributeValue" name="3" length=20 required></td></tr>
		</table><BR>
		<input type=submit>
	</form>
	<BR><BR>

<%

}
else
{
	cs5530.Connector connector = new Connector();
	try
	{
	String[] info = new String[2];

	int i = 1;
	String attributeValue = request.getParameter("" + i);
	while(attributeValue != null && i < 3)
	{
		info[i - 1] = attributeValue;
		
		i++;
		attributeValue = request.getParameter("" + i);
	}
	
	int num = Integer.parseInt(request.getParameter("3"));
	
	cs5530.Order order = new Order();
%>  

  <p><b>Book copy addition results:</b><BR><BR>

  <%=order.add_copies(info, num, connector)%><BR><BR>

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
