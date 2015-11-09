package pt.ist.fenixedu.giaf.invoices;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Base64;
import java.util.function.Function;
import java.util.function.Predicate;

import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.GiafInvoiceConfiguration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pt.ist.fenixframework.DomainObject;
import pt.ist.giaf.client.financialDocuments.InvoiceClient;

public class GiafInvoice {

    private static final String DIR = GiafInvoiceConfiguration.getConfiguration().giafInvoiceDir();

    public static void createInvoice(final ErrorConsumer<Event> consumer, final Event event) {
        createInvoice(event, (e) -> Utils.validate(consumer, e), (e) -> Utils.toJson(e));
    }

    public static void createInvoice(final ErrorConsumer<AccountingTransactionDetail> consumer,
            final AccountingTransactionDetail detail) {
        createInvoice(detail, (d) -> Utils.validate(consumer, d), (d) -> Utils.toJson(d));
    }

    public static InputStream invoiceStream(final Event event) throws IOException {
        return streamFor(event);
    }

    public static InputStream invoiceStream(final AccountingTransactionDetail detail) throws IOException {
        return streamFor(detail);
    }

    public static String documentNumberFor(final Event event) {
        final String id = Utils.idFor(event);
        final File file = fileForDocumentNumber(id);
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

    private static <T extends DomainObject> InputStream streamFor(final T t) throws IOException {
        final String id = Utils.idFor(t);
        final File file = fileForDocument(id);
        return new FileInputStream(file);
    }

    private static <T extends DomainObject> void createInvoice(final T t, final Predicate<T> p, final Function<T, JsonObject> f) {
        final String id = Utils.idFor(t);
        final File file = fileForDocumentNumber(id);
        if (!file.exists() && p.test(t)) {
            final JsonObject jo = f.apply(t);
            final String documentNumber = createInvoice(jo);
            if (documentNumber != null) {
                try {
                    Files.write(file.toPath(), documentNumber.getBytes());
                } catch (final IOException e) {
                    throw new Error(e);
                }
            }
        }
    }

    private static String createInvoice(final JsonObject jo) {
        final String id = jo.get("id").getAsString();
        final File file = fileForDocument(id);
        if (!file.exists()) {
            final JsonObject result = InvoiceClient.produceInvoice(jo);

            final JsonElement errorMessage = result.get("errorMessage");
            if (errorMessage != null) {
                System.out.println("   Error: " + errorMessage.getAsString());
            }
            final JsonElement documentNumber = result.get("documentNumber");
            if (documentNumber != null) {
                System.out.println("   Document Number: " + documentNumber.getAsString());
            }
            final JsonElement pdfBase64 = result.get("pdfBase64");
            if (pdfBase64 != null) {
                System.out.println("   Document Size: " + pdfBase64.getAsString().length());
                try {
                    Files.write(file.toPath(), Base64.getDecoder().decode(pdfBase64.getAsString()));
                } catch (final IOException e) {
                    throw new Error(e);
                }
            }
            return documentNumber == null ? null : documentNumber.getAsString();
        }
        return null;
    }

    private static File fileForDocumentNumber(final String id) {
        return fileFor(id, ".txt");
    }

    private static File fileForDocument(final String id) {
        return fileFor(id, ".pdf");
    }

    private static File fileFor(final String id, final String extension) {
        final String dirPath = DIR + splitPath(id);
        final File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        final String filePath = dirPath + id + extension;
        return new File(filePath);
    }

    private static String splitPath(final String id) {
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < id.length() - 1; i++, i++) {
            b.append(id.charAt(i));
            b.append(id.charAt(i + 1));
            b.append(File.separatorChar);
        }
        return b.toString();
    }

}
