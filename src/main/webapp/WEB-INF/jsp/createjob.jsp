<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<spring:url value="/" var="base" htmlEscape="true" />
<spring:url value="/savejob" var="saveURL" />
<spring:url value="/viewjoblist" var="listURL" />
<c:set var="heading" value="${jobinfo.jobkey > 0 ? 'Edit job' : 'Add job'}" />
<rms:layout title="${heading}" active="jobs">
	<div class="rms-page-header">
		<div>
			<h1><c:out value="${heading}" /></h1>
			<p>An opening for a position. Nothing closes a job automatically: set it to Closed when it is filled.</p>
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
			<form:form id="job" modelAttribute="jobinfo" method="POST" action="${saveURL}">
				<form:hidden path="jobkey"/>
				<div class="mb-3">
					<label for="positionkey" class="form-label">Position</label>
					<form:select path="positionkey" cssClass="form-select" id="positionkey">
						<form:option value="0" label="Choose a position"/>
						<form:options items="${positionlist}" itemValue="positionkey" itemLabel="positionname"/>
					</form:select>
				</div>
				<div class="row g-3 mb-3">
					<div class="col-sm-6">
						<label for="vacancies" class="form-label">Vacancies</label>
						<form:input path="vacancies" type="number" min="1" max="999" cssClass="form-control" id="vacancies"/>
					</div>
					<div class="col-sm-6">
						<label for="closingdate" class="form-label">Closing date <span class="text-body-secondary">(optional)</span></label>
						<form:input path="closingdate" type="date" cssClass="form-control" id="closingdate"/>
					</div>
				</div>
				<div class="mb-4">
					<label for="status" class="form-label">Status</label>
					<form:select path="status" cssClass="form-select" id="status">
						<form:option value="OPEN" label="Open"/>
						<form:option value="CLOSED" label="Closed"/>
					</form:select>
				</div>
				<div class="d-flex gap-2">
					<button type="submit" class="btn btn-primary">Save</button>
					<a class="btn btn-outline-secondary" href="${listURL}">Cancel</a>
				</div>
			</form:form>
		</div>
	</div>
</rms:layout>
