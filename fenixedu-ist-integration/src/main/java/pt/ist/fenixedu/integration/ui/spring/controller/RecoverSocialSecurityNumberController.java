/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Integration.
 *
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.integration.ui.spring.controller;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.rest.JsonAwareResource;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import pt.ist.fenixframework.Atomic;

@SpringApplication(path = "recoverSocialSecurityNumber", hint = "Person", group = "#missingSocialSecurityNumber",
        title = "label.recover.social.security.number")
@SpringFunctionality(app = RecoverSocialSecurityNumberController.class, title = "title.recover.social.security.number")
@RequestMapping("/recoverSocialSecurityNumber")
public class RecoverSocialSecurityNumberController extends JsonAwareResource {

    @RequestMapping(method = RequestMethod.GET)
    public String show(final Model model) {
        final User user = Authenticate.getUser();
        final Person person = user.getPerson();
        final String vatNumber = person.getSocialSecurityNumber();
        return vatNumber != null && isVatValid(vatNumber) ? "fenixedu-ist-integration/validSocialSecurityNumber" : "fenixedu-ist-integration/recoverSocialSecurityNumber";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String save(@RequestParam final String vatNumber, final Model model) {
        if (isVatValid(vatNumber)) {
            setVatNumber(vatNumber);
        }
        return "redirect:/recoverSocialSecurityNumber";
    }

    @Atomic
    private void setVatNumber(final String vatNumber) {
        final User user = Authenticate.getUser();
        final Person person = user.getPerson();
        person.setSocialSecurityNumber(vatNumber);
    }

    private boolean isVatValid(final String vat) {
        if (vat.length() != 9) {
            return false;
        }
        for (int i = 0; i < 9; i++) {
            if (!Character.isDigit(vat.charAt(i))) {
                return false;
            }
        }
        if (Integer.parseInt(vat) <= 0) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            final int c = Character.getNumericValue(vat.charAt(i));
            sum += c * (9 - i);
        }
        final int controleDigit = Character.getNumericValue(vat.charAt(8));
        final int remainder = sum % 11;
        int digit = 11 - remainder;
        return digit > 9 ? controleDigit == 0 : digit == controleDigit;
    }

}
