<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /viewactivity (ActivityController): the newest activity log entries, Super Admin only --%>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<rms:layout title="Activity Log" active="activity" tables="true">
	<div class="rms-page-header">
		<div>
			<h1>Activity Log</h1>
			<p>Who did what: evaluations, decisions, interviews, candidates and users. The newest 500 entries; times are the server's local time.</p>
		</div>
		<a class="btn btn-outline-primary d-inline-flex align-items-center gap-2" href="${base}exportactivity">
			<svg class="rms-icon" aria-hidden="true"><use href="${icons}#list-check"/></svg>Download CSV</a>
	</div>
	<form method="get" action="${base}viewactivity" class="row g-2 align-items-end mb-3">
		<div class="col-auto">
			<label for="action" class="form-label">Action</label>
			<select id="action" name="action" class="form-select">
				<option value="">All actions</option>
				<c:forEach items="${actions}" var="item">
				<option value="<c:out value='${item}'/>"${item eq selectedaction ? ' selected' : ''}><c:out value="${item}"/></option>
				</c:forEach>
			</select>
		</div>
		<div class="col-auto">
			<button type="submit" class="btn btn-primary">Show</button>
		</div>
	</form>
	<div class="card rms-card">
		<div class="card-body">
			<table id="activitytable" class="table table-striped table-hover align-middle w-100" data-rms-table>
				<thead>
					<tr>
						<th>When</th>
						<th>User</th>
						<th>Action</th>
						<th>Record</th>
						<th>Detail</th>
					</tr>
				</thead>
				<tbody>
				<c:forEach items="${activitylist}" var="item">
					<tr>
						<td class="text-nowrap"><c:out value="${item.createdlabel}"/></td>
						<td class="rms-name"><c:out value="${empty item.username ? item.userid : item.username}"/>
							<span class="text-body-secondary small"><c:out value="${item.userid}"/></span></td>
						<td><c:out value="${item.actionlabel}"/></td>
						<td><c:out value="${item.entitytype}"/> <c:out value="${item.entityid}"/></td>
						<td><c:out value="${item.detail}"/></td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
		</div>
	</div>
</rms:layout>
