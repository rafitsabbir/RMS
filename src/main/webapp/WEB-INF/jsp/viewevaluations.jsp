<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /viewevaluations?candidateid= and POST /savedecision (EvaluationController): each interviewer's scores and
	comments, the averages, the decision history; Super Admin and HR record the decision --%>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<spring:url value="/savedecision" var="decisionURL" />
<spring:url value="/adminviewmarks" var="statusURL" />
<c:url value="/viewcandidate" var="profileURL"><c:param name="candidateid" value="${candidate.candidateid}" /></c:url>
<c:set var="role" value="${sessionScope.user.role}" />
<c:set var="canedit" value="${role eq 'SUPER_ADMIN' or role eq 'HR'}" />
<c:set var="fullname" value="${candidate.firstname} ${candidate.lastname}" />
<rms:layout title="Evaluations of ${fullname}" active="marks">
	<div class="rms-page-header">
		<div>
			<h1>Evaluations of <c:out value="${fullname}" /></h1>
			<p>Candidate <c:out value="${candidate.candidateid}" /> &middot; <c:out value="${candidate.positionname}" /> &middot; <c:out value="${candidate.languagename}" /> &middot; <rms:status value="${candidate.candidatestatus}" icons="${icons}"/></p>
		</div>
		<div class="d-flex gap-2">
			<a class="btn btn-outline-secondary d-inline-flex align-items-center gap-2" href="${statusURL}">
				<svg class="rms-icon" aria-hidden="true"><use href="${icons}#arrow-left"/></svg>Candidate Status</a>
			<a class="btn btn-outline-primary d-inline-flex align-items-center gap-2" href="<c:out value='${profileURL}'/>">
				<svg class="rms-icon" aria-hidden="true"><use href="${icons}#people"/></svg>Profile</a>
		</div>
	</div>

	<c:if test="${not empty message}">
	<div class="alert alert-success d-flex align-items-center gap-2" role="status">
		<svg class="rms-icon" aria-hidden="true"><use href="${icons}#check-circle-fill"/></svg>
		<span><c:out value="${message}"/></span>
	</div>
	</c:if>
	<c:if test="${not empty errorMessage}">
	<div class="alert alert-danger d-flex align-items-center gap-2" role="alert">
		<svg class="rms-icon" aria-hidden="true"><use href="${icons}#exclamation-triangle-fill"/></svg>
		<span><c:out value="${errorMessage}"/></span>
	</div>
	</c:if>

	<div class="card rms-card mb-3">
		<div class="card-body">
			<h2 class="h5 mb-1">Scores</h2>
			<p class="text-body-secondary mb-3">Each criterion from 1 to 10; the average row is what Candidate Status shows.</p>
			<c:choose>
			<c:when test="${empty evaluations}">
			<p class="mb-0">No evaluations yet.</p>
			</c:when>
			<c:otherwise>
			<div class="table-responsive">
			<table id="evaluationtable" class="table table-sm align-middle">
				<thead>
					<tr>
						<th>Interviewer</th>
						<c:forEach items="${criteria}" var="criterion">
						<th class="small"><c:out value="${criterion.label}"/></th>
						</c:forEach>
						<th>Total</th>
					</tr>
				</thead>
				<tbody>
				<c:forEach items="${evaluations}" var="evaluation">
					<tr>
						<td>
							<c:out value="${empty evaluation.interviewername ? evaluation.interviewerid : evaluation.interviewername}"/>
							<c:if test="${not evaluation.intervieweractive}"><span class="badge rounded-pill text-bg-warning ms-1">interviewer inactive</span></c:if>
							<c:if test="${not evaluation.assigned}"><span class="badge rounded-pill text-bg-secondary ms-1">unassigned</span></c:if>
							<div class="small text-body-secondary"><c:out value="${evaluation.updatedlabel}"/></div>
						</td>
						<c:forEach items="${evaluation.scores}" var="score">
						<td>${score}</td>
						</c:forEach>
						<td class="fw-semibold">${evaluation.total}</td>
					</tr>
				</c:forEach>
					<tr class="table-light fw-semibold">
						<td>Average of ${summary.evaluations}</td>
						<c:forEach items="${summary.roundedaverages}" var="average">
						<td>${average}</td>
						</c:forEach>
						<td>${summary.total}</td>
					</tr>
				</tbody>
			</table>
			</div>
			<h3 class="h6 mt-3">Comments</h3>
			<c:forEach items="${evaluations}" var="evaluation">
			<div class="mb-3">
				<div class="fw-semibold"><c:out value="${empty evaluation.interviewername ? evaluation.interviewerid : evaluation.interviewername}"/></div>
				<c:if test="${not empty evaluation.comments}"><p class="mb-1"><c:out value="${evaluation.comments}"/></p></c:if>
				<ul class="small mb-0">
				<c:forEach items="${criteria}" var="criterion" varStatus="row">
					<c:if test="${not empty evaluation.criterioncomments[row.index]}">
					<li><c:out value="${criterion.label}"/>: <c:out value="${evaluation.criterioncomments[row.index]}"/></li>
					</c:if>
				</c:forEach>
				</ul>
				<c:if test="${empty evaluation.comments}"><p class="small text-body-secondary mb-0">No overall comment.</p></c:if>
			</div>
			</c:forEach>
			</c:otherwise>
			</c:choose>
		</div>
	</div>

	<div class="row g-3">
		<c:if test="${canedit}">
		<div class="col-xl-6">
			<div class="card rms-card h-100" id="decision">
				<div class="card-body">
					<h2 class="h5 mb-1">Decision</h2>
					<p class="text-body-secondary mb-3">Selected and Rejected lock the evaluations; On hold doesn't. A decision can be changed later; every change is kept below.</p>
					<c:if test="${empty evaluations}">
					<div class="alert alert-warning d-flex align-items-center gap-2" role="status">
						<svg class="rms-icon" aria-hidden="true"><use href="${icons}#exclamation-triangle-fill"/></svg>
						<span>There are no evaluations yet. A decision is still possible.</span>
					</div>
					</c:if>
					<form:form id="savedecision" modelAttribute="decisioninfo" method="POST" action="${decisionURL}">
						<form:hidden path="candidateid"/>
						<div class="row g-3 mb-3">
							<div class="col-sm-6">
								<label for="status" class="form-label">Decision</label>
								<form:select path="status" id="status" cssClass="form-select">
									<form:option value="" label="Choose"/>
									<form:options items="${statuses}" itemValue="name" itemLabel="label"/>
								</form:select>
							</div>
							<div class="col-sm-6">
								<label for="decisiondate" class="form-label">Date</label>
								<form:input path="decisiondate" type="date" id="decisiondate" cssClass="form-control"/>
							</div>
						</div>
						<div class="mb-3">
							<label for="reason" class="form-label">Reason</label>
							<form:textarea path="reason" id="reason" cssClass="form-control" rows="3" maxlength="500"/>
						</div>
						<button type="submit" class="btn btn-primary">Save decision</button>
					</form:form>
				</div>
			</div>
		</div>
		</c:if>
		<div class="col-xl-6">
			<div class="card rms-card h-100">
				<div class="card-body">
					<h2 class="h5 mb-3">Decision history</h2>
					<c:choose>
					<c:when test="${empty decisions}">
					<p class="mb-0">No decisions recorded in RMS yet.</p>
					</c:when>
					<c:otherwise>
					<ul class="list-unstyled mb-0">
					<c:forEach items="${decisions}" var="decision">
						<li class="mb-3">
							<rms:status value="${decision.status}" icons="${icons}"/>
							<span class="small text-body-secondary ms-1"><c:out value="${decision.decisiondate}"/>, <c:out value="${empty decision.decidedbyname ? decision.decidedby : decision.decidedbyname}"/></span>
							<div><c:out value="${decision.reason}"/></div>
						</li>
					</c:forEach>
					</ul>
					</c:otherwise>
					</c:choose>
				</div>
			</div>
		</div>
	</div>
</rms:layout>
