package pt.ist.fenixedu.giaf.invoices;

import java.time.Year;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

public class InvoiceProblems extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final int year = Year.now().getValue();

        final Stream<AccountingTransactionDetail> stream = Bennu.getInstance().getAccountingTransactionDetailsSet().stream();
        final Spreadsheet sheet = new Spreadsheet("Report");
        final ErrorConsumer<AccountingTransactionDetail> consumer = new ErrorConsumer<AccountingTransactionDetail>() {
            @Override
            public void accept(final AccountingTransactionDetail detail, final String error, final String args) {
                final AccountingTransaction transaction = detail.getTransaction();
                final Event event = transaction == null ? null : transaction.getEvent();
                final Person person = event == null || event.getParty().isUnit() ? null : event.getPerson();
                final User user = person == null ? null : person.getUser();
                final ExecutionYear debtYear = event == null ? null : Utils.executionYearOf(event);
                final DebtCycleType cycleType = event == null || debtYear == null ? null : Utils.cycleTypeFor(event, debtYear);
                final String eventDescription = event.getDescription().toString();

                final Row row = sheet.addRow();
                row.setCell("id", detail.getExternalId());
                row.setCell("value", Utils.valueOf(detail));
                row.setCell("error", error);
                row.setCell("args", args == null ? "" : args);
                row.setCell("user", user == null ? "" : user.getUsername());
                row.setCell("cycle type", cycleType == null ? "" : cycleType.getDescription());
                row.setCell("eventDescription", eventDescription);
            }
        };
        stream.filter(atd -> atd.getWhenRegistered().getYear() == year).forEach(d -> Utils.validate(consumer, d));

        output("errors.xls", Utils.toBytes(sheet));
    }

}
