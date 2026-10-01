<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" isELIgnored="false"%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ page import="java.util.*"%>
<%@ page import="rms.model.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet"
	href="https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/css/bootstrap.min.css" integrity="sha384-HSMxcRTRxnN+Bdg0JdbxYKrThecOKuH5zCYotlSAcp1+c8xmyTe9GYg1l9a69psu" crossorigin="anonymous">
<link rel="stylesheet"
	href="https://cdn.datatables.net/1.13.11/css/dataTables.bootstrap.min.css" integrity="sha384-l2WpiBvV3jxT9soajqlYVZkCyShrAMkhYlR3YpVxutac8q2X3W7p5kDvjkQgyM+Y" crossorigin="anonymous">
<spring:url value="/resources/css/datatables.css" var="datatablescss" />
<link rel="stylesheet" type="text/css" href="${datatablescss}"/>
<script
	src="https://code.jquery.com/jquery-3.7.1.min.js" integrity="sha384-1H217gwSVyLSIfaLxHbE7dRb3v4mYCKbpQvzx0cegeju1MVsGrX5xXxAvs/HgeFs" crossorigin="anonymous"></script>
<script
	src="https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/js/bootstrap.min.js" integrity="sha384-aJ21OjlMXNL5UyIl/XNwTMqvzeRMZH2w8c5cRVpzpU8Y5bApTppSuUkhZXN0VxHd" crossorigin="anonymous"></script>
<script
	src="https://cdn.datatables.net/1.13.11/js/jquery.dataTables.min.js" integrity="sha384-xbKh5PcHqYD2znaTJ+mPamIq8ERw8yRfp72NI8CGRg51FffJAXidmePtdkmCjEh9" crossorigin="anonymous"></script>
<script
	src="https://cdn.datatables.net/1.13.11/js/dataTables.bootstrap.min.js" integrity="sha384-xX2rLG/IDoD8nMCCawO1tSmnmivygPR0hHih92wcA9NqItz/WQBRYL3LcGloEQnU" crossorigin="anonymous"></script>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title></title>

</head>
<body>
	<script>
		$(document).ready(function() {
			$('#languagetable').DataTable();
		});
	</script>

	<table id="languagetable" class="table table-striped table-bordered"
		style="width: 100%">
		<thead>
			<tr>
				<th>Language Id</th>
				<th>Language Name</th>
				<th>Update </th>
				<th>Delete </th>
			</tr>
		</thead>
		<tbody>
		<c:forEach items="${languagelist}" var="languagelist"> 
			<tr>
				<td>${languagelist.languagekey } </td>
				<td><c:out value="${languagelist.languagename }"/> </td>
				<td>
					<spring:url value="/updatelanguage/${languagelist.languagekey }" var="updateURL" />
					<a href="${updateURL}">Update</a>			
				</td>
				<td>
					<spring:url value="/deletelanguage/${languagelist.languagekey }" var="deleteURL" />
					<form method="post" action="${deleteURL}" style="margin: 0;">
						<button type="submit" class="btn btn-link" style="padding: 0;">Delete</button>
					</form>
				</td>
			</tr>
		</c:forEach>			
		</tbody>
	</table>
</body>
</html>