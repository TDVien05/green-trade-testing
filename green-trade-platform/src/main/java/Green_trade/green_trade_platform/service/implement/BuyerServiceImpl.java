package Green_trade.green_trade_platform.service.implement;

import Green_trade.green_trade_platform.enumerate.AccountStatus;
import Green_trade.green_trade_platform.enumerate.OrderStatus;
import Green_trade.green_trade_platform.exception.*;
import Green_trade.green_trade_platform.model.*;
import Green_trade.green_trade_platform.repository.*;
import Green_trade.green_trade_platform.request.MailRequest;
import Green_trade.green_trade_platform.request.PlaceOrderRequest;
import Green_trade.green_trade_platform.model.Wallet;
import Green_trade.green_trade_platform.repository.BuyerRepository;
import Green_trade.green_trade_platform.repository.WalletRepository;
import Green_trade.green_trade_platform.request.ProfileRequest;
import Green_trade.green_trade_platform.request.UpdateBuyerProfileRequest;
import Green_trade.green_trade_platform.util.DateUtils;
import Green_trade.green_trade_platform.util.FileUtils;
import Green_trade.green_trade_platform.util.StringUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
@AllArgsConstructor
public class BuyerServiceImpl {
    private final BuyerRepository buyerRepository;
    private final CloudinaryService cloudinaryService;
    //    private final DateUtils dateUtils;
//    private final FileUtils fileUtils;
    private final WalletRepository walletRepository;
    private final OrderRepository orderRepository;
    //    private final PaymentRepository paymentRepository;
//    private final TransactionRepository transactionRepository;
    private final ShippingPartnerRepository shippingPartnerRepository;
    private final StringUtils stringUtils;
    private final WalletServiceImpl walletService;
    private final PostProductRepository postProductRepository;
    private final MailServiceImpl mailSender;

    public Map<String, Object> uploadBuyerProfile(ProfileRequest request, MultipartFile avatarFile) throws IOException {
        Buyer buyer = getCurrentUser();

        Map<String, Object> body = new HashMap<>();
        String avatarUrl = (buyer.getAvatarUrl() == null) ? "" : buyer.getAvatarUrl();
        if (!avatarUrl.isEmpty()) {
            throw new DuplicateProfileException("Profile already exits.");
        }
        // // Check date and parse into LocalDate
        //// LocalDate dob = dateUtils.parseAndValidateDob(request.getDob());
        LocalDate dob = LocalDate.parse(request.getDob());
        log.info(">>> [Buyer service] Profile request: {}", request.toString());

        try {
            if (!avatarFile.isEmpty() && !avatarFile.isEmpty()) {
                Map<String, String> uploadResult = cloudinaryService.upload(avatarFile,
                        "buyers/" + buyer.getBuyerId() + ":" + buyer.getUsername() + "/avatar");
                avatarUrl = uploadResult.get("fileUrl");
                buyer.setAvatarPublicId(uploadResult.get("publicId"));
                body.put("avatar", avatarUrl);
            }
            buyer.setAvatarUrl(avatarUrl);
            buyer.setStreet(request.getStreet());
            buyer.setWardName(request.getWardName());
            buyer.setDistrictName(request.getDistrictName());
            buyer.setProvinceName(request.getProvinceName());
            buyer.setFullName(request.getFullName());
            buyer.setPhoneNumber(request.getPhoneNumber());
            buyer.setDob(dob);
            buyer.setGender(request.getGender());
            buyerRepository.save(buyer);
            body.put("profile", buyer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return body;
    }

    public Buyer updateProfile(UpdateBuyerProfileRequest request, MultipartFile avatarFile) throws Exception {
        try {
            log.info(">>> [Update user profile service] Starting update profile service");
            Buyer buyer = getCurrentUser();
            log.info(">>> [Update user profile service] Buyer's information: {}", buyer);
            Long id = buyer.getBuyerId();

            log.info(">>> [Update profile services] profile request: {}.", request);
            buyer.setFullName(request.getFullName() == null ? "" : request.getFullName());
            buyer.setEmail(request.getEmail() == null ? "" : request.getEmail());
            buyer.setGender(request.getGender());
            buyer.setDob(request.getDob());
            buyer.setPhoneNumber(request.getPhoneNumber() == null ? "" : request.getPhoneNumber());
            buyer.setStreet(request.getStreet());
            buyer.setWardName(request.getWardName());
            buyer.setDistrictName(request.getDistrictName());
            buyer.setProvinceName(request.getProvinceName());
            log.info(">>> [Update profile services] set text data into buyer profile.");

            // delete old avatar on cloudinary
            if (avatarFile != null && !avatarFile.isEmpty()) {
                log.info(">>> [Update profile services] Starting delete old avatar on Cloudinary.");
                if (buyer.getAvatarUrl() != null) {
                    boolean isDeleted = cloudinaryService.delete(
                            buyer.getAvatarPublicId(),
                            "buyers/" + buyer.getBuyerId() + ":" + buyer.getUsername() + "/avatar");

                    if (!isDeleted) {
                        log.info(">>> [Update profile services] Error occur when deleting old avatar.");
                        throw new ProfileException("Avatar Profile is deleted failed");
                    }
                    log.info(">>> [Update profile services] Delete old avatar successfully.");
                }

                // upload new avatar on cloudinary
                log.info(">>> [Update profile services] Starting upload new avatar into Cloudinary");
                Map<String, String> uploadResult = cloudinaryService.upload(
                        avatarFile,
                        "buyers/" + buyer.getBuyerId() + ":" + buyer.getUsername() + "/avatar");

                if (uploadResult == null) {
                    log.info(">>> [Update profile services] Error occur when uploading new avatar into Cloudinary.");
                    throw new Exception("Avatar Profile is saved failed");
                }

                log.info(">>> [Update profile services] Avatar is uploaded into Cloudinary.");

                buyer.setAvatarUrl(uploadResult.get("fileUrl"));
                buyer.setAvatarPublicId(uploadResult.get("publicId"));
            }
            log.info(">>> [Update profile services] Update buyer profile successfully.");
            return buyerRepository.save(buyer);
        } catch (Exception e) {
            log.info(">>> [Update profile services] Error at buyerServiceImpl: {}", e.getMessage());
            throw e;
        }
    }

    public Buyer getCurrentUser() {
        log.info(">>> [Buyer Service] Get current user: started.");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info(">>> [Buyer Service] Authentication: {}", authentication);
        String username = authentication.getName();

        return buyerRepository.findByUsername(username)
                .orElseThrow(() -> new ProfileException("User is not existed: " + username));
    }

    public Buyer findBuyerById(Long id) {
        return buyerRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can not find buyer with this id: " + id)
        );
    }

    public Buyer getBuyerFromVnPayRequest(String vnpOtherType) {
        String[] temp = vnpOtherType.split(" ");
        return buyerRepository.findById(Long.parseLong(temp[0]))
                .orElseThrow(() -> new UsernameNotFoundException("User is not existed: " + temp[0]));
    }

    public BigDecimal getWalletBalance() {
        Buyer buyer = getCurrentUser();
        return buyerRepository.findBalanceByBuyerId(buyer.getBuyerId());
    }

    public boolean isBuyerExisted(Long buyerId) {
        boolean result = false;
        Optional<Buyer> buyerOpt = buyerRepository.findById(buyerId);
        if (buyerOpt.isPresent()) {
            result = true;
        }
        return result;
    }

    public boolean isBuyerExisted(String username) {
        boolean result = false;
        Optional<Buyer> buyerOpt = buyerRepository.findByUsername(username);
        if (buyerOpt.isPresent()) {
            result = true;
        }
        return result;
    }

    public Order placeOrderCOD(PlaceOrderRequest request) throws Exception {
        Optional<Buyer> buyerOpt = buyerRepository.findByUsername(request.getUsername());
        Optional<PostProduct> postProductOpt = postProductRepository.findById(request.getPostProductId());

        // kiểm tra các thứ
        if (!isBuyerExisted(request.getUsername())) {
            throw new ProfileException("User is not existed");
        }

        if (postProductOpt.isEmpty()) {
            throw new PostProductNotFound();
        }

        if (postProductOpt.get().isSold()) {
            throw new ProductSoldOutException();
        }

        // tạo mới một đơn hàng
        Order newOrder = Order.builder()
                .buyer(buyerOpt.get())
                .orderCode(null)
                .shippingAddress(
                        stringUtils.fullAddress(
                                request.getStreet(),
                                request.getWardName(),
                                request.getDistrictName(),
                                request.getProvinceName())
                )
                .phoneNumber(
                        request.getPhoneNumber().isBlank() ? buyerOpt.get().getPhoneNumber() : request.getPhoneNumber())
                .transactions(null)
                .price(postProductOpt.get().getPrice())
                .status(OrderStatus.PENDING)
                .cancelOrderReason(null)
                .canceledAt(null)
                .build();

        return orderRepository.save(newOrder);
    }

    public Order placeOrder(PlaceOrderRequest request, String shippingFee) throws Exception {
        // kiểm tra các thứ
        if (!isBuyerExisted(request.getUsername())) {
            throw new ProfileException("User is not existed");
        }
        log.info(">>> [BuyerServiceImpl] checked buyer existed passed");
        Optional<Buyer> buyerOpt = buyerRepository.findByUsername(request.getUsername());
        if (!walletService.isBuyerHasWallet(buyerOpt.get())) {
            throw new WalletNotFoundException("The wallet of User is not existed");
        }
        log.info(">>> [BuyerServiceImpl] checked buyer wallet existed passed");
        Optional<PostProduct> postProductOpt = postProductRepository.findById(request.getPostProductId());
        if (postProductOpt.isEmpty()) {
            throw new PostProductNotFound();
        }
        log.info(">>> [BuyerServiceImpl] checked post product existed passed");
        if (postProductOpt.get().isSold()) {
            throw new ProductSoldOutException();
        }
        log.info(">>> [BuyerServiceImpl] get post product passed");

        ShippingPartner shippingPartner = shippingPartnerRepository.findById(request.getShippingPartnerId())
                .orElseThrow(
                        () -> new Exception("Shipping Partner is not existed"));
        log.info(">>> [BuyerServiceImpl] checked shipping partner existed passed");

        // tạo mới một đơn hàng
        Order newOrder = Order.builder()
                .postProduct(postProductOpt.get())
                .buyer(buyerOpt.get())
                .orderCode(null)
                .shippingAddress(
                        stringUtils.fullAddress(
                                request.getStreet(),
                                request.getWardName(),
                                request.getDistrictName(),
                                request.getProvinceName())
                )
                .phoneNumber(
                        request.getPhoneNumber().isBlank() ? buyerOpt.get().getPhoneNumber() : request.getPhoneNumber())
                .shippingPartner(shippingPartner)
                .shippingFee(new BigDecimal(shippingFee))
                .transactions(null)
                .price(postProductOpt.get().getPrice())
                .status(OrderStatus.PENDING)
                .cancelOrderReason(null)
                .canceledAt(null)
                .build();
        log.info(">>> [BuyerServiceImpl] checked post product existed passed");

        return orderRepository.save(newOrder);
    }

    public Order updateOrderCode(Order newOrder, String shippingCode) {
        newOrder.setOrderCode(shippingCode);
        return orderRepository.save(newOrder);
    }

    public Buyer findBuyerByUsername(String username) {
        Buyer foundBuyer = null;
        Optional<Buyer> buyerOpt = buyerRepository.findByUsername(username);
        if (buyerOpt.isPresent()) {
            foundBuyer = buyerOpt.get();
        }
        return foundBuyer;
    }

    public Buyer findBuyerBySellerId(Long sellerId) {
        return buyerRepository.findBySeller_SellerId(sellerId).orElseThrow(
                () -> new IllegalArgumentException("Can not find buyer with this seller id.")
        );
    }

    public Wallet getWallet() {
        Buyer buyer = getCurrentUser();
        return walletRepository.findByBuyer(buyer).orElseThrow();
    }

    public Page<Buyer> getListBuyers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("buyerId").ascending());
        return buyerRepository.findAll(pageable);
    }

    public void blockAccount(long id, String message, String activity) {
        log.info(">>> [Buyer Service] Block account: Started.");
        Buyer buyer = buyerRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Can not find buyer with this id: " + id)
        );
        log.info(">>> [Seller Service] Buyer info: {}", buyer.getFullName());
        buyer.setActive(false);
        if (activity.equalsIgnoreCase("block")) {
            buyer.setActive(false);
        } else if (activity.equalsIgnoreCase("unblock")) {
            buyer.setActive(true);
        } else {
            throw new IllegalArgumentException("Activity must be 'block' or 'unblock'");
        }
        buyerRepository.save(buyer);
        // ✅ Soạn nội dung email HTML
        String action = activity.equalsIgnoreCase("block") ? "bị khóa" : "được mở khóa";
        String htmlMessage = """
                <div style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>
                    <h2 style='color: #4CAF50;'>🌿 Thông báo từ Green Trade Platform</h2>
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Tài khoản của bạn đã <strong style='color:%s;'>%s</strong> bởi hệ thống quản trị.</p>
                    <p><strong>Lý do:</strong> %s</p>
                    <hr style='border: none; border-top: 1px solid #ccc;'/>
                    <p>Nếu bạn cho rằng đây là nhầm lẫn, vui lòng liên hệ 
                    <a href='mailto:green.trade.platform.391@gmail.com' style='color:#4CAF50;font-weight:bold;'>
                        đội ngũ hỗ trợ Green Trade
                    </a> để được giúp đỡ.</p>
                    <p>💚 Cảm ơn bạn đã tin tưởng sử dụng nền tảng Green Trade!</p>
                </div>
                """.formatted(
                buyer.getFullName(),
                activity.equalsIgnoreCase("block") ? "#e74c3c" : "#4CAF50",
                action.toUpperCase(),
                message
        );

        // ✅ Gửi mail đẹp
        MailRequest mailRequest = MailRequest.builder()
                .from("green.trade.platform.391@gmail.com")
                .to(buyer.getEmail())
                .subject("Green Trade - Thông báo " + (activity.equalsIgnoreCase("block") ? "Khóa tài khoản" : "Mở khóa tài khoản"))
                .message(htmlMessage)
                .build();

        mailSender.sendBeautifulMail(mailRequest);
    }
}
