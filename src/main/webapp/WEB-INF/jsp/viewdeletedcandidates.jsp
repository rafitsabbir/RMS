<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /viewdeletedcandidates (CandidateController, Super Admin only): deleted candidates and the permanent delete of their documents (POST /purgedocuments) --%>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<spring:url value="/purgedocuments" var="purgeURL" />
<rms:layout title="Deleted Candidates" active="deleted" tables="true">
	<div class="rms-page-header">
		<div>
			<h1>Deleted Candidates</h1>
			<p>Candidates removed from the lists. Their records stay; here a Super Admin can delete the stored document files permanently, for example when the retention period has ended.</p>
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
	<div class="card rms-card">
		<div class="card-body">
			<table id="deletedtable" class="table table-striped table-hover align-middle w-100" data-rms-table>
				<thead>
					<tr>
						<th>Candidate</th>
						<th>Files stored</th>
						<th class="rms-col-actions text-end" data-orderable="false" data-searchable="false">Delete files permanently</th>
					</tr>
				</thead>
				<tbody>
				<c:forEach items="${candidatelist}" var="item">
					<tr>
						<td class="rms-name"><c:out value="${item.firstname} ${item.lastname}"/>
							<span class="text-body-secondary small"><c:out value="${item.candidateid}"/></span></td>
						<td><c:out value="${item.unpurgedcount}"/></td>
						<td class="text-end">
							<c:if test="${item.unpurgedcount gt 0}">
							<form:form id="purge-${item.candidateid}" method="post" action="${purgeURL}" cssClass="d-inline-flex gap-2"
								data-rms-confirm="Permanently delete all document files of this candidate? This can't be undone.">
								<input type="hidden" name="candidateid" value="<c:out value='${item.candidateid}'/>"/>
								<input type="text" name="reason" class="form-control form-control-sm" maxlength="255" required
									autocomplete="off" placeholder="Reason (required)" aria-label="Reason for deleting the files">
								<button type="submit" class="btn btn-sm btn-outline-danger d-inline-flex align-items-center gap-1">
									<svg class="rms-icon" aria-hidden="true"><use href="${icons}#trash"/></svg><span class="rms-btn-label">Delete</span></button>
							</form:form>
							</c:if>
						</td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
		</div>
	</div>
</rms:layout>
