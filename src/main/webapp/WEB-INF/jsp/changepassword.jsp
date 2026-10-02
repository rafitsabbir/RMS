<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /changepassword (AccountController, every logged-in user) --%>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<spring:url value="/savepassword" var="saveURL" />
<rms:layout title="Change password" active="password">
	<div class="rms-page-header">
		<div>
			<h1>Change password</h1>
			<p>At least 8 characters. Choose one you don't use anywhere else.</p>
		</div>
	</div>
	<div class="card rms-card rms-form-card">
		<div class="card-body p-4">
			<c:if test="${changed}">
			<div class="alert alert-success d-flex align-items-center gap-2" role="status">
				<svg class="rms-icon" aria-hidden="true"><use href="${icons}#check-circle-fill"/></svg>
				<span>Your password was changed.</span>
			</div>
			</c:if>
			<c:if test="${mustchange}">
			<div class="alert alert-warning d-flex align-items-center gap-2" role="alert">
				<svg class="rms-icon" aria-hidden="true"><use href="${icons}#exclamation-triangle-fill"/></svg>
				<span>Your password was set by an administrator. Please choose your own before you continue.</span>
			</div>
			</c:if>
			<c:if test="${not empty errorMessage}">
			<div class="alert alert-danger d-flex align-items-center gap-2" role="alert">
				<svg class="rms-icon" aria-hidden="true"><use href="${icons}#exclamation-triangle-fill"/></svg>
				<span><c:out value="${errorMessage}"/></span>
			</div>
			</c:if>
			<form:form id="changepassword" method="post" action="${saveURL}">
				<div class="mb-3">
					<label for="currentpassword" class="form-label">Current password</label>
					<input type="password" class="form-control" id="currentpassword" name="currentpassword" autocomplete="current-password">
				</div>
				<div class="mb-3">
					<label for="newpassword" class="form-label">New password</label>
					<input type="password" class="form-control" id="newpassword" name="newpassword" autocomplete="new-password">
				</div>
				<div class="mb-4">
					<label for="confirmpassword" class="form-label">Repeat the new password</label>
					<input type="password" class="form-control" id="confirmpassword" name="confirmpassword" autocomplete="new-password">
				</div>
				<div class="d-flex gap-2">
					<button type="submit" class="btn btn-primary">Change password</button>
					<c:if test="${not mustchange}">
					<a class="btn btn-outline-secondary" href="${base}home">Cancel</a>
					</c:if>
				</div>
			</form:form>
		</div>
	</div>
</rms:layout>
