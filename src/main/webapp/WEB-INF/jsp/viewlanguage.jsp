<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<spring:url value="/createlanguage" var="createURL" />
<rms:layout title="Languages" active="languages" tables="true">
	<div class="rms-page-header">
		<div>
			<h1>Languages</h1>
			<p>The languages and skills candidates are assessed on.</p>
		</div>
		<a class="btn btn-primary d-inline-flex align-items-center gap-2" href="${createURL}">
			<svg class="rms-icon" aria-hidden="true"><use href="${icons}#plus-lg"/></svg>Add language</a>
	</div>
	<div class="card rms-card">
		<div class="card-body">
			<table id="languagetable" class="table table-striped table-hover align-middle w-100" data-rms-table>
				<thead>
					<tr>
						<th class="rms-col-id">Language Id</th>
						<th>Language Name</th>
						<th class="rms-col-actions text-end" data-orderable="false" data-searchable="false">Actions</th>
					</tr>
				</thead>
				<tbody>
				<c:forEach items="${languagelist}" var="languagelist">
					<tr>
						<td>${languagelist.languagekey}</td>
						<td class="rms-name"><c:out value="${languagelist.languagename}"/></td>
						<td class="text-end text-nowrap">
							<spring:url value="/updatelanguage/${languagelist.languagekey}" var="updateURL" />
							<a class="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-1" href="${updateURL}">
								<svg class="rms-icon" aria-hidden="true"><use href="${icons}#pencil"/></svg><span class="rms-btn-label">Edit</span></a>
							<spring:url value="/deletelanguage/${languagelist.languagekey}" var="deleteURL" />
							<form:form id="delete-${languagelist.languagekey}" method="post" action="${deleteURL}" cssClass="d-inline"
								data-rms-confirm="Delete this language? Candidates already scored on it keep it on Candidate Status.">
								<button type="submit" class="btn btn-sm btn-outline-danger d-inline-flex align-items-center gap-1">
									<svg class="rms-icon" aria-hidden="true"><use href="${icons}#trash"/></svg><span class="rms-btn-label">Delete</span></button>
							</form:form>
						</td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
		</div>
	</div>
</rms:layout>
