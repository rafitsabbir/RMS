<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /createuser and /updateuser (UserController, Super Admin only) --%>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<spring:url value="/saveuser" var="saveURL" />
<spring:url value="/resetpassword" var="resetURL" />
<spring:url value="/viewuserlist" var="listURL" />
<c:set var="heading" value="${update ? 'Edit user' : 'Add user'}" />
<rms:layout title="${heading}" active="users">
	<div class="rms-page-header">
		<div>
			<h1><c:out value="${heading}" /></h1>
			<p>The role decides which pages the user can open.<c:if test="${not update}"> The user ID is assigned when you save, and the user must choose their own password when they first sign in.</c:if></p>
		</div>
	</div>
	<div class="card rms-card rms-form-card">
		<div class="card-body p-4">
			<c:if test="${not empty errorMessage}">
			<div class="alert alert-danger d-flex align-items-center gap-2" role="alert">
				<svg class="rms-icon" aria-hidden="true"><use href="${icons}#exclamation-triangle-fill"/></svg>
				<span><c:out value="${errorMessage}"/></span>
			</div>
			</c:if>
			<form:form id="user" modelAttribute="userinfo" method="POST" action="${saveURL}">
				<c:choose>
				<c:when test="${update}">
				<input type="hidden" name="update" value="true"/>
				<form:hidden path="userid"/>
				<div class="row g-3 mb-3">
					<div class="col-sm-6">
						<label for="useridtext" class="form-label">User ID</label>
						<input type="text" class="form-control" id="useridtext" value="<c:out value='${userinfo.userid}'/>" readonly>
					</div>
					<div class="col-sm-6">
						<label for="usernametext" class="form-label">Username</label>
						<input type="text" class="form-control" id="usernametext" value="<c:out value='${empty userinfo.username ? "No login" : userinfo.username}'/>" readonly
							aria-describedby="username-help">
						<div id="username-help" class="form-text">The ID and username can't be changed.</div>
					</div>
				</div>
				</c:when>
				<c:otherwise>
				<div class="mb-3">
					<label for="username" class="form-label">Username</label>
					<form:input path="username" cssClass="form-control" id="username" autocomplete="off" aria-describedby="username-help"/>
					<div id="username-help" class="form-text">3 to 100 letters, digits or . _ @ - characters. Used to sign in.</div>
				</div>
				</c:otherwise>
				</c:choose>
				<div class="row g-3 mb-3">
					<div class="col-sm-6">
						<label for="firstname" class="form-label">First name</label>
						<form:input path="firstname" cssClass="form-control" id="firstname" autocomplete="off"/>
					</div>
					<div class="col-sm-6">
						<label for="lastname" class="form-label">Last name</label>
						<form:input path="lastname" cssClass="form-control" id="lastname" autocomplete="off"/>
					</div>
				</div>
				<div class="row g-3 mb-3">
					<div class="col-sm-6">
						<label for="email" class="form-label">E-mail <span class="text-body-secondary">(optional)</span></label>
						<form:input path="email" type="email" cssClass="form-control" id="email" autocomplete="off"/>
					</div>
					<div class="col-sm-6">
						<label for="phone" class="form-label">Phone <span class="text-body-secondary">(optional)</span></label>
						<form:input path="phone" cssClass="form-control" id="phone" autocomplete="off"/>
					</div>
				</div>
				<div class="mb-3">
					<label for="designation" class="form-label">Designation <span class="text-body-secondary">(optional)</span></label>
					<form:input path="designation" cssClass="form-control" id="designation" autocomplete="off"/>
				</div>
				<div class="mb-4">
					<label for="${self ? 'roletext' : 'role'}" class="form-label">Role</label>
					<c:choose>
					<c:when test="${self}">
					<%-- A Super Admin can't change their own role (UserController) --%>
					<form:hidden path="role" id="rolevalue"/>
					<input type="text" class="form-control" id="roletext" value="<c:out value='${userinfo.rolelabel}'/>" readonly aria-describedby="role-help">
					<div id="role-help" class="form-text">You can't change your own role.</div>
					</c:when>
					<c:otherwise>
					<form:select path="role" cssClass="form-select" id="role">
						<form:option value="" label="Choose a role"/>
						<c:forEach items="${roles}" var="choice">
						<form:option value="${choice}" label="${choice.label}"/>
						</c:forEach>
					</form:select>
					</c:otherwise>
					</c:choose>
				</div>
				<c:if test="${not update}">
				<div class="row g-3 mb-4">
					<div class="col-sm-6">
						<label for="password" class="form-label">Temporary password</label>
						<input type="password" class="form-control" id="password" name="password" autocomplete="new-password" aria-describedby="password-help">
						<div id="password-help" class="form-text">At least 8 characters. Give it to the user; they must change it at first sign-in.</div>
					</div>
					<div class="col-sm-6">
						<label for="confirmpassword" class="form-label">Repeat the password</label>
						<input type="password" class="form-control" id="confirmpassword" name="confirmpassword" autocomplete="new-password">
					</div>
				</div>
				</c:if>
				<div class="d-flex gap-2">
					<button type="submit" class="btn btn-primary">Save</button>
					<a class="btn btn-outline-secondary" href="${listURL}">Cancel</a>
				</div>
			</form:form>
		</div>
	</div>

	<c:if test="${update and not self and not empty userinfo.username}">
	<div class="card rms-card rms-form-card mt-4">
		<div class="card-body p-4">
			<h2 class="h5">Reset password</h2>
			<p class="text-body-secondary">Sets a temporary password. The user must choose their own at their next page.</p>
			<c:if test="${passwordreset}">
			<div class="alert alert-success d-flex align-items-center gap-2" role="status">
				<svg class="rms-icon" aria-hidden="true"><use href="${icons}#check-circle-fill"/></svg>
				<span>The password was reset. Give the temporary password to the user.</span>
			</div>
			</c:if>
			<c:if test="${not empty passwordMessage}">
			<div class="alert alert-danger d-flex align-items-center gap-2" role="alert">
				<svg class="rms-icon" aria-hidden="true"><use href="${icons}#exclamation-triangle-fill"/></svg>
				<span><c:out value="${passwordMessage}"/></span>
			</div>
			</c:if>
			<form:form id="resetpassword" method="post" action="${resetURL}"
				data-rms-confirm="Reset this user's password? Their current password stops working.">
				<input type="hidden" name="userid" value="<c:out value='${userinfo.userid}'/>"/>
				<div class="row g-3 mb-3">
					<div class="col-sm-6">
						<label for="newpassword" class="form-label">Temporary password</label>
						<input type="password" class="form-control" id="newpassword" name="newpassword" autocomplete="new-password">
					</div>
					<div class="col-sm-6">
						<label for="confirmnewpassword" class="form-label">Repeat the password</label>
						<input type="password" class="form-control" id="confirmnewpassword" name="confirmpassword" autocomplete="new-password">
					</div>
				</div>
				<button type="submit" class="btn btn-outline-danger">Reset password</button>
			</form:form>
		</div>
	</div>
	</c:if>
</rms:layout>
