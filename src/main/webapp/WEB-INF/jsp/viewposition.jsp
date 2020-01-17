<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1" isELIgnored="false"%>
	<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
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

</head>
<body>
	<script>
		$(document).ready(function() {
			$('#positiontable').DataTable();
		});
	</script>

	<table id="positiontable" class="table table-striped table-bordered"
		style="width: 100%">
		<thead>
			<tr>
				<th>Position Id</th>
				<th>Position Name</th>
				<th>Update </th>
				<th>Delete </th>
			</tr>
		</thead>
		<tbody>
		<c:forEach items="${positionlist}" var="positionlist"> 
			<tr>
				<td>${positionlist.positionkey } </td>
				<td>${positionlist.positionname } </td>
				<td>
					<spring:url value="/updateposition/${positionlist.positionkey }" var="updateURL" />
					<a href="${updateURL}">Update</a>			
				</td>
				<td>
					<spring:url value="/deleteposition/${positionlist.positionkey }" var="deleteURL" />
					<a href="${deleteURL}">Delete</a>			
				</td>
			</tr>
		</c:forEach>
		</tbody>
	</table>
</body>
</html>