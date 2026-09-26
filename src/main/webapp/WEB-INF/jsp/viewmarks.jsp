<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="java.util.*"%>
<%@ page import="rms.model.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/css/bootstrap.min.css" integrity="sha384-HSMxcRTRxnN+Bdg0JdbxYKrThecOKuH5zCYotlSAcp1+c8xmyTe9GYg1l9a69psu" crossorigin="anonymous">
<script
	src="https://code.jquery.com/jquery-3.7.1.min.js" integrity="sha384-1H217gwSVyLSIfaLxHbE7dRb3v4mYCKbpQvzx0cegeju1MVsGrX5xXxAvs/HgeFs" crossorigin="anonymous"></script>
<script
	src="https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/js/bootstrap.min.js" integrity="sha384-aJ21OjlMXNL5UyIl/XNwTMqvzeRMZH2w8c5cRVpzpU8Y5bApTppSuUkhZXN0VxHd" crossorigin="anonymous"></script>
<script src="https://code.jquery.com/jquery-3.7.1.js" integrity="sha384-wsqsSADZR1YRBEZ4/kKHNSmU+aX8ojbnKUMN4RyD3jDkxw5mHtoe2z/T/n4l56U/" crossorigin="anonymous"></script>
<script
	src="https://cdn.datatables.net/1.13.11/js/jquery.dataTables.min.js" integrity="sha384-xbKh5PcHqYD2znaTJ+mPamIq8ERw8yRfp72NI8CGRg51FffJAXidmePtdkmCjEh9" crossorigin="anonymous"></script>
<script
	src="https://cdn.datatables.net/1.13.11/js/dataTables.bootstrap4.min.js" integrity="sha384-vCX+UFRnh1Gp0hr9dL82snXI1HvdBaApGHMjbewoGQ69VkYcHt9jvTy+Q4CAWwPX" crossorigin="anonymous"></script>
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