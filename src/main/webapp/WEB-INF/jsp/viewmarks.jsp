<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false" trimDirectiveWhitespaces="true"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>
<%@ taglib prefix="rms" tagdir="/WEB-INF/tags" %>
<spring:url value="/" var="base" htmlEscape="true" />
<c:set var="icons" value="${base}resources/img/icons.svg" />
<rms:layout title="Candidate Status" active="marks" tables="true" stylesheet="viewmarks.css">
	<div class="rms-page-header">
		<div>
			<h1>Candidate Status</h1>
			<p>Scores on 10 criteria, with each candidate's total and selection status.</p>
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
						<th>Work Experience</th>
						<th>Technical Knowledge</th>
						<th>Leadership Skill</th>
						<th>Decision Making</th>
						<th>Problem Solving Skill</th>
						<th>Stress Tolerance</th>
						<th>Educational Background</th>
						<th>Communication Skill</th>
						<th>Attitude</th>
						<th>Personality</th>
						<th>Total Score</th>
						<th>Status</th>
					</tr>
				</thead>
				<tbody>
				<c:forEach items="${markslist}" var="marks">
					<%-- The DAO puts the candidate's name in candidateid (G10) --%>
					<tr>
						<td><c:out value="${marks.candidateid}"/></td>
						<td><c:out value="${marks.position}"/></td>
						<td><c:out value="${marks.language}"/></td>
						<td>${marks.workexp}</td>
						<td>${marks.techknowledge}</td>
						<td>${marks.leadership}</td>
						<td>${marks.decision}</td>
						<td>${marks.probsolving}</td>
						<td>${marks.stress}</td>
						<td>${marks.education}</td>
						<td>${marks.comskill}</td>
						<td>${marks.attitude}</td>
						<td>${marks.personality}</td>
						<td class="fw-semibold">${marks.workexp + marks.techknowledge + marks.leadership + marks.decision + marks.probsolving + marks.stress + marks.education + marks.comskill + marks.attitude + marks.personality}</td>
						<td>
						<c:choose>
							<c:when test="${fn:toUpperCase(marks.candidateStatus) eq 'S'}">
								<span class="badge rounded-pill text-bg-success rms-status"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#check-circle-fill"/></svg>Selected</span>
							</c:when>
							<c:when test="${fn:toUpperCase(marks.candidateStatus) eq 'R'}">
								<span class="badge rounded-pill text-bg-danger rms-status"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#x-circle-fill"/></svg>Rejected</span>
							</c:when>
							<c:otherwise>
								<span class="badge rounded-pill text-bg-secondary rms-status"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#hourglass-split"/></svg>Pending</span>
							</c:otherwise>
						</c:choose>
						</td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
		</div>
	</div>
</rms:layout>
