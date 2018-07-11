<%--

    Copyright © 2018 Instituto Superior Técnico

    This file is part of FenixEdu IST Integration.

    FenixEdu IST Integration is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu IST Integration is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@page import="org.fenixedu.bennu.core.domain.User"%>
<%@page import="org.fenixedu.bennu.core.security.Authenticate"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
    final String contextPath = request.getContextPath();
    final User user = Authenticate.getUser();
%>

<style>
.glyphicon {
    font-size: 25px;
}
.panel-body {
    font-size: 16px;
    font-weight: 300;
}
.panel form {
    margin-left: 25px;
    margin-right: 25px;
}
form {
    margin-top: 10px;
}
</style>

<div class="page-header">
	<h1>
		<spring:message code="privacy.title" />
	</h1>
</div>

<div class="page-body">

    <div class="panel panel-default">
        <div class="panel-heading"><span class="glyphicon glyphicon-info-sign"> <spring:message code="privacy.information" text="Information"/></span></div>
        <div class="panel-body">
            <spring:message code="privacy.information.details" text="Information Details" arguments="<a href='https://tecnico.ulisboa.pt/'>https://tecnico.ulisboa.pt/</a>"/>
        </div>
    </div>

    <div class="panel panel-default">
        <div class="panel-heading"><span class="glyphicon glyphicon-user"> <spring:message code="privacy.photo" text="Photo Options"/></span></div>
        <div class="panel-body">
            <spring:message code="privacy.photo.details" text="Photo Details" />
            <form action="<%= contextPath %>/privacy/updatePhotoPreference" method="POST">
                ${csrf.field()}
                <% if (user.getPerson().getPhotoAvailable()) { %>
                    <div id="photoIsPublic">
                        <div class="form-group row">
                            <spring:message code="privacy.photo.is.public" text="Your photo is publically available." />
                        </div>
                        <div class="form-group row">
                            <button type="submit" class="btn btn-default"><spring:message code="privacy.photo.hide" text="Hide Photo" /></button>
                        </div>
                    </div>
                <% } else { %>
                    <div id="photoIsPrivate">
                        <div class="form-group row">
                            <spring:message code="privacy.photo.is.private" text="Your photo is only available to logged users." />
                        </div>
                        <div class="form-group row">
                            <button type="submit" class="btn btn-default"><spring:message code="privacy.photo.show" text="Show Photo" /></button>
                        </div>
                    </div>
                <% } %>
            </form>
        </div>
    </div>

    <div class="panel panel-default">
        <div class="panel-heading"><span class="glyphicon glyphicon-earphone"> <spring:message code="privacy.contacts" text="Contact Options"/></span></div>
        <div class="panel-body"><spring:message code="privacy.contacts.details" text="Contact Details" /></div>
    </div>

    <div class="panel panel-default">
        <div class="panel-heading"><span class="glyphicon glyphicon-envelope"> <spring:message code="privacy.email" text="E-mail Options"/></span></div>
        <div class="panel-body"><spring:message code="privacy.email.details" text="E-mail Option Details" /></div>
    </div>

    <div class="panel panel-default">
        <div class="panel-heading"><span class="glyphicon glyphicon-credit-card"> <spring:message code="privacy.bank.card.santander" text="Cartão do Técnico / Santander"/></span></div>
        <div class="panel-body"><spring:message code="privacy.bank.card.santander" text="Cartão do Técnico / Santander Details" /></div>
    </div>

    <div class="panel panel-default">
        <div class="panel-heading"><span class="glyphicon glyphicon-credit-card"> <spring:message code="privacy.bank.card.santander" text="Cartão ULisboa / SAS / CGD"/></span></div>
        <div class="panel-body"><spring:message code="privacy.bank.card.santander" text="Cartão ULisboa / SAS / CGD Details" /></div>
    </div>

    <div class="panel panel-default">
        <div class="panel-heading"><span class="glyphicon glyphicon-credit-card"> <spring:message code="privacy.bank.card.santander" text="Cartão da Associação de Estudantes / BPI"/></span></div>
        <div class="panel-body"><spring:message code="privacy.bank.card.santander" text="Cartão da Associação de Estudantes / BPI Details" /></div>
    </div>

    <div class="panel panel-default">
        <div class="panel-heading"><span class="glyphicon glyphicon-list-alt"> <spring:message code="privacy.logs" text="Logs"/></span></div>
        <div class="panel-body"><spring:message code="privacy.logs.details" text="Logs Details" /></div>
    </div>

    
</div>
