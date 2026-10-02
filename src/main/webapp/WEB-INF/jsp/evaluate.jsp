<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /evaluate?candidateid= and POST /saveevaluation (EvaluationController): the interviewer's own evaluation.
	Editable while they are assigned and the candidate isn't Selected or Rejected; read-only otherwise --%>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<spring:url value="/saveevaluation" var="saveURL" />
<spring:url value="/myevaluations" var="listURL" />
<c:set var="fullname" value="${candidate.firstname} ${candidate.lastname}" />
<rms:layout title="Evaluate ${fullname}" active="evaluations">
	<div class="rms-page-header">
		<div>
			<h1>Evaluate <c:out value="${fullname}" /></h1>
			<p>Candidate <c:out value="${candidate.candidateid}" /> &middot; <c:out value="${candidate.positionname}" /> &middot; <c:out value="${candidate.languagename}" /> &middot; <rms:status value="${candidate.candidatestatus}" icons="${icons}"/></p>
		</div>
		<a class="btn btn-outline-secondary d-inline-flex align-items-center gap-2" href="${listURL}">
			<svg class="rms-icon" aria-hidden="true"><use href="${icons}#arrow-left"/></svg>My Evaluations</a>
	</div>

	<c:if test="${not empty errorMessage}">
	<div class="alert alert-danger d-flex align-items-center gap-2" role="alert">
		<svg class="rms-icon" aria-hidden="true"><use href="${icons}#exclamation-triangle-fill"/></svg>
		<span><c:out value="${errorMessage}"/></span>
	</div>
	</c:if>
	<c:if test="${not editable and empty errorMessage}">
	<div class="alert alert-secondary d-flex align-items-center gap-2" role="status">
		<svg class="rms-icon" aria-hidden="true"><use href="${icons}#clock"/></svg>
		<span>
			<c:choose>
				<c:when test="${not assigned}">You are no longer assigned to this candidate. Your evaluation still counts, but can't be changed.</c:when>
				<c:otherwise>This candidate has been <c:out value="${candidate.statuslabel}"/>, so the evaluations can't be changed.</c:otherwise>
			</c:choose>
		</span>
	</div>
	</c:if>

	<div class="row g-3">
		<div class="col-xl-8">
			<div class="card rms-card">
				<div class="card-body p-4">
					<c:choose>
					<c:when test="${editable}">
					<form:form id="evaluation" modelAttribute="marksinfo" method="POST" action="${saveURL}">
						<form:hidden path="candidateid"/>
						<p class="text-body-secondary">Score each criterion from 1 (poor) to 10 (excellent). Comments are optional.</p>
						<c:forEach items="${criteria}" var="criterion">
						<div class="row g-2 mb-3 align-items-start">
							<div class="col-md-4">
								<label for="${criterion.field}" class="form-label mb-0 fw-semibold"><c:out value="${criterion.label}"/></label>
							</div>
							<div class="col-4 col-md-2">
								<form:select path="${criterion.field}" id="${criterion.field}" cssClass="form-select">
									<form:option value="0" label="-"/>
									<c:forEach begin="1" end="10" var="score">
									<form:option value="${score}" label="${score}"/>
									</c:forEach>
								</form:select>
							</div>
							<div class="col-8 col-md-6">
								<label for="${criterion.commentfield}" class="visually-hidden">Comment on <c:out value="${criterion.label}"/></label>
								<form:input path="${criterion.commentfield}" id="${criterion.commentfield}" cssClass="form-control"
									maxlength="255" placeholder="Comment (optional)" autocomplete="off"/>
							</div>
						</div>
						</c:forEach>
						<div class="mb-4">
							<label for="comments" class="form-label fw-semibold">Overall comment <span class="text-body-secondary fw-normal">(optional)</span></label>
							<form:textarea path="comments" id="comments" cssClass="form-control" rows="4" maxlength="1000"/>
						</div>
						<div class="d-flex gap-2">
							<button type="submit" class="btn btn-primary">Save evaluation</button>
							<a class="btn btn-outline-secondary" href="${listURL}">Cancel</a>
						</div>
					</form:form>
					</c:when>
					<c:otherwise>
					<table id="evaluationtable" class="table align-middle mb-3">
						<thead><tr><th>Criterion</th><th>Score</th><th>Comment</th></tr></thead>
						<tbody>
						<c:forEach items="${criteria}" var="criterion" varStatus="row">
							<tr>
								<td><c:out value="${criterion.label}"/></td>
								<td>${marksinfo.scores[row.index] == 0 ? '-' : marksinfo.scores[row.index]}</td>
								<td><c:out value="${marksinfo.criterioncomments[row.index]}"/></td>
							</tr>
						</c:forEach>
							<tr class="fw-semibold"><td>Total</td><td>${marksinfo.total}</td><td></td></tr>
						</tbody>
					</table>
					<c:if test="${not empty marksinfo.comments}">
					<h2 class="h6">Overall comment</h2>
					<p class="mb-0"><c:out value="${marksinfo.comments}"/></p>
					</c:if>
					</c:otherwise>
					</c:choose>
				</div>
			</div>
		</div>

		<c:if test="${assigned}">
		<div class="col-xl-4">
			<div class="card rms-card">
				<div class="card-body">
					<h2 class="h5 mb-1">Documents</h2>
					<p class="text-body-secondary mb-3">For reference; they open as downloads.</p>
					<c:choose>
					<c:when test="${empty documents}">
					<p class="mb-0">No documents uploaded.</p>
					</c:when>
					<c:otherwise>
					<ul class="list-unstyled mb-0">
					<c:forEach items="${documents}" var="doc">
						<li class="mb-2">
							<div class="small text-body-secondary"><c:out value="${doc.doctypelabel}"/><c:if test="${not empty doc.title}">: <c:out value="${doc.title}"/></c:if></div>
							<c:choose>
								<c:when test="${storageready}">
								<spring:url value="/downloaddocument/${doc.documentkey}" var="downloadURL" />
								<a href="${downloadURL}"><c:out value="${doc.originalname}"/></a>
								</c:when>
								<c:otherwise><c:out value="${doc.originalname}"/></c:otherwise>
							</c:choose>
						</li>
					</c:forEach>
					</ul>
					</c:otherwise>
					</c:choose>
				</div>
			</div>
		</div>
		</c:if>
	</div>
</rms:layout>
