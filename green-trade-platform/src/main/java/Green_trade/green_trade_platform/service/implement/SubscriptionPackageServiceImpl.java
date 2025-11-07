package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.mapper.SubscriptionMapper;
import Green_trade.green_trade_platform.mapper.SubscriptionPackageMapper;
import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Seller;
import Green_trade.green_trade_platform.model.Subscription;
import Green_trade.green_trade_platform.model.SubscriptionPackages;
import Green_trade.green_trade_platform.repository.SellerRepository;
import Green_trade.green_trade_platform.repository.SubscriptionPackagesRepository;
import Green_trade.green_trade_platform.repository.SubscriptionRepository;
import Green_trade.green_trade_platform.request.SignPackageRequest;
import Green_trade.green_trade_platform.response.SignPackageResponse;
import Green_trade.green_trade_platform.response.SubscriptionPackageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPackageServiceImpl {

    private final SubscriptionPackagesRepository subscriptionPackageRepository;
    private final SubscriptionPackageMapper subscriptionPackageMapper;
    private final BuyerServiceImpl buyerService;
    private final WalletServiceImpl walletService;
    private final SubscriptionRepository subscriptionRepository;
    private final SellerRepository sellerRepository;
    private final SubscriptionMapper subscriptionMapper;

    public Page<SubscriptionPackages> getActivePackages(Pageable pageable) {
        return subscriptionPackageRepository.findByIsActiveTrue(pageable);
    }

    public Page<SubscriptionPackageResponse> getActivePackageResponses(Pageable pageable) {
        return getActivePackages(pageable)
                .map(subscriptionPackageMapper::toResponse);
    }

    public Map<String, Object> handlesignPackage(SignPackageRequest request) {
        Map<String, Object> result = new HashMap<>();
        Buyer buyer = buyerService.getCurrentUser();
        Seller seller = sellerRepository.findByBuyer(buyer)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người bán với id: " + buyer.getBuyerId()));
        SubscriptionPackages subscriptionPackages = subscriptionPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói người bán với id " + request.getPackageId()));



        Optional<Subscription> exitsSubscription = subscriptionRepository.findFirstBySeller_SellerIdOrderByEndDayDesc(seller.getSellerId());
        if (exitsSubscription.isPresent() && exitsSubscription.get().getIsActive() == true) {
            throw new IllegalArgumentException("Bạn đã đăng kí gói. Vui lòng hủy gói để đăng kí gói mới.");
        }

        boolean isValidBalance = isValidWalletBalance(request);
        if (!isValidBalance) {
            result.put("success", false);
            result.put("data", null);
            return result;
        }

        Map<String, Object> walletResult = walletService.handleSignPackageForSeller(buyer, request.getPrice());

        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(request.getDurationByDay());
        Subscription subscription = Subscription.builder()
                .seller(seller)
                .subscriptionPackage(subscriptionPackages)
                .startDay(startDate)
                .endDay(endDate)
                .remainPost(subscriptionPackages.getMaxProduct())
                .build();

        Subscription temp = subscriptionRepository.save(subscription);
        SignPackageResponse signPackageResponse = subscriptionMapper.
                toSignPackageResponse(subscriptionPackages.getName(),
                        buyer.getFullName(),
                        request.getPrice(), ChronoUnit.DAYS.between(startDate, endDate),
                        startDate,
                        endDate);

        result.put("success", true);
        result.put("subscription", signPackageResponse);

        return result;
    }

    public boolean isValidWalletBalance(SignPackageRequest request) {
        BigDecimal walletBalance = buyerService.getWalletBalance();
        log.info(">>> Buyer's wallet balance: {}", walletBalance);
        log.info(">>> Package price: {}", request.getPrice());
        return (walletBalance.doubleValue() >= request.getPrice());
    }

    public void cancelSubscription(Seller seller) {
        Subscription subscription = subscriptionRepository
                .findFirstBySeller_SellerIdOrderByEndDayDesc(seller.getSellerId())
                .orElseThrow(() ->
                        new IllegalArgumentException("This seller has not signed any subscription packages yet.")
                );

        if (subscription.getIsActive()) {
            subscription.setIsActive(false);
            subscription.setEndDay(LocalDateTime.now());
            subscriptionRepository.save(subscription);
        } else {
            throw new IllegalArgumentException("This seller's subscription is already inactive or expired.");
        }
    }

    public Subscription getCurrentSubscription(Seller seller) {
        log.info(">>> [SubscriptionService] Retrieving current active subscription...");

        Subscription subscription = subscriptionRepository
                .findFirstBySeller_SellerIdOrderByEndDayDesc(seller.getSellerId())
                .orElseThrow(() ->
                        new IllegalArgumentException("This seller has not signed any subscription packages yet.")
                );

        log.info(">>> Subscription active? {}", subscription.getIsActive());

        if (subscription.getIsActive() == null || !subscription.getIsActive()) {
            throw new IllegalArgumentException("The current subscription is out of date.");
        }

        log.info(">>> [SubscriptionService] Active subscription retrieved successfully.");
        return subscription;
    }

    public Subscription updateRemainPost(Seller seller) {
        Subscription subscription = getCurrentSubscription(seller);
        long remainPost = subscription.getRemainPost();

        if (remainPost <= 0) {
            throw new IllegalStateException("No remaining posts available for this subscription.");
        }

        subscription.setRemainPost(remainPost - 1);
        return subscriptionRepository.save(subscription);
    }
}