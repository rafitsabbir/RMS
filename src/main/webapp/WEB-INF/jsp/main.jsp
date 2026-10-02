<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /home (LoginController.home). Spring Security already requires a login (SecurityConfig); this is a fallback --%>
<c:if test="${empty userinfo}">
	<c:redirect url="/login" />
</c:if>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<c:set var="role" value="${fn:toUpperCase(userinfo.isinterviewer)}" />
<rms:layout title="Home" active="home">
	<div class="rms-page-header">
		<div>
			<h1>Welcome, <c:out value="${userinfo.firstname}" /></h1>
			<p><c:out value="${userinfo.designation}" /><c:if test="${not empty userinfo.email}"> &middot; <c:out value="${userinfo.email}" /></c:if></p>
		</div>
	</div>

	<c:if test="${role eq 'N'}">
	<div class="row g-3">
		<div class="col-sm-6 col-xl-4">
			<div class="card rms-card rms-tile">
				<div class="card-body">
					<span class="rms-tile-icon"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#clipboard-data"/></svg></span>
					<h2><a class="stretched-link text-reset text-decoration-none" href="${base}adminviewmarks">Candidate Status</a></h2>
					<p>Scores on 10 criteria, with each candidate's total and selection status.</p>
				</div>
			</div>
		</div>
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
		<div class="col-12">
			<div class="card rms-card rms-tile rms-tile-soon">
				<div class="card-body d-flex align-items-center gap-3">
					<span class="rms-tile-icon mb-0"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#clock"/></svg></span>
					<div>
						<h2 class="mb-1">Coming soon</h2>
						<p>Candidates, interviewers, interview schedules and jobs.</p>
					</div>
				</div>
			</div>
		</div>
	</div>
	</c:if>

	<c:if test="${role eq 'Y'}">
	<div class="row g-3">
		<div class="col-12">
			<div class="card rms-card rms-tile rms-tile-soon">
				<div class="card-body d-flex align-items-center gap-3">
					<span class="rms-tile-icon mb-0"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#pencil-square"/></svg></span>
					<div>
						<h2 class="mb-1">Evaluations are coming soon</h2>
						<p>You will enter and review your candidate scores here.</p>
					</div>
				</div>
			</div>
		</div>
	</div>
	</c:if>
</rms:layout>
