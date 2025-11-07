package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.model.Invoice;
import Green_trade.green_trade_platform.model.Order;
import Green_trade.green_trade_platform.repository.InvoiceRepository;
import Green_trade.green_trade_platform.service.InvoiceService;
import Green_trade.green_trade_platform.util.InvoiceUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceUtils invoiceUtils;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository, InvoiceUtils invoiceUtils) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceUtils = invoiceUtils;
    }

    public Invoice createInvoiceInstance(Order order, String note, double taxRate) {
        Invoice newInvoice = Invoice.builder()
                .invoiceNumber(invoiceNumberGenerator())
                .note(note)
                .concurrency("VND")
                .taxRate(taxRate)
                .order(order)
                .pdfUrl("")
                .build();
        return invoiceRepository.save(newInvoice);
    }

    public String generateInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn!"));

        String pdfUrl = invoiceUtils.generateInvoicePDFAndUpload(invoice);

        invoice.setPdfUrl(pdfUrl);
        invoiceRepository.save(invoice);

        return pdfUrl;
    }

    public String invoiceNumberGenerator() {
        long timestamp = System.currentTimeMillis();
        return "INV" + timestamp;
    }

}
