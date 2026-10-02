<%@ tag language="java" pageEncoding="ISO-8859-1" body-content="empty" trimDirectiveWhitespaces="true"
	description="The decision badge: S Selected, R Rejected, H On hold, anything else (NULL too) Pending (rms.model.DecisionStatus)" %>
<%@ attribute name="value" required="false" description="The stored candidatestatus, in any case" %>
<%@ attribute name="icons" required="true" description="The icon sprite URL" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.functions" prefix="fn" %>
<c:set var="code" value="${fn:toUpperCase(fn:trim(value))}" />
<c:choose>
	<c:when test="${code eq 'S'}"><span class="badge rounded-pill text-bg-success rms-status"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#check-circle-fill"/></svg>Selected</span></c:when>
	<c:when test="${code eq 'R'}"><span class="badge rounded-pill text-bg-danger rms-status"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#x-circle-fill"/></svg>Rejected</span></c:when>
	<c:when test="${code eq 'H'}"><span class="badge rounded-pill text-bg-warning rms-status"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#clock"/></svg>On hold</span></c:when>
	<c:otherwise><span class="badge rounded-pill text-bg-secondary rms-status"><svg class="rms-icon" aria-hidden="true"><use href="${icons}#hourglass-split"/></svg>Pending</span></c:otherwise>
</c:choose>
