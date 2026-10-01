<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<spring:url value="/" var="base" htmlEscape="true" />
<spring:url value="/welcome" var="loginUrl" />
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="ISO-8859-1">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Sign in - RMS</title>
<link rel="icon" type="image/svg+xml" href="${base}resources/img/favicon.svg">
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" integrity="sha384-sRIl4kxILFvY47J16cr9ZwB07vP4J8+LH7qKQnuqkuIAvNWLzeN8tE5YBujZqJLB" crossorigin="anonymous">
<link rel="stylesheet" href="${base}resources/css/rms.css">
</head>
<body>
<div class="rms-login">
	<header class="rms-login-banner">
		<div class="rms-login-brand">
			<img src="${base}resources/img/favicon.svg" width="48" height="48" alt="">
			<span><span class="rms-brand-name">RMS</span><span class="rms-brand-sub">Recruitment Management</span></span>
		</div>
		<p class="rms-login-tagline">Positions, skills and candidate evaluations in one place.</p>
	</header>
	<main class="rms-login-panel">
		<div class="card rms-card rms-login-card">
			<h1 class="mb-1">Sign in</h1>
			<p class="text-body-secondary mb-4">Enter your user name and password.</p>
			<c:if test="${not empty errorMessage}">
			<div class="alert alert-danger d-flex align-items-center gap-2" role="alert">
				<svg class="rms-icon" aria-hidden="true"><use href="${base}resources/img/icons.svg#exclamation-triangle-fill"/></svg>
				<span><c:out value="${errorMessage}"/></span>
			</div>
			</c:if>
			<form:form id="Login" action="${loginUrl}" method="POST">
				<div class="mb-3">
					<label for="username" class="form-label">User name</label>
					<input type="text" class="form-control form-control-lg" id="username" name="username"
						autocomplete="username" autocapitalize="none" spellcheck="false" required autofocus>
				</div>
				<div class="mb-4">
					<label for="password" class="form-label">Password</label>
					<input type="password" class="form-control form-control-lg" id="password" name="password"
						autocomplete="current-password" required>
				</div>
				<button type="submit" class="btn btn-primary btn-lg w-100">Sign in</button>
			</form:form>
		</div>
	</main>
</div>
</body>
</html>
