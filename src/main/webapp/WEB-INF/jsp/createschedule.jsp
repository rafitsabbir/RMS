<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /createschedule (step 1: choose the candidate; step 2, with ?candidateid=: the interview), GET /updateschedule/{key} --%>
<spring:url value="/" var="base" htmlEscape="true" />
<spring:url value="/saveschedule" var="saveURL" />
<spring:url value="/createschedule" var="chooseURL" />
<spring:url value="/viewschedulelist" var="listURL" />
<c:set var="heading" value="${empty scheduleinfo ? 'Schedule interview' : (scheduleinfo.schedulekey > 0 ? 'Edit interview' : 'Schedule interview')}" />
<rms:layout title="${heading}" active="schedule">
	<div class="rms-page-header">
		<div>
			<h1><c:out value="${heading}" /></h1>
			<p>The interviewer must be assigned to the candidate and active. Enter the time as the server's local time.</p>
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
			<c:choose>
			<c:when test="${empty scheduleinfo}">
			<%-- Step 1: a GET form, so it needs no CSRF token and changes nothing --%>
			<form method="get" action="${chooseURL}">
				<div class="mb-4">
					<label for="candidateid" class="form-label">Candidate</label>
					<select name="candidateid" id="candidateid" class="form-select" required>
						<option value="">Choose a candidate</option>
						<c:forEach items="${candidatelist}" var="candidate">
						<option value="<c:out value='${candidate.candidateid}'/>"><c:out value="${candidate.candidateid}"/> - <c:out value="${candidate.firstname} ${candidate.lastname}"/></option>
						</c:forEach>
					</select>
				</div>
				<div class="d-flex gap-2">
					<button type="submit" class="btn btn-primary">Next</button>
					<a class="btn btn-outline-secondary" href="${listURL}">Cancel</a>
				</div>
			</form>
			</c:when>
			<c:otherwise>
			<form:form id="schedule" modelAttribute="scheduleinfo" method="POST" action="${saveURL}">
				<form:hidden path="schedulekey"/>
				<form:hidden path="candidateid"/>
				<div class="mb-3">
					<span class="form-label d-block">Candidate</span>
					<strong><c:out value="${candidate.firstname} ${candidate.lastname}"/></strong>
					<span class="text-body-secondary"><c:out value="${candidate.candidateid}"/></span>
				</div>
				<div class="mb-3">
					<label for="interviewerid" class="form-label">Interviewer</label>
					<form:select path="interviewerid" cssClass="form-select" id="interviewerid">
						<form:option value="" label="Choose an interviewer"/>
						<form:options items="${interviewerlist}" itemValue="interviewerid" itemLabel="interviewername"/>
					</form:select>
					<c:if test="${empty interviewerlist}">
					<div class="form-text">No active interviewer is assigned to this candidate. Assign one on the candidate's profile first.</div>
					</c:if>
				</div>
				<div class="row g-3 mb-3">
					<div class="col-sm-6">
						<label for="startat" class="form-label">Date and time</label>
						<form:input path="startat" type="datetime-local" cssClass="form-control" id="startat"/>
					</div>
					<div class="col-sm-6">
						<label for="location" class="form-label">Location <span class="text-body-secondary">(optional)</span></label>
						<form:input path="location" maxlength="200" cssClass="form-control" id="location"/>
					</div>
				</div>
				<c:if test="${scheduleinfo.schedulekey > 0}">
				<div class="mb-4">
					<label for="status" class="form-label">Status</label>
					<form:select path="status" cssClass="form-select" id="status">
						<form:option value="SCHEDULED" label="Scheduled"/>
						<form:option value="DONE" label="Done"/>
					</form:select>
					<div class="form-text">To cancel an interview, use Cancel on the list.</div>
				</div>
				</c:if>
				<div class="d-flex gap-2">
					<button type="submit" class="btn btn-primary">Save</button>
					<a class="btn btn-outline-secondary" href="${listURL}">Cancel</a>
				</div>
			</form:form>
			</c:otherwise>
			</c:choose>
		</div>
	</div>
</rms:layout>
