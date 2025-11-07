package Green_trade.green_trade_platform.mapper;

import Green_trade.green_trade_platform.model.Buyer;
import Green_trade.green_trade_platform.model.Seller;
import Green_trade.green_trade_platform.request.UpgradeAccountRequest;
import Green_trade.green_trade_platform.response.SellerResponse;
import org.springframework.stereotype.Component;

@Component
public class SellerMapper {
    public Seller toEntity(UpgradeAccountRequest request, Buyer buyer, String frontIdentity,
                           String license, String backIdentity, String selfie, String policy) {
        return Seller.builder().buyer(buyer)
                .identityFrontImageUrl(frontIdentity)
                .businessLicenseUrl(license)
                .identityBackImageUrl(backIdentity)
                .selfieUrl(selfie)
                .storeName(request.getStoreName())
                .taxNumber(request.getTaxNumber())
                .identityNumber(request.getIdentityNumber())
                .sellerName(request.getSellerName())
                .nationality(request.getNationality())
                .home(request.getHome())
                .storePolicyUrl(policy)
                .build();
    }

    public SellerResponse toDto(Seller seller) {
        if (seller == null) return null;

        Buyer buyer = seller.getBuyer();

        return SellerResponse.builder()
                .sellerId(seller.getSellerId())
                .storeName(seller.getStoreName())
                .status(seller.getStatus())
                .storePolicyUrl(seller.getStorePolicyUrl())
                .taxNumber(seller.getTaxNumber())
                .createAt(seller.getCreatedAt())
                .updateAt(seller.getUpdatedAt())
                .identityFrontImageUrl(seller.getIdentityFrontImageUrl())
                .identityBackImageUrl(seller.getIdentityBackImageUrl())
                .businessLicenseUrl(seller.getBusinessLicenseUrl())
                .selfieUrl(seller.getSelfieUrl())
                .sellerName(seller.getSellerName())
                .nationality(seller.getNationality())
                .home(seller.getHome())
                .build();
    }
}
