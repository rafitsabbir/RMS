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
			<p>The position the candidate applies for and the language they are assessed on.</p>
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
				<c:if test="${update}"><input type="hidden" name="update" value="true"/></c:if>
				<div class="mb-3">
					<label for="candidateid" class="form-label">Candidate ID</label>
					<c:choose>
						<c:when test="${update}">
							<form:input path="candidateid" cssClass="form-control" id="candidateid" readonly="true"
								aria-describedby="candidateid-help"/>
							<div id="candidateid-help" class="form-text">The ID can't be changed.</div>
						</c:when>
						<c:otherwise>
							<form:input path="candidateid" cssClass="form-control" id="candidateid" autocomplete="off"
								aria-describedby="candidateid-help"/>
							<div id="candidateid-help" class="form-text">Must be unique.</div>
						</c:otherwise>
					</c:choose>
				</div>
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
				<div class="mb-3">
					<label for="positionkey" class="form-label">Position</label>
					<form:select path="positionkey" cssClass="form-select" id="positionkey">
						<form:option value="0" label="Choose a position"/>
						<form:options items="${positionlist}" itemValue="positionkey" itemLabel="positionname"/>
					</form:select>
				</div>
				<div class="mb-4">
					<label for="languagekey" class="form-label">Language</label>
					<form:select path="languagekey" cssClass="form-select" id="languagekey">
						<form:option value="0" label="Choose a language"/>
						<form:options items="${languagelist}" itemValue="languagekey" itemLabel="languagename"/>
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
