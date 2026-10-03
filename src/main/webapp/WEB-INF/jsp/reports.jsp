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
	</div>
</rms:layout>
