<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<!------ Include the above in your HEAD tag ---------->


<html>
<head>
<link rel="stylesheet" type="text/css" href="/resources/css/login.css">
<link
	href="//maxcdn.bootstrapcdn.com/bootstrap/4.1.1/css/bootstrap.min.css"
	rel="stylesheet" id="bootstrap-css">
<script
	src="//maxcdn.bootstrapcdn.com/bootstrap/4.1.1/js/bootstrap.min.js"></script>
<script
	src="//cdnjs.cloudflare.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>
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
