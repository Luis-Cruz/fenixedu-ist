<%--

    Copyright � 2018 Instituto Superior T�cnico

    This file is part of FenixEdu IST GIAF Invoices.

    FenixEdu IST GIAF Invoices is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu IST GIAF Invoices is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu IST GIAF Invoices.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@page import="pt.ist.fenixedu.domain.SapRequestType"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<% final String contextPath = request.getContextPath(); %>

<style>
<!--
.strikeLine {
    text-decoration: line-through;
}
-->
</style>

<div class="page-header">
	<h1>
		<spring:message code="title.invoice.viewer" text="Invoices"/>
	</h1>
</div>

<table id="invoiceTable" class="table tdmiddle">
	<thead>
		<tr>
			<th><spring:message code="label.id" text="ID"/></th>
			<th><spring:message code="label.event.description" text="Event"/></th>
            <th><spring:message code="label.value" text="Value"/></th>
            <th><spring:message code="label.document.type" text="Document Type"/></th>
            <th><spring:message code="label.document.number" text="Document Number"/></th>
            <th><spring:message code="label.document" text="Document"/></th>
		</tr>
	</thead>
	<tbody id="invoiceList">
	</tbody>
</table>

<script type="text/javascript">
	var sapRequests = ${sapRequests};
	var giafDocuments = ${giafDocuments};
	var contextPath = '<%= contextPath %>';

    function sapDocumentNumberPart(sapRequest) {
        if (sapRequest.sapDocumentNumber == null) {
            return '';
        } else {
            var docName = sapRequest.sapDocumentNumber.replace("\/", "_");
            var link = contextPath + '/invoice-downloader/sap/' + sapRequest.id + '/' + docName + '.pdf';
            return '<a href="' + link + '">' + sapRequest.sapDocumentNumber + '</a>';
        }
    }

    function giafDocumentNumberPart(giafDocument) {
        var link = contextPath + '/invoice-downloader/giaf/' + giafDocument.eventId + '/' + giafDocument.receiptNumber + '.pdf';
        return '<a href="' + link + '">' + entry.receiptNumber + '</a>';
    }

    function documentType(requestType) {
    	if (requestType == 'ADVANCEMENT') {
    		return '<spring:message code="label.document.type.ADVANCEMENT"/>';
    	}
    	if (requestType == 'CREDIT') {
            return '<spring:message code="label.document.type.CREDIT"/>';
        }
        if (requestType == 'DEBT') {
            return '<spring:message code="label.document.type.DEBT"/>';
        }
        if (requestType == 'DEBT_CREDIT') {
            return '<spring:message code="label.document.type.DEBT_CREDIT"/>';
        }
        if (requestType == 'INVOICE') {
            return '<spring:message code="label.document.type.INVOICE"/>';
        }
        if (requestType == 'INVOICE_INTEREST') {
            return '<spring:message code="label.document.type.INVOICE_INTEREST"/>';
        }
        if (requestType == 'PAYMENT') {
            return '<spring:message code="label.document.type.PAYMENT"/>';
        }
        if (requestType == 'PAYMENT_INTEREST') {
            return '<spring:message code="label.document.type.PAYMENT_INTEREST"/>';
        }
        if (requestType == 'REIMBURSEMENT') {
            return '<spring:message code="label.document.type.REIMBURSEMENT"/>';
        }
        if (requestType == 'ANNULMENT') {
            return '<spring:message code="label.document.type.ANNULMENT"/>';
        }
        if (requestType == 'FINE') {
            return '<spring:message code="label.document.type.FINE"/>';
        }
        return '';
    }

	$(document).ready(function() {
        $(giafDocuments).each(function(i, giafDocument) {
			row = $('<tr/>').appendTo($('#invoiceTable'))
                .append($('<td/>').text(giafDocument.invoiceNumber))
                .append($('<td/>').text(giafDocument.description))
                .append($('<td/>').text(giafDocument.value))
                .append($('<td/>').text('<spring:message code="label.document.type.INVOICE"/>'))
                .append($('<td/>').text(giafDocument.receiptId))
                .append($('<td/>').html(giafDocumentNumberPart(giafDocument)))
                ;
        });
        $(sapRequests).each(function(i, sapRequest) {
            rowStyle = sapRequest.isCanceled || sapRequest.ignore ? "text-decoration: line-through;" : "";
            row = $('<tr style="' + rowStyle + '"/>').appendTo($('#invoiceTable'))
                .append($('<td/>').text(sapRequest.id))
                .append($('<td/>').text(sapRequest.description))
                .append($('<td/>').text(sapRequest.value))
                .append($('<td/>').text(documentType(sapRequest.requestType)))
                .append($('<td/>').text(sapRequest.documentNumber))
                .append($('<td/>').html(sapDocumentNumberPart(sapRequest)))
                ;
        });
	});
</script>
