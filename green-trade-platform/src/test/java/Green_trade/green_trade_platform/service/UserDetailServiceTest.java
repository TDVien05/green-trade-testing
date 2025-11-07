package Green_trade.green_trade_platform.service;

import Green_trade.green_trade_platform.enumerate.SellerStatus;
import Green_trade.green_trade_platform.model.Admin;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Seller;
import Green_trade.green_trade_platform.repository.AdminRepository;
import Green_trade.green_trade_platform.repository.BuyerRepository;
import Green_trade.green_trade_platform.repository.SellerRepository;
import Green_trade.green_trade_platform.service.implement.UserDetailsServiceCustomer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserDetailServiceTest {

    private BuyerRepository buyerRepository;
    private SellerRepository sellerRepository;
    private AdminRepository adminRepository;
    private UserDetailsServiceCustomer service;

    @BeforeEach
    void setup() {
        buyerRepository = mock(BuyerRepository.class);
        sellerRepository = mock(SellerRepository.class);
        adminRepository = mock(AdminRepository.class);
        service = new UserDetailsServiceCustomer(buyerRepository, sellerRepository, adminRepository);
    }

    @Test
    void shouldReturnAdminUserDetailsWhenEmployeeNumberMatches() {
        String employeeNumber = "1234567890";
        Admin admin = Admin.builder()
                .employeeNumber(employeeNumber)
                .password("adminPass")
                .build();

        when(adminRepository.findByEmployeeNumber(employeeNumber)).thenReturn(Optional.of(admin));

        UserDetails details = service.loadUserByUsername(employeeNumber);

        assertEquals(employeeNumber, details.getUsername());
        assertEquals("adminPass", details.getPassword());
        assertTrue(details.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals));
        verify(adminRepository).findByEmployeeNumber(employeeNumber);
        verifyNoInteractions(buyerRepository);
        verifyNoInteractions(sellerRepository);
    }

    @Test
    void shouldReturnSellerRoleForBuyerWithAcceptedSeller() {
        String username = "buyerUser";
        Buyer buyer = Buyer.builder().username(username).password("buyerPass").build();
        Seller seller = Seller.builder().status(SellerStatus.ACCEPTED).buyer(buyer).build();

        when(buyerRepository.findByUsername(username)).thenReturn(Optional.of(buyer));
        when(sellerRepository.findByBuyer(buyer)).thenReturn(Optional.of(seller));

        UserDetails details = service.loadUserByUsername(username);

        assertEquals(username, details.getUsername());
        assertEquals("buyerPass", details.getPassword());
        assertTrue(details.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_SELLER"::equals));
        verify(buyerRepository).findByUsername(username);
        verify(sellerRepository).findByBuyer(buyer);
        verify(adminRepository, never()).findByEmployeeNumber(anyString());
    }

    @Test
    void shouldReturnBuyerRoleWhenNoAcceptedSeller() {
        String username = "simpleBuyer";
        Buyer buyer = Buyer.builder().username(username).password("pass").build();

        when(buyerRepository.findByUsername(username)).thenReturn(Optional.of(buyer));
        when(sellerRepository.findByBuyer(buyer)).thenReturn(Optional.empty());

        UserDetails details = service.loadUserByUsername(username);

        assertEquals(username, details.getUsername());
        assertEquals("pass", details.getPassword());
        List<String> authorities = details.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertTrue(authorities.contains("ROLE_BUYER"));
        assertFalse(authorities.contains("ROLE_SELLER"));
        verify(buyerRepository).findByUsername(username);
        verify(sellerRepository).findByBuyer(buyer);
        verify(adminRepository, never()).findByEmployeeNumber(anyString());
    }

    @Test
    void shouldThrowWhenNoAdminAndNoBuyerFound() {
        String username = "0123456789"; // matches admin regex

        when(adminRepository.findByEmployeeNumber(username)).thenReturn(Optional.empty());
        when(buyerRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername(username));

        verify(adminRepository).findByEmployeeNumber(username);
        verify(buyerRepository).findByUsername(username);
        verifyNoInteractions(sellerRepository);
    }

    @Test
    void shouldNotGrantSellerRoleWhenSellerNotAccepted() {
        String username = "buyerWithPendingSeller";
        Buyer buyer = Buyer.builder().username(username).password("pwd").build();
        Seller pendingSeller = Seller.builder().status(SellerStatus.PENDING).buyer(buyer).build();

        when(buyerRepository.findByUsername(username)).thenReturn(Optional.of(buyer));
        when(sellerRepository.findByBuyer(buyer)).thenReturn(Optional.of(pendingSeller));

        UserDetails details = service.loadUserByUsername(username);

        assertEquals(username, details.getUsername());
        assertEquals("pwd", details.getPassword());
        List<String> authorities = details.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertTrue(authorities.contains("ROLE_BUYER"));
        assertFalse(authorities.contains("ROLE_SELLER"));
        verify(buyerRepository).findByUsername(username);
        verify(sellerRepository).findByBuyer(buyer);
        verify(adminRepository, never()).findByEmployeeNumber(anyString());
    }

    @Test
    void shouldBypassAdminCheckForNonNumericUsername() {
        String username = "user123abc"; // non 10-digit
        Buyer buyer = Buyer.builder().username(username).password("pwd2").build();

        when(buyerRepository.findByUsername(username)).thenReturn(Optional.of(buyer));
        when(sellerRepository.findByBuyer(buyer)).thenReturn(Optional.empty());

        UserDetails details = service.loadUserByUsername(username);

        assertEquals(username, details.getUsername());
        assertEquals("pwd2", details.getPassword());
        assertTrue(details.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_BUYER"::equals));
        verify(adminRepository, never()).findByEmployeeNumber(ArgumentMatchers.anyString());
        verify(buyerRepository).findByUsername(username);
        verify(sellerRepository).findByBuyer(buyer);
    }
}
