package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.enumerate.OrderStatus;
import Green_trade.green_trade_platform.exception.DuplicateProfileException;
import Green_trade.green_trade_platform.exception.PostProductNotFound;
import Green_trade.green_trade_platform.exception.ProductSoldOutException;
import Green_trade.green_trade_platform.exception.ProfileException;
import Green_trade.green_trade_platform.exception.WalletNotFoundException;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Order;
import Green_trade.green_trade_platform.model.PostProduct;
import Green_trade.green_trade_platform.model.ShippingPartner;
import Green_trade.green_trade_platform.model.Wallet;
import Green_trade.green_trade_platform.repository.BuyerRepository;
import Green_trade.green_trade_platform.repository.OrderRepository;
import Green_trade.green_trade_platform.repository.PostProductRepository;
import Green_trade.green_trade_platform.repository.ShippingPartnerRepository;
import Green_trade.green_trade_platform.repository.WalletRepository;
import Green_trade.green_trade_platform.request.PlaceOrderRequest;
import Green_trade.green_trade_platform.request.ProfileRequest;
import Green_trade.green_trade_platform.request.UpdateBuyerProfileRequest;
import Green_trade.green_trade_platform.service.implement.BuyerServiceImpl;
import Green_trade.green_trade_platform.service.implement.CloudinaryService;
import Green_trade.green_trade_platform.service.implement.MailServiceImpl;
import Green_trade.green_trade_platform.service.implement.WalletServiceImpl;
import Green_trade.green_trade_platform.util.StringUtils;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BuyerServiceTest {

    @Mock
    private BuyerRepository buyerRepository;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ShippingPartnerRepository shippingPartnerRepository;
    @Mock
    private StringUtils stringUtils;
    @Mock
    private WalletServiceImpl walletService;
    @Mock
    private PostProductRepository postProductRepository;
    @Mock
    private MailServiceImpl mailService;

    @Mock
    private Authentication authentication;

    private BuyerServiceImpl buyerService;

    @BeforeEach
    void setUp() {
        buyerService = new BuyerServiceImpl(
                buyerRepository,
                cloudinaryService,
                walletRepository,
                orderRepository,
                shippingPartnerRepository,
                stringUtils,
                walletService,
                postProductRepository,
                mailService
        );

        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.getName()).thenReturn("john_doe");
    }

    private Buyer makeBuyer() {
        Buyer b = new Buyer();
        b.setBuyerId(1L);
        b.setUsername("john_doe");
        b.setEmail("john@example.com");
        b.setPhoneNumber("0123456789");
        b.setFullName("John Doe");
        b.setAvatarUrl(null);
        b.setAvatarPublicId(null);
        return b;
    }

    @Test
    void shouldUpdateOrderCodeAndSave() {
        Order order = Order.builder().id(1L).orderCode(null).build();
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = buyerService.updateOrderCode(order, "SHIP123");

        assertEquals("SHIP123", result.getOrderCode());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldReturnBuyerWhenUsernameExists() {
        Buyer buyer = Buyer.builder().username("john").build();
        when(buyerRepository.findByUsername("john")).thenReturn(Optional.of(buyer));

        Buyer result = buyerService.findBuyerByUsername("john");
        assertEquals(buyer, result);
    }

    @Test
    void shouldReturnNullWhenUsernameNotFound() {
        when(buyerRepository.findByUsername("missing")).thenReturn(Optional.empty());
        assertNull(buyerService.findBuyerByUsername("missing"));
    }

    @Test
    void shouldReturnBuyerBySellerId() {
        Buyer buyer = Buyer.builder().buyerId(2L).build();
        when(buyerRepository.findBySeller_SellerId(5L)).thenReturn(Optional.of(buyer));

        Buyer result = buyerService.findBuyerBySellerId(5L);
        assertEquals(buyer, result);
    }

    @Test
    void shouldThrowWhenBuyerNotFoundBySellerId() {
        when(buyerRepository.findBySeller_SellerId(9L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> buyerService.findBuyerBySellerId(9L));
    }


    @Test
    void shouldReturnPagedListOfBuyers() {
        Buyer buyer1 = Buyer.builder().buyerId(1L).build();
        Buyer buyer2 = Buyer.builder().buyerId(2L).build();

        Pageable pageable = PageRequest.of(0, 2, Sort.by("buyerId").ascending());
        Page<Buyer> page = new PageImpl<>(List.of(buyer1, buyer2), pageable, 2);

        when(buyerRepository.findAll(pageable)).thenReturn(page);

        Page<Buyer> result = buyerService.getListBuyers(0, 2);
        assertEquals(2, result.getTotalElements());
        verify(buyerRepository).findAll(pageable);
    }

    @Test
    void shouldReturnBuyerWhenIdExists() {
        Buyer buyer = Buyer.builder().buyerId(11L).build();
        when(buyerRepository.findById(11L)).thenReturn(Optional.of(buyer));

        Buyer result = buyerService.findBuyerById(11L);
        assertEquals(buyer, result);
    }

    @Test
    void shouldThrowWhenBuyerNotFoundById() {
        when(buyerRepository.findById(22L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> buyerService.findBuyerById(22L));
    }

    @Test
    void shouldReturnBuyerFromVnPayRequest() {
        Buyer buyer = Buyer.builder().buyerId(1L).build();
        when(buyerRepository.findById(1L)).thenReturn(Optional.of(buyer));

        Buyer result = buyerService.getBuyerFromVnPayRequest("1 VNPay");
        assertEquals(buyer, result);
    }

    @Test
    void shouldThrowWhenVnPayBuyerNotFound() {
        when(buyerRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> buyerService.getBuyerFromVnPayRequest("5 something"));
    }

    @Test
    void shouldReturnTrueWhenBuyerExistsById() {
        when(buyerRepository.findById(1L)).thenReturn(Optional.of(new Buyer()));
        assertTrue(buyerService.isBuyerExisted(1L));
    }

    @Test
    void shouldReturnFalseWhenBuyerNotExistsById() {
        when(buyerRepository.findById(2L)).thenReturn(Optional.empty());
        assertFalse(buyerService.isBuyerExisted(2L));
    }

    @Test
    void shouldReturnTrueWhenBuyerExistsByUsername() {
        when(buyerRepository.findByUsername("john")).thenReturn(Optional.of(new Buyer()));
        assertTrue(buyerService.isBuyerExisted("john"));
    }

    @Test
    void shouldReturnFalseWhenBuyerNotExistsByUsername() {
        when(buyerRepository.findByUsername("doe")).thenReturn(Optional.empty());
        assertFalse(buyerService.isBuyerExisted("doe"));
    }

    @Test
    void shouldPlaceOrderSuccessfully() throws Exception {
        Buyer buyer = Buyer.builder().username("john").phoneNumber("123456").build();
        PostProduct product = PostProduct.builder().id(5L).price(BigDecimal.valueOf(99.99)).sold(false).build();

        PlaceOrderRequest request = PlaceOrderRequest.builder()
                .username("john")
                .postProductId(5L)
                .street("123")
                .wardName("Ward 1")
                .districtName("District 9")
                .provinceName("HCMC")
                .phoneNumber("")
                .build();

        when(buyerRepository.findByUsername("john")).thenReturn(Optional.of(buyer));
        when(postProductRepository.findById(5L)).thenReturn(Optional.of(product));
        when(buyerRepository.findByUsername("john")).thenReturn(Optional.of(buyer));
        when(stringUtils.fullAddress(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("123 Ward 1, District 9, HCMC");

        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(100L);
            return o;
        });

        Order result = buyerService.placeOrderCOD(request);

        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(buyer, result.getBuyer());
        assertEquals(BigDecimal.valueOf(99.99), result.getPrice());
        assertEquals("123 Ward 1, District 9, HCMC", result.getShippingAddress());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldThrowWhenBuyerDoesNotExist() {
        PlaceOrderRequest request = PlaceOrderRequest.builder()
                .username("ghost").postProductId(1L).build();

        when(buyerRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(ProfileException.class, () -> buyerService.placeOrderCOD(request));
    }

    @Test
    void shouldThrowWhenPostProductNotFound() {
        Buyer buyer = Buyer.builder().username("john").build();
        PlaceOrderRequest request = PlaceOrderRequest.builder()
                .username("john").postProductId(10L).build();

        when(buyerRepository.findByUsername("john")).thenReturn(Optional.of(buyer));
        when(postProductRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(PostProductNotFound.class, () -> buyerService.placeOrderCOD(request));
    }

    @Test
    void shouldThrowWhenProductAlreadySold() {
        Buyer buyer = Buyer.builder().username("john").build();
        PostProduct post = PostProduct.builder().id(1L).sold(true).build();

        PlaceOrderRequest request = PlaceOrderRequest.builder()
                .username("john").postProductId(1L).build();

        when(buyerRepository.findByUsername("john")).thenReturn(Optional.of(buyer));
        when(postProductRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThrows(ProductSoldOutException.class, () -> buyerService.placeOrderCOD(request));
    }

    @Test
    void shouldUploadBuyerProfileWithAvatarSuccessfully() throws IOException {
        Buyer current = makeBuyer();
        when(buyerRepository.findByUsername("john_doe")).thenReturn(Optional.of(current));

        MultipartFile avatar = mock(MultipartFile.class);
        when(avatar.isEmpty()).thenReturn(false);

        Map<String, String> uploadRes = new HashMap<>();
        uploadRes.put("fileUrl", "http://cdn/avatar.jpg");
        uploadRes.put("publicId", "public123");
        when(cloudinaryService.upload(eq(avatar), anyString())).thenReturn(uploadRes);

        ProfileRequest req = ProfileRequest.builder()
                .fullName("John Doe")
                .phoneNumber("0123456789")
                .street("123 Street")
                .wardName("Ward")
                .districtName("District")
                .provinceName("Province")
                .gender(null)
                .dob(LocalDate.now().minusYears(20).toString())
                .build();

        when(buyerRepository.save(any(Buyer.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = buyerService.uploadBuyerProfile(req, avatar);

        assertEquals("http://cdn/avatar.jpg", result.get("avatar"));
        Buyer saved = (Buyer) result.get("profile");
        assertEquals("John Doe", saved.getFullName());
        assertEquals("0123456789", saved.getPhoneNumber());
        assertEquals("123 Street", saved.getStreet());
        assertEquals("Ward", saved.getWardName());
        assertEquals("District", saved.getDistrictName());
        assertEquals("Province", saved.getProvinceName());
        assertNotNull(saved.getDob());
        verify(cloudinaryService, times(1)).upload(eq(avatar), contains("buyers/1:john_doe/avatar"));
        verify(buyerRepository, times(1)).save(any(Buyer.class));
    }

    @Test
    void shouldUpdateProfileAndReplaceAvatarSuccessfully() throws Exception {
        Buyer current = makeBuyer();
        current.setAvatarUrl("http://old/avatar.jpg");
        current.setAvatarPublicId("oldPublicId");

        when(buyerRepository.findByUsername("john_doe")).thenReturn(Optional.of(current));

        MultipartFile newAvatar = mock(MultipartFile.class);
        when(newAvatar.isEmpty()).thenReturn(false);

        when(cloudinaryService.delete(eq("oldPublicId"), contains("buyers/1:john_doe/avatar"))).thenReturn(true);

        Map<String, String> uploadRes = new HashMap<>();
        uploadRes.put("fileUrl", "http://cdn/newAvatar.jpg");
        uploadRes.put("publicId", "newPublicId");
        when(cloudinaryService.upload(eq(newAvatar), anyString())).thenReturn(uploadRes);

        UpdateBuyerProfileRequest req = UpdateBuyerProfileRequest.builder()
                .fullName("Johnathan Doe")
                .email("johnathan@example.com")
                .gender(null)
                .dob(LocalDate.now().minusYears(25))
                .phoneNumber("0987654321")
                .street("456 Avenue")
                .wardName("NewWard")
                .districtName("NewDistrict")
                .provinceName("NewProvince")
                .build();

        when(buyerRepository.save(any(Buyer.class))).thenAnswer(inv -> inv.getArgument(0));

        Buyer updated = buyerService.updateProfile(req, newAvatar);

        assertEquals("Johnathan Doe", updated.getFullName());
        assertEquals("johnathan@example.com", updated.getEmail());
        assertEquals("0987654321", updated.getPhoneNumber());
        assertEquals("456 Avenue", updated.getStreet());
        assertEquals("NewWard", updated.getWardName());
        assertEquals("NewDistrict", updated.getDistrictName());
        assertEquals("NewProvince", updated.getProvinceName());
        assertEquals("http://cdn/newAvatar.jpg", updated.getAvatarUrl());
        assertEquals("newPublicId", updated.getAvatarPublicId());

        verify(cloudinaryService).delete(eq("oldPublicId"), contains("buyers/1:john_doe/avatar"));
        verify(cloudinaryService).upload(eq(newAvatar), contains("buyers/1:john_doe/avatar"));
        verify(buyerRepository).save(any(Buyer.class));
    }

    @Test
    void shouldPlacePrepaidOrderSuccessfully() throws Exception {
        // buyer and wallet exist
        Buyer buyer = makeBuyer();
        when(buyerRepository.findByUsername("john_doe")).thenReturn(Optional.of(buyer));
        when(buyerRepository.findByUsername("john_doe")).thenReturn(Optional.of(buyer));
        when(buyerRepository.existsByUsername("john_doe")).thenReturn(true); // not used but safety
        when(buyerRepository.findByUsername(anyString())).thenReturn(Optional.of(buyer));
        when(buyerRepository.findByUsername(eq("john_doe"))).thenReturn(Optional.of(buyer));

        when(walletService.isBuyerHasWallet(buyer)).thenReturn(true);

        // product
        PostProduct product = new PostProduct();
        product.setPrice(new BigDecimal("150000"));
        product.setSold(false);
        when(postProductRepository.findById(10L)).thenReturn(Optional.of(product));

        // shipping partner
        ShippingPartner partner = new ShippingPartner();
        partner.setId(5L);
        when(shippingPartnerRepository.findById(5L)).thenReturn(Optional.of(partner));

        // address
        when(stringUtils.fullAddress("123 Street", "Ward", "District", "Province"))
                .thenReturn("123 Street, Ward, District, Province");

        // request
        PlaceOrderRequest req = PlaceOrderRequest.builder()
                .postProductId(10L)
                .username("john_doe")
                .fullName("John Doe")
                .street("123 Street")
                .wardName("Ward")
                .districtName("District")
                .provinceName("Province")
                .phoneNumber("0123456789")
                .shippingPartnerId(5L)
                .paymentId(1L)
                .build();

        Order savedOrder = Order.builder().orderCode(null).build();
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = buyerService.placeOrder(req, "20000");

        assertNotNull(result);
        assertEquals("123 Street, Ward, District, Province", result.getShippingAddress());
        assertEquals("0123456789", result.getPhoneNumber());
        assertEquals(new BigDecimal("150000"), result.getPrice());
        assertEquals(partner, result.getShippingPartner());
        assertNotNull(result.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void shouldThrowWhenUploadingProfileIfAvatarAlreadyExists() throws IOException {
        Buyer current = makeBuyer();
        current.setAvatarUrl("http://already/exists.jpg");
        when(buyerRepository.findByUsername("john_doe")).thenReturn(Optional.of(current));

        MultipartFile avatar = mock(MultipartFile.class);

        ProfileRequest req = ProfileRequest.builder()
                .fullName("John Doe")
                .phoneNumber("0123456789")
                .street("123 Street")
                .wardName("Ward")
                .districtName("District")
                .provinceName("Province")
                .gender(null)
                .dob(LocalDate.now().minusYears(20).toString())
                .build();

        assertThrows(DuplicateProfileException.class, () -> buyerService.uploadBuyerProfile(req, avatar));
        verify(cloudinaryService, never()).upload(any(), anyString());
        verify(buyerRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenDeleteOldAvatarFailsDuringUpdate() throws IOException {
        Buyer current = makeBuyer();
        current.setAvatarUrl("http://old/avatar.jpg");
        current.setAvatarPublicId("oldPublicId");
        when(buyerRepository.findByUsername("john_doe")).thenReturn(Optional.of(current));

        MultipartFile newAvatar = mock(MultipartFile.class);
        when(newAvatar.isEmpty()).thenReturn(false);

        when(cloudinaryService.delete(eq("oldPublicId"), contains("buyers/1:john_doe/avatar"))).thenReturn(false);

        UpdateBuyerProfileRequest req = UpdateBuyerProfileRequest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .dob(LocalDate.now().minusYears(30))
                .phoneNumber("0123456789")
                .street("123 Street")
                .wardName("Ward")
                .districtName("District")
                .provinceName("Province")
                .build();

        assertThrows(ProfileException.class, () -> buyerService.updateProfile(req, newAvatar));
        verify(cloudinaryService).delete(eq("oldPublicId"), contains("buyers/1:john_doe/avatar"));
        verify(cloudinaryService, never()).upload(any(), anyString());
        verify(buyerRepository, never()).save(any());
    }

    @Test
    void shouldHandleBlockUnblockAndRejectInvalidActivity() {
        Buyer user = makeBuyer();
        user.setActive(true);
        when(buyerRepository.findById(1L)).thenReturn(Optional.of(user));
        when(buyerRepository.save(any(Buyer.class))).thenAnswer(inv -> inv.getArgument(0));

        // block
        buyerService.blockAccount(1L, "violation", "block");
        assertFalse(user.isActive());
        verify(mailService, times(1)).sendBeautifulMail(any());

        // unblock
        when(buyerRepository.findById(1L)).thenReturn(Optional.of(user));
        buyerService.blockAccount(1L, "resolved", "unblock");
        assertTrue(user.isActive());
        verify(mailService, times(2)).sendBeautifulMail(any());

        // invalid
        when(buyerRepository.findById(1L)).thenReturn(Optional.of(user));
        assertThrows(IllegalArgumentException.class, () -> buyerService.blockAccount(1L, "n/a", "freeze"));
        // ensure no additional email sent
        verify(mailService, times(2)).sendBeautifulMail(any());
    }
}
