<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<%-- GET /adminviewmarks (MarksController): one row per active candidate with the averages of their evaluations --%>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<rms:layout title="Candidate Status" active="marks" tables="true" stylesheet="viewmarks.css">
	<div class="rms-page-header">
		<div>
			<h1>Candidate Status</h1>
			<p>The average of each criterion over all of a candidate's evaluations (each scored 1 to 10), the total of the averages (at most 100), and the decision. Open a name for each interviewer's scores and comments.</p>
		</div>
	</div>
	<div class="card rms-card rms-results-card">
		<div class="card-body">
			<table id="markstable" class="table table-sm table-striped table-hover align-middle w-100 results-table" data-rms-table>
				<thead>
					<tr>
						<th>Candidate Name</th>
						<th>Position</th>
						<th>Language</th>
						<th>Evaluations</th>
						<c:forEach items="${criteria}" var="criterion">
						<th><c:out value="${criterion.label}"/></th>
						</c:forEach>
						<th>Total Score</th>
						<th>Status</th>
					</tr>
				</thead>
				<tbody>
				<c:forEach items="${resultlist}" var="result" varStatus="row">
					<tr>
						<c:url value="/viewevaluations" var="detailURL"><c:param name="candidateid" value="${result.candidateid}" /></c:url>
						<td data-order="${row.index}"><a href="<c:out value='${detailURL}'/>"><c:out value="${result.firstname} ${result.lastname}"/></a></td>
						<td><c:out value="${result.positionname}"/></td>
						<td><c:out value="${result.languagename}"/></td>
						<td>${result.evaluations}</td>
						<c:forEach items="${result.roundedaverages}" var="average">
						<td>${empty average ? '-' : average}</td>
						</c:forEach>
						<td class="fw-semibold">${empty result.total ? '-' : result.total}</td>
						<td><rms:status value="${result.candidatestatus}" icons="${icons}"/></td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
		</div>
	</div>
</rms:layout>
