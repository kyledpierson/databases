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

	Search by any of the following criteria:<BR><BR>
	<form name="browse" method=get action="browse.jsp">
		<input type=hidden name="searchAttribute" value="browse">
		<table id="no_border">
		<tr><td><label for "1">Author:&nbsp;</label></td><td><input type=text class="attributeValue" name="1" length=20></td></tr>
		<tr><td><label for "2">Publisher:&nbsp;</label></td><td><input type=text class="attributeValue" name="2" length=20></td></tr>
		<tr><td><label for "3">Title:&nbsp;</label></td><td><input type=text class="attributeValue" name="3" length=20></td></tr>
		<tr><td><label for "4">Subject:&nbsp;</label></td><td><input type=text class="attributeValue" name="4" length=20></td></tr>
		</table><BR>
		
		Include:<BR>
		<INPUT TYPE="radio" NAME="include" VALUE="0" CHECKED>
        All Available Books
        <BR>
        <INPUT TYPE="radio" NAME="include" VALUE="1">
        All Books
        <BR>
		
		Sort By:<BR>
		<INPUT TYPE="radio" NAME="sort" VALUE="0" CHECKED>
        Year
        <BR>
        <INPUT TYPE="radio" NAME="sort" VALUE="1">
        Average Score
        <BR>
        <INPUT TYPE="radio" NAME="sort" VALUE="2">
        Popularity
        <BR>
		<input type=submit>
	</form>
	<BR><BR>

<%

}
else
{
	String[] info = new String[4];

	int i = 1;	
	String attributeValue = request.getParameter("" + i);
	while(i < 5)
	{
		if(attributeValue == null)
			attributeValue = "";
		
		info[i - 1] = attributeValue;
		
		i++;
		attributeValue = request.getParameter("" + i);
	}
	
	int include = Integer.parseInt(request.getParameter("include"));
	int sort = Integer.parseInt(request.getParameter("sort"));
	
	cs5530.Connector connector = new Connector();
	cs5530.Order order = new Order();
%>  

  <p><b>Book browsing results:</b><BR><BR>

  <%=order.browseBooks(info[0], info[1], info[2], info[3], include, sort, connector)%><BR><BR>

<%
 connector.closeStatement();
 connector.closeConnection();
}
%>

<BR><a href="index.html"><button>Homepage</button></a></p>

</body>
