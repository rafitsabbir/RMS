<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /viewuserlist (UserController, Super Admin only) --%>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<spring:url value="/createuser" var="createURL" />
<spring:url value="/deactivateuser" var="deactivateURL" />
<spring:url value="/reactivateuser" var="reactivateURL" />
<rms:layout title="Users and Roles" active="users" tables="true">
	<div class="rms-page-header">
		<div>
			<h1>Users and Roles</h1>
			<p>Who can sign in to RMS, and with which role. Deactivating a user blocks their login only; their history stays.</p>
		</div>
		<a class="btn btn-primary d-inline-flex align-items-center gap-2" href="${createURL}">
			<svg class="rms-icon" aria-hidden="true"><use href="${icons}#plus-lg"/></svg>Add user</a>
	</div>
	<c:if test="${not empty errorMessage}">
	<div class="alert alert-danger d-flex align-items-center gap-2" role="alert">
		<svg class="rms-icon" aria-hidden="true"><use href="${icons}#exclamation-triangle-fill"/></svg>
		<span><c:out value="${errorMessage}"/></span>
	</div>
	</c:if>
	<div class="card rms-card">
		<div class="card-body">
			<table id="usertable" class="table table-striped table-hover align-middle w-100" data-rms-table>
				<thead>
					<tr>
						<th class="rms-col-id">User Id</th>
						<th>Name</th>
						<th>Username</th>
						<th>Role</th>
						<th>Designation</th>
						<th>Status</th>
						<th class="rms-col-actions text-end" data-orderable="false" data-searchable="false">Actions</th>
					</tr>
				</thead>
				<tbody>
				<c:forEach items="${userlist}" var="account" varStatus="row">
					<tr>
						<%-- Sorting the ID column keeps the server order (U2 before U10), not text order --%>
						<td data-order="${row.index}"><c:out value="${account.userid}"/></td>
						<td class="rms-name"><c:out value="${account.firstname} ${account.lastname}"/></td>
						<td>
						<c:choose>
							<c:when test="${empty account.username}"><span class="text-body-secondary">No login</span></c:when>
							<c:otherwise><c:out value="${account.username}"/></c:otherwise>
						</c:choose>
						</td>
						<td>
						<c:choose>
							<c:when test="${empty account.role}"><span class="text-body-secondary">No role</span></c:when>
							<c:otherwise><c:out value="${account.rolelabel}"/></c:otherwise>
						</c:choose>
						</td>
						<td><c:out value="${account.designation}"/></td>
						<td>
						<c:choose>
							<c:when test="${account.isactive eq 1}">
								<span class="badge rounded-pill text-bg-success rms-status"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#check-circle-fill"/></svg>Active</span>
							</c:when>
							<c:otherwise>
								<span class="badge rounded-pill text-bg-secondary rms-status"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#x-circle-fill"/></svg>Inactive</span>
							</c:otherwise>
						</c:choose>
						<c:if test="${account.mustchangepassword eq 1}">
							<span class="badge rounded-pill text-bg-warning">Must change password</span>
						</c:if>
						</td>
						<td class="text-end text-nowrap">
							<%-- The ID may be free text (older rows), so it goes in a query parameter (see viewcandidate.jsp) --%>
							<c:url value="/updateuser" var="updateURL">
								<c:param name="userid" value="${account.userid}" />
							</c:url>
							<a class="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-1" href="<c:out value='${updateURL}'/>">
								<svg class="rms-icon" aria-hidden="true"><use href="${icons}#pencil"/></svg><span class="rms-btn-label">Edit</span></a>
							<c:if test="${account.userid ne currentuserid}">
							<c:choose>
								<c:when test="${account.isactive eq 1}">
								<form:form id="deactivate-${row.index}" method="post" action="${deactivateURL}" cssClass="d-inline"
									data-rms-confirm="Deactivate this user? They can no longer sign in. Their evaluations, schedules and assignments stay.">
									<input type="hidden" name="userid" value="<c:out value='${account.userid}'/>"/>
									<button type="submit" class="btn btn-sm btn-outline-danger d-inline-flex align-items-center gap-1">
										<svg class="rms-icon" aria-hidden="true"><use href="${icons}#x-circle-fill"/></svg><span class="rms-btn-label">Deactivate</span></button>
								</form:form>
								</c:when>
								<c:otherwise>
								<form:form id="reactivate-${row.index}" method="post" action="${reactivateURL}" cssClass="d-inline">
									<input type="hidden" name="userid" value="<c:out value='${account.userid}'/>"/>
									<button type="submit" class="btn btn-sm btn-outline-success d-inline-flex align-items-center gap-1">
										<svg class="rms-icon" aria-hidden="true"><use href="${icons}#check-circle-fill"/></svg><span class="rms-btn-label">Reactivate</span></button>
								</form:form>
								</c:otherwise>
							</c:choose>
							</c:if>
						</td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
		</div>
	</div>
</rms:layout>
