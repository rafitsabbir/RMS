<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<spring:url value="/createcandidate" var="createURL" />
<spring:url value="/deletecandidate" var="deleteURL" />
<spring:url value="/viewcandidatelist" var="listURL" />
<%-- The CSV of what the list shows: the same filter parameters --%>
<c:url value="/exportcandidates" var="csvURL" />
<c:if test="${filter.active and not empty pageContext.request.queryString}">
<c:set var="csvURL" value="${csvURL}?${pageContext.request.queryString}" />
</c:if>
<%-- Hiring Managers see the list read-only; SecurityConfig refuses them the add, edit and delete URLs --%>
<c:set var="canedit" value="${sessionScope.user.role eq 'SUPER_ADMIN' or sessionScope.user.role eq 'HR'}" />
<rms:layout title="Candidates" active="candidates" tables="true">
	<div class="rms-page-header">
		<div>
			<h1>Candidates</h1>
			<p>The people being interviewed, with the position and language they are assessed on. Open a name for the profile and documents.</p>
		</div>
		<c:if test="${canedit}">
		<a class="btn btn-primary d-inline-flex align-items-center gap-2" href="${createURL}">
			<svg class="rms-icon" aria-hidden="true"><use href="${icons}#plus-lg"/></svg>Add candidate</a>
		</c:if>
	</div>
	<c:if test="${not empty errorMessage}">
	<div class="alert alert-danger d-flex align-items-center gap-2" role="alert">
		<svg class="rms-icon" aria-hidden="true"><use href="${icons}#exclamation-triangle-fill"/></svg>
		<span><c:out value="${errorMessage}"/></span>
	</div>
	</c:if>
	<%-- GET form: the filters are in the URL, so a filtered list can be bookmarked or shared within the team --%>
	<form id="candidatefilter" method="get" action="${listURL}" class="card rms-card mb-3" aria-labelledby="filter-heading">
		<div class="card-body">
			<h2 id="filter-heading" class="h6 mb-3">Filter</h2>
			<div class="row g-3 align-items-end">
				<div class="col-sm-6 col-lg-3">
					<label for="f-jobkey" class="form-label">Job</label>
					<select name="jobkey" id="f-jobkey" class="form-select">
						<option value="0">Any</option>
						<option value="-1" ${filter.jobkey == -1 ? 'selected' : ''}>No job</option>
						<c:forEach items="${joblist}" var="job">
						<option value="${job.jobkey}" ${filter.jobkey == job.jobkey ? 'selected' : ''}><c:out value="${job.label}"/></option>
						</c:forEach>
					</select>
				</div>
				<div class="col-sm-6 col-lg-2">
					<label for="f-status" class="form-label">Status</label>
					<select name="status" id="f-status" class="form-select">
						<option value="">Any</option>
						<c:forEach items="${statuslist}" var="s">
						<option value="${s.name}" ${filter.status eq s.name ? 'selected' : ''}><c:out value="${s.label}"/></option>
						</c:forEach>
						<option value="P" ${filter.status eq 'P' ? 'selected' : ''}>Pending</option>
					</select>
				</div>
				<div class="col-sm-6 col-lg-2">
					<label for="f-source" class="form-label">Source</label>
					<select name="source" id="f-source" class="form-select">
						<option value="">Any</option>
						<c:forEach items="${sourcelist}" var="s">
						<option value="${s.name}" ${filter.source eq s.name ? 'selected' : ''}><c:out value="${s.label}"/></option>
						</c:forEach>
						<option value="NONE" ${filter.source eq 'NONE' ? 'selected' : ''}>Not recorded</option>
					</select>
				</div>
				<div class="col-6 col-lg-2">
					<label for="f-appliedfrom" class="form-label">Applied from</label>
					<input type="date" name="appliedfrom" id="f-appliedfrom" class="form-control" value="<c:out value='${filter.appliedfrom}'/>"/>
				</div>
				<div class="col-6 col-lg-2">
					<label for="f-appliedto" class="form-label">Applied to</label>
					<input type="date" name="appliedto" id="f-appliedto" class="form-control" value="<c:out value='${filter.appliedto}'/>"/>
				</div>
				<div class="col-12 d-flex flex-wrap align-items-center gap-3">
					<div class="form-check">
						<input class="form-check-input" type="checkbox" name="nocv" value="true" id="f-nocv" ${filter.nocv ? 'checked' : ''}/>
						<label class="form-check-label" for="f-nocv">No CV yet</label>
					</div>
					<div class="form-check">
						<input class="form-check-input" type="checkbox" name="notevaluated" value="true" id="f-notevaluated" ${filter.notevaluated ? 'checked' : ''}/>
						<label class="form-check-label" for="f-notevaluated">Not evaluated yet</label>
					</div>
					<div class="ms-auto d-flex gap-2">
						<button type="submit" class="btn btn-primary">Apply</button>
						<c:if test="${filter.active}"><a class="btn btn-outline-secondary" href="${listURL}">Clear</a></c:if>
					</div>
				</div>
			</div>
		</div>
	</form>
	<div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-2">
		<p class="mb-0" role="status">
			<c:choose>
				<c:when test="${filter.active}">Showing <strong>${fn:length(candidatelist)}</strong> of ${totalcount} candidates.</c:when>
				<c:otherwise>${totalcount} candidates.</c:otherwise>
			</c:choose>
		</p>
		<a class="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-2" href="<c:out value='${csvURL}'/>">
			<svg class="rms-icon" aria-hidden="true"><use href="${icons}#list-check"/></svg>Download these as CSV</a>
	</div>
	<div class="card rms-card">
		<div class="card-body">
			<table id="candidatetable" class="table table-striped table-hover align-middle w-100" data-rms-table>
				<thead>
					<tr>
						<th class="rms-col-id">Candidate Id</th>
						<th>Name</th>
						<th>Position</th>
						<th>Language</th>
						<th>Documents</th>
						<th>Status</th>
						<c:if test="${canedit}">
						<th class="rms-col-actions text-end" data-orderable="false" data-searchable="false">Actions</th>
						</c:if>
					</tr>
				</thead>
				<tbody>
				<c:forEach items="${candidatelist}" var="candidate" varStatus="row">
					<tr>
						<%-- Sorting the ID column keeps the server order (C2 before C10), not text order --%>
						<td data-order="${row.index}"><c:out value="${candidate.candidateid}"/></td>
						<%-- The profile, with the documents; the ID is free text, so c:param encodes it --%>
						<c:url value="/viewcandidate" var="profileURL">
							<c:param name="candidateid" value="${candidate.candidateid}" />
						</c:url>
						<td class="rms-name"><a href="<c:out value='${profileURL}'/>"><c:out value="${candidate.firstname} ${candidate.lastname}"/></a></td>
						<td><c:out value="${candidate.positionname}"/></td>
						<td><c:out value="${candidate.languagename}"/></td>
						<%-- Only the CV is required; n/6 counts the single-slot kinds (rms.model.DocumentType) --%>
						<td class="text-nowrap" data-order="${candidate.hascv ? 10 : 0}${candidate.slotcount}">
							<c:choose>
								<c:when test="${candidate.hascv}"><span class="badge rounded-pill text-bg-success">CV &#10003;</span></c:when>
								<c:otherwise><span class="badge rounded-pill text-bg-warning">No CV</span></c:otherwise>
							</c:choose>
							&middot; ${candidate.slotcount}/${candidate.slottypes}
						</td>
						<td><rms:status value="${candidate.candidatestatus}" icons="${icons}"/></td>
						<c:if test="${canedit}">
						<td class="text-end text-nowrap">
							<%-- The ID is free text, so it goes in a query parameter. c:param encodes "+" as %2B; spring:param
								leaves it, and the server would read it as a space --%>
							<c:url value="/updatecandidate" var="updateURL">
								<c:param name="candidateid" value="${candidate.candidateid}" />
							</c:url>
							<a class="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-1" href="<c:out value='${updateURL}'/>">
								<svg class="rms-icon" aria-hidden="true"><use href="${icons}#pencil"/></svg><span class="rms-btn-label">Edit</span></a>
							<%-- The ID is posted as a form field, not in the URL, for the same reason --%>
							<form:form id="delete-${row.index}" method="post" action="${deleteURL}" cssClass="d-inline"
								data-rms-confirm="Delete this candidate? Their scores stay on Candidate Status.">
								<input type="hidden" name="candidateid" value="<c:out value='${candidate.candidateid}'/>"/>
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
