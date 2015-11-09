package pt.ist.fenixedu.giaf.invoices;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.AcademicEvent;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.PaymentMode;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeAndInsuranceEvent;
import org.fenixedu.academic.domain.accounting.events.AnnualEvent;
import org.fenixedu.academic.domain.accounting.events.ImprovementOfApprovedEnrolmentEvent;
import org.fenixedu.academic.domain.accounting.events.candidacy.IndividualCandidacyEvent;
import org.fenixedu.academic.domain.accounting.events.dfa.DFACandidacyEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEventWithPaymentPlan;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.phd.debts.PhdEvent;
import org.fenixedu.academic.domain.phd.debts.PhdGratuityEvent;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import pt.ist.fenixframework.DomainObject;

public class Utils {

    public static boolean validate(final ErrorConsumer<AccountingTransactionDetail> consumer, final AccountingTransactionDetail detail) {
        final AccountingTransaction transaction = detail.getTransaction();
        final Event event = transaction.getEvent();
        final String eventDescription;
        try {
            eventDescription = event.getDescription().toString();
        } catch (final NullPointerException ex) {
            consumer.accept(detail, "No Description Available", ex.getMessage());
            return false;
        }
        try {
            transaction.getAmountWithAdjustment().getAmount();
        } catch (final DomainException ex) {
            consumer.accept(detail, "Unable to Determine Amount", ex.getMessage());
            return false;
        }
        final String articleCode = Utils.mapToArticleCode(event, eventDescription);
        if (articleCode == null) {
            if (eventDescription.indexOf("Pagamento da resid") != 0) {
                consumer.accept(detail, "No Article Code", eventDescription);
            }
            return false;
        }

        final Person person = event.getPerson();
        final Country country = person.getCountry();
        if (event.getParty().isPerson()) {
            if (country == null) {
                consumer.accept(detail, "No Country", person.getUsername());
                return false;
            }
            final PhysicalAddress address = Utils.toAddress(person);
            if (address == null) {
                consumer.accept(detail, "No Address", person.getUsername());
                return false;
            }
            final Country countryOfAddress = address.getCountryOfResidence();
            if (countryOfAddress == null) {
                consumer.accept(detail, "No Valid Country for Address", person.getUsername());
                return false;
            } else if ("PT".equals(countryOfAddress.getCode()) || "PT".equals(country.getCode())) {
                if (!Utils.isValidPostCode(address.getAreaCode())) {
                    consumer.accept(detail, "No Valid Post Code For Address For", person.getUsername());
                    return false;
                }
            }

            final String vat = Utils.toVatNumber(person);
            if (vat == null) {
                consumer.accept(detail, "No VAT Number", person.getUsername());
                return false;
            }
            if ("PT".equals(country.getCode())) {
                if (!Utils.isVatValidForPT(vat)) {
                    consumer.accept(detail, "No a Valid PT VAT Number", vat);
                    return false;
                }
            }
        } else {
            consumer.accept(detail, "Not a person", event.getParty().toString());
            return false;
        }
        final BigDecimal amount = transaction.getAmountWithAdjustment().getAmount();
        final AccountingTransaction adjustedTransaction = transaction.getAdjustedTransaction();
        if (adjustedTransaction != null && adjustedTransaction.getAmountWithAdjustment().getAmount().signum() <= 0) {
            // consumer.accept(detail, "Ignore Adjusting Transaction", detail.getExternalId());
            return false;
        }
        if (amount.signum() <= 0) {
            if (event.isCancelled()) {
                // consumer.accept(detail, "Canceled Transaction", detail.getExternalId());
                return false;
            } else {
                if (transaction.getAdjustmentTransactionsSet().isEmpty()) {
                    consumer.accept(detail, "Zero Value For Transaction", detail.getExternalId());
                    return false;
                } else {
                    // consumer.accept(detail, "Ignore Adjustment Transaction", detail.getExternalId());
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean validate(final ErrorConsumer<Event> consumer, final Event event) {
        final String eventDescription;
        try {
            eventDescription = event.getDescription().toString();
        } catch (final NullPointerException ex) {
            consumer.accept(event, "No Description Available", ex.getMessage());
            return false;
        }
        try {
            event.getOriginalAmountToPay();
        } catch (final DomainException ex) {
            consumer.accept(event, "Unable to Determine Amount", ex.getMessage());
            return false;
        } catch (final NullPointerException ex) {
            consumer.accept(event, "Unable to Determine Amount", ex.getMessage());
            return false;            
        }
        final String articleCode = Utils.mapToArticleCode(event, eventDescription);
        if (articleCode == null) {
            if (eventDescription.indexOf("Pagamento da resid") != 0) {
                consumer.accept(event, "No Article Code", eventDescription);
            }
            return false;
        }

        final Person person = event.getPerson();
        final Country country = person.getCountry();
        if (event.getParty().isPerson()) {
            if (country == null) {
                consumer.accept(event, "No Country", person.getUsername());
                return false;
            }
            final PhysicalAddress address = Utils.toAddress(person);
            if (address == null) {
                consumer.accept(event, "No Address", person.getUsername());
                return false;
            }
            final Country countryOfAddress = address.getCountryOfResidence();
            if (countryOfAddress == null) {
                consumer.accept(event, "No Valid Country for Address", person.getUsername());
                return false;
            } else if ("PT".equals(countryOfAddress.getCode()) || "PT".equals(country.getCode())) {
                if (!Utils.isValidPostCode(address.getAreaCode())) {
                    consumer.accept(event, "No Valid Post Code For Address For", person.getUsername());
                    return false;
                }
            }

            final String vat = Utils.toVatNumber(person);
            if (vat == null) {
                consumer.accept(event, "No VAT Number", person.getUsername());
                return false;
            }
            if ("PT".equals(country.getCode())) {
                if (!Utils.isVatValidForPT(vat)) {
                    consumer.accept(event, "No a Valid PT VAT Number", vat);
                    return false;
                }
            }
        } else {
            consumer.accept(event, "Not a person", event.getParty().toString());
            return false;
        }
        final BigDecimal amount = event.getOriginalAmountToPay().getAmount();
        if (amount.signum() <= 0) {
            if (event.isCancelled()) {
                // consumer.accept(detail, "Canceled Transaction", detail.getExternalId());
                return false;
            } else {
                consumer.accept(event, "Zero Value For Transaction", event.getExternalId());
                return false;
            }
        }
        return true;
    }

    public static JsonObject toJson(final Person person) throws IOException {
        final String clientCode = toClientCode(person);

        final String vat = toVatNumber(person);
        final String vatCountry = countryForVat(vat, person);

        final PhysicalAddress address = toAddress(person);
        final String street = limitFormat(60, address.getAddress()).replace('\t', ' ');
        final String locality = limitFormat(35, address.getAreaOfAreaCode());
        final String postCode = hackAreaCode(address.getAreaCode(), address.getCountryOfResidence(), person);
        final String country = address.getCountryOfResidence().getCode();
        final String name = limitFormat(50, getDisplayName(person));

        final JsonObject jo = new JsonObject();
        jo.addProperty("id", clientCode);
        jo.addProperty("name", limitFormat(60, name));
        jo.addProperty("type", "S");
        jo.addProperty("countryOfVatNumber", vatCountry);
        jo.addProperty("vatNumber", vat);
        jo.addProperty("address", street);
        jo.addProperty("locality", locality);
        jo.addProperty("postCode", postCode);
        jo.addProperty("countryOfAddress",country);
        jo.addProperty("phone", "");
        jo.addProperty("fax", "");
        jo.addProperty("email", "");
        jo.addProperty("ban", "");
        jo.addProperty("iban", "");
        jo.addProperty("swift", "");
        jo.addProperty("paymentMethod", "CH");

        return jo;
    }

    public static JsonObject toJson(final Event event) {
        final Person person = event.getPerson();
        final ExecutionYear debtYear = executionYearOf(event);
        final DebtCycleType cycleType = cycleTypeFor(event, debtYear);
        final String eventDescription = event.getDescription().toString();
        final String articleCode = mapToArticleCode(event, eventDescription);
        final String costCenter = costCenterFor(event);
        final String clientId = toClientCode(person);

        final JsonObject o = new JsonObject();
        o.addProperty("id", idFor(event));
        o.addProperty("date", toString(new Date()));
        o.addProperty("type", "V");
        o.addProperty("series", "13");
        o.addProperty("group", "212");
        o.addProperty("clientId", clientId);

        o.addProperty("vatNumber", "");
        o.addProperty("name", "");
        o.addProperty("country", "");
        o.addProperty("postalCode", "");
        o.addProperty("locality", "");
        o.addProperty("street", "");

        o.addProperty("doorNumber", 1);// TODO
        o.addProperty("paymentType", "PP");
        o.addProperty("sellerId", costCenter);// TODO
        o.addProperty("currency", "EUR");
        o.addProperty("accountingUnit", "10");// TODO
        o.addProperty("reference", debtYear.getName());
        o.addProperty("observation", cycleType == null ? "" : cycleType.getDescription());
        o.addProperty("username", "CRISTINAC");

        o.addProperty("dataVencimento", toString(getDueDate(event)));

        final JsonArray a = new JsonArray();
        {
            final JsonObject e = new JsonObject();
            e.addProperty("line", 1);
            e.addProperty("type", "2");// TODO
            e.addProperty("article", articleCode);
            e.addProperty("description", eventDescription);
            e.addProperty("unitType", "UN");
            e.addProperty("quantity", BigDecimal.ONE);
            e.addProperty("unitPrice", event.getOriginalAmountToPay().getAmount());
            e.addProperty("vat", BigDecimal.ZERO);
            e.addProperty("discount", BigDecimal.ZERO);
            e.addProperty("costCenter", costCenter);// TODO
            e.addProperty("responsible", "9910");// TODO
            e.addProperty("subCenter", "RP" + costCenter);// TODO
            e.addProperty("legalArticle", "M99");
            e.addProperty("rubrica", "");
            e.addProperty("observation", "");
            a.add(e);
        }
        o.add("entries", a);
        return o;
    }

    public static JsonObject toJson(final AccountingTransactionDetail detail) {
        final AccountingTransaction transaction = detail.getTransaction();
        final Event event = transaction.getEvent();
        final Person person = event.getPerson();
//        final Student student = person.getStudent();
//        final PhysicalAddress address = toAddress(person);
        final ExecutionYear debtYear = executionYearOf(event);
        final DebtCycleType cycleType = cycleTypeFor(event, debtYear);
        final String eventDescription = event.getDescription().toString();
        final String articleCode = mapToArticleCode(event, eventDescription);
        final String costCenter = costCenterFor(event);
        final String clientId = toClientCode(person);
//        final String vatNumber = toVatNumber(person);

        final JsonObject o = new JsonObject();
        o.addProperty("id", idFor(detail));
        o.addProperty("invoiceId", invoiceIdFor(detail));
        o.addProperty("date", toString(new Date()));
        o.addProperty("type", "V");
        o.addProperty("series", "13");
        o.addProperty("group", "212");
        o.addProperty("clientId", clientId);

        o.addProperty("vatNumber", "");
        o.addProperty("name", "");
        o.addProperty("country", "");
        o.addProperty("postalCode", "");
        o.addProperty("locality", "");
        o.addProperty("street", "");

//        o.addProperty("vatNumber", vatNumber);
//        final String name = getDisplayName(person);
//        o.addProperty("name", limitFormat(50, name));
//        o.addProperty("country", address.getCountryOfResidence().getCode());
//        //o.addProperty("country", person.getCountry().getCode()); // TODO : check if two or three letter country code
//        o.addProperty("postalCode", hackAreaCode(address.getAreaCode(), address.getCountryOfResidence(), person));
//        o.addProperty("locality", limitFormat(60, address.getAreaOfAreaCode()));
//        o.addProperty("street", limitFormat(60, address.getAddress()));

        o.addProperty("doorNumber", 1);// TODO
        o.addProperty("paymentType", "PP");
        o.addProperty("sellerId", costCenter);// TODO
        o.addProperty("currency", "EUR");
        o.addProperty("accountingUnit", "10");// TODO
        o.addProperty("reference", debtYear.getName());
        o.addProperty("observation", cycleType == null ? "" : cycleType.getDescription());
        o.addProperty("username", "CRISTINAC");

        //o.addProperty("dataVencimento", toString(getDueDate(detail)));
        o.addProperty("dataPagamento", toString(transaction.getWhenRegistered().toDate()));
        o.addProperty("meioPagamento", toPaymentMethod(transaction.getPaymentMode()));
        o.addProperty("numeroDocumento", toPaymentDocumentNumber(detail));

        final JsonArray a = new JsonArray();
        {
            final JsonObject e = new JsonObject();
            e.addProperty("line", 1);
            e.addProperty("type", "2");// TODO
            e.addProperty("article", articleCode);
            e.addProperty("description", eventDescription);
            e.addProperty("unitType", "UN");
            e.addProperty("quantity", BigDecimal.ONE);
            e.addProperty("unitPrice", transaction.getAmountWithAdjustment().getAmount());
            e.addProperty("vat", BigDecimal.ZERO);
            e.addProperty("discount", BigDecimal.ZERO);
            e.addProperty("costCenter", costCenter);// TODO
            e.addProperty("responsible", "9910");// TODO
            e.addProperty("subCenter", "RP" + costCenter);// TODO
            e.addProperty("legalArticle", "M99");
            e.addProperty("rubrica", "");
            e.addProperty("observation", "");
            a.add(e);
        }
        o.add("entries", a);
        return o;
    }

    public static byte[] toBytes(final Spreadsheet sheet) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            sheet.exportToXLSSheet(bos);
        } catch (final IOException e) {
            throw new Error(e);
        }
        return bos.toByteArray();
    }

    public static String valueOf(final AccountingTransactionDetail detail) {
        try {
            return detail.getTransaction().getAmountWithAdjustment().getAmount().toString();
        } catch (final DomainException ex) {
            return "?";
        }
    }

    public static ExecutionYear executionYearOf(final Event event) {
        return event instanceof AnnualEvent ? ((AnnualEvent) event).getExecutionYear() : ExecutionYear
                .readByDateTime(event.getWhenOccured());
    }

    private static DebtCycleType getCycleType(Collection<CycleType> cycleTypes) {
        if (cycleTypes.size() > 1) {
            return DebtCycleType.INTEGRATED_MASTER;
        } else if (cycleTypes.size() > 0) {
            return DebtCycleType.valueOf(cycleTypes.iterator().next());
        }
        return null;
    }

    public static DebtCycleType cycleTypeFor(Event event, ExecutionYear executionYear) {
        if (event instanceof PhdEvent || event.isFctScholarshipPhdGratuityContribuitionEvent()) {
            return DebtCycleType.THIRD_CYCLE;
        }
        if (event.getParty().isPerson()) {
            Student student = event.getPerson().getStudent();
            if (student != null) {
                for (Registration registration : student.getRegistrationsSet()) {
                    StudentCurricularPlan scp = registration.getStudentCurricularPlan(executionYear);
                    if (scp == null) {
                        StudentCurricularPlan lastStudentCurricularPlan = registration.getLastStudentCurricularPlan();
                        if (lastStudentCurricularPlan != null
                                && lastStudentCurricularPlan.getStartExecutionYear() == executionYear) {
                            scp = lastStudentCurricularPlan;
                        }
                    }
                    if (scp != null) {
                        Collection<CycleType> cycleTypes = registration.getDegree().getCycleTypes();
                        DebtCycleType cycleType = getCycleType(cycleTypes);
                        if (cycleType != null) {
                            return cycleType;
                        }
                    }
                }
            } else if (event.isIndividualCandidacyEvent()) {
                IndividualCandidacyEvent candidacyEvent = (IndividualCandidacyEvent) event;
                Set<DebtCycleType> cycleTypes = new HashSet<DebtCycleType>();
                for (Degree degree : candidacyEvent.getIndividualCandidacy().getAllDegrees()) {
                    DebtCycleType cycleType = getCycleType(degree.getCycleTypes());
                    if (cycleType != null) {
                        cycleTypes.add(cycleType);
                    }
                }
                if (cycleTypes.size() == 1) {
                    return cycleTypes.iterator().next();
                }
            }
        }
        return null;
    }

    private static String mapToArticleCode(final Event event, final String eventDescription) {
        if (event.isGratuity()) {
            final GratuityEvent gratuityEvent = (GratuityEvent) event;
            final StudentCurricularPlan scp = gratuityEvent.getStudentCurricularPlan();
            final Degree degree = scp.getDegree();
            if (scp.getRegistration().getRegistrationProtocol().isAlien()) {
                return "FEINTERN";// PROPINAS INTERNACIONAL
            }
            if (degree.isFirstCycle() && degree.isSecondCycle()) {
                return "FEMESTIN";// 724114 PROPINAS MESTRADO INTEGRADO
            }
            if (degree.isFirstCycle()) {
                return "FE1CICLO";// 724111 PROPINAS 1 CICLO
            }
            if (degree.isSecondCycle()) {
                return "FE2CICLO";// 724112 PROPINAS 2 CICLO
            }
            if (degree.isThirdCycle()) {
                return "FE3CICLO";// 724113 PROPINAS 3 CICLO
            }
            return "FEOUTPRO";// 724116 PROPINAS - OUTROS
        }
        if (event instanceof PhdGratuityEvent) {
            return "FE3CICLO";// 724113 PROPINAS 3 CICLO
        }
        if (event.isResidenceEvent()) {
            return null;
        }
        if (event.isFctScholarshipPhdGratuityContribuitionEvent()) {
            return null;
        }
        if (event.isAcademicServiceRequestEvent()) {
            if (eventDescription.indexOf(" Reingresso") >= 0) {
                return "FETAXAOUT";// 72419 OUTRAS TAXAS
            }
            return "FEEMOL";// 7246 EMOLUMENTOS
        }
        if (event.isDfaRegistrationEvent()) {
            return "FETAXAMAT";// 72412 TAXAS DE MATRICULA
        }
        if (event.isIndividualCandidacyEvent()) {
            return "FETAXAMAT";// 72412 TAXAS DE MATRICULA
        }
        if (event.isEnrolmentOutOfPeriod()) {
            return "FETAXAOUT";// 72419 OUTRAS TAXAS
        }
        if (event instanceof AdministrativeOfficeFeeAndInsuranceEvent) {
            return "FETAXAMAT";// 72412 TAXAS DE MATRICULA
        }
        if (event instanceof InsuranceEvent) {
            return "FESEGESC";// 72415 SEGURO ESCOLAR
        }
        if (event.isSpecializationDegreeRegistrationEvent()) {
            return "FETAXAMAT";// 72412 TAXAS DE MATRICULA
        }
        if (event instanceof ImprovementOfApprovedEnrolmentEvent) {
            return "FETAXAMN";// 72414 TAXAS DE MELHORIAS DE NOTAS
        }
        if (event instanceof DFACandidacyEvent) {
            return "FETAXAMAT";// 72412 TAXAS DE MATRICULA"
        }
        if (event.isPhdEvent()) {
            if (eventDescription.indexOf("Taxa de Inscri") >= 0) {
                return "FETAXAMAT";// 72412 TAXAS DE MATRICULA
            }
            if (eventDescription.indexOf("Requerimento de provas") >= 0) {
                return "FETAXAEX";// 72413 TAXAS  DE EXAMES
            }
            return "FETAXAMAT";// 72412 TAXAS DE MATRICULA
        }
        throw new Error("not.supported: " + event.getExternalId());
    }

    private static PhysicalAddress toAddress(final Person person) {
        PhysicalAddress address = person.getDefaultPhysicalAddress();
        if (address == null) {
            for (final PartyContact contact : person.getPartyContactsSet()) {
                if (contact instanceof PhysicalAddress) {
                    address = (PhysicalAddress) contact;
                    break;
                }
            }
        }
        return address;
    }

    private static boolean isValidPostCode(final String postalCode) {
        if (postalCode != null) {
            final String v = postalCode.trim();
            return v.length() == 8 && v.charAt(4) == '-' && StringUtils.isNumeric(v.substring(0, 4))
                    && StringUtils.isNumeric(v.substring(5));
        }
        return false;
    }

    private static String toVatNumber(final Person person) {
        final Country country = person.getCountry();
        final String ssn = person.getSocialSecurityNumber();
        final String vat = toVatNumber(ssn);
        if (vat != null && isVatValidForPT(vat)) {
            return vat;
        }
        if (country != null && "PT".equals(country.getCode())) {
            return null;
        }
        final User user = person.getUser();
        return user == null ? makeUpSomeRandomNumber(person) : user.getUsername();
    }

    private static String makeUpSomeRandomNumber(final Person person) {
        final String id = person.getExternalId();
        return "FE" + id.substring(id.length() - 10, id.length());
    }

    private static String toVatNumber(final String ssn) {
        return ssn == null ? null : ssn.startsWith("PT") ? ssn.substring(2) : ssn;
    }

    private static boolean isVatValidForPT(final String vat) {
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

    private static String costCenterFor(final Event event) {
        if (event instanceof PhdGratuityEvent) {
            return "8312";
        }
        if (event instanceof AcademicEvent) {
            final AcademicEvent academicEvent = (AcademicEvent) event;
            final AdministrativeOffice administrativeOffice = academicEvent.getAdministrativeOffice();
            if (administrativeOffice != null) {
                final Unit unit = administrativeOffice.getUnit();
                if (unit != null) {
                    final Integer costCenter = unit.getCostCenterCode();
                    if (costCenter != null) {
                        return costCenter.toString();
                    }
                }
            }
        }
        if (event instanceof InsuranceEvent) {
            final InsuranceEvent insuranceEvent = (InsuranceEvent) event;
            final ExecutionYear executionYear = insuranceEvent.getExecutionYear();
            final Person person = event.getPerson();
            if (!person.getPhdIndividualProgramProcessesSet().isEmpty()) {
                return "8312";
            }
            final Student student = person.getStudent();
            if (student != null) {
                for (final Registration registration : student.getRegistrationsSet()) {
                    for (final RegistrationState registrationState : registration.getRegistrationStates(executionYear)) {
                        if (registrationState.isActive()) {
                            final DegreeType degreeType = registration.getDegree().getDegreeType();
                            if (degreeType.isAdvancedFormationDiploma() || degreeType.isAdvancedSpecializationDiploma()
                                    || degreeType.isSpecializationCycle() || degreeType.isSpecializationDegree()
                                    || degreeType.isThirdCycle()) {
                                return "8312";
                            }
                            final Space campus = registration.getCampus(executionYear);
                            if (campus != null && campus.getName().startsWith("T")) {
                                return "7640";
                            }
                        }

                    }
                }
            }
        }
        throw new Error("Unknown cost center for event: " + event.getExternalId());
    }

    private static String toClientCode(final Person person) {
        final User user = person.getUser();
        return user == null ? makeUpSomeRandomNumber(person) : user.getUsername();
    }

    private static String toString(final Date d) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }

    private static String toPaymentMethod(final PaymentMode paymentMode) {
        switch (paymentMode) {
        case CASH:
            return "N";
        case ATM:
            return "SIBS";
        default:
            throw new Error();
        }
    }

    private static String toPaymentDocumentNumber(final AccountingTransactionDetail detail) {
        return detail instanceof SibsTransactionDetail ? ((SibsTransactionDetail) detail).getSibsCode() : "";
    }

    private static Date getDueDate(final Event event) {
        final DateTime dueDate;
        if (event instanceof GratuityEventWithPaymentPlan) {
            final GratuityEventWithPaymentPlan gratuityEventWithPaymentPlan = (GratuityEventWithPaymentPlan) event;
            dueDate = findLastDueDate(gratuityEventWithPaymentPlan);
        } else if (event instanceof PhdGratuityEvent) {
            final PhdGratuityEvent phdGratuityEvent = (PhdGratuityEvent) event;
            dueDate = phdGratuityEvent.getLimitDateToPay();
        } else {
            dueDate = event.getWhenOccured();
        }
        return dueDate.toDate();
    }

    private static DateTime findLastDueDate(final GratuityEventWithPaymentPlan event) {
        return event.getInstallments().stream().map(i -> i.getEndDate().toDateTimeAtMidnight()).max(new Comparator<DateTime>() {
            @Override
            public int compare(DateTime o1, DateTime o2) {
                return o1.compareTo(o2);
            }
        }).orElse(null);
    }

    public static String limitFormat(final int maxSize, String in) {
        if (in == null) {
            return "";
        }
        final String out = StringNormalizer.normalizeAndRemoveAccents(in).toUpperCase();
        return out.length() > maxSize ? out.substring(0, maxSize) : out;
    }

    public static String countryForVat(final String vat, Person person) {
        return isVatValidForPT(vat) ? "PT" : person.getCountry().getCode();
    }

    private static String hackAreaCode(final String areaCode, final Country countryOfResidence, final Person person) {
        return countryOfResidence != null && !"PT".equals(countryOfResidence.getCode()) ? "0" : areaCode;
    }

    private static String getDisplayName(final Person person) {
        final User user = person.getUser();
        final UserProfile profile = user == null ? null : user.getProfile();
        final String displayName = profile == null ? null : profile.getDisplayName();
        return displayName == null ? person.getName() : displayName;
    }

    public static String idFor(final DomainObject object) {
        return object.getExternalId();
    }

    private static String invoiceIdFor(final AccountingTransactionDetail detail) {
        final Event event = detail.getEvent();
        return GiafInvoice.documentNumberFor(event);
    }

}
