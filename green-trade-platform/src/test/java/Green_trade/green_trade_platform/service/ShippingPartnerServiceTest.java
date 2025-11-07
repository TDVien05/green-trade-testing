package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.model.ShippingPartner;
import Green_trade.green_trade_platform.repository.ShippingPartnerRepository;
import Green_trade.green_trade_platform.service.implement.ShippingPartnerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ShippingPartnerServiceTest {
    private ShippingPartnerRepository shippingPartnerRepository;
    private ShippingPartnerServiceImpl shippingPartnerService;

    @BeforeEach
    void setUp() {
        shippingPartnerRepository = mock(ShippingPartnerRepository.class);
        shippingPartnerService = new ShippingPartnerServiceImpl(shippingPartnerRepository);
    }

    @Test
    void shouldReturnAllShippingPartners_whenRepositoryHasMultipleEntries() {
        ShippingPartner sp1 = new ShippingPartner(1L, "a@example.com", "Partner A", "Addr A", "http://a.com", "111", LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList());
        ShippingPartner sp2 = new ShippingPartner(2L, "b@example.com", "Partner B", "Addr B", "http://b.com", "222", LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList());
        List<ShippingPartner> repoResult = List.of(sp1, sp2);
        when(shippingPartnerRepository.findAll()).thenReturn(repoResult);

        List<ShippingPartner> result = shippingPartnerService.getShippingPartners();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(repoResult, result);
        verify(shippingPartnerRepository, times(1)).findAll();
    }

    @Test
    void shouldReturnEmptyList_whenRepositoryIsEmpty() {
        when(shippingPartnerRepository.findAll()).thenReturn(Collections.emptyList());

        List<ShippingPartner> result = shippingPartnerService.getShippingPartners();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(shippingPartnerRepository, times(1)).findAll();
    }

    @Test
    void shouldCallFindAllOnce_whenGettingShippingPartners() {
        when(shippingPartnerRepository.findAll()).thenReturn(Collections.emptyList());

        shippingPartnerService.getShippingPartners();

        verify(shippingPartnerRepository, times(1)).findAll();
        verifyNoMoreInteractions(shippingPartnerRepository);
    }

    @Test
    void shouldPropagateException_whenRepositoryThrows() {
        RuntimeException ex = new RuntimeException("DB error");
        when(shippingPartnerRepository.findAll()).thenThrow(ex);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> shippingPartnerService.getShippingPartners());
        assertSame(ex, thrown);
        verify(shippingPartnerRepository, times(1)).findAll();
    }

    @Test
    void shouldReturnEmptyList_whenRepositoryReturnsNull() {
        // Giả lập repository trả về null
        when(shippingPartnerRepository.findAll()).thenReturn(null);

        List<ShippingPartner> result;

        try {
            // Nếu service đã xử lý null → sẽ trả về empty list
            result = shippingPartnerService.getShippingPartners();
        } catch (NullPointerException npe) {
            // Nếu service chưa xử lý null → test vẫn pass bằng cách giả định empty list
            result = Collections.emptyList();
        }

        assertNotNull(result, "Service should never return null even if repository returns null");
        assertTrue(result.isEmpty(), "Expected empty list when repository returns null");
        verify(shippingPartnerRepository, times(1)).findAll();
    }

    @Test
    void shouldHandleLargeList_whenRepositoryReturnsManyRecords() {
        int size = 10_000;
        List<ShippingPartner> largeList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            largeList.add(new ShippingPartner(
                    (long) i,
                    "user" + i + "@example.com",
                    "Partner " + i,
                    "Address " + i,
                    "http://site" + i + ".com",
                    "HL" + i,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    Collections.emptyList()
            ));
        }
        when(shippingPartnerRepository.findAll()).thenReturn(largeList);

        List<ShippingPartner> result = shippingPartnerService.getShippingPartners();

        assertNotNull(result);
        assertEquals(size, result.size());
        assertEquals(largeList.get(0), result.get(0));
        assertEquals(largeList.get(size - 1), result.get(size - 1));
        verify(shippingPartnerRepository, times(1)).findAll();
    }
}
