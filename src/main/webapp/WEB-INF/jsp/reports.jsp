<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /reports (ReportController): CSV downloads for staff; the activity log download is for Super Admins --%>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<rms:layout title="Reports" active="reports">
	<div class="rms-page-header">
		<div>
			<h1>Reports</h1>
			<p>Download the lists as CSV files; they open in Excel. Each download is recorded in the activity log.</p>
		</div>
	</div>
	<div class="row g-3">
		<div class="col-sm-6 col-xl-4">
			<div class="card rms-card rms-tile">
				<div class="card-body">
					<span class="rms-tile-icon"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#people"/></svg></span>
					<h2><a class="stretched-link text-reset text-decoration-none" href="${base}exportcandidates">Candidates</a></h2>
					<p>Every candidate with position, language, contact details, status and the reason for the decision.</p>
				</div>
			</div>
		</div>
		<div class="col-sm-6 col-xl-4">
			<div class="card rms-card rms-tile">
				<div class="card-body">
					<span class="rms-tile-icon"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#clipboard-data"/></svg></span>
					<h2><a class="stretched-link text-reset text-decoration-none" href="${base}exportresults">Candidate Status</a></h2>
					<p>The number of evaluations and the average of each criterion and in total, with the decision.</p>
				</div>
			</div>
		</div>
		<div class="col-sm-6 col-xl-4">
			<div class="card rms-card rms-tile">
				<div class="card-body">
					<span class="rms-tile-icon"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#calendar-event"/></svg></span>
					<h2><a class="stretched-link text-reset text-decoration-none" href="${base}exportschedule">Interview Schedule</a></h2>
					<p>Every interview with its candidate, interviewer, time, location and status.</p>
				</div>
			</div>
		</div>
		<c:if test="${sessionScope.user.role eq 'SUPER_ADMIN'}">
		<div class="col-sm-6 col-xl-4">
			<div class="card rms-card rms-tile">
				<div class="card-body">
					<span class="rms-tile-icon"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#clock"/></svg></span>
					<h2><a class="stretched-link text-reset text-decoration-none" href="${base}exportactivity">Activity Log</a></h2>
					<p>The newest 500 entries of the activity log.</p>
				</div>
			</div>
		</div>
		</c:if>
		<div class="col-sm-6 col-xl-4">
			<div class="card rms-card rms-tile">
				<div class="card-body">
					<span class="rms-tile-icon"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#briefcase"/></svg></span>
					<h2><a class="stretched-link text-reset text-decoration-none" href="${base}exporthiring">Hiring Report</a></h2>
					<p>The figures below: decision times, candidates per stage, job and source, and evaluations still to do.</p>
				</div>
			</div>
		</div>
	</div>

	<%-- The hiring report: counts of the active candidates, computed on every visit (HiringReportServiceImpl) --%>
	<h2 class="h4 mt-5 mb-1" id="hiring">Hiring report</h2>
	<c:choose>
	<c:when test="${empty hiring}">
	<div class="alert alert-warning mt-3" role="status"><c:out value="${hiringerror}"/></div>
	</c:when>
	<c:otherwise>
	<p class="text-body-secondary">Active candidates only. Days count from the applied date to the date of the current decision; candidates without both dates are left out.</p>
	<div class="row g-3 mb-3">
		<c:forEach items="${[hiring.toselected, hiring.torejected]}" var="time" varStatus="t">
		<div class="col-md-6">
			<div class="card rms-card h-100">
				<div class="card-body">
					<h3 class="h6">${t.first ? 'Days to Selected' : 'Days to Rejected'}</h3>
					<c:choose>
					<c:when test="${time.count == 0}"><p class="mb-0 text-body-secondary">No ${t.first ? 'Selected' : 'Rejected'} candidate with both dates yet.</p></c:when>
					<c:otherwise>
					<p class="display-6 mb-1"><c:out value="${time.median}"/> <span class="fs-6 text-body-secondary">days (median)</span></p>
					<p class="mb-0 small">Average <c:out value="${time.average}"/> &middot; shortest <c:out value="${time.shortest}"/> &middot;
						longest <c:out value="${time.longest}"/> &middot; ${time.count} candidate${time.count == 1 ? '' : 's'}</p>
					</c:otherwise>
					</c:choose>
				</div>
			</div>
		</div>
		</c:forEach>
	</div>
	<c:set var="kinds" value="${['Stage', 'Job', 'Source']}" />
	<c:forEach items="${[hiring.bystage, hiring.byjob, hiring.bysource]}" var="rows" varStatus="g">
	<c:set var="kind" value="${kinds[g.index]}" />
	<div class="card rms-card mb-3">
		<div class="card-body">
			<h3 class="h6">Candidates per <c:out value="${kind.toLowerCase()}"/></h3>
			<c:choose>
			<c:when test="${empty rows}"><p class="mb-0 text-body-secondary">No candidates yet.</p></c:when>
			<c:otherwise>
			<%-- Focusable, so the table can be scrolled with the keyboard at phone width --%>
			<div class="table-responsive" tabindex="0" role="region" aria-label="Candidates per ${kind.toLowerCase()}">
				<table id="hiringby${kind.toLowerCase()}" class="table table-sm table-striped align-middle mb-0">
					<thead>
						<tr>
							<th scope="col"><c:out value="${kind}"/></th>
							<th scope="col" class="text-end">Candidates</th>
							<th scope="col" class="text-end">Pending</th>
							<th scope="col" class="text-end">On hold</th>
							<th scope="col" class="text-end">Selected</th>
							<th scope="col" class="text-end">Rejected</th>
							<th scope="col" class="text-end">No CV</th>
							<th scope="col" class="text-end">Not evaluated</th>
							<th scope="col" class="text-end">Avg. days to Selected</th>
						</tr>
					</thead>
					<tbody>
					<c:forEach items="${rows}" var="row">
						<tr>
							<th scope="row" class="fw-normal"><c:out value="${row.label}"/></th>
							<td class="text-end">${row.total}</td>
							<td class="text-end">${row.pending}</td>
							<td class="text-end">${row.onhold}</td>
							<td class="text-end">${row.selected}</td>
							<td class="text-end">${row.rejected}</td>
							<td class="text-end">${row.nocv}</td>
							<td class="text-end">${row.notevaluated}</td>
							<td class="text-end"><c:out value="${empty row.averagedaystoselected ? '-' : row.averagedaystoselected}"/></td>
						</tr>
					</c:forEach>
					</tbody>
					<tfoot>
						<tr class="fw-semibold">
							<th scope="row"><c:out value="${hiring.overall.label}"/></th>
							<td class="text-end">${hiring.overall.total}</td>
							<td class="text-end">${hiring.overall.pending}</td>
							<td class="text-end">${hiring.overall.onhold}</td>
							<td class="text-end">${hiring.overall.selected}</td>
							<td class="text-end">${hiring.overall.rejected}</td>
							<td class="text-end">${hiring.overall.nocv}</td>
							<td class="text-end">${hiring.overall.notevaluated}</td>
							<td class="text-end"><c:out value="${empty hiring.overall.averagedaystoselected ? '-' : hiring.overall.averagedaystoselected}"/></td>
						</tr>
					</tfoot>
				</table>
			</div>
			</c:otherwise>
			</c:choose>
		</div>
	</div>
	</c:forEach>
	<div class="card rms-card">
		<div class="card-body">
			<h3 class="h6">Evaluations still to do</h3>
			<p class="small text-body-secondary">Assigned, active interviewers and candidates without a final decision that they haven't evaluated yet.</p>
			<c:choose>
			<c:when test="${empty hiring.pendingbyinterviewer}"><p class="mb-0">None: every assigned interviewer has evaluated their candidates.</p></c:when>
			<c:otherwise>
			<table id="hiringpending" class="table table-sm table-striped align-middle mb-0">
				<thead><tr><th scope="col">Interviewer</th><th scope="col" class="text-end">Evaluations still to do</th></tr></thead>
				<tbody>
				<c:forEach items="${hiring.pendingbyinterviewer}" var="row">
					<tr><th scope="row" class="fw-normal"><c:out value="${row.label}"/></th><td class="text-end">${row.total}</td></tr>
				</c:forEach>
				</tbody>
			</table>
			</c:otherwise>
			</c:choose>
		</div>
	</div>
	</c:otherwise>
	</c:choose>
</rms:layout>
