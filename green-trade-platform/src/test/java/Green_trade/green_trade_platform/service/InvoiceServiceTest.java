package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.model.Invoice;
import Green_trade.green_trade_platform.model.Order;
import Green_trade.green_trade_platform.repository.InvoiceRepository;
import Green_trade.green_trade_platform.service.implement.InvoiceServiceImpl;
import Green_trade.green_trade_platform.util.InvoiceUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceUtils invoiceUtils;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder().id(1L).orderCode("ORD-001").build();
    }

    @Test
    void shouldCreateAndSaveInvoiceWithGivenFields() {
        // Arrange
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        double taxRate = 10.0;
        String note = "Test note";
        Invoice saved = invoiceService.createInvoiceInstance(sampleOrder, note, taxRate);

        // Assert
        verify(invoiceRepository, times(1)).save(invoiceCaptor.capture());
        Invoice toSave = invoiceCaptor.getValue();
        assertNotNull(toSave.getInvoiceNumber());
        assertTrue(toSave.getInvoiceNumber().startsWith("INV"));
        assertEquals(note, toSave.getNote());
        assertEquals("VND", toSave.getConcurrency());
        assertEquals(taxRate, toSave.getTaxRate(), 0.0001);
        assertEquals(sampleOrder, toSave.getOrder());
        assertEquals("", toSave.getPdfUrl());

        // Returned object should reflect what repository returned
        assertEquals(toSave, saved);
    }

    @Test
    void shouldGeneratePdfAndUpdateInvoiceUrl() {
        // Arrange
        Long invoiceId = 100L;
        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .invoiceNumber("INV123")
                .order(sampleOrder)
                .pdfUrl("")
                .build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceUtils.generateInvoicePDFAndUpload(invoice)).thenReturn("https://cloud/pdf/inv123.pdf");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String url = invoiceService.generateInvoice(invoiceId);

        // Assert
        assertEquals("https://cloud/pdf/inv123.pdf", url);
        assertEquals("https://cloud/pdf/inv123.pdf", invoice.getPdfUrl());

        InOrder inOrder = inOrder(invoiceRepository, invoiceUtils);
        inOrder.verify(invoiceRepository).findById(invoiceId);
        inOrder.verify(invoiceUtils).generateInvoicePDFAndUpload(invoice);
        inOrder.verify(invoiceRepository).save(invoice);
    }

    @Test
    void shouldGenerateInvoiceNumberWithInvPrefixAndTimestamp() {
        // Act
        String number = invoiceService.invoiceNumberGenerator();

        // Assert
        assertNotNull(number);
        assertTrue(number.startsWith("INV"));
        String ts = number.substring(3);
        assertFalse(ts.isEmpty());
        assertDoesNotThrow(() -> Long.parseLong(ts));
    }

    @Test
    void shouldThrowWhenInvoiceIdNotFoundOnGenerate() {
        // Arrange
        Long missingId = 999L;
        when(invoiceRepository.findById(missingId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> invoiceService.generateInvoice(missingId));
        assertTrue(ex.getMessage().contains("Không tìm thấy hóa đơn!"));
        verify(invoiceUtils, never()).generateInvoicePDFAndUpload(any());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldPropagateWhenPdfGenerationFails() {
        // Arrange
        Long invoiceId = 200L;
        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .invoiceNumber("INV_ERR")
                .order(sampleOrder)
                .pdfUrl("")
                .build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceUtils.generateInvoicePDFAndUpload(invoice)).thenThrow(new RuntimeException("Cloud error"));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> invoiceService.generateInvoice(invoiceId));
        assertEquals("Cloud error", ex.getMessage());
        // Ensure invoice not saved after failure
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void shouldCreateInvoiceWhenNoteIsNullOrEmpty() {
        // Arrange
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Null note
        Invoice invNull = invoiceService.createInvoiceInstance(sampleOrder, null, 5.0);
        assertNotNull(invNull.getInvoiceNumber());
        assertTrue(invNull.getInvoiceNumber().startsWith("INV"));
        assertNull(invNull.getNote());
        assertEquals("VND", invNull.getConcurrency());
        assertEquals(5.0, invNull.getTaxRate(), 0.0001);
        assertEquals(sampleOrder, invNull.getOrder());
        assertEquals("", invNull.getPdfUrl());

        // Empty note
        Invoice invEmpty = invoiceService.createInvoiceInstance(sampleOrder, "", 7.5);
        assertNotNull(invEmpty.getInvoiceNumber());
        assertTrue(invEmpty.getInvoiceNumber().startsWith("INV"));
        assertEquals("", invEmpty.getNote());
        assertEquals("VND", invEmpty.getConcurrency());
        assertEquals(7.5, invEmpty.getTaxRate(), 0.0001);
        assertEquals(sampleOrder, invEmpty.getOrder());
        assertEquals("", invEmpty.getPdfUrl());

        verify(invoiceRepository, times(2)).save(any(Invoice.class));
    }
}
