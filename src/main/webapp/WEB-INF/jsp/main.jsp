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
		<div class="col-12">
			<div class="card rms-card rms-tile rms-tile-soon">
				<div class="card-body d-flex align-items-center gap-3">
					<span class="rms-tile-icon mb-0"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#clock"/></svg></span>
					<div>
						<h2 class="mb-1">Coming soon</h2>
						<p>Interview schedules.</p>
					</div>
				</div>
			</div>
		</div>
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
	</div>
	</c:if>
</rms:layout>
