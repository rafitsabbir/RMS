<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /myschedule (ScheduleController): the interviewer's own interviews; the user comes from the session --%>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<rms:layout title="My Schedule" active="myschedule" tables="true">
	<div class="rms-page-header">
		<div>
			<h1>My Schedule</h1>
			<p>Your interviews, from the HR team. Times are the server's local time.</p>
		</div>
	</div>
	<div class="card rms-card">
		<div class="card-body">
			<table id="myscheduletable" class="table table-striped table-hover align-middle w-100" data-rms-table>
				<thead>
					<tr>
						<th>When</th>
						<th>Candidate</th>
						<th>Location</th>
						<th>Status</th>
					</tr>
				</thead>
				<tbody>
				<c:forEach items="${schedulelist}" var="item">
					<tr>
						<td class="text-nowrap"><c:out value="${item.startlabel}"/></td>
						<td class="rms-name">
							<c:out value="${item.candidatename}"/>
							<span class="text-body-secondary small"><c:out value="${item.candidateid}"/></span>
						</td>
						<td><c:out value="${item.location}"/></td>
						<td><rms:schedulestatus value="${item.status}" icons="${icons}"/></td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
		</div>
	</div>
</rms:layout>
