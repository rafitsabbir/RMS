<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /home (LoginController.home). Spring Security already requires a login (SecurityConfig); this is a fallback --%>
<c:if test="${empty userinfo}">
	<c:redirect url="/login" />
</c:if>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<%-- The effective role (rms.model.Role); no role sees only the greeting (G16) --%>
<c:set var="role" value="${userinfo.role}" />
<rms:layout title="Home" active="home">
	<div class="rms-page-header">
		<div>
			<h1>Welcome, <c:out value="${userinfo.firstname}" /></h1>
			<p><c:out value="${userinfo.designation}" /><c:if test="${not empty userinfo.email}"> &middot; <c:out value="${userinfo.email}" /></c:if></p>
		</div>
	</div>

	<%-- The dashboard (DashboardService, Phase 4): absent when it couldn't be read, for example before migration 006 --%>
	<c:if test="${not empty dashboard and not dashboard.personal}">
	<h2 class="rms-section-title">Candidates by status <span class="text-body-secondary fw-normal">(<c:out value="${dashboard.totalcandidates}"/> active)</span></h2>
	<div class="row g-3 mb-4">
		<div class="col-6 col-md-3"><a class="card rms-card rms-stat" href="${base}adminviewmarks"><div class="card-body"><div class="rms-stat-value">${dashboard.selected}</div><div class="rms-stat-label">Selected</div></div></a></div>
		<div class="col-6 col-md-3"><a class="card rms-card rms-stat" href="${base}adminviewmarks"><div class="card-body"><div class="rms-stat-value">${dashboard.rejected}</div><div class="rms-stat-label">Rejected</div></div></a></div>
		<div class="col-6 col-md-3"><a class="card rms-card rms-stat" href="${base}adminviewmarks"><div class="card-body"><div class="rms-stat-value">${dashboard.onhold}</div><div class="rms-stat-label">On hold</div></div></a></div>
		<div class="col-6 col-md-3"><a class="card rms-card rms-stat" href="${base}adminviewmarks"><div class="card-body"><div class="rms-stat-value">${dashboard.pending}</div><div class="rms-stat-label">Pending</div></div></a></div>
	</div>
	<h2 class="rms-section-title">Needs attention</h2>
	<div class="row g-3 mb-4">
		<div class="col-6 col-lg"><a class="card rms-card rms-stat" href="${base}viewcandidatelist"><div class="card-body"><div class="rms-stat-value">${dashboard.pendingevaluations}</div><div class="rms-stat-label">Evaluations still to do</div></div></a></div>
		<div class="col-6 col-lg"><a class="card rms-card rms-stat" href="${base}viewschedulelist"><div class="card-body"><div class="rms-stat-value">${dashboard.upcominginterviews}</div><div class="rms-stat-label">Upcoming interviews</div></div></a></div>
		<div class="col-6 col-lg"><a class="card rms-card rms-stat" href="${base}viewjoblist"><div class="card-body"><div class="rms-stat-value">${dashboard.openjobs}</div><div class="rms-stat-label">Open jobs</div></div></a></div>
		<div class="col-6 col-lg"><a class="card rms-card rms-stat${dashboard.candidateswithoutcv > 0 ? ' rms-stat-warn' : ''}" href="${base}viewcandidatelist"><div class="card-body"><div class="rms-stat-value">${dashboard.candidateswithoutcv}</div><div class="rms-stat-label">Candidates with no CV</div></div></a></div>
		<div class="col-6 col-lg"><a class="card rms-card rms-stat${dashboard.candidateswithinactiveinterviewer > 0 ? ' rms-stat-warn' : ''}" href="${base}viewcandidatelist"><div class="card-body"><div class="rms-stat-value">${dashboard.candidateswithinactiveinterviewer}</div><div class="rms-stat-label">Candidates with an inactive interviewer</div></div></a></div>
	</div>
	<c:if test="${not empty dashboard.nextinterviews}">
	<h2 class="rms-section-title">Next interviews</h2>
	<div class="card rms-card mb-4">
		<ul class="list-group list-group-flush">
			<c:forEach items="${dashboard.nextinterviews}" var="interview">
			<li class="list-group-item d-flex flex-wrap gap-2 justify-content-between">
				<span><strong><c:out value="${interview.startlabel}"/></strong> &middot; <c:out value="${interview.candidatename}"/></span>
				<span class="text-body-secondary"><c:out value="${empty interview.interviewername ? interview.interviewerid : interview.interviewername}"/><c:if test="${not empty interview.location}"> &middot; <c:out value="${interview.location}"/></c:if></span>
			</li>
			</c:forEach>
		</ul>
	</div>
	</c:if>
	</c:if>

	<c:if test="${not empty dashboard and dashboard.personal}">
	<div class="row g-3 mb-4">
		<div class="col-6 col-md-4"><a class="card rms-card rms-stat${dashboard.pendingevaluations > 0 ? ' rms-stat-warn' : ''}" href="${base}myevaluations"><div class="card-body"><div class="rms-stat-value">${dashboard.pendingevaluations}</div><div class="rms-stat-label">Evaluations still to do</div></div></a></div>
		<div class="col-6 col-md-4"><a class="card rms-card rms-stat" href="${base}myschedule"><div class="card-body"><div class="rms-stat-value">${dashboard.upcominginterviews}</div><div class="rms-stat-label">Upcoming interviews</div></div></a></div>
	</div>
	<c:if test="${not empty dashboard.nextinterviews}">
	<h2 class="rms-section-title">My next interviews</h2>
	<div class="card rms-card mb-4">
		<ul class="list-group list-group-flush">
			<c:forEach items="${dashboard.nextinterviews}" var="interview">
			<li class="list-group-item d-flex flex-wrap gap-2 justify-content-between">
				<span><strong><c:out value="${interview.startlabel}"/></strong> &middot; <c:out value="${interview.candidatename}"/></span>
				<span class="text-body-secondary"><c:out value="${interview.location}"/></span>
			</li>
			</c:forEach>
		</ul>
	</div>
	</c:if>
	</c:if>

	<c:if test="${role eq 'SUPER_ADMIN' or role eq 'HR' or role eq 'HIRING_MANAGER'}">
	<div class="row g-3">
		<div class="col-sm-6 col-xl-4">
			<div class="card rms-card rms-tile">
				<div class="card-body">
					<span class="rms-tile-icon"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#clipboard-data"/></svg></span>
					<h2><a class="stretched-link text-reset text-decoration-none" href="${base}adminviewmarks">Candidate Status</a></h2>
					<p>Average scores on 10 criteria over all evaluations, with each candidate's total and decision.</p>
				</div>
			</div>
		</div>
		<div class="col-sm-6 col-xl-4">
			<div class="card rms-card rms-tile">
				<div class="card-body">
					<span class="rms-tile-icon"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#people"/></svg></span>
					<h2><a class="stretched-link text-reset text-decoration-none" href="${base}viewcandidatelist">Candidates</a></h2>
					<p>The people being interviewed, with their profile and documents.</p>
				</div>
			</div>
		</div>
		<div class="col-sm-6 col-xl-4">
			<div class="card rms-card rms-tile">
				<div class="card-body">
					<span class="rms-tile-icon"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#megaphone"/></svg></span>
					<h2><a class="stretched-link text-reset text-decoration-none" href="${base}viewjoblist">Jobs</a></h2>
					<p>Openings for a position, with vacancies, a closing date and a status.</p>
				</div>
			</div>
		</div>
		<div class="col-sm-6 col-xl-4">
			<div class="card rms-card rms-tile">
				<div class="card-body">
					<span class="rms-tile-icon"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#calendar-event"/></svg></span>
					<h2><a class="stretched-link text-reset text-decoration-none" href="${base}viewschedulelist">Interview Schedule</a></h2>
					<p>Who interviews which candidate, when and where.</p>
				</div>
			</div>
		</div>
		<c:if test="${role eq 'SUPER_ADMIN' or role eq 'HR'}">
		<div class="col-sm-6 col-xl-4">
			<div class="card rms-card rms-tile">
				<div class="card-body">
					<span class="rms-tile-icon"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#briefcase"/></svg></span>
					<h2><a class="stretched-link text-reset text-decoration-none" href="${base}viewpositionlist">Positions</a></h2>
					<p>The job positions candidates are interviewed for.</p>
				</div>
			</div>
		</div>
		<div class="col-sm-6 col-xl-4">
			<div class="card rms-card rms-tile">
				<div class="card-body">
					<span class="rms-tile-icon"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#code-slash"/></svg></span>
					<h2><a class="stretched-link text-reset text-decoration-none" href="${base}viewlanguagelist">Languages</a></h2>
					<p>The languages and skills candidates are assessed on.</p>
				</div>
			</div>
		</div>
		<c:if test="${role eq 'SUPER_ADMIN'}">
		<div class="col-sm-6 col-xl-4">
			<div class="card rms-card rms-tile">
				<div class="card-body">
					<span class="rms-tile-icon"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#person-badge"/></svg></span>
					<h2><a class="stretched-link text-reset text-decoration-none" href="${base}viewuserlist">Users and Roles</a></h2>
					<p>Who can sign in, with which role; reset passwords.</p>
				</div>
			</div>
		</div>
		</c:if>
		</c:if>
	</div>
	</c:if>

	<c:if test="${role eq 'INTERVIEWER'}">
	<div class="row g-3">
		<div class="col-sm-6 col-xl-4">
			<div class="card rms-card rms-tile">
				<div class="card-body">
					<span class="rms-tile-icon"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#pencil-square"/></svg></span>
					<h2><a class="stretched-link text-reset text-decoration-none" href="${base}myevaluations">My Evaluations</a></h2>
					<p>The candidates assigned to you: score them on 10 criteria, from 1 to 10, with comments.</p>
				</div>
			</div>
		</div>
		<div class="col-sm-6 col-xl-4">
			<div class="card rms-card rms-tile">
				<div class="card-body">
					<span class="rms-tile-icon"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#calendar-event"/></svg></span>
					<h2><a class="stretched-link text-reset text-decoration-none" href="${base}myschedule">My Schedule</a></h2>
					<p>Your interviews, with the time and place.</p>
				</div>
			</div>
		</div>
	</div>
	</c:if>
</rms:layout>
