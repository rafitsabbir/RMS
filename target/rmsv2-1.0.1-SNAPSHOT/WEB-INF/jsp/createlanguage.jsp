<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" isELIgnored="false"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet"
	href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css">
<script
	src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
<script
	src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title></title>
</head>
<body>
<spring:url value="/savelanguage" var="saveURL" />

	<form:form id="language" modelAttribute="languageinfo" method="POST" action="${saveURL}">
	<form:hidden path="languagekey"/>
		<div class="form-group">
			<label for="usr">Language Name:</label> 
			<form:input path="languagename" class="form-control" style="text-transform: uppercase; width: auto;"
			id="languagename" name="languagename"/>			
			<%
				if (null != request.getAttribute("errorMessage")) {
			%>
		</div>
		<div class="form-group">
			<div class="alert alert-danger">
				<strong>Already exist!</strong>
			</div>
		</div>
		<%
			}
			if (null != request.getAttribute("successMessage")) {
		%>
		<div class="form-group">
			<div class="alert alert-success">
				<strong>Language created Successfully.</strong>
			</div>
			<%
				}
			%>
		</div>
		<button type="submit" class="btn btn-primary">Save</button>
	</form:form>

</body>
</html>