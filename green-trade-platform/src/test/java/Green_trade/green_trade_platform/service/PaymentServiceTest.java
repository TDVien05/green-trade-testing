package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.model.Payment;
import Green_trade.green_trade_platform.repository.PaymentRepository;
import Green_trade.green_trade_platform.service.implement.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        samplePayment = Payment.builder()
                .id(123L)
                .description("Test payment")
                .gatewayName("TestGateway")
                .build();
    }

    @Test
    void shouldReturnPaymentWhenFoundById() {
        Long id = 1L;
        when(paymentRepository.findById(id)).thenReturn(Optional.of(samplePayment));

        Payment result = paymentService.findPaymentMethodById(id);

        assertNotNull(result);
        assertEquals(samplePayment.getId(), result.getId());
        assertEquals(samplePayment.getDescription(), result.getDescription());
        assertEquals(samplePayment.getGatewayName(), result.getGatewayName());
        verify(paymentRepository, times(1)).findById(id);
    }

    @Test
    void shouldCallRepositoryFindByIdWithGivenId() {
        Long id = 42L;
        when(paymentRepository.findById(id)).thenReturn(Optional.empty());

        paymentService.findPaymentMethodById(id);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(paymentRepository, times(1)).findById(captor.capture());
        assertEquals(id, captor.getValue());
    }

    @Test
    void shouldReturnSamePaymentInstanceFromRepository() {
        Long id = 7L;
        when(paymentRepository.findById(id)).thenReturn(Optional.of(samplePayment));

        Payment result = paymentService.findPaymentMethodById(id);

        assertSame(samplePayment, result);
        verify(paymentRepository).findById(id);
    }

    @Test
    void shouldReturnNullWhenPaymentNotFound() {
        Long id = 999L;
        when(paymentRepository.findById(id)).thenReturn(Optional.empty());

        Payment result = paymentService.findPaymentMethodById(id);

        assertNull(result);
        verify(paymentRepository).findById(id);
    }

    @Test
    void shouldHandleNullIdGracefully() {
        when(paymentRepository.findById(null)).thenReturn(Optional.empty());

        Payment result = paymentService.findPaymentMethodById(null);

        assertNull(result);
        verify(paymentRepository).findById(null);
    }

    @Test
    void shouldPropagateRepositoryExceptions() {
        Long id = 5L;
        RuntimeException boom = new RuntimeException("DB down");
        when(paymentRepository.findById(id)).thenThrow(boom);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            paymentService.findPaymentMethodById(id);
        });

        assertSame(boom, thrown);
        verify(paymentRepository).findById(id);
    }
}
