<%@ tag language="java" pageEncoding="ISO-8859-1" body-content="empty" trimDirectiveWhitespaces="true"
	description="Badge for an interview's status: Scheduled, Done or Cancelled" %>
<%@ attribute name="value" required="false" description="SCHEDULED, DONE or CANCELLED" %>
<%@ attribute name="icons" required="true" description="The icon sprite URL" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>
<c:choose>
	<c:when test="${fn:toUpperCase(value) eq 'DONE'}">
		<span class="badge rounded-pill text-bg-success rms-status"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#check-circle-fill"/></svg>Done</span>
	</c:when>
	<c:when test="${fn:toUpperCase(value) eq 'CANCELLED'}">
		<span class="badge rounded-pill text-bg-secondary rms-status"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#x-circle-fill"/></svg>Cancelled</span>
	</c:when>
	<c:otherwise>
		<span class="badge rounded-pill text-bg-primary rms-status"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#clock"/></svg>Scheduled</span>
	</c:otherwise>
</c:choose>
