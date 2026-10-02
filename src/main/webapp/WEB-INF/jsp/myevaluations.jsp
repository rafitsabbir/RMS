<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /myevaluations (EvaluationController): the interviewer's assigned candidates, and earlier evaluations --%>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<rms:layout title="My Evaluations" active="evaluations" tables="true">
	<div class="rms-page-header">
		<div>
			<h1>My Evaluations</h1>
			<p>The candidates assigned to you. Score each on 10 criteria from 1 to 10; you can change your evaluation until the candidate is selected or rejected.</p>
		</div>
	</div>
	<c:if test="${not empty message}">
	<div class="alert alert-success d-flex align-items-center gap-2" role="status">
		<svg class="rms-icon" aria-hidden="true"><use href="${icons}#check-circle-fill"/></svg>
		<span><c:out value="${message}"/></span>
	</div>
	</c:if>
	<div class="card rms-card">
		<div class="card-body">
			<table id="myevaluationtable" class="table table-striped table-hover align-middle w-100" data-rms-table>
				<thead>
					<tr>
						<th class="rms-col-id">Candidate Id</th>
						<th>Name</th>
						<th>Position</th>
						<th>Assigned</th>
						<th>My evaluation</th>
						<th>Status</th>
						<th class="rms-col-actions text-end" data-orderable="false" data-searchable="false">Actions</th>
					</tr>
				</thead>
				<tbody>
				<c:forEach items="${candidatelist}" var="item" varStatus="row">
					<tr>
						<td data-order="${row.index}"><c:out value="${item.candidateid}"/></td>
						<td class="rms-name"><c:out value="${item.candidatefirstname} ${item.candidatelastname}"/></td>
						<td><c:out value="${item.positionname}"/></td>
						<td class="small">
							<c:choose>
								<c:when test="${item.assigned}"><c:out value="${item.assignedlabel}"/></c:when>
								<c:otherwise><span class="badge rounded-pill text-bg-secondary">No longer assigned</span></c:otherwise>
							</c:choose>
						</td>
						<td>
							<c:choose>
								<c:when test="${item.evaluated}"><span class="badge rounded-pill text-bg-success">Submitted</span></c:when>
								<c:otherwise><span class="badge rounded-pill text-bg-warning">To do</span></c:otherwise>
							</c:choose>
						</td>
						<td><rms:status value="${item.candidatestatus}" icons="${icons}"/></td>
						<td class="text-end text-nowrap">
							<c:url value="/evaluate" var="evaluateURL"><c:param name="candidateid" value="${item.candidateid}" /></c:url>
							<a class="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-1" href="<c:out value='${evaluateURL}'/>">
								<svg class="rms-icon" aria-hidden="true"><use href="${icons}#pencil-square"/></svg><span class="rms-btn-label">${item.assigned and not item.locked ? (item.evaluated ? 'Edit' : 'Evaluate') : 'View'}</span></a>
							<c:if test="${item.assigned}">
							<c:url value="/viewcandidate" var="profileURL"><c:param name="candidateid" value="${item.candidateid}" /></c:url>
							<a class="btn btn-sm btn-outline-secondary d-inline-flex align-items-center gap-1" href="<c:out value='${profileURL}'/>">
								<svg class="rms-icon" aria-hidden="true"><use href="${icons}#people"/></svg><span class="rms-btn-label">Profile</span></a>
							</c:if>
						</td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
		</div>
	</div>
</rms:layout>
