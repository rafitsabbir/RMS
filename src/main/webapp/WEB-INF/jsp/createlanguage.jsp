<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<spring:url value="/" var="base" htmlEscape="true" />
<spring:url value="/savelanguage" var="saveURL" />
<spring:url value="/viewlanguagelist" var="listURL" />
<c:set var="heading" value="${languageinfo.languagekey > 0 ? 'Edit language' : 'Add language'}" />
<rms:layout title="${heading}" active="languages">
	<div class="rms-page-header">
		<div>
			<h1><c:out value="${heading}" /></h1>
			<p>Names are saved in capital letters and must be unique.</p>
		</div>
	</div>
	<div class="card rms-card rms-form-card">
		<div class="card-body p-4">
			<c:if test="${not empty errorMessage}">
			<div class="alert alert-danger d-flex align-items-center gap-2" role="alert">
				<svg class="rms-icon" aria-hidden="true"><use href="${base}resources/img/icons.svg#exclamation-triangle-fill"/></svg>
				<span><c:out value="${errorMessage}"/></span>
			</div>
			</c:if>
			<form:form id="language" modelAttribute="languageinfo" method="POST" action="${saveURL}">
				<form:hidden path="languagekey"/>
				<div class="mb-4">
					<label for="languagename" class="form-label">Language name</label>
					<form:input path="languagename" cssClass="form-control rms-uppercase" id="languagename"
						autocomplete="off" aria-describedby="languagename-help"/>
					<div id="languagename-help" class="form-text">For example: Java.</div>
				</div>
				<div class="d-flex gap-2">
					<button type="submit" class="btn btn-primary">Save</button>
					<a class="btn btn-outline-secondary" href="${listURL}">Cancel</a>
				</div>
			</form:form>
		</div>
	</div>
</rms:layout>
