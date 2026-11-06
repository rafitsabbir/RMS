<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /viewretention and POST /applyretention (RetentionController, F11; Super Admin): candidates due for the clean-up
	of their personal data, which a Super Admin confirms. Off until RMS_RETENTION_MONTHS is set --%>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<spring:url value="/applyretention" var="applyURL" />
<rms:layout title="Data retention" active="retention">
	<div class="rms-page-header">
		<div>
			<h1>Data retention</h1>
			<p>Candidates who weren't hired and have had no activity for the retention period. Cleaning them up deletes their documents and removes their personal data; their scores and decisions stay in the counts.</p>
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
	<c:choose>
	<c:when test="${months == 0}">
	<div class="card rms-card" id="retention-off">
		<div class="card-body">
			<h2 class="h5">Retention is off</h2>
			<p class="mb-0">Nothing is ever due. To turn it on, the IT team sets <code>RMS_RETENTION_MONTHS</code> to the number of months (1 to 240) your policy keeps the data of candidates who weren't hired, and restarts RMS.</p>
		</div>
	</div>
	</c:when>
	<c:otherwise>
	<p id="retention-period">Period: <strong><c:out value="${months}${months == 1 ? ' month' : ' months'}"/></strong> without activity (applied, decided, a stage change, an interview, an evaluation, an upload or an offer). Hired candidates are never due.</p>
	<c:choose>
	<c:when test="${empty duelist}"><div class="card rms-card"><div class="card-body"><p class="mb-0" id="retention-none">No candidate is due.</p></div></div></c:when>
	<c:otherwise>
	<form:form id="applyretention" method="post" action="${applyURL}"
		data-rms-confirm="Delete the documents and remove the personal data of the selected candidates? This can't be undone.">
	<div class="card rms-card mb-3">
		<div class="card-body">
			<table id="retentiontable" class="table table-striped align-middle w-100">
				<thead><tr><th data-orderable="false"><span class="visually-hidden">Clean up</span></th><th class="rms-col-id">Candidate Id</th><th>Name</th><th>Stage</th><th>Status</th><th>Last activity</th></tr></thead>
				<tbody>
				<c:forEach items="${duelist}" var="c">
					<tr>
						<td><input class="form-check-input" type="checkbox" name="candidateid" value="<c:out value='${c.candidateid}'/>" checked aria-label="Clean up <c:out value='${c.candidateid}'/>"></td>
						<td><c:out value="${c.candidateid}"/></td>
						<td class="rms-name"><c:out value="${c.name}"/><c:if test="${not c.active}"> <span class="badge rounded-pill text-bg-secondary">deleted</span></c:if></td>
						<td><c:out value="${c.stagelabel}"/></td>
						<td><c:out value="${c.statuslabel}"/></td>
						<td><c:out value="${c.lastactivity}"/></td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
		</div>
	</div>
	<button type="submit" class="btn btn-danger">Clean up the selected candidates</button>
	</form:form>
	</c:otherwise>
	</c:choose>
	</c:otherwise>
	</c:choose>
</rms:layout>
