<%@ tag pageEncoding="UTF-8" body-content="empty" description="Affiche un montant en euros (ex. 1 450,00 €)" %>
<%@ attribute name="value" required="true" type="java.lang.Object" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="fr_FR" /><fmt:formatNumber value="${value}" type="currency" currencyCode="EUR" />
