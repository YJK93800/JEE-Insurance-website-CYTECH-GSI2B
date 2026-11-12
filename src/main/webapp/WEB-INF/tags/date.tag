<%@ tag pageEncoding="UTF-8" body-content="empty" description="Affiche une LocalDate / LocalDateTime au format jj/mm/aaaa" %>
<%@ attribute name="valeur" required="true" type="java.lang.Object" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Fonctionne avec java.time.LocalDate et LocalDateTime (getDayOfMonth, getMonthValue, getYear). --%>
<c:choose><c:when test="${empty valeur}">—</c:when><c:otherwise>${valeur.dayOfMonth lt 10 ? '0' : ''}${valeur.dayOfMonth}/${valeur.monthValue lt 10 ? '0' : ''}${valeur.monthValue}/${valeur.year}</c:otherwise></c:choose>
