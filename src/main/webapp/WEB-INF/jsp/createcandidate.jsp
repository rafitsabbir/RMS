<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<spring:url value="/" var="base" htmlEscape="true" />
<spring:url value="/savecandidate" var="saveURL" />
<spring:url value="/viewcandidatelist" var="listURL" />
<c:set var="heading" value="${update ? 'Edit candidate' : 'Add candidate'}" />
<rms:layout title="${heading}" active="candidates">
	<div class="rms-page-header">
		<div>
			<h1><c:out value="${heading}" /></h1>
			<p>The position the candidate applies for and the language they are assessed on.<c:if test="${not update}"> The candidate ID is assigned when you save.</c:if></p>
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
			<form:form id="candidate" modelAttribute="candidateinfo" method="POST" action="${saveURL}">
				<%-- A new candidate gets the next ID (C1, C2, ...) when saved; an existing one keeps its ID --%>
				<c:if test="${update}">
				<input type="hidden" name="update" value="true"/>
				<div class="mb-3">
					<label for="candidateid" class="form-label">Candidate ID</label>
					<form:input path="candidateid" cssClass="form-control" id="candidateid" readonly="true"
						aria-describedby="candidateid-help"/>
					<div id="candidateid-help" class="form-text">The ID can't be changed.</div>
				</div>
				</c:if>
				<div class="row g-3 mb-3">
					<div class="col-sm-6">
						<label for="firstname" class="form-label">First name</label>
						<form:input path="firstname" cssClass="form-control" id="firstname" autocomplete="off"/>
					</div>
					<div class="col-sm-6">
						<label for="lastname" class="form-label">Last name</label>
						<form:input path="lastname" cssClass="form-control" id="lastname" autocomplete="off"/>
					</div>
				</div>
				<div class="row g-3 mb-3">
					<div class="col-sm-6">
						<label for="email" class="form-label">E-mail <span class="text-body-secondary">(optional)</span></label>
						<form:input path="email" type="email" cssClass="form-control" id="email" autocomplete="off" maxlength="150"/>
					</div>
					<div class="col-sm-6">
						<label for="phone" class="form-label">Phone <span class="text-body-secondary">(optional)</span></label>
						<form:input path="phone" type="tel" cssClass="form-control" id="phone" autocomplete="off" maxlength="30"/>
					</div>
				</div>
				<div class="mb-3">
					<label for="jobkey" class="form-label">Job <span class="text-body-secondary">(optional)</span></label>
					<form:select path="jobkey" cssClass="form-select" id="jobkey" aria-describedby="jobkey-help">
						<form:option value="0" label="No job"/>
						<form:options items="${joblist}" itemValue="jobkey" itemLabel="label"/>
					</form:select>
					<div id="jobkey-help" class="form-text">With a job, the candidate gets the job's position, whatever is chosen below.</div>
				</div>
				<div class="mb-3">
					<label for="positionkey" class="form-label">Position</label>
					<form:select path="positionkey" cssClass="form-select" id="positionkey">
						<form:option value="0" label="Choose a position"/>
						<form:options items="${positionlist}" itemValue="positionkey" itemLabel="positionname"/>
					</form:select>
				</div>
				<div class="mb-3">
					<label for="languagekey" class="form-label">Language</label>
					<form:select path="languagekey" cssClass="form-select" id="languagekey">
						<form:option value="0" label="Choose a language"/>
						<form:options items="${languagelist}" itemValue="languagekey" itemLabel="languagename"/>
					</form:select>
				</div>
				<div class="row g-3 mb-4">
					<div class="col-sm-6">
						<label for="source" class="form-label">Source <span class="text-body-secondary">(optional)</span></label>
						<form:select path="source" cssClass="form-select" id="source">
							<form:option value="" label="Not recorded"/>
							<form:options items="${sourcelist}" itemValue="name" itemLabel="label"/>
							<%-- A source this version doesn't know, kept as it is --%>
							<c:if test="${not candidateinfo.knownsource}">
							<form:option value="${candidateinfo.source}" label="${candidateinfo.source}"/>
							</c:if>
						</form:select>
					</div>
					<div class="col-sm-6">
						<label for="applieddate" class="form-label">Applied on <span class="text-body-secondary">(optional)</span></label>
						<form:input path="applieddate" type="date" cssClass="form-control" id="applieddate"/>
					</div>
				</div>
				<div class="d-flex gap-2">
					<button type="submit" class="btn btn-primary">Save</button>
					<a class="btn btn-outline-secondary" href="${listURL}">Cancel</a>
				</div>
			</form:form>
		</div>
	</div>
</rms:layout>
