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

	Review a book:<BR><BR>
	<form name="review" method=get action="review.jsp">
		<input type=hidden name="searchAttribute" value="review">
		<table id="no_border">
		<tr><td><label for "1">Username:&nbsp;</label></td><td><input type=text class="attributeValue" name="1" length=20 required></td></tr>
		<tr><td><label for "2">Book Title or ISBN:&nbsp;</label></td><td><input type=text class="attributeValue" name="2" length=20 required></td></tr>
		<tr><td><label for "3">Score:&nbsp;</label></td><td>
		<select name = "3">
			<option value="1" selected="true">1</option>
			<option value="2">2</option>
			<option value="3">3</option>
			<option value="4">4</option>
			<option value="5">5</option>
			<option value="6">6</option>
			<option value="7">7</option>
			<option value="8">8</option>
			<option value="9">9</option>
			<option value="10">10</option>
		</select>
		</td></tr>
		<tr><td><label for "4">Review:&nbsp;</label></td><td><textarea cols='20' rows='3' class="attributeValue" name="4" required></textarea></td></tr>
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
	int score = Integer.parseInt(request.getParameter("3"));
	String text = request.getParameter("4");
	
	cs5530.Connector connector = new Connector();
	cs5530.Order order = new Order();
%>  

  <p><b>Review results:</b><BR><BR>

  <%=order.review(user, book, score, text, connector)%><BR><BR>

<%
 connector.closeStatement();
 connector.closeConnection();
}
%>

<BR><a href="../index.html"><button>Homepage</button></a></p>

</body>
