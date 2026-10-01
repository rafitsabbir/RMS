<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<spring:url value="/" var="base" htmlEscape="true" />
<spring:url value="/saveposition" var="saveURL" />
<spring:url value="/viewpositionlist" var="listURL" />
<c:set var="heading" value="${positioninfo.positionkey > 0 ? 'Edit position' : 'Add position'}" />
<rms:layout title="${heading}" active="positions">
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
			<form:form id="position" modelAttribute="positioninfo" method="POST" action="${saveURL}">
				<form:hidden path="positionkey"/>
				<div class="mb-4">
					<label for="positionname" class="form-label">Position name</label>
					<form:input path="positionname" cssClass="form-control rms-uppercase" id="positionname"
						autocomplete="off" aria-describedby="positionname-help"/>
					<div id="positionname-help" class="form-text">For example: Software Engineer.</div>
				</div>
				<div class="d-flex gap-2">
					<button type="submit" class="btn btn-primary">Save</button>
					<a class="btn btn-outline-secondary" href="${listURL}">Cancel</a>
				</div>
			</form:form>
		</div>
	</div>
</rms:layout>
