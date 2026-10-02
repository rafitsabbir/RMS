<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<spring:url value="/createcandidate" var="createURL" />
<spring:url value="/deletecandidate" var="deleteURL" />
<%-- Hiring Managers see the list read-only; SecurityConfig refuses them the add, edit and delete URLs --%>
<c:set var="canedit" value="${sessionScope.user.role eq 'SUPER_ADMIN' or sessionScope.user.role eq 'HR'}" />
<rms:layout title="Candidates" active="candidates" tables="true">
	<div class="rms-page-header">
		<div>
			<h1>Candidates</h1>
			<p>The people being interviewed, with the position and language they are assessed on.</p>
		</div>
		<c:if test="${canedit}">
		<a class="btn btn-primary d-inline-flex align-items-center gap-2" href="${createURL}">
			<svg class="rms-icon" aria-hidden="true"><use href="${icons}#plus-lg"/></svg>Add candidate</a>
		</c:if>
	</div>
	<div class="card rms-card">
		<div class="card-body">
			<table id="candidatetable" class="table table-striped table-hover align-middle w-100" data-rms-table>
				<thead>
					<tr>
						<th class="rms-col-id">Candidate Id</th>
						<th>Name</th>
						<th>Position</th>
						<th>Language</th>
						<th>Status</th>
						<c:if test="${canedit}">
						<th class="rms-col-actions text-end" data-orderable="false" data-searchable="false">Actions</th>
						</c:if>
					</tr>
				</thead>
				<tbody>
				<c:forEach items="${candidatelist}" var="candidate" varStatus="row">
					<tr>
						<%-- Sorting the ID column keeps the server order (C2 before C10), not text order --%>
						<td data-order="${row.index}"><c:out value="${candidate.candidateid}"/></td>
						<td class="rms-name"><c:out value="${candidate.firstname} ${candidate.lastname}"/></td>
						<td><c:out value="${candidate.positionname}"/></td>
						<td><c:out value="${candidate.languagename}"/></td>
						<td>
						<c:choose>
							<c:when test="${fn:toUpperCase(candidate.candidatestatus) eq 'S'}">
								<span class="badge rounded-pill text-bg-success rms-status"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#check-circle-fill"/></svg>Selected</span>
							</c:when>
							<c:when test="${fn:toUpperCase(candidate.candidatestatus) eq 'R'}">
								<span class="badge rounded-pill text-bg-danger rms-status"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#x-circle-fill"/></svg>Rejected</span>
							</c:when>
							<c:otherwise>
								<span class="badge rounded-pill text-bg-secondary rms-status"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#hourglass-split"/></svg>Pending</span>
							</c:otherwise>
						</c:choose>
						</td>
						<c:if test="${canedit}">
						<td class="text-end text-nowrap">
							<%-- The ID is free text, so it goes in a query parameter. c:param encodes "+" as %2B; spring:param
								leaves it, and the server would read it as a space --%>
							<c:url value="/updatecandidate" var="updateURL">
								<c:param name="candidateid" value="${candidate.candidateid}" />
							</c:url>
							<a class="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-1" href="<c:out value='${updateURL}'/>">
								<svg class="rms-icon" aria-hidden="true"><use href="${icons}#pencil"/></svg><span class="rms-btn-label">Edit</span></a>
							<%-- The ID is posted as a form field, not in the URL, for the same reason --%>
							<form:form id="delete-${row.index}" method="post" action="${deleteURL}" cssClass="d-inline"
								data-rms-confirm="Delete this candidate? Their scores stay on Candidate Status.">
								<input type="hidden" name="candidateid" value="<c:out value='${candidate.candidateid}'/>"/>
								<button type="submit" class="btn btn-sm btn-outline-danger d-inline-flex align-items-center gap-1">
									<svg class="rms-icon" aria-hidden="true"><use href="${icons}#trash"/></svg><span class="rms-btn-label">Delete</span></button>
							</form:form>
						</td>
						</c:if>
					</tr>
				</c:forEach>
				</tbody>
			</table>
		</div>
	</div>
</rms:layout>
