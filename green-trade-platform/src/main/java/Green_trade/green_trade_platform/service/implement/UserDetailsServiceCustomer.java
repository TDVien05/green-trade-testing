package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.enumerate.SellerStatus;
import Green_trade.green_trade_platform.model.Admin;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Seller;
import Green_trade.green_trade_platform.repository.AdminRepository;
import Green_trade.green_trade_platform.repository.BuyerRepository;
import Green_trade.green_trade_platform.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceCustomer implements UserDetailsService {
    private final BuyerRepository buyerRepository;
    private final SellerRepository sellerRepository;
    private final AdminRepository adminRepository;
    private final String regex = "^\\d{10}$";

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Check admin
        if (username.matches(regex)) {
            Optional<Admin> admin = adminRepository.findByEmployeeNumber(username);
            if (admin.isPresent()) {
                return new org.springframework.security.core.userdetails.User(
                        admin.get().getEmployeeNumber(),
                        admin.get().getPassword(),
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );
            }
        }

        // Check buyer table
        Optional<Buyer> buyer = buyerRepository.findByUsername(username);
        if (buyer.isPresent()) {
            Optional<Seller> seller = sellerRepository.findByBuyer(buyer.get());
            if (seller.isPresent() && seller.get().getStatus() == SellerStatus.ACCEPTED) {
                return new org.springframework.security.core.userdetails.User(
                        buyer.get().getUsername(),
                        buyer.get().getPassword(),
                        List.of(new SimpleGrantedAuthority("ROLE_SELLER"))
                );
            }

            return new org.springframework.security.core.userdetails.User(
                    buyer.get().getUsername(),
                    buyer.get().getPassword(),
                    List.of(new SimpleGrantedAuthority("ROLE_BUYER"))
            );
        }
        throw new UsernameNotFoundException("User not found with username: " + username);
    }
}
