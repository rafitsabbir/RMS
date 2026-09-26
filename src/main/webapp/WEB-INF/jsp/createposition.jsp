<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/css/bootstrap.min.css" integrity="sha384-HSMxcRTRxnN+Bdg0JdbxYKrThecOKuH5zCYotlSAcp1+c8xmyTe9GYg1l9a69psu" crossorigin="anonymous">
<script
	src="https://code.jquery.com/jquery-3.7.1.min.js" integrity="sha384-1H217gwSVyLSIfaLxHbE7dRb3v4mYCKbpQvzx0cegeju1MVsGrX5xXxAvs/HgeFs" crossorigin="anonymous"></script>
<script
	src="https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/js/bootstrap.min.js" integrity="sha384-aJ21OjlMXNL5UyIl/XNwTMqvzeRMZH2w8c5cRVpzpU8Y5bApTppSuUkhZXN0VxHd" crossorigin="anonymous"></script>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title></title>
</head>
<body>
<spring:url value="/saveposition" var="saveURL" />
	<form:form id="position"  modelAttribute="positioninfo" method="POST" action="${saveURL}">
	<form:hidden path="positionkey"/>
		<div class="form-group">
			<label for="usr">Position Name:</label> 
			<form:input path="positionname" class="form-control" style="text-transform: uppercase; width: auto;"
			id="positionname" name="positionname"/>	
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
				<strong>Position created Successfully.</strong>
			</div>
			<%
				}
			%>
		</div>
		<button type="submit" class="btn btn-primary">Save</button>
	</form:form>

</body>
</html>