package pt.ist.fenixedu.giaf.invoices;

import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

public class CreateInvoices extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Stream<Event> eventStream = Bennu.getInstance().getAccountingEventsSet().stream();
        final Spreadsheet sheet = new Spreadsheet("Report");
        final ErrorConsumer<Event> consumer = new ErrorConsumer<Event>() {
            @Override
            public void accept(Event event, String error, String args) {
                final Person person = event == null || event.getParty().isUnit() ? null : event.getPerson();
                final User user = person == null ? null : person.getUser();
                final ExecutionYear debtYear = event == null ? null : Utils.executionYearOf(event);
                final DebtCycleType cycleType = event == null || debtYear == null ? null : Utils.cycleTypeFor(event, debtYear);
                final String eventDescription = event.getDescription().toString();

                final Row row = sheet.addRow();
                row.setCell("id", event.getExternalId());
                row.setCell("value", getValue(event));
                row.setCell("error", error);
                row.setCell("args", args == null ? "" : args);
                row.setCell("user", user == null ? "" : user.getUsername());
                row.setCell("cycle type", cycleType == null ? "" : cycleType.getDescription());
                row.setCell("eventDescription", eventDescription);
            }

            private String getValue(Event event) {
                try {
                    return event.getOriginalAmountToPay().getAmount().toString();
                } catch (final DomainException ex) {
                    return "?";
                } catch (final NullPointerException ex) {
                    return "?";
                }
            }
        };
        eventStream.filter(this::needsProcessing).filter(e -> Utils.validate(consumer, e)).forEach(this::process);

        output("errors.xls", Utils.toBytes(sheet));
    }

    private boolean needsProcessing(final Event event) {
        final ExecutionYear executionYear = Utils.executionYearOf(event);
        return executionYear.isCurrent();
    }

    private void process(final Event event) {
        Utils.toJson(event);
    }

}
