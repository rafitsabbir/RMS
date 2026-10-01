<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<spring:url value="/createposition" var="createURL" />
<rms:layout title="Positions" active="positions" tables="true">
	<div class="rms-page-header">
		<div>
			<h1>Positions</h1>
			<p>The job positions candidates are interviewed for.</p>
		</div>
		<a class="btn btn-primary d-inline-flex align-items-center gap-2" href="${createURL}">
			<svg class="rms-icon" aria-hidden="true"><use href="${icons}#plus-lg"/></svg>Add position</a>
	</div>
	<div class="card rms-card">
		<div class="card-body">
			<table id="positiontable" class="table table-striped table-hover align-middle w-100" data-rms-table>
				<thead>
					<tr>
						<th class="rms-col-id">Position Id</th>
						<th>Position Name</th>
						<th class="rms-col-actions text-end" data-orderable="false" data-searchable="false">Actions</th>
					</tr>
				</thead>
				<tbody>
				<c:forEach items="${positionlist}" var="positionlist">
					<tr>
						<td>${positionlist.positionkey}</td>
						<td class="rms-name"><c:out value="${positionlist.positionname}"/></td>
						<td class="text-end text-nowrap">
							<spring:url value="/updateposition/${positionlist.positionkey}" var="updateURL" />
							<a class="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-1" href="${updateURL}">
								<svg class="rms-icon" aria-hidden="true"><use href="${icons}#pencil"/></svg><span class="rms-btn-label">Edit</span></a>
							<spring:url value="/deleteposition/${positionlist.positionkey}" var="deleteURL" />
							<form:form id="delete-${positionlist.positionkey}" method="post" action="${deleteURL}" cssClass="d-inline"
								data-rms-confirm="Delete this position? Candidates already scored for it keep it on Candidate Status.">
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
