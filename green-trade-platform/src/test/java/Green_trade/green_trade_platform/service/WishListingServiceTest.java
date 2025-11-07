package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.enumerate.WishListPriority;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.PostProduct;
import Green_trade.green_trade_platform.model.WishListing;
import Green_trade.green_trade_platform.repository.WishListingRepository;
import Green_trade.green_trade_platform.service.implement.WishListingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WishListingServiceTest {

    @Mock
    private WishListingRepository wishListingRepository;

    @InjectMocks
    private WishListingServiceImpl wishListingService;

    private Buyer buyer;

    @BeforeEach
    void setUp() {
        buyer = Buyer.builder()
                .buyerId(123L)
                .username("buyer1")
                .password("pwd")
                .email("buyer1@example.com")
                .isActive(true)
                .build();
    }

    @Test
    void shouldSaveWishListingAndReturnPersistedEntity() {
        WishListing toSave = WishListing.builder()
                .id(null)
                .buyer(buyer)
                .priority(WishListPriority.HIGH)
                .note("note")
                .postProduct(PostProduct.builder().id(10L).build())
                .build();

        WishListing persisted = WishListing.builder()
                .id(1L)
                .buyer(buyer)
                .priority(WishListPriority.HIGH)
                .note("note")
                .postProduct(PostProduct.builder().id(10L).build())
                .build();

        when(wishListingRepository.save(toSave)).thenReturn(persisted);

        WishListing result = wishListingService.addWishList(toSave);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(wishListingRepository, times(1)).save(toSave);
    }

    @Test
    void shouldDeleteWishListingWhenIdExists() {
        long id = 5L;
        WishListing existing = WishListing.builder().id(id).buyer(buyer).priority(WishListPriority.MEDIUM).note("n").build();
        when(wishListingRepository.findById(id)).thenReturn(Optional.of(existing));

        wishListingService.removePostProduct(id);

        ArgumentCaptor<WishListing> captor = ArgumentCaptor.forClass(WishListing.class);
        verify(wishListingRepository, times(1)).delete(captor.capture());
        assertEquals(id, captor.getValue().getId());
    }

    @Test
    void shouldGetWishListByBuyerAndPriorityWithPagination() {
        int page = 0;
        int size = 2;
        WishListPriority priority = WishListPriority.LOW;

        Pageable expectedPageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<WishListing> content = List.of(
                WishListing.builder().id(1L).buyer(buyer).priority(priority).note("a").build(),
                WishListing.builder().id(2L).buyer(buyer).priority(priority).note("b").build()
        );
        Page<WishListing> mockPage = new PageImpl<>(content, expectedPageable, 2);

        when(wishListingRepository.findByBuyer_BuyerIdAndPriority(eq(buyer.getBuyerId()), eq(priority), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<WishListing> result = wishListingService.getWishList(buyer, page, size, priority);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(wishListingRepository, times(1))
                .findByBuyer_BuyerIdAndPriority(eq(buyer.getBuyerId()), eq(priority), any(Pageable.class));
    }

    @Test
    void shouldThrowWhenDeletingNonExistingWishListingId() {
        long id = 999L;
        when(wishListingRepository.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> wishListingService.removePostProduct(id));

        assertTrue(ex.getMessage().contains("Can not find wish list with id: " + id));
        verify(wishListingRepository, never()).delete(any());
    }


    @Test
    void shouldGetWishListByBuyerWithoutPriorityWithPagination() {
        // Pageable trong Spring bắt đầu từ 0, không phải 1
        int page = 0;
        int size = 3;

        Pageable expectedPageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<WishListing> content = List.of(
                WishListing.builder().id(3L).buyer(buyer).priority(WishListPriority.HIGH).note("x").build()
        );

        // Tổng phần tử phải khớp với content.size()
        Page<WishListing> mockPage = new PageImpl<>(content, expectedPageable, content.size());

        when(wishListingRepository.findByBuyer_BuyerId(eq(buyer.getBuyerId()), any(Pageable.class)))
                .thenReturn(mockPage);

        // Gọi service
        Page<WishListing> result = wishListingService.getWishList(buyer, page, size, null);

        assertNotNull(result);
        assertEquals(content.size(), result.getTotalElements());
        assertEquals(content.size(), result.getContent().size());
        assertEquals(content.get(0).getId(), result.getContent().get(0).getId());
        
        verify(wishListingRepository, times(1))
                .findByBuyer_BuyerId(eq(buyer.getBuyerId()), any(Pageable.class));
        verify(wishListingRepository, never())
                .findByBuyer_BuyerIdAndPriority(anyLong(), any(), any());
    }

    @Test
    void shouldSaveWishListingEvenWhenBuyerOrPostProductIsNull() {
        WishListing withNulls = WishListing.builder()
                .id(null)
                .buyer(null)
                .postProduct(null)
                .priority(WishListPriority.MEDIUM)
                .note("nullable fields")
                .build();

        WishListing persisted = WishListing.builder()
                .id(42L)
                .buyer(null)
                .postProduct(null)
                .priority(WishListPriority.MEDIUM)
                .note("nullable fields")
                .build();

        when(wishListingRepository.save(withNulls)).thenReturn(persisted);

        WishListing result = wishListingService.addWishList(withNulls);

        assertNotNull(result);
        assertEquals(42L, result.getId());
        verify(wishListingRepository, times(1)).save(withNulls);
    }
}
