package Green_trade.green_trade_platform.repository;

import Green_trade.green_trade_platform.enumerate.VerifiedDecisionStatus;
import Green_trade.green_trade_platform.model.PostProduct;
import Green_trade.green_trade_platform.model.Seller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostProductRepository extends JpaRepository<PostProduct, Long> {
    Page<PostProduct> findBySeller(Seller seller, Pageable pageable);

    Page<PostProduct> findAllBySoldFalse(Pageable pageable);

    Page<PostProduct> findAllBySoldFalseAndActiveTrue(Pageable pageable);

    Page<PostProduct> findAllByVerifiedDecisionstatus(VerifiedDecisionStatus status, Pageable pageable);

    List<PostProduct> findAllBySeller(Seller seller);
}
