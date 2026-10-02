<%@ tag language="java" pageEncoding="ISO-8859-1" body-content="scriptless" trimDirectiveWhitespaces="true"
	description="Page shell for every page behind the login: head, sidebar menu, top bar and content" %>
<%@ attribute name="title" required="true" description="Page title, shown as 'title - RMS'" %>
<%@ attribute name="active" required="false" description="Menu key of the current page: home, marks, candidates, jobs, positions, languages, users or password" %>
<%@ attribute name="tables" required="false" type="java.lang.Boolean" description="true loads jQuery and DataTables" %>
<%@ attribute name="stylesheet" required="false" description="An extra stylesheet in resources/css, for example viewmarks.css" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<spring:url value="/" var="base" htmlEscape="true" />
<spring:url value="/logout" var="logoutURL" />
<c:set var="user" value="${sessionScope.user}" />
<%-- The effective role (rms.model.Role: admin.role, or the isinterviewer fallback); no role sees only Home and Account (G16) --%>
<c:set var="role" value="${user.role}" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="ISO-8859-1">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title><c:out value="${title}" /> - RMS</title>
<link rel="icon" type="image/svg+xml" href="${base}resources/img/favicon.svg">
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" integrity="sha384-sRIl4kxILFvY47J16cr9ZwB07vP4J8+LH7qKQnuqkuIAvNWLzeN8tE5YBujZqJLB" crossorigin="anonymous">
<c:if test="${tables}">
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/datatables.net-bs5@2.3.8/css/dataTables.bootstrap5.min.css" integrity="sha384-q6bAgUAsga3oT16XWJ1toXdKcHmBp45jM5roe3RCQ6dET9xGL89Qmpx4tJAI2pm2" crossorigin="anonymous">
<link rel="stylesheet" href="${base}resources/css/datatables.css">
</c:if>
<link rel="stylesheet" href="${base}resources/css/rms.css">
<c:if test="${not empty stylesheet}">
<link rel="stylesheet" href="${base}resources/css/<c:out value="${stylesheet}" />">
</c:if>
<script defer
	src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js" integrity="sha384-FKyoEForCGlyvwx9Hj09JcYn3nv7wiPVlz7YYwJrWVcXK/BmnVDxM+D2scQbITxI" crossorigin="anonymous"></script>
<c:if test="${tables}">
<script defer
	src="https://cdn.jsdelivr.net/npm/jquery@3.7.1/dist/jquery.min.js" integrity="sha384-1H217gwSVyLSIfaLxHbE7dRb3v4mYCKbpQvzx0cegeju1MVsGrX5xXxAvs/HgeFs" crossorigin="anonymous"></script>
<script defer
	src="https://cdn.jsdelivr.net/npm/datatables.net@2.3.8/js/dataTables.min.js" integrity="sha384-vKSHfmUIK/B5LfOmzrP+424efQhNcgZm+sqxgq/DNCicqQRp/V6pVc4HCaRzeQZw" crossorigin="anonymous"></script>
<script defer
	src="https://cdn.jsdelivr.net/npm/datatables.net-bs5@2.3.8/js/dataTables.bootstrap5.min.js" integrity="sha384-3BApNGXgbm9rg2kjIbaEVprAGb2B0n9QyLjBrH090WdkzZ3IiUv8RZoTh5uP8oWH" crossorigin="anonymous"></script>
</c:if>
<script defer src="${base}resources/js/rms.js"></script>
</head>
<body>
<a class="visually-hidden-focusable position-absolute top-0 start-0 m-2 p-2 bg-white rounded" href="#rms-main">Skip to content</a>
<div class="rms-shell">
	<aside class="offcanvas-lg offcanvas-start rms-sidebar" tabindex="-1" id="rms-sidebar" aria-label="Menu">
		<div class="d-flex align-items-center">
			<a class="rms-brand" href="${base}home">
				<img src="${base}resources/img/favicon.svg" width="36" height="36" alt="">
				<span><span class="rms-brand-name">RMS</span><span class="rms-brand-sub">Recruitment Management</span></span>
			</a>
			<button type="button" class="btn-close btn-close-white d-lg-none ms-auto me-3" data-bs-dismiss="offcanvas"
				data-bs-target="#rms-sidebar" aria-label="Close menu"></button>
		</div>
		<nav class="rms-nav" aria-label="Main">
			<ul class="nav flex-column">
				<li class="nav-item">
					<a class="nav-link${active eq 'home' ? ' active' : ''}" href="${base}home"${active eq 'home' ? ' aria-current="page"' : ''}>
						<svg class="rms-icon" aria-hidden="true"><use href="${icons}#house-door"/></svg>Home</a>
				</li>
				<c:if test="${role eq 'SUPER_ADMIN' or role eq 'HR' or role eq 'HIRING_MANAGER'}">
				<li class="rms-nav-heading">Recruitment</li>
				<li class="nav-item">
					<a class="nav-link${active eq 'marks' ? ' active' : ''}" href="${base}adminviewmarks"${active eq 'marks' ? ' aria-current="page"' : ''}>
						<svg class="rms-icon" aria-hidden="true"><use href="${icons}#clipboard-data"/></svg>Candidate Status</a>
				</li>
				<li class="nav-item">
					<a class="nav-link${active eq 'candidates' ? ' active' : ''}" href="${base}viewcandidatelist"${active eq 'candidates' ? ' aria-current="page"' : ''}>
						<svg class="rms-icon" aria-hidden="true"><use href="${icons}#people"/></svg>Candidates</a>
				</li>
				<li class="nav-item">
					<a class="nav-link${active eq 'jobs' ? ' active' : ''}" href="${base}viewjoblist"${active eq 'jobs' ? ' aria-current="page"' : ''}>
						<svg class="rms-icon" aria-hidden="true"><use href="${icons}#megaphone"/></svg>Jobs</a>
				</li>
				</c:if>
				<c:if test="${role eq 'SUPER_ADMIN' or role eq 'HR'}">
				<li class="rms-nav-heading">Master data</li>
				<li class="nav-item">
					<a class="nav-link${active eq 'positions' ? ' active' : ''}" href="${base}viewpositionlist"${active eq 'positions' ? ' aria-current="page"' : ''}>
						<svg class="rms-icon" aria-hidden="true"><use href="${icons}#briefcase"/></svg>Positions</a>
				</li>
				<li class="nav-item">
					<a class="nav-link${active eq 'languages' ? ' active' : ''}" href="${base}viewlanguagelist"${active eq 'languages' ? ' aria-current="page"' : ''}>
						<svg class="rms-icon" aria-hidden="true"><use href="${icons}#code-slash"/></svg>Languages</a>
				</li>
				<%-- A module with no backend yet (G3): shown, but not a link --%>
				<li class="rms-nav-heading">Coming soon</li>
				<li class="nav-item"><span class="nav-link disabled" aria-disabled="true">
					<svg class="rms-icon" aria-hidden="true"><use href="${icons}#calendar-event"/></svg>Interview Schedules<span class="rms-soon">Soon</span></span></li>
				</c:if>
				<c:if test="${role eq 'SUPER_ADMIN'}">
				<li class="rms-nav-heading">Administration</li>
				<li class="nav-item">
					<a class="nav-link${active eq 'users' ? ' active' : ''}" href="${base}viewuserlist"${active eq 'users' ? ' aria-current="page"' : ''}>
						<svg class="rms-icon" aria-hidden="true"><use href="${icons}#person-badge"/></svg>Users and Roles</a>
				</li>
				</c:if>
				<c:if test="${role eq 'INTERVIEWER'}">
				<%-- Interviewer score entry has no backend yet (G5-G7) --%>
				<li class="rms-nav-heading">Coming soon</li>
				<li class="nav-item"><span class="nav-link disabled" aria-disabled="true">
					<svg class="rms-icon" aria-hidden="true"><use href="${icons}#pencil-square"/></svg>Evaluation<span class="rms-soon">Soon</span></span></li>
				<li class="nav-item"><span class="nav-link disabled" aria-disabled="true">
					<svg class="rms-icon" aria-hidden="true"><use href="${icons}#list-check"/></svg>Show Evaluation<span class="rms-soon">Soon</span></span></li>
				</c:if>
				<li class="rms-nav-heading">Account</li>
				<li class="nav-item">
					<a class="nav-link${active eq 'password' ? ' active' : ''}" href="${base}changepassword"${active eq 'password' ? ' aria-current="page"' : ''}>
						<svg class="rms-icon" aria-hidden="true"><use href="${icons}#pencil-square"/></svg>Change password</a>
				</li>
			</ul>
		</nav>
	</aside>
	<div class="rms-content">
		<header class="rms-topbar">
			<button type="button" class="btn rms-menu-toggle d-lg-none" data-bs-toggle="offcanvas" data-bs-target="#rms-sidebar"
				aria-controls="rms-sidebar" aria-label="Open menu"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#list"/></svg></button>
			<a class="rms-topbar-brand d-lg-none" href="${base}home">
				<img src="${base}resources/img/favicon.svg" width="28" height="28" alt="">RMS</a>
			<div class="rms-user">
				<span class="rms-avatar" aria-hidden="true"><c:out value="${fn:substring(user.firstname, 0, 1)}${fn:substring(user.lastname, 0, 1)}" /></span>
				<span class="rms-user-text">
					<span class="rms-user-name"><c:out value="${user.firstname} ${user.lastname}" /></span>
					<span class="rms-user-role"><c:out value="${user.designation}" /></span>
				</span>
				<%-- A POST with the CSRF token that form:form adds (SecurityConfig, G32) --%>
				<form:form id="logout" method="post" action="${logoutURL}" cssClass="d-inline">
					<button type="submit" class="btn btn-sm btn-outline-secondary d-inline-flex align-items-center gap-1">
						<svg class="rms-icon" aria-hidden="true"><use href="${icons}#box-arrow-right"/></svg><span>Log out</span></button>
				</form:form>
			</div>
		</header>
		<main id="rms-main" class="rms-main" tabindex="-1">
			<jsp:doBody />
		</main>
	</div>
</div>
</body>
</html>
