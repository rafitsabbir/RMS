<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<spring:url value="/createjob" var="createURL" />
<%-- Hiring Managers see the list read-only; SecurityConfig refuses them the add, edit and delete URLs --%>
<c:set var="canedit" value="${sessionScope.user.role eq 'SUPER_ADMIN' or sessionScope.user.role eq 'HR'}" />
<rms:layout title="Jobs" active="jobs" tables="true">
	<div class="rms-page-header">
		<div>
			<h1>Jobs</h1>
			<p>Openings built on a position. Candidates can be linked to an open job.</p>
		</div>
		<c:if test="${canedit}">
		<a class="btn btn-primary d-inline-flex align-items-center gap-2" href="${createURL}">
			<svg class="rms-icon" aria-hidden="true"><use href="${icons}#plus-lg"/></svg>Add job</a>
		</c:if>
	</div>
	<div class="card rms-card">
		<div class="card-body">
			<table id="jobtable" class="table table-striped table-hover align-middle w-100" data-rms-table>
				<thead>
					<tr>
						<th class="rms-col-id">Job Id</th>
						<th>Position</th>
						<th>Vacancies</th>
						<th>Closing date</th>
						<th>Status</th>
						<c:if test="${canedit}">
						<th class="rms-col-actions text-end" data-orderable="false" data-searchable="false">Actions</th>
						</c:if>
					</tr>
				</thead>
				<tbody>
				<c:forEach items="${joblist}" var="job">
					<tr>
						<td>${job.jobkey}</td>
						<td class="rms-name"><c:out value="${empty job.positionname ? ('Position ' += job.positionkey) : job.positionname}"/></td>
						<td>${job.vacancies}</td>
						<td><c:out value="${job.closingdate}"/></td>
						<td>
						<c:choose>
							<c:when test="${job.open}">
								<span class="badge rounded-pill text-bg-success rms-status"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#check-circle-fill"/></svg>Open</span>
							</c:when>
							<c:otherwise>
								<span class="badge rounded-pill text-bg-secondary rms-status"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#x-circle-fill"/></svg>Closed</span>
							</c:otherwise>
						</c:choose>
						</td>
						<c:if test="${canedit}">
						<td class="text-end text-nowrap">
							<spring:url value="/updatejob/${job.jobkey}" var="updateURL" />
							<a class="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-1" href="${updateURL}">
								<svg class="rms-icon" aria-hidden="true"><use href="${icons}#pencil"/></svg><span class="rms-btn-label">Edit</span></a>
							<spring:url value="/deletejob/${job.jobkey}" var="deleteURL" />
							<form:form id="delete-${job.jobkey}" method="post" action="${deleteURL}" cssClass="d-inline"
								data-rms-confirm="Delete this job? Candidates linked to it keep the link.">
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
