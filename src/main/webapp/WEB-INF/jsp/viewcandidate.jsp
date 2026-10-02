<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<spring:url value="/createcandidate" var="createURL" />
<rms:layout title="Candidates" active="candidates" tables="true">
	<div class="rms-page-header">
		<div>
			<h1>Candidates</h1>
			<p>The people being interviewed, with the position and language they are assessed on.</p>
		</div>
		<a class="btn btn-primary d-inline-flex align-items-center gap-2" href="${createURL}">
			<svg class="rms-icon" aria-hidden="true"><use href="${icons}#plus-lg"/></svg>Add candidate</a>
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
						<th class="rms-col-actions text-end" data-orderable="false" data-searchable="false">Actions</th>
					</tr>
				</thead>
				<tbody>
				<c:forEach items="${candidatelist}" var="candidate">
					<tr>
						<td><c:out value="${candidate.candidateid}"/></td>
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
						<td class="text-end text-nowrap">
							<%-- The ID is free text, so it goes in a query parameter. c:param encodes "+" as %2B; spring:param
								leaves it, and the server would read it as a space --%>
							<c:url value="/updatecandidate" var="updateURL">
								<c:param name="candidateid" value="${candidate.candidateid}" />
							</c:url>
							<a class="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-1" href="<c:out value='${updateURL}'/>">
								<svg class="rms-icon" aria-hidden="true"><use href="${icons}#pencil"/></svg><span class="rms-btn-label">Edit</span></a>
						</td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
		</div>
	</div>
</rms:layout>
