<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /viewschedulelist (ScheduleController): every interview. Hiring Managers read only; SecurityConfig refuses them the other URLs --%>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<spring:url value="/createschedule" var="createURL" />
<c:set var="canedit" value="${sessionScope.user.role eq 'SUPER_ADMIN' or sessionScope.user.role eq 'HR'}" />
<rms:layout title="Interview Schedule" active="schedule" tables="true">
	<div class="rms-page-header">
		<div>
			<h1>Interview Schedule</h1>
			<p>Interviews of candidates by their assigned interviewers. Times are the server's local time.</p>
		</div>
		<c:if test="${canedit}">
		<a class="btn btn-primary d-inline-flex align-items-center gap-2" href="${createURL}">
			<svg class="rms-icon" aria-hidden="true"><use href="${icons}#plus-lg"/></svg>Schedule interview</a>
		</c:if>
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
	<div class="card rms-card">
		<div class="card-body">
			<table id="scheduletable" class="table table-striped table-hover align-middle w-100" data-rms-table>
				<thead>
					<tr>
						<th>When</th>
						<th>Candidate</th>
						<th>Interviewer</th>
						<th>Location</th>
						<th>Status</th>
						<c:if test="${canedit}">
						<th class="rms-col-actions text-end" data-orderable="false" data-searchable="false">Actions</th>
						</c:if>
					</tr>
				</thead>
				<tbody>
				<c:forEach items="${schedulelist}" var="item">
					<tr>
						<td class="text-nowrap"><c:out value="${item.startlabel}"/></td>
						<td class="rms-name">
							<c:url value="/viewcandidate" var="profileURL"><c:param name="candidateid" value="${item.candidateid}" /></c:url>
							<a href="<c:out value='${profileURL}'/>"><c:out value="${item.candidatename}"/></a>
							<span class="text-body-secondary small"><c:out value="${item.candidateid}"/></span>
						</td>
						<td class="rms-name"><c:out value="${empty item.interviewername ? item.interviewerid : item.interviewername}"/></td>
						<td><c:out value="${item.location}"/></td>
						<td><rms:schedulestatus value="${item.status}" icons="${icons}"/></td>
						<c:if test="${canedit}">
						<td class="text-end text-nowrap">
							<c:if test="${not item.cancelled}">
							<spring:url value="/updateschedule/${item.schedulekey}" var="updateURL" />
							<a class="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-1" href="${updateURL}">
								<svg class="rms-icon" aria-hidden="true"><use href="${icons}#pencil"/></svg><span class="rms-btn-label">Edit</span></a>
							</c:if>
							<c:if test="${item.scheduled}">
							<spring:url value="/cancelschedule/${item.schedulekey}" var="cancelURL" />
							<form:form id="cancel-${item.schedulekey}" method="post" action="${cancelURL}" cssClass="d-inline"
								data-rms-confirm="Cancel this interview? It stays in the list as cancelled.">
								<button type="submit" class="btn btn-sm btn-outline-danger d-inline-flex align-items-center gap-1">
									<svg class="rms-icon" aria-hidden="true"><use href="${icons}#x-circle-fill"/></svg><span class="rms-btn-label">Cancel</span></button>
							</form:form>
							</c:if>
						</td>
						</c:if>
					</tr>
				</c:forEach>
				</tbody>
			</table>
		</div>
	</div>
</rms:layout>
