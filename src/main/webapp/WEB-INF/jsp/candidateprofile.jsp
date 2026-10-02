<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /viewcandidate?candidateid= (CandidateController.profile): details, job and documents --%>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<spring:url value="/viewcandidatelist" var="listURL" />
<spring:url value="/purgedocuments" var="purgeURL" />
<%-- Super Admin and HR change documents; Hiring Managers only download (SecurityConfig refuses them the rest) --%>
<c:set var="role" value="${sessionScope.user.role}" />
<c:set var="canedit" value="${role eq 'SUPER_ADMIN' or role eq 'HR'}" />
<%-- The ID is free text (G22), so every URL carrying it is built with c:param, which encodes it --%>
<c:url value="/updatecandidate" var="editURL"><c:param name="candidateid" value="${candidate.candidateid}" /></c:url>
<c:url value="/uploaddocument" var="uploadURL"><c:param name="candidateid" value="${candidate.candidateid}" /></c:url>
<c:set var="fullname" value="${candidate.firstname} ${candidate.lastname}" />
<rms:layout title="${fullname}" active="candidates">
	<div class="rms-page-header">
		<div>
			<h1><c:out value="${fullname}" /></h1>
			<p>Candidate <c:out value="${candidate.candidateid}" /> &middot;
				<c:choose>
					<c:when test="${candidate.hascv}">CV &#10003;</c:when>
					<c:otherwise>No CV</c:otherwise>
				</c:choose>
				&middot; ${candidate.slotcount}/${candidate.slottypes} documents<c:if test="${candidate.professionalcount > 0}">, ${candidate.professionalcount} professional</c:if></p>
		</div>
		<div class="d-flex gap-2">
			<a class="btn btn-outline-secondary d-inline-flex align-items-center gap-2" href="${listURL}">
				<svg class="rms-icon" aria-hidden="true"><use href="${icons}#arrow-left"/></svg>Candidates</a>
			<c:if test="${canedit}">
			<a class="btn btn-primary d-inline-flex align-items-center gap-2" href="<c:out value='${editURL}'/>">
				<svg class="rms-icon" aria-hidden="true"><use href="${icons}#pencil"/></svg>Edit</a>
			</c:if>
		</div>
	</div>

	<c:if test="${not empty documentMessage}">
	<div class="alert alert-success d-flex align-items-center gap-2" role="status">
		<svg class="rms-icon" aria-hidden="true"><use href="${icons}#check-circle-fill"/></svg>
		<span><c:out value="${documentMessage}"/></span>
	</div>
	</c:if>
	<c:if test="${not empty documentError}">
	<div class="alert alert-danger d-flex align-items-center gap-2" role="alert">
		<svg class="rms-icon" aria-hidden="true"><use href="${icons}#exclamation-triangle-fill"/></svg>
		<span><c:out value="${documentError}"/></span>
	</div>
	</c:if>

	<div class="row g-3">
		<div class="col-xl-4">
			<div class="card rms-card h-100">
				<div class="card-body">
					<h2 class="h5 mb-3">Details</h2>
					<dl class="row mb-0">
						<dt class="col-5">E-mail</dt>
						<dd class="col-7"><c:out value="${empty candidate.email ? '-' : candidate.email}"/></dd>
						<dt class="col-5">Phone</dt>
						<dd class="col-7"><c:out value="${empty candidate.phone ? '-' : candidate.phone}"/></dd>
						<dt class="col-5">Position</dt>
						<dd class="col-7"><c:out value="${empty candidate.positionname ? '-' : candidate.positionname}"/></dd>
						<dt class="col-5">Language</dt>
						<dd class="col-7"><c:out value="${empty candidate.languagename ? '-' : candidate.languagename}"/></dd>
						<dt class="col-5">Job</dt>
						<dd class="col-7">
							<c:choose>
								<c:when test="${candidate.jobkey == 0}">-</c:when>
								<c:when test="${empty candidate.jobstatus}">Job ${candidate.jobkey} (deleted)</c:when>
								<c:when test="${candidate.jobstatus eq 'OPEN'}">Job ${candidate.jobkey} (open)</c:when>
								<c:otherwise>Job ${candidate.jobkey} (closed)</c:otherwise>
							</c:choose>
						</dd>
						<dt class="col-5">Source</dt>
						<dd class="col-7"><c:out value="${empty candidate.sourcelabel ? '-' : candidate.sourcelabel}"/></dd>
						<dt class="col-5">Applied on</dt>
						<dd class="col-7"><c:out value="${empty candidate.applieddate ? '-' : candidate.applieddate}"/></dd>
						<dt class="col-5">Status</dt>
						<dd class="col-7">
							<c:choose>
								<c:when test="${fn:toUpperCase(candidate.candidatestatus) eq 'S'}">Selected</c:when>
								<c:when test="${fn:toUpperCase(candidate.candidatestatus) eq 'R'}">Rejected</c:when>
								<c:otherwise>Pending</c:otherwise>
							</c:choose>
						</dd>
					</dl>
				</div>
			</div>
		</div>

		<div class="col-xl-8">
			<div class="card rms-card h-100">
				<div class="card-body">
					<h2 class="h5 mb-1">Documents</h2>
					<p class="text-body-secondary mb-3">Only the CV is required. Files open as downloads.</p>
					<c:if test="${not storageready}">
					<div class="alert alert-warning d-flex align-items-center gap-2" role="alert">
						<svg class="rms-icon" aria-hidden="true"><use href="${icons}#exclamation-triangle-fill"/></svg>
						<span>Document storage isn't configured, so documents can't be uploaded or opened. Please tell IT.</span>
					</div>
					</c:if>
					<div class="table-responsive">
					<table id="documenttable" class="table align-middle mb-0">
						<thead>
							<tr>
								<th>Kind</th>
								<th>File</th>
								<th>Uploaded</th>
								<th class="text-end"><span class="visually-hidden">Actions</span></th>
							</tr>
						</thead>
						<tbody>
						<c:forEach items="${doctypes}" var="type">
							<c:set var="typedocs" value="${documents[type]}" />
							<c:if test="${empty typedocs}">
							<tr>
								<td class="text-nowrap"><c:out value="${type.label}"/></td>
								<td colspan="2">
									<c:choose>
										<c:when test="${type.name eq 'CV'}"><span class="badge rounded-pill text-bg-warning">Missing</span></c:when>
										<c:otherwise><span class="text-body-secondary">Not uploaded</span></c:otherwise>
									</c:choose>
								</td>
								<td class="text-end text-nowrap">
									<c:if test="${canedit and storageready}">
									<c:url value="/viewcandidate" var="addURL"><c:param name="candidateid" value="${candidate.candidateid}" /><c:param name="doctype" value="${type.name}" /></c:url>
									<a class="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-1" href="<c:out value='${addURL}'/>#upload">
										<svg class="rms-icon" aria-hidden="true"><use href="${icons}#plus-lg"/></svg><span class="rms-btn-label">Upload</span></a>
									</c:if>
								</td>
							</tr>
							</c:if>
							<c:forEach items="${typedocs}" var="doc">
							<tr>
								<td class="text-nowrap"><c:out value="${type.label}"/></td>
								<td>
									<spring:url value="/downloaddocument/${doc.documentkey}" var="downloadURL" />
									<a href="${downloadURL}"><c:out value="${doc.originalname}"/></a>
									<span class="text-body-secondary">(<c:out value="${doc.sizelabel}"/>)</span>
									<c:if test="${not empty doc.title}">
									<div class="small"><c:out value="${doc.title}"/><c:if test="${not empty doc.issuer}">, <c:out value="${doc.issuer}"/></c:if><c:if test="${not empty doc.issueyear}">, ${doc.issueyear}</c:if></div>
									</c:if>
								</td>
								<td class="small text-nowrap"><c:out value="${doc.uploadedlabel}"/><div class="text-body-secondary"><c:out value="${empty doc.uploadedbyname ? doc.uploadedby : doc.uploadedbyname}"/></div></td>
								<td class="text-end text-nowrap">
									<c:if test="${canedit}">
									<c:if test="${type.singleSlot and storageready}">
									<c:url value="/viewcandidate" var="replaceURL"><c:param name="candidateid" value="${candidate.candidateid}" /><c:param name="doctype" value="${type.name}" /></c:url>
									<a class="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-1" href="<c:out value='${replaceURL}'/>#upload">
										<svg class="rms-icon" aria-hidden="true"><use href="${icons}#pencil"/></svg><span class="rms-btn-label">Replace</span></a>
									</c:if>
									<spring:url value="/deletedocument/${doc.documentkey}" var="deleteURL" />
									<form:form id="deletedocument-${doc.documentkey}" method="post" action="${deleteURL}" cssClass="d-inline"
										data-rms-confirm="Delete this document? The file is kept until a Super Admin deletes the candidate's documents permanently.">
										<button type="submit" class="btn btn-sm btn-outline-danger d-inline-flex align-items-center gap-1">
											<svg class="rms-icon" aria-hidden="true"><use href="${icons}#trash"/></svg><span class="rms-btn-label">Delete</span></button>
									</form:form>
									</c:if>
								</td>
							</tr>
							</c:forEach>
						</c:forEach>
						</tbody>
					</table>
					</div>
				</div>
			</div>
		</div>

		<c:if test="${canedit and storageready}">
		<div class="col-xl-8 offset-xl-4">
			<div class="card rms-card" id="upload">
				<div class="card-body">
					<h2 class="h5 mb-1">Upload a document</h2>
					<p class="text-body-secondary mb-3">PDF, JPG or PNG, at most 5 MB. A kind that holds one file (all but professional certificates) replaces its current file; up to 5 professional certificates.</p>
					<%-- The candidate ID is only in the URL, not also a hidden field (the server would read "C1,C1"): SecurityConfig
						reads it there when an upload is over the size limit and the body isn't read --%>
					<form:form id="uploaddocument" method="post" action="${uploadURL}" enctype="multipart/form-data">
						<div class="row g-3 mb-3">
							<div class="col-md-6">
								<label for="doctype" class="form-label">Kind</label>
								<select name="doctype" id="doctype" class="form-select">
								<c:forEach items="${doctypes}" var="type">
									<option value="${type.name}"${type.name eq selecteddoctype ? ' selected' : ''}><c:out value="${type.label}"/></option>
								</c:forEach>
								</select>
							</div>
							<div class="col-md-6">
								<label for="file" class="form-label">File</label>
								<input type="file" name="file" id="file" class="form-control" accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
									data-rms-maxbytes="5242880" data-rms-toolarge="The file is larger than 5 MB.">
							</div>
						</div>
						<fieldset class="mb-3">
							<legend class="form-label fs-6">Professional certificates only</legend>
							<div class="row g-3">
								<div class="col-md-5">
									<label for="title" class="form-label">Title</label>
									<input type="text" name="title" id="title" class="form-control" maxlength="100" placeholder="For example PMP" autocomplete="off">
								</div>
								<div class="col-md-5">
									<label for="issuer" class="form-label">Issuer <span class="text-body-secondary">(optional)</span></label>
									<input type="text" name="issuer" id="issuer" class="form-control" maxlength="100" autocomplete="off">
								</div>
								<div class="col-md-2">
									<label for="issueyear" class="form-label">Year <span class="text-body-secondary">(optional)</span></label>
									<input type="text" name="issueyear" id="issueyear" class="form-control" inputmode="numeric" maxlength="4" autocomplete="off">
								</div>
							</div>
						</fieldset>
						<button type="submit" class="btn btn-primary">Upload</button>
					</form:form>
				</div>
			</div>
		</div>
		</c:if>

		<c:if test="${role eq 'SUPER_ADMIN'}">
		<div class="col-xl-8 offset-xl-4">
			<div class="card rms-card border-danger-subtle">
				<div class="card-body">
					<h2 class="h5 mb-1">Permanently delete documents</h2>
					<p class="text-body-secondary mb-3">Removes the stored files of all this candidate's documents, including replaced and deleted ones. The records stay, marked as deleted. This can't be undone.</p>
					<form:form id="purgedocuments" method="post" action="${purgeURL}"
						data-rms-confirm="Permanently delete all document files of this candidate? This can't be undone.">
						<input type="hidden" name="candidateid" value="<c:out value='${candidate.candidateid}'/>"/>
						<div class="mb-3">
							<label for="reason" class="form-label">Reason</label>
							<input type="text" name="reason" id="reason" class="form-control" maxlength="255" required autocomplete="off"
								aria-describedby="reason-help">
							<div id="reason-help" class="form-text">Required. Kept with the records; for example "Retention period ended".</div>
						</div>
						<button type="submit" class="btn btn-outline-danger d-inline-flex align-items-center gap-2">
							<svg class="rms-icon" aria-hidden="true"><use href="${icons}#trash"/></svg>Delete permanently</button>
					</form:form>
				</div>
			</div>
		</div>
		</c:if>

		<div class="col-12">
			<div class="card rms-card rms-tile-soon">
				<div class="card-body d-flex align-items-center gap-3">
					<span class="rms-tile-icon mb-0"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#clock"/></svg></span>
					<div>
						<h2 class="h5 mb-1">Interviewers, evaluations and decisions</h2>
						<p class="mb-0">Coming in the next release.</p>
					</div>
				</div>
			</div>
		</div>
	</div>
</rms:layout>
