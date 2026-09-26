<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<!------ Include the above in your HEAD tag ---------->


<html>
<head>
<spring:url value="/resources/css/login.css" var="logincss" />
<link rel="stylesheet" type="text/css" href="${logincss}"/>
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@4.6.2/dist/css/bootstrap.min.css" integrity="sha384-xOolHFLEh07PJGoPkLv1IbcEPTNtaed2xpHsD9ESMhqIYd0nLMwNLD69Npy4HI+N" crossorigin="anonymous"
	rel="stylesheet" id="bootstrap-css"/>
<script
	src="https://cdn.jsdelivr.net/npm/bootstrap@4.6.2/dist/js/bootstrap.min.js" integrity="sha384-+sLIOodYLS7CIrQpBjl+C7nPvqq+FbNUBDunl/OZv93DB7Ln/533i8e/mZXLi/P+" crossorigin="anonymous"></script>
<script
	src="https://code.jquery.com/jquery-3.7.1.min.js" integrity="sha384-1H217gwSVyLSIfaLxHbE7dRb3v4mYCKbpQvzx0cegeju1MVsGrX5xXxAvs/HgeFs" crossorigin="anonymous"></script>
<!------ Include the above in your HEAD tag ---------->
</head>
<body id="LoginForm">
	<div class="container">
		<div class="login-form">
			<div class="main-div">
				<div class="panel">
					<h2>Login</h2>
					<p>Please enter your User Name and Password</p>
				</div>
				<spring:url value="/welcome" var="loginUrl" />
				<form:form id="Login" action="${loginUrl}" method="POST">

					<div class="form-group">


						<input type="text" class="form-control" id="username"
							name="username" placeholder="User Name">

					</div>

					<div class="form-group">

						<input type="password" class="form-control" id="password"
							name="password" placeholder="Password">

					</div>
					<button type="submit" class="btn btn-primary">Login</button>

				</form:form>
			</div>

			<%
				if (null != request.getAttribute("errorMessage")) {
			%>

			<div class="form-group">
				<div class="alert alert-danger">
					<%
						out.println(request.getAttribute("errorMessage"));
					%>
				</div>
			</div>
			<%
				}
			%>

		</div>
	</div>


</body>
</html>
