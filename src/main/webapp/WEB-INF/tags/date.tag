<%@ tag pageEncoding="UTF-8" body-content="empty" description="Affiche une LocalDate / LocalDateTime au format jj/mm/aaaa" %>
<%@ attribute name="value" required="true" type="java.lang.Object" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Fonctionne avec java.time.LocalDate et LocalDateTime (getDayOfMonth, getMonthValue, getYear). --%>
<c:choose><c:when test="${empty value}">—</c:when><c:otherwise>${value.dayOfMonth lt 10 ? '0' : ''}${value.dayOfMonth}/${value.monthValue lt 10 ? '0' : ''}${value.monthValue}/${value.year}</c:otherwise></c:choose>
