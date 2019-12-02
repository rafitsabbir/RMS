<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="java.util.*"%>
<%@ page import="rms.model.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet"
	href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css">
<script
	src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
<script
	src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
<script src="https://code.jquery.com/jquery-3.3.1.js"></script>
<script
	src="https://cdn.datatables.net/1.10.19/js/jquery.dataTables.min.js"></script>
<script
	src="https://cdn.datatables.net/1.10.19/js/dataTables.bootstrap4.min.js"></script>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title></title>
<%
    @SuppressWarnings("unchecked")
	List<MarksInfo>  markslist =  (ArrayList<MarksInfo>) request.getAttribute("markslist");
	MarksInfo marksinfo = null;
	
%>
<%!
public int getFullmarks(MarksInfo marksinfo) {
	// TODO Auto-generated method stub
	return marksinfo.getWorkexp() + marksinfo.getTechknowledge()
			+ marksinfo.getLeadership() + marksinfo.getDecision()
			+ marksinfo.getProbsolving() + marksinfo.getStress()
			+ marksinfo.getEducation() + marksinfo.getComskill()
			+ marksinfo.getAttitude() + marksinfo.getPersonality();
}
%>
</head>
<body>
	<script>
		$(document).ready(function() {
			$('#markstable').DataTable();
		});
	</script>

	<table id="markstable" class="table table-striped table-bordered"
		style="width: 100%">
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
			<%
				if (markslist != null) {
					for (int i = 0; i < markslist.size(); i++) {
						marksinfo = (MarksInfo) markslist.get(i);
			%>
			<tr>
				<td><%=marksinfo.getCandidateid()%></td>
				<td><%=marksinfo.getPosition()%></td>
				<td><%=marksinfo.getLanguage()%></td>
				<td><%=marksinfo.getWorkexp()%></td>
				<td><%=marksinfo.getTechknowledge()%></td>
				<td><%=marksinfo.getLeadership()%></td>
				<td><%=marksinfo.getDecision()%></td>
				<td><%=marksinfo.getProbsolving()%></td>
				<td><%=marksinfo.getStress()%></td>
				<td><%=marksinfo.getEducation()%></td>
				<td><%=marksinfo.getComskill()%></td>
				<td><%=marksinfo.getAttitude()%></td>
				<td><%=marksinfo.getPersonality()%></td>
				<td><%=getFullmarks(marksinfo)%></td>
				<td>
				<%if(marksinfo.getCandidateStatus().equalsIgnoreCase("S")){%>
				<img alt="" src="resources/happy.jpg" style="width:100px; height100px;"  align="center">
				<%}else if(marksinfo.getCandidateStatus().equalsIgnoreCase("R")){ %>
				<img alt="" src="resources/sad.jpg" style="width:100px; height100px;"  align="center">
				<%}else{ %>
				<img alt="" src="resources/new.jpg" style="width:100px; height100px;"  align="center">
				<%} %>
				</td>
			</tr>
			<%
				}
				}
			%>
		</tbody>
	</table>
</body>
</html>