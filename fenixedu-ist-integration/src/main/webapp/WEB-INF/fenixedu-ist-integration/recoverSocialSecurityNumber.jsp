<%--

    Copyright © 2013 Instituto Superior Técnico

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
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>

<h2>
	<spring:message code="title.recover.social.security.number"/>
</h2>

<div class="alert well well-sm">
<spring:message code="label.social.security.number.recovery.instructions"/>
</div>

<form method="post">
	<div class="form-group">
		<div class="roe">
			<div class="col-md-2">
				<label for="vatNumber">
					<spring:message code="label.social.security.number"/>:
				</label>
			</div>
			<div class="col-md-2">
				<input class="form-control" id="vatNumber" name="vatNumber" type="number" value="" required="required" oninput="checkit();"/>
			</div>
		</div>
	</div>
	<div>
		<button type="submit" class="btn btn-default" id="save" disabled="disabled">
			<spring:message code="label.save"/>
		</button>
	</div>
</form>
<script type="text/javascript">

	function checkit() {
		var vatNumber = document.getElementById('vatNumber').value;
		var button = document.getElementById('save');
		if (isNumeroContribuinte(vatNumber)) {
			button.disabled = false;
			button.className = "btn btn-success";
		} else {
			button.disabled = true;
			button.className = "btn btn-default";
		}
		return true;
	};

	function isNumeroContribuinte(value) {
		if ( value.length != 9 ) {
			return false;
		}
		if( value != parseInt(value, 10) ) {
			return false;
		}
		if( value <= 0 ) {
			return false;
		}

		var soma, resto, digi, i;
		var nif = new Array(9);
		for (i = 0; i < 9; i++) {
			nif[i] = Number(value.substring(i, i + 1));
		}
		for (i = 0, soma = 0; i < 8; i++) {
			soma += nif[i] * (9 - i);
		}
		resto = soma % 11;
   		digi = 11 - resto;
		if ( digi > 9 ) {
			digi = 0;
		}
		return (digi === nif[8]);
	}
</script>