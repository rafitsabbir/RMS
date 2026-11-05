<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /importcandidates, POST /previewimport and /confirmimport (ImportController, F4): upload a CSV, see what it
	would add, then import. Nothing is saved before Import --%>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<spring:url value="/previewimport" var="previewURL" />
<spring:url value="/confirmimport" var="confirmURL" />
<spring:url value="/candidatetemplate" var="templateURL" />
<spring:url value="/viewcandidatelist" var="listURL" />
<rms:layout title="Import candidates" active="candidates">
	<div class="rms-page-header">
		<div>
			<h1>Import candidates</h1>
			<p>Add many candidates at once from a CSV file, as Excel saves it. You see what would be added before anything is saved.</p>
		</div>
		<a class="btn btn-outline-secondary d-inline-flex align-items-center gap-2" href="${listURL}">
			<svg class="rms-icon" aria-hidden="true"><use href="${icons}#arrow-left"/></svg>Candidates</a>
	</div>

	<c:if test="${not empty errorMessage}">
	<div class="alert alert-danger d-flex align-items-center gap-2" role="alert">
		<svg class="rms-icon" aria-hidden="true"><use href="${icons}#exclamation-triangle-fill"/></svg>
		<span><c:out value="${errorMessage}"/></span>
	</div>
	</c:if>

	<c:choose>
	<c:when test="${empty preview}">
	<div class="row g-3">
		<div class="col-xl-7">
			<div class="card rms-card">
				<div class="card-body p-4">
					<h2 class="h5">1. Prepare the file</h2>
					<p>Download the <a href="${templateURL}">template</a>, fill in one candidate per line in Excel, and save it as CSV. The first line names the columns:</p>
					<p class="small"><c:forEach items="${columns}" var="column" varStatus="row"><code><c:out value="${column}"/></code>${row.last ? '' : ', '}</c:forEach></p>
					<ul class="small mb-0">
						<li>Required: First name, Last name, Language, and a Position or a Job Id (the number on the Jobs page).</li>
						<li>Position and Language as they are named in RMS. Source: Referral, Job board, Website, Agency, Walk-in or Other.</li>
						<li>Applied date as YYYY-MM-DD, for example 2026-11-05. English letters only in names.</li>
						<li>At most ${maxrows} candidates per file. Other columns (such as an exported Candidate ID) are ignored.</li>
					</ul>
				</div>
			</div>
		</div>
		<div class="col-xl-5">
			<div class="card rms-card">
				<div class="card-body p-4">
					<h2 class="h5">2. Upload it</h2>
					<form:form id="importfile" method="post" action="${previewURL}" enctype="multipart/form-data">
						<div class="mb-3">
							<label for="file" class="form-label">CSV file</label>
							<input type="file" name="file" id="file" class="form-control" accept=".csv,text/csv"
								data-rms-maxbytes="524288" data-rms-toolarge="The file is larger than 512 KB.">
						</div>
						<button type="submit" class="btn btn-primary d-inline-flex align-items-center gap-2">
							<svg class="rms-icon" aria-hidden="true"><use href="${icons}#upload"/></svg>Check the file</button>
					</form:form>
				</div>
			</div>
		</div>
	</div>
	</c:when>
	<c:otherwise>
	<div class="card rms-card mb-3">
		<div class="card-body p-4">
			<h2 class="h5">3. Check and import</h2>
			<p id="importsummary" class="mb-3">
				<span class="badge rounded-pill text-bg-success">${preview.readycount} ready</span>
				<span class="badge rounded-pill text-bg-warning">${preview.duplicatecount} possible duplicates</span>
				<span class="badge rounded-pill text-bg-danger">${preview.invalidcount} with a problem</span>
			</p>
			<p class="text-body-secondary small">Lines with a problem are never imported: correct them in the file and import them later. Possible duplicates are only imported if you tick the box.</p>
			<form:form id="confirmimport" method="post" action="${confirmURL}">
				<c:if test="${preview.duplicatecount > 0}">
				<div class="form-check mb-3">
					<input class="form-check-input" type="checkbox" name="includeduplicates" value="true" id="includeduplicates">
					<label class="form-check-label" for="includeduplicates">Also import the ${preview.duplicatecount} possible duplicates (they are different people)</label>
				</div>
				</c:if>
				<div class="d-flex gap-2">
					<button type="submit" class="btn btn-primary"${preview.readycount + preview.duplicatecount == 0 ? ' disabled' : ''}>Import</button>
					<spring:url value="/importcandidates" var="againURL" />
					<a class="btn btn-outline-secondary" href="${againURL}">Choose another file</a>
				</div>
			</form:form>
		</div>
	</div>
	<div class="card rms-card">
		<div class="card-body">
			<div class="table-responsive" tabindex="0" role="region" aria-label="Lines of the file">
			<table id="importtable" class="table table-sm align-middle">
				<thead>
					<tr><th>Line</th><th>Name</th><th>Position</th><th>Language</th><th>E-mail</th><th>Phone</th><th>Result</th></tr>
				</thead>
				<tbody>
				<c:forEach items="${preview.rows}" var="row">
					<tr class="${row.valid ? (row.duplicate ? 'table-warning' : '') : 'table-danger'}">
						<td>${row.line}</td>
						<td class="rms-name"><c:out value="${row.candidate.firstname} ${row.candidate.lastname}"/></td>
						<td><c:out value="${row.candidate.positionname}"/></td>
						<td><c:out value="${row.candidate.languagename}"/></td>
						<td><c:out value="${row.candidate.email}"/></td>
						<td><c:out value="${row.candidate.phone}"/></td>
						<td>
							<c:choose>
							<c:when test="${not row.valid}"><c:out value="${row.problem}"/></c:when>
							<c:when test="${row.duplicate}">Possible duplicate of <c:forEach items="${row.duplicates}" var="dup" varStatus="d"><c:out value="${dup}"/>${d.last ? '' : '; '}</c:forEach></c:when>
							<c:otherwise>Ready</c:otherwise>
							</c:choose>
						</td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
			</div>
		</div>
	</div>
	</c:otherwise>
	</c:choose>
</rms:layout>
