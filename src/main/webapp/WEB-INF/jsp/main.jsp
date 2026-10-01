<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!------ Include the above in your HEAD tag ---------->
<%@ page import="rms.model.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<spring:url value="/resources/css/main.css" var="maincss" />
<spring:url value="/adminviewmarks" var="adminviewmarks" htmlEscape="true" javaScriptEscape="true" />

<spring:url value="/createlanguage" var="createlanguage" htmlEscape="true" javaScriptEscape="true" />
<spring:url value="/viewlanguagelist" var="viewlanguagelist" htmlEscape="true" javaScriptEscape="true" />

<spring:url value="/createposition" var="createposition" htmlEscape="true" javaScriptEscape="true" />
<spring:url value="/viewpositionlist" var="viewpositionlist" htmlEscape="true" javaScriptEscape="true" />

<spring:url value="/createcandidate" var="createcandidate" htmlEscape="true" javaScriptEscape="true" />
<spring:url value="/viewcandidatelist" var="viewcandidatelist" htmlEscape="true" javaScriptEscape="true" />

<link rel="stylesheet" type="text/css" href="${maincss }">
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet"
	href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css" integrity="sha384-wvfXpqpZZVQGK6TAh5PVlGOfQNHSoD2xbE+QkPxCAFlNEevoEH3Sl0sibVcOQVnN" crossorigin="anonymous">
<title>Welcome to RMS</title>

</head>
<%
	UserInfo userinfo = null;
	userinfo = (UserInfo) request.getAttribute("userinfo");
	if (userinfo != null) {
%>


<body>

	<div class="sidenav">
		
		 <% if ("N".equalsIgnoreCase(userinfo.getIsinterviewer())){ %>
		 <a onclick="return load_view_admin_marks();">Candidate Status</a>
		<button class="dropdown-btn">
			Create <i class="fa fa-caret-down"></i>
		</button>
		<div class="dropdown-container">
			<a onclick="return load_language();">Language</a> 
			<a onclick="return load_position();">Position</a> 
			<a onclick="return load_candidate();">Candidate</a>
			<a onclick="return load_interviewer();">Interviewer</a>
			<a onclick="return load_schedule();">Interview Schedule</a>
			<a onclick="return load_job();">Job</a>
		</div>
		
		<button class="dropdown-btn">
			View <i class="fa fa-caret-down"></i>
		</button>
		<div class="dropdown-container">
			<a onclick="return load_view_language();">Language</a> 
			<a onclick="return load_view_position();">Position</a> 
			<a onclick="return load_view_candidate();">Candidate</a>
			<a onclick="return load_view_interviewer();">Interviewer</a>
			<a onclick="return load_view_schedule();">Interview Schedule</a>
			<a onclick="return load_view_job();">Job</a>
		</div>
		<%} if ("Y".equalsIgnoreCase(userinfo.getIsinterviewer())){ %>
		<a onclick="return load_marks();">Evaluation</a>
		<a onclick="return load_view_marks();">Show Evaluation</a>
		<%} %>
		<a href="login">Logout</a>
		
	</div>
	<div class="header" id="header">
		<img alt="" src="resources/login.jpg"
			style="margin-left: -1170px; position: absolute;" width="65%"
			height="100%" align="left">
		<table width="100%" height="100%">
			<tbody>
				<tr>
					<td>Name : <c:out value="<%=userinfo.getFirstname()%>"/> <c:out value="<%=userinfo.getLastname()%>"/></td>
				</tr>
				<tr>
					<td>Role : <c:out value="<%=userinfo.getDesignation()%>"/></td>
				</tr>
				<tr>
					<td>E-mail : <c:out value="<%=userinfo.getEmail()%>"/>
					</td>
				</tr>
			</tbody>
		</table>
	</div>

	<div class="main" id="container" style="height: 500px;">
		
	</div>

	<script>
		/* Loop through all dropdown buttons to toggle between hiding and showing its dropdown content - This allows the user to have multiple dropdowns without any conflict */
		var dropdown = document.getElementsByClassName("dropdown-btn");
		var i;

		for (i = 0; i < dropdown.length; i++) {
			dropdown[i].addEventListener("click", function() {
				this.classList.toggle("active");
				var dropdownContent = this.nextElementSibling;
				if (dropdownContent.style.display === "block") {
					dropdownContent.style.display = "none";
				} else {
					dropdownContent.style.display = "block";
				}
			});
		}

		function load_language() {
			document.getElementById("container").innerHTML = '<object type="text/html" data="${createlanguage}" ></object>';
		}
		function load_view_language() {
			document.getElementById("container").innerHTML = '<object type="text/html" data="${viewlanguagelist}" ></object>';
		}
		function load_position() {
			document.getElementById("container").innerHTML = '<object type="text/html" data="${createposition}" ></object>';
		}
		function load_view_position() {
			document.getElementById("container").innerHTML = '<object type="text/html" data="${viewpositionlist}" ></object>';
		}
		function load_candidate() {
			document.getElementById("container").innerHTML = '<object type="text/html" data="${createcandidate}" ></object>';
		}
		function load_view_candidate() {
			document.getElementById("container").innerHTML = '<object type="text/html" data="${viewcandidatelist}" ></object>';
		}
		function load_interviewer() {
			document.getElementById("container").innerHTML = '<object type="text/html" data="InterviewerController" ></object>';
		}
		function load_view_interviewer() {
			document.getElementById("container").innerHTML = '<object type="text/html" data="InterviewerController?param=VIEW" ></object>';
		}
		function load_schedule() {
			document.getElementById("container").innerHTML = '<object type="text/html" data="ScheduleController" ></object>';
		}
		function load_view_schedule() {
			document.getElementById("container").innerHTML = '<object type="text/html" data="ScheduleController?param=VIEW" ></object>';
		}
		function load_marks() {
			document.getElementById("container").innerHTML = '<object type="text/html" data="MarksController" ></object>';
		}
		function load_view_marks() {
			document.getElementById("container").innerHTML = '<object type="text/html" data="MarksController?param=VIEW" ></object>';
		}
		function load_view_admin_marks() {
			document.getElementById("container").innerHTML = '<object type="text/html" data="${adminviewmarks}" ></object>';
		}
		function load_job() {
			document.getElementById("container").innerHTML = '<object type="text/html" data="JobController" ></object>';
		}
		function load_view_job() {
			document.getElementById("container").innerHTML = '<object type="text/html" data="JobController?param=VIEW" ></object>';
		}
	</script>

</body>

<%
	} else {
		response.sendRedirect(request.getContextPath() + "/login");
	}
%>
</html>