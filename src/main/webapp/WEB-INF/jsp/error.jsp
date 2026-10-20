<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true" session="false"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%-- Every error the container answers (web.xml <error-page>): no exception text, stack trace or server version, and
	nothing the Content-Security-Policy blocks. Works without a session or a signed-in user. --%>
<c:set var="base" value="${pageContext.request.contextPath}/" />
<c:set var="status" value="${requestScope['jakarta.servlet.error.status_code']}" />
<c:choose>
	<c:when test="${status == 403}">
		<c:set var="heading" value="You don't have access to this page" />
		<c:set var="detail" value="Your role doesn't include this page, or the form had expired. Go back to the page you came from, or start again from Home." />
	</c:when>
	<c:when test="${status == 404}">
		<c:set var="heading" value="Page not found" />
		<c:set var="detail" value="The address may be mistyped, or the record no longer exists." />
	</c:when>
	<c:when test="${status == 405}">
		<c:set var="heading" value="This page can't be opened that way" />
		<c:set var="detail" value="Use the buttons and links in RMS rather than a saved or typed address." />
	</c:when>
	<c:when test="${status == 503}">
		<c:set var="heading" value="RMS isn't available right now" />
		<c:set var="detail" value="Please try again in a few minutes." />
	</c:when>
	<c:otherwise>
		<c:set var="heading" value="Something went wrong" />
		<c:set var="detail" value="The problem has been logged. Please try again; if it keeps happening, tell your administrator what you were doing and when." />
	</c:otherwise>
</c:choose>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="ISO-8859-1">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title><c:out value="${heading}" /> - RMS</title>
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
	</header>
	<main class="rms-login-panel">
		<div class="card rms-card rms-login-card">
			<c:if test="${not empty status}">
			<p class="text-body-secondary mb-1">Error <c:out value="${status}" /></p>
			</c:if>
			<h1 class="mb-2"><c:out value="${heading}" /></h1>
			<p class="mb-4"><c:out value="${detail}" /></p>
			<c:choose>
				<c:when test="${not empty pageContext.request.userPrincipal}">
				<a class="btn btn-primary btn-lg w-100" href="${base}home">Home</a>
				</c:when>
				<c:otherwise>
				<a class="btn btn-primary btn-lg w-100" href="${base}login">Sign in</a>
				</c:otherwise>
			</c:choose>
		</div>
	</main>
</div>
</body>
</html>
