package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.model.CancelOrderReason;
import Green_trade.green_trade_platform.repository.CancelOrderReasonRepository;
import Green_trade.green_trade_platform.service.implement.CancelOrderReasonServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CancelOrderServiceTest {

    @Mock
    private CancelOrderReasonRepository cancelOrderReasonRepository;

    private CancelOrderReasonServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CancelOrderReasonServiceImpl(cancelOrderReasonRepository);
    }

    @Test
    void shouldReturnAllCancelOrderReasonsWhenRepositoryHasMultipleEntries() {
        CancelOrderReason r1 = CancelOrderReason.builder().id(1L).cancelReasonName("Reason A").build();
        CancelOrderReason r2 = CancelOrderReason.builder().id(2L).cancelReasonName("Reason B").build();
        when(cancelOrderReasonRepository.findAll()).thenReturn(Arrays.asList(r1, r2));

        List<CancelOrderReason> result = service.getAllCancelOrderReasons();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(r1, result.get(0));
        assertEquals(r2, result.get(1));
        verify(cancelOrderReasonRepository, times(1)).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenRepositoryIsEmpty() {
        when(cancelOrderReasonRepository.findAll()).thenReturn(Collections.emptyList());

        List<CancelOrderReason> result = service.getAllCancelOrderReasons();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(cancelOrderReasonRepository, times(1)).findAll();
    }

    @Test
    void shouldPreserveOrderProvidedByRepository() {
        CancelOrderReason r1 = CancelOrderReason.builder().id(1L).cancelReasonName("First").build();
        CancelOrderReason r2 = CancelOrderReason.builder().id(2L).cancelReasonName("Second").build();
        CancelOrderReason r3 = CancelOrderReason.builder().id(3L).cancelReasonName("Third").build();
        List<CancelOrderReason> repositoryList = Arrays.asList(r2, r3, r1);
        when(cancelOrderReasonRepository.findAll()).thenReturn(repositoryList);

        List<CancelOrderReason> result = service.getAllCancelOrderReasons();

        assertEquals(repositoryList, result);
        verify(cancelOrderReasonRepository, times(1)).findAll();
    }

    @Test
    void shouldPropagateExceptionWhenRepositoryThrows() {
        RuntimeException repoEx = new RuntimeException("DB failure");
        when(cancelOrderReasonRepository.findAll()).thenThrow(repoEx);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.getAllCancelOrderReasons());
        assertSame(repoEx, thrown);
        verify(cancelOrderReasonRepository, times(1)).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenRepositoryReturnsNull() {
        when(cancelOrderReasonRepository.findAll()).thenReturn(null);

        try {
            List<CancelOrderReason> result = service.getAllCancelOrderReasons();
            // If service handles null defensively, result should be empty
            assertNotNull(result);
            assertTrue(result.isEmpty());
        } catch (NullPointerException npe) {
            fail("Service should handle null return from repository gracefully without throwing NPE");
        }
        verify(cancelOrderReasonRepository, times(1)).findAll();
    }

    @Test
    void shouldNotModifyListReturnedByRepository() {
        CancelOrderReason r1 = CancelOrderReason.builder().id(1L).cancelReasonName("X").build();
        List<CancelOrderReason> repositoryList = new ArrayList<>(Collections.singletonList(r1));
        when(cancelOrderReasonRepository.findAll()).thenReturn(repositoryList);

        List<CancelOrderReason> result = service.getAllCancelOrderReasons();

        // Ensure service returns the same contents and does not modify the source list
        assertEquals(1, repositoryList.size());
        assertEquals(1, result.size());
        assertEquals(r1, repositoryList.get(0));
        assertEquals(r1, result.get(0));

        // Mutate the returned list to ensure it doesn't back-modify repositoryList (no side effects expected)
        result.clear();
        assertEquals(1, repositoryList.size(), "Repository list should remain unaffected by modifications to the returned list");

        verify(cancelOrderReasonRepository, times(1)).findAll();
    }
}
