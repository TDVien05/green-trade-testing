-- =========================================================
-- 🚀 RESET DATABASE DỮ LIỆU DEMO
-- =========================================================
SET
GLOBAL time_zone = 'Asia/Ho_Chi_Minh';
SET
FOREIGN_KEY_CHECKS = 0;
SET
GLOBAL event_scheduler = ON;


DELETE
FROM product_image;
DELETE
FROM post_product;
DELETE
FROM wallet;
DELETE
FROM seller;
DELETE
FROM buyer;
DELETE
FROM admin;
DELETE
FROM subscription_packages;
DELETE
FROM package_price;
DELETE
FROM category;
DELETE
FROM system_policy;
DELETE
FROM subscription;
DELETE
FROM shipping_partner;
DELETE
FROM dispute_category;
DELETE
FROM orders;
DELETE
FROM payment;
DELETE
FROM dispute;
DELETE
FROM evidence;
DELETE
FROM notification;
DELETE
FROM system_wallet;
DELETE
FROM wallet_transaction;
DELETE
FROM conversation;
DELETE
FROM wish_listing;
DELETE
FROM reviews;
DELETE
FROM product_image;
DELETE
FROM review_image;
DELETE
FROM transactions;
DELETE
FROM cancel_order_reason;
DELETE
FROM invoice;
DELETE FROM conversation;
DELETE FROM message;
DROP
EVENT IF EXISTS auto_resolve_escrow;

ALTER TABLE message AUTO_INCREMENT = 1;
ALTER TABLE conversation AUTO_INCREMENT = 1;
ALTER TABLE transactions AUTO_INCREMENT = 1;
ALTER TABLE review_image AUTO_INCREMENT = 1;
ALTER TABLE product_image AUTO_INCREMENT = 1;
ALTER TABLE conversation AUTO_INCREMENT = 1;
ALTER TABLE wish_listing AUTO_INCREMENT = 1;
ALTER TABLE reviews AUTO_INCREMENT = 1;
ALTER TABLE product_image AUTO_INCREMENT = 1;
ALTER TABLE post_product AUTO_INCREMENT = 1;
ALTER TABLE wallet AUTO_INCREMENT = 1;
ALTER TABLE seller AUTO_INCREMENT = 1;
ALTER TABLE buyer AUTO_INCREMENT = 1;
ALTER TABLE admin AUTO_INCREMENT = 1;
ALTER TABLE subscription_packages AUTO_INCREMENT = 1;
ALTER TABLE package_price AUTO_INCREMENT = 1;
ALTER TABLE category AUTO_INCREMENT = 1;
ALTER TABLE orders AUTO_INCREMENT = 1;
ALTER TABLE shipping_partner AUTO_INCREMENT = 1;
ALTER TABLE dispute_category AUTO_INCREMENT = 1;
ALTER TABLE subscription AUTO_INCREMENT = 1;
ALTER TABLE payment AUTO_INCREMENT = 1;
ALTER TABLE dispute AUTO_INCREMENT = 1;
ALTER TABLE evidence AUTO_INCREMENT = 1;
ALTER TABLE notification AUTO_INCREMENT = 1;
ALTER TABLE system_wallet AUTO_INCREMENT = 1;
ALTER TABLE wallet_transaction AUTO_INCREMENT = 1;


CREATE
EVENT IF NOT EXISTS auto_resolve_escrow
ON SCHEDULE EVERY 1 DAY
DO
UPDATE wallet_system ws
    JOIN wallet w
ON ws.seller_wallet_id = w.wallet_id
    SET
        w.balance = w.balance + ws.balance, ws.status = 'IS_SOLVE'
WHERE
    ws.status = 'ESCROW_HOLD'
  AND ws.created_at <= NOW() - INTERVAL 14 DAY;

SET
FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- 📦 INSERT GÓI ĐĂNG KÝ
-- =========================================================
INSERT INTO subscription_packages (subscription_package_id,
                                   package_name,
                                   description,
                                   is_active,
                                   max_product,
                                   max_img_per_post,
                                   created_at,
                                   updated_at)
VALUES
-- BASIC PLAN
(1, 'Basic Plan',
 CONCAT(
         'Gói Cơ Bản – Dành cho cá nhân trải nghiệm', CHAR(10), 'Thời hạn: 30 ngày.', CHAR(10),
         'Quản lý & Sản phẩm: đăng tối đa 10 sản phẩm, mỗi sản phẩm tối đa 5 hình ảnh.', CHAR(10),
         'Hiển thị & Thương hiệu: hiển thị cơ bản trong danh mục và kết quả tìm kiếm.', CHAR(10),
         'Hỗ trợ & Phí: hỗ trợ qua email hoặc chat với thời gian phản hồi tiêu chuẩn.', CHAR(10),
         'Phí hoa hồng doanh thu khoảng 7%.'
 ),
 TRUE, 10, 5, NOW(), NOW()),

-- PRO PLAN
(2, 'Pro Plan',
 CONCAT(
         'Gói Pro – Dành cho doanh nghiệp nhỏ', CHAR(10), 'Thời hạn: 30 ngày.', CHAR(10),
         'Quản lý & Sản phẩm: đăng tối đa 30 sản phẩm, mỗi sản phẩm tối đa 7 hình ảnh.', CHAR(10),
         'Hiển thị & Thương hiệu: sản phẩm được ưu tiên hiển thị cao hơn trong danh mục sản phẩm.', CHAR(10),
         'Hỗ trợ & Phí: phản hồi nhanh hơn qua email/chat, có hotline trong giờ hành chính.', CHAR(10),
         'Phí hoa hồng doanh thu khoảng 5%.'
 ),
 TRUE, 30, 7, NOW(), NOW()),

-- VIP PLAN
(3, 'VIP Plan',
 CONCAT(
         'Gói VIP – Dành cho doanh nghiệp lớn', CHAR(10), 'Thời hạn: 30 ngày.', CHAR(10),
         'Quản lý & Sản phẩm: đăng tối đa 100 sản phẩm, mỗi sản phẩm tối đa 10 hình ảnh.', CHAR(10),
         'Hiển thị & Thương hiệu: sản phẩm được ưu tiên cao nhất trong kết quả tìm kiếm và có thể hiển thị logo thương hiệu.', CHAR(10),
         'Hỗ trợ & Phí: hỗ trợ 24/7 với thời gian phản hồi nhanh nhất.', CHAR(10), 'Phí hoa hồng doanh thu khoảng 3%.'
 ),
 TRUE, 100, 10, NOW(), NOW()),

-- LEGACY PLAN
(4, 'Legacy Plan',
 'Gói cũ, không còn được hỗ trợ hoặc cập nhật. Dành cho người dùng đã đăng ký trước khi hệ thống nâng cấp.',
 FALSE, 20, 5, NOW(), NOW()) ON DUPLICATE KEY
UPDATE subscription_package_id = subscription_package_id;


-- =========================================================
-- 💰 INSERT GIÁ CÁC GÓI
-- =========================================================
INSERT INTO package_price (price,
                           is_active,
                           duration_by_day,
                           currency,
                           discount_percent,
                           created_at,
                           updated_at,
                           package_id)
VALUES (200000, TRUE, 30, 'VND', 0, NOW(), NOW(), 1),
       (540000, TRUE, 90, 'VND', 7, NOW(), NOW(), 1),
       (900000, TRUE, 90, 'VND', 10, NOW(), NOW(), 1),

       (400000, TRUE, 30, 'VND', 0, NOW(), NOW(), 2),
       (1080000, TRUE, 90, 'VND', 8, NOW(), NOW(), 2),
       (1800000, TRUE, 180, 'VND', 15, NOW(), NOW(), 2),

       (1200000, TRUE, 30, 'VND', 0, NOW(), NOW(), 3),
       (3240000, TRUE, 90, 'VND', 10, NOW(), NOW(), 3),
       (5400000, TRUE, 180, 'VND', 20, NOW(), NOW(), 3),

       (99000, FALSE, 30, 'VND', 0, NOW(), NOW(), 4) ON DUPLICATE KEY
UPDATE package_id = package_id;

-- =========================================================
-- 🧑‍💼 ADMIN
-- =========================================================
INSERT INTO admin (avatar_url,
                   employee_number,
                   password,
                   full_name,
                   phone_number,
                   is_super_admin,
                   email,
                   status,
                   gender,
                   created_at,
                   updated_at)
VALUES ('https://cdn.example.com/avatar/admin1.png',
        '1234567890',
        '{bcrypt}$2a$10$0lvhh4z1X9DR5/6bJUacEux35ayoj1xsVeGIE3IED.e6Gs0.VPSi2', -- password: Vien.123456@
        'Nguyen Van Quan Tri',
        '0901123456',
        TRUE,
        'admin@example.com',
        'ACTIVE',
        'MALE',
        NOW(),
        NOW());

-- =========================================================
-- 👤 BUYER
-- =========================================================
-- SỬ DỤNG DEFAULT ACCOUNT DÙM CON NHA MẤY MÁ
-- TẠI SEED DATA NÊN MỖI LẦN CHẠY LẠI LÀ CÁC ACCOUNT CŨ KHI SIGN UP ĐỒ NÀY NỌ LÀ NÓ SẼ BỊ MẤT NHA MẤY MẸ
-- DEFAULT PASSWORD : Vien.123456@
INSERT INTO buyer (avatar_public_id,
                   avatar_url,
                   created_at,
                   street,
                   deleted_at,
                   district_name,
                   date_of_birth,
                   email,
                   full_name,
                   gender,
                   is_active,
                   password,
                   phone_number,
                   province_name,
                   updated_at,
                   username,
                   ward_name)
VALUES ('ae8ed05a-6eef-4f3c-ae27-63b6c8c04314',
        'https://res.cloudinary.com/dzhxwm90k/image/upload/v1761368550/buyers/1:doanvien/avatar/ed4086f1-9cf3-48ae-8cac-3f493f07f9e7.jpg',
        NOW(), '129 Nguyễn Văn Cừ', NULL, 'Quận 5', '2005-11-19', 'vientruongdoan@gmail.com', 'Truong Doan Vien',
        'MALE', 1, '{bcrypt}$2a$10$0lvhh4z1X9DR5/6bJUacEux35ayoj1xsVeGIE3IED.e6Gs0.VPSi2', '0792043114', 'Hồ Chí Minh',
        NULL, 'doanvien', 'Phường 7'),
       ('fee6981f-33a2-4208-978c-b5c8ffaad9ba',
        'https://res.cloudinary.com/dzhxwm90k/image/upload/v1762071147/buyers/2:kimthuydoan/avatar/fee6981f-33a2-4208-978c-b5c8ffaad9ba.jpg',
        NOW(), '123', NULL, 'Thành phố Dĩ An', '2004-11-19', 'kimthuydoan22082005@gmail.com', 'Đoàn Thị Kim Thúy',
        'FEMALE', 1, '{bcrypt}$2a$10$0lvhh4z1X9DR5/6bJUacEux35ayoj1xsVeGIE3IED.e6Gs0.VPSi2', '0780453118', 'Bình Dương',
        NULL, 'kimthuydoan', 'Phường Bình An'),
       (NULL, NULL, NOW(), NULL, NULL, NULL, NULL, 'hanhtransdr@gmail.com', NULL, 'FEMALE', 1,
        '{bcrypt}$2a$10$0lvhh4z1X9DR5/6bJUacEux35ayoj1xsVeGIE3IED.e6Gs0.VPSi2', NULL, NULL, NULL, 'tranthihanh', NULL);

-- =========================================================
-- 🏪 WALLET
-- =========================================================

INSERT INTO wallet (balance, concurrency, provider, created_at, buyer_id)
VALUES (1000000000.00, 'VND', 'VNPay', NOW(), 1),
       (1100000000.00, 'VND', 'VNPay', NOW(), 2),
       (1200000000.00, 'VND', 'VNPay', NOW(), 3);

-- =========================================================
-- 🏪 WALLET TRANSACTION
-- =========================================================
INSERT INTO wallet_transaction(amount, balance_before, created_at, description, status, type, wallet_id)
VALUES (10000000.00, 0.00, NOW(), 'Nap tien vao vi nguoi dung', 'SUCCESS', 'DEPOSIT', 1),
       (10000000.00, 0.00, NOW(), 'Nap tien vao vi nguoi dung', 'SUCCESS', 'DEPOSIT', 2),
       (10000000.00, 0.00, NOW(), 'Nap tien vao vi nguoi dung', 'SUCCESS', 'DEPOSIT', 3);

-- =========================================================
-- 🏪 SELLER
-- =========================================================
INSERT INTO seller(front_of_identity_url,
                   back_of_identity_url,
                   business_license_url,
                   selfie_image_url,
                   status,
                   store_name,
                   ghn_shop_id,
                   store_policy_url,
                   tax_number,
                   identity_number,
                   seller_name,
                   nationality,
                   home,
                   buyer_id,
                   admin_id,
                   created_at,
                   updated_at,
                   deleted_at)
VALUES ('https://res.cloudinary.com/dzhxwm90k/image/upload/v1761369239/sellers/1:doanvien/identity_front_image/8141997c-cf68-43ff-bfbd-c54534be6372.jpg',
        'https://res.cloudinary.com/dzhxwm90k/image/upload/v1761369244/sellers/1:doanvien/identity_back_image/551938c5-7612-464e-94b8-1eaf453085e9.jpg',
        'https://res.cloudinary.com/dzhxwm90k/image/upload/v1761369242/sellers/1:doanvien/business_license_image/521e8ddf-e05a-41a8-af94-fbb4b89a0655.jpg',
        'https://res.cloudinary.com/dzhxwm90k/image/upload/v1761369246/sellers/1:doanvien/selfie_image/d3953b0b-8423-47c0-864c-2e4bd4f2d2d9.jpg',
        'ACCEPTED',
        'Chuyên xe máy, phụ tùng xe điện Đoàn Viên',
        '197764',
        'https://res.cloudinary.com/dzhxwm90k/image/upload/v1761369249/sellers/1:doanvien/policy_image/96443852-fe1c-419b-bf60-8197d48f29ea.jpg',
        '0751487961',
        '075205014623',
        'TRƯƠNG ĐOÀN VIÊN',
        'VIỆT NAM',
        'MỸ LỢI, PHÙ MỸ, BÌNH ĐỊNH',
        1,
        1,
        NOW(), -- ✅ giá trị thời gian cho created_at
        NULL,
        NULL);

-- ghn_id : 197764
-- =========================================================
-- 🧾 SUBSCRIPTION - GÁN GÓI CHO SELLER
-- =========================================================
INSERT INTO subscription (seller_id, subscription_package_id, is_active, start_day, end_day, remain_post)
VALUES (1, 3, TRUE, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 100);


-- =========================================================
-- 🗂 CATEGORY
-- =========================================================
INSERT INTO category (name, description)
VALUES
-- 🛵 Danh mục 1: Xe điện
(N'Xe điện',
 CONCAT(
         'Danh mục bao gồm các loại xe điện hiện đại như xe máy điện, xe đạp điện và ô tô điện.', CHAR(10),
         'Xe điện mang đến trải nghiệm di chuyển thân thiện với môi trường, tiết kiệm năng lượng và chi phí vận hành thấp.', CHAR(10),
         'Các sản phẩm trong nhóm này phù hợp cho học sinh, sinh viên, người đi làm và cả gia đình có nhu cầu di chuyển hàng ngày.', CHAR(10),
         'Ngoài ra, còn có nhiều mẫu xe với thiết kế thời trang, động cơ mạnh mẽ và công nghệ pin tiên tiến.', CHAR(10),
         'Khách hàng có thể lựa chọn giữa nhiều thương hiệu và mức giá khác nhau tùy theo nhu cầu sử dụng.'
 )),

-- 🔋 Danh mục 2: Pin điện
(N'Pin điện',
 CONCAT(
         'Danh mục pin điện bao gồm pin sạc, pin lithium, ắc quy và các bộ lưu trữ năng lượng cho xe điện hoặc thiết bị gia dụng.', CHAR(10),
         'Các sản phẩm được chọn lọc từ nhiều thương hiệu uy tín, đảm bảo an toàn, độ bền cao và dung lượng ổn định.', CHAR(10),
         'Phù hợp cho nhu cầu thay thế pin xe điện, pin lưu trữ năng lượng mặt trời hoặc các thiết bị điện khác.', CHAR(10),
         'Người dùng có thể lựa chọn theo dung lượng, điện áp và kích thước phù hợp với thiết bị của mình.', CHAR(10),
         'Tất cả sản phẩm đều được kiểm tra chất lượng và bảo hành theo tiêu chuẩn nhà sản xuất.'
 ));

-- =========================================================
-- 🚗 POST_PRODUCT
-- =========================================================
INSERT INTO post_product
(title, brand, model, manufacture_year, used_duration, rejected_reason, condition_level, price, width, height, length,
 weight, description, location_trading, is_sold, is_active, verified_decision_status, is_verified, created_at, updated_at,
 deleted_at, category_id, admin_id, seller_id)
VALUES ('Xe đạp điện Pega Aura 2022 - còn mới 90%', 'Pega', 'Aura', 2022, '18 tháng', NULL, 'Good', 8900000, '68',
        '105', '148', '28000', 'Xe đạp điện chạy êm, pin thay gần đây, phanh còn tốt, đi học đi làm ổn định.',
        'Quận 7, TP.HCM', TRUE, FALSE, 'APPROVED', TRUE, '2025-09-30 10:15:00', '2025-10-10 08:40:00', NULL, 1, NULL,
        1),
       ('Pin LFP 60V 20Ah cho xe điện - đã test dung lượng', 'CATL', 'LFP-60V-20Ah', 2023, '6 tháng', NULL, 'Like New',
        4200000, '18', '20', '35', '7200',
        'Pin LiFePO4 60V 20Ah, đã test nội trở, còn ~92% dung lượng, dùng cho nhiều dòng xe điện.', 'Cầu Giấy, Hà Nội',
        TRUE, FALSE, 'APPROVED', TRUE, '2025-10-01 09:00:00', '2025-10-15 11:20:00', NULL, 2, NULL, 1),
       ('Xe máy điện VinFast Feliz S 2022 - pin thuê', 'VinFast', 'Feliz S', 2022, '20 tháng', NULL, 'Good', 16500000,
        '70', '112', '150', '47000',
        'Bản S, đi lại hằng ngày, khung vỏ còn đẹp, pin đang thuê VinFast (không kèm pin).', 'Thanh Khê, Đà Nẵng', TRUE,
        FALSE, 'APPROVED', TRUE, '2025-10-05 14:05:00', '2025-10-20 09:30:00', NULL, 1, NULL, 1),
       ('Pin NMC 72V 30Ah tháo xe - còn 85% SOH', 'EVE', 'NMC-72V-30Ah', 2021, '24 tháng', NULL, 'Fair', 5500000, '20',
        '22', '42', '12500',
        'Pack NMC 72V 30Ah, đã cân bằng cell, còn ~85% SOH, phù hợp xe máy điện công suất trung bình.',
        'Thủ Đức, TP.HCM', TRUE, FALSE, 'APPROVED', TRUE, '2025-09-25 16:00:00', '2025-10-12 13:10:00', NULL, 2, NULL,
        1),
       ('Xe đạp điện Giant M3 cũ - bảo dưỡng định kỳ', 'Giant', 'M3', 2020, '3 năm', NULL, 'Fair', 6500000, '66', '103',
        '145', '30000', 'Đã thay lốp và phanh, pin còn dùng tốt ~70-75%, có trầy nhẹ theo thời gian.',
        'Biên Hòa, Đồng Nai', TRUE, FALSE, 'PENDING', FALSE, '2025-10-18 10:45:00', '2025-10-18 10:45:00', NULL, 1,
        NULL, 1),
       ('Pin LFP 48V 12Ah cho xe đạp điện - nhẹ, bền', 'Gotion', 'LFP-48V-12Ah', 2024, '4 tháng', NULL, 'Like New',
        1900000, '15', '16', '32', '5200', 'Pin LiFePO4 an toàn, vòng sạc thấp, phù hợp xe đạp điện học sinh.',
        'Nha Trang, Khánh Hòa', FALSE, TRUE, 'APPROVED', TRUE, '2025-10-02 08:00:00', '2025-10-22 17:25:00', NULL, 2,
        NULL, 1),
       ('Xe máy điện Pega eSH 2021 - đã thay pin', 'Pega', 'eSH', 2021, '30 tháng', NULL, 'Good', 13900000, '69', '110',
        '149', '48000', 'Động cơ mạnh, pin thay 2024 (LFP 60V 24Ah), bô phận điện ổn định, hình thức đẹp.',
        'Hải Châu, Đà Nẵng', FALSE, TRUE, 'PENDING', FALSE, '2025-10-19 12:10:00', '2025-10-21 09:50:00', NULL, 1, NULL,
        1),
       ('Pack pin ioni 60V 30Ah tháo VinFast Klara - còn 88%', 'VinFast', 'Klara-Pack-60V30Ah', 2022, '16 tháng', NULL,
        'Good', 6200000, '19', '21', '40', '11800', 'Pack tháo xe, đã kiểm thử SoH 88%, thích hợp retrofit/độ xe điện.',
        'Thủ Dầu Một, Bình Dương', FALSE, TRUE, 'APPROVED', TRUE, '2025-10-06 15:35:00', '2025-10-16 09:05:00', NULL, 2,
        NULL, 1),
       ('Xe đạp điện Xiaomi HIMO C20 2023 - gấp gọn', 'Xiaomi', 'HIMO C20', 2023, '10 tháng', NULL, 'Like New',
        12500000, '58', '102', '145', '21800', 'Bản gấp gọn tiện mang đi chung cư/xe hơi, pin tháo rời, odo ~800 km.',
        'Quận 3, TP.HCM', FALSE, TRUE, 'APPROVED', TRUE, '2025-10-09 19:00:00', '2025-10-23 10:15:00', NULL, 1, NULL,
        1),
       ('Pin thay thế 72V 20Ah cho xe máy điện - BMS thông minh', 'LG Energy', 'NMC-72V-20Ah-SmartBMS', 2024, '3 tháng',
        'Ảnh chụp mờ, yêu cầu bổ sung ảnh rõ hơn', 'Like New', 4800000, '18', '19', '38', '9800',
        'Pack cell LG, BMS cân bằng chủ động, cổng giao tiếp UART, còn tem bảo hành.', 'Cần Thơ', FALSE, TRUE,
        'REJECTED', FALSE, '2025-10-11 11:45:00', '2025-10-11 11:45:00', NULL, 2, NULL, 1),
       ('Xe máy điện Dibao Pansy S 2021 - màu đỏ đô', 'Dibao', 'Pansy S', 2021, '28 tháng', NULL, 'Good', 11900000,
        '68', '108', '145', '43000', 'Xe máy điện Dibao bản S, chạy ổn định, pin còn tốt, có trầy nhẹ ở yếm.',
        'Hoàng Mai, Hà Nội', FALSE, TRUE, 'APPROVED', TRUE, '2025-09-28 08:45:00', '2025-10-20 10:00:00', NULL, 1, NULL,
        1),
       ('Pin Li-ion 48V 20Ah tháo xe - dùng được 85%', 'Samsung SDI', '48V-20Ah', 2022, '15 tháng', NULL, 'Fair',
        2100000, '16', '18', '30', '8000', 'Pack pin 48V 20Ah, đo dung lượng còn ~85%, phù hợp xe đạp điện phổ thông.',
        'Bắc Từ Liêm, Hà Nội', FALSE, TRUE, 'APPROVED', TRUE, '2025-10-03 09:10:00', '2025-10-15 14:30:00', NULL, 2,
        NULL, 1),
       ('Xe đạp điện Yadea iGo 2023 - mới 95%', 'Yadea', 'iGo', 2023, '8 tháng', NULL, 'Like New', 9800000, '66', '105',
        '140', '26500', 'Xe đạp điện gọn nhẹ, pin tháo rời, khung nhôm, phù hợp học sinh và dân văn phòng.',
        'Hà Đông, Hà Nội', FALSE, TRUE, 'APPROVED', TRUE, '2025-09-29 11:25:00', '2025-10-12 15:00:00', NULL, 1, NULL,
        1),
       ('Pin LFP 60V 24Ah - pin xe máy điện cũ còn tốt', 'CATL', 'LFP-60V-24Ah', 2022, '12 tháng', NULL, 'Good',
        3900000, '19', '21', '36', '9500',
        'Pin LiFePO4 60V 24Ah, an toàn, dòng xả cao, đã test dung lượng thực tế còn 93%.', 'Long Biên, Hà Nội', FALSE,
        TRUE, 'APPROVED', TRUE, '2025-10-02 09:00:00', '2025-10-22 11:00:00', NULL, 2, NULL, 1),
       ('Xe máy điện YADEA E3 2020 - pin còn dùng tốt', 'Yadea', 'E3', 2020, '36 tháng', NULL, 'Fair', 8500000, '70',
        '110', '147', '44000', 'Xe máy điện Yadea E3 bản thường, pin zin, động cơ êm, khung vững.',
        'Thủ Dầu Một, Bình Dương', FALSE, TRUE, 'PENDING', FALSE, '2025-10-10 13:15:00', '2025-10-10 13:15:00', NULL, 1,
        NULL, 1),
       ('Pin thay thế 72V 25Ah - cell LG, BMS mới', 'LG Chem', '72V-25Ah', 2024, '5 tháng', NULL, 'Like New', 5200000,
        '18', '19', '40', '10000', 'Pack pin LG Chem, mới 95%, có BMS mới, tương thích nhiều mẫu xe điện phổ biến.',
        'TP. Pleiku, Gia Lai', FALSE, TRUE, 'APPROVED', TRUE, '2025-10-04 10:45:00', '2025-10-18 17:50:00', NULL, 2,
        NULL, 1),
       ('Xe máy điện DatBike Weaver++ 2023 - xe công ty test', 'DatBike', 'Weaver++', 2023, '10 tháng', NULL,
        'Like New', 29000000, '72', '118', '150', '48000',
        'Xe test nội bộ, odo ~1500 km, pin zin, động cơ mạnh, bảo dưỡng đầy đủ.', 'Tân Bình, TP.HCM', FALSE, TRUE,
        'APPROVED', TRUE, '2025-09-27 14:20:00', '2025-10-22 09:30:00', NULL, 1, NULL, 1),
       ('Pin xe đạp điện 36V 12Ah - còn mới 90%', 'Gotion', '36V-12Ah', 2023, '8 tháng', NULL, 'Like New', 1300000,
        '12', '15', '28', '4200', 'Pin nhỏ gọn, thích hợp xe đạp điện mini, trọng lượng nhẹ, dễ tháo lắp.', 'Huế',
        FALSE, TRUE, 'APPROVED', TRUE, '2025-10-09 08:00:00', '2025-10-18 19:00:00', NULL, 2, NULL, 1),
       ('Xe đạp điện Suzika Eco 2022 - chính chủ', 'Suzika', 'Eco', 2022, '18 tháng', NULL, 'Good', 7500000, '65',
        '103', '142', '28000', 'Xe đi học, còn pin tốt, động cơ ổn định, chính chủ sang tay.', 'Ninh Kiều, Cần Thơ',
        FALSE, TRUE, 'APPROVED', TRUE, '2025-10-06 07:40:00', '2025-10-20 10:10:00', NULL, 1, NULL, 1),
       ('Pin NMC 60V 15Ah tháo xe VinFast - còn 88%', 'VinFast', '60V15Ah', 2022, '16 tháng', NULL, 'Good', 3100000,
        '17', '19', '33', '7600', 'Pack pin tháo xe Klara, còn dung lượng tốt, có BMS nguyên bản, chưa can thiệp.',
        'Cẩm Lệ, Đà Nẵng', FALSE, TRUE, 'APPROVED', TRUE, '2025-09-26 09:50:00', '2025-10-16 08:40:00', NULL, 2, NULL,
        1),
       ('Xe máy điện Pega Cap-A 2021 - pin thay mới 2024', 'Pega', 'Cap-A', 2021, '24 tháng', NULL, 'Good', 12500000,
        '70', '110', '145', '45000', 'Xe còn nguyên tem, pin thay mới đầu 2024, phanh đĩa, vận hành ổn định.',
        'Quận 5, TP.HCM', FALSE, TRUE, 'APPROVED', TRUE, '2025-09-29 09:00:00', '2025-10-20 15:45:00', NULL, 1, NULL,
        1),
       ('Pin LiFePO4 60V 30Ah tháo xe DatBike - còn 90%', 'CATL', 'LFP-60V-30Ah', 2023, '10 tháng', NULL, 'Like New',
        4800000, '19', '21', '38', '9700', 'Pack pin CATL chuẩn, còn dung lượng cao, thích hợp xe máy điện hoặc DIY.',
        'Tân Bình, TP.HCM', FALSE, TRUE, 'APPROVED', TRUE, '2025-09-25 12:10:00', '2025-10-15 08:30:00', NULL, 2, NULL,
        1),
       ('Xe đạp điện Anbico AP1508 2022 - còn zin', 'Anbico', 'AP1508', 2022, '14 tháng', NULL, 'Good', 8300000, '67',
        '104', '145', '29000', 'Xe đạp điện Anbico, chạy nhẹ, tiết kiệm điện, pin sạc đầy đi được 40km.',
        'Bình Thạnh, TP.HCM', FALSE, TRUE, 'PENDING', FALSE, '2025-10-02 10:45:00', '2025-10-19 09:20:00', NULL, 1,
        NULL, 1),
       ('Pin 72V 32Ah tháo xe VinFast Evo200 - còn 85%', 'VinFast', '72V-32Ah', 2022, '18 tháng', NULL, 'Fair', 5900000,
        '20', '23', '44', '13500', 'Pin tháo xe VinFast Evo200, SoH ~85%, có thể sử dụng tiếp 2–3 năm.', 'Ninh Bình',
        FALSE, TRUE, 'APPROVED', TRUE, '2025-09-27 14:30:00', '2025-10-18 13:00:00', NULL, 2, NULL, 1),
       ('Xe máy điện YADEA BuyE 2023 - chính chủ', 'Yadea', 'BuyE', 2023, '9 tháng', NULL, 'Like New', 17500000, '71',
        '115', '148', '47000', 'Xe còn rất mới, odo 900km, pin zin, động cơ êm, chính chủ bán.', 'Hải Phòng', FALSE,
        TRUE, 'APPROVED', TRUE, '2025-09-30 09:15:00', '2025-10-22 08:50:00', NULL, 1, NULL, 1),
       ('Pin NMC 60V 24Ah tháo xe - còn 87% dung lượng', 'Samsung SDI', 'NMC-60V-24Ah', 2021, '20 tháng', NULL, 'Good',
        3800000, '18', '20', '35', '9200', 'Pin tháo xe điện cao cấp, cell Samsung SDI, kiểm định dung lượng còn 87%.',
        'Biên Hòa, Đồng Nai', FALSE, TRUE, 'APPROVED', TRUE, '2025-10-06 07:50:00', '2025-10-19 17:10:00', NULL, 2,
        NULL, 1),
       ('Xe máy điện VinFast Evo200 Lite 2023 - demo hãng', 'VinFast', 'Evo200 Lite', 2023, '6 tháng', NULL, 'Like New',
        23900000, '72', '117', '150', '48000', 'Xe trưng bày, đi test 500km, bảo hành còn 2 năm.', 'Quận 2, TP.HCM',
        FALSE, TRUE, 'APPROVED', TRUE, '2025-10-10 09:00:00', '2025-10-21 10:45:00', NULL, 1, NULL, 1),
       ('Pin LFP 48V 15Ah - cell Gotion, dòng xả cao', 'Gotion', 'LFP-48V-15Ah', 2024, '4 tháng', NULL, 'Like New',
        2200000, '14', '16', '33', '5800', 'Pin LFP cell Gotion, dùng cho xe đạp điện, dòng xả cao, an toàn.',
        'Vinh, Nghệ An', FALSE, TRUE, 'APPROVED', TRUE, '2025-10-03 08:10:00', '2025-10-16 13:00:00', NULL, 2, NULL, 1),
       ('Xe máy điện Pega Aura S 2020 - đã thay pin', 'Pega', 'Aura S', 2020, '36 tháng', NULL, 'Fair', 9500000, '69',
        '110', '147', '46000', 'Xe đi học sinh viên, pin thay năm 2024, khung còn chắc, có xước nhẹ.', 'Bắc Giang',
        FALSE, TRUE, 'PENDING', FALSE, '2025-09-26 15:00:00', '2025-10-14 09:00:00', NULL, 1, NULL, 1),
       ('Pin xe máy điện 60V 25Ah BMS thông minh - chưa dùng', 'LG Energy', '60V-25Ah-BMS', 2024, '1 tháng',
        'Thiếu giấy kiểm định pin', 'Like New', 5000000, '19', '21', '36', '8900',
        'Pack pin LG Energy mới 99%, có cổng giao tiếp CAN, thích hợp xe điện hiện đại.', 'Đà Lạt, Lâm Đồng', FALSE,
        TRUE, 'REJECTED', FALSE, '2025-10-12 11:00:00', '2025-10-12 11:00:00', NULL, 2, NULL, 1),
       ('Xe máy điện Dibao Gogo SS 2022 - bản giới hạn', 'Dibao', 'Gogo SS', 2022, '16 tháng', NULL, 'Like New',
        15500000, '70', '113', '148', '46500', 'Xe máy điện Dibao bản giới hạn, màu xanh ngọc, pin còn rất tốt.',
        'Thanh Xuân, Hà Nội', FALSE, TRUE, 'APPROVED', TRUE, '2025-10-01 09:20:00', '2025-10-23 09:00:00', NULL, 1,
        NULL, 1),
       ('Pin LFP 60V 20Ah tháo xe Yadea - dung lượng 92%', 'CATL', 'LFP-60V-20Ah', 2023, '8 tháng', NULL, 'Good',
        3500000, '18', '20', '35', '7200', 'Pin LFP chuẩn CATL, còn dung lượng 92%, thích hợp cho xe Yadea, VinFast.',
        'Hòa Bình', FALSE, TRUE, 'APPROVED', TRUE, '2025-09-29 10:15:00', '2025-10-18 08:30:00', NULL, 2, NULL, 1),
       ('Xe đạp điện DK Bike Cap A 2021 - pin còn tốt', 'DK Bike', 'Cap A', 2021, '30 tháng', NULL, 'Fair', 7200000,
        '65', '103', '145', '29000', 'Xe đạp điện DK Bike chạy ổn định, có trầy nhẹ, pin còn đi được 35km.',
        'Tân Phú, TP.HCM', FALSE, TRUE, 'PENDING', FALSE, '2025-09-30 11:00:00', '2025-10-10 10:10:00', NULL, 1, NULL,
        1),
       ('Pin NMC 72V 20Ah - cell LG tháo xe VinFast', 'LG Chem', 'NMC-72V-20Ah', 2022, '14 tháng', NULL, 'Good',
        4600000, '19', '21', '38', '9800', 'Pack pin tháo xe VinFast Feliz, cell LG Chem, còn dung lượng 90%.',
        'Hải Dương', TRUE, TRUE, 'APPROVED', TRUE, '2025-09-25 13:00:00', '2025-10-17 15:00:00', NULL, 2, NULL, 1),
       ('Xe máy điện YADEA Xmen Neo 2023 - mới 98%', 'Yadea', 'Xmen Neo', 2023, '5 tháng', NULL, 'Like New', 18900000,
        '72', '115', '150', '48000', 'Xe mới sử dụng nhẹ, odo 700km, còn bảo hành chính hãng.', 'Nam Định', FALSE, TRUE,
        'APPROVED', TRUE, '2025-10-08 10:00:00', '2025-10-20 08:40:00', NULL, 1, NULL, 1),
       ('Pin xe đạp điện 36V 15Ah - Gotion, dòng xả cao', 'Gotion', '36V-15Ah', 2024, '3 tháng', NULL, 'Like New',
        1500000, '13', '14', '30', '4600', 'Pin Gotion LFP, mới 97%, dòng xả cao, an toàn, nhẹ.', 'Bình Thuận', FALSE,
        TRUE, 'APPROVED', TRUE, '2025-10-06 09:10:00', '2025-10-17 11:20:00', NULL, 2, NULL, 1),
       ('Xe đạp điện Pega Cap X 2020 - xe học sinh cũ', 'Pega', 'Cap X', 2020, '40 tháng', NULL, 'Fair', 5900000, '66',
        '104', '145', '28000', 'Xe học sinh, pin yếu còn đi được 20km, khung chắc chắn, giá rẻ.', 'Hà Đông, Hà Nội',
        TRUE, TRUE, 'APPROVED', TRUE, '2025-09-23 15:40:00', '2025-10-14 09:15:00', NULL, 1, NULL, 1),
       ('Pin thay thế 60V 25Ah BMS bluetooth - hàng mới tháo', 'LG Energy', '60V-25Ah-BT', 2024, '2 tháng', NULL,
        'Like New', 5200000, '18', '19', '37', '8800', 'Pin LG Energy, có Bluetooth BMS theo dõi pin qua app.', 'Huế',
        FALSE, TRUE, 'APPROVED', TRUE, '2025-09-28 11:45:00', '2025-10-19 13:00:00', NULL, 2, NULL, 1),
       ('Xe máy điện DatBike Weaver 200 2022 - pin zin', 'DatBike', 'Weaver 200', 2022, '18 tháng', NULL, 'Good',
        25900000, '71', '115', '148', '47500', 'Xe chính chủ, pin zin 2022, động cơ mạnh, bảo dưỡng đều.', 'Cần Thơ',
        FALSE, TRUE, 'APPROVED', TRUE, '2025-09-27 10:00:00', '2025-10-16 09:10:00', NULL, 1, NULL, 1),
       ('Pin LFP 48V 20Ah - tháo xe học sinh, còn 88%', 'CATL', 'LFP-48V-20Ah', 2022, '20 tháng',
        'Ảnh mờ, yêu cầu bổ sung ảnh', 'Good', 2300000, '16', '18', '34', '7100',
        'Pin CATL còn dung lượng tốt, phù hợp xe đạp điện phổ thông.', 'Buôn Ma Thuột, Đắk Lắk', FALSE, TRUE,
        'REJECTED', FALSE, '2025-10-07 10:40:00', '2025-10-07 10:40:00', NULL, 2, NULL, 1);

---- =========================================================
---- 🖼 PRODUCT_IMAGE
---- =========================================================
INSERT INTO product_image (order_image, image_url, post_id)
VALUES
-- Post 1
(1, 'https://i.vietgiaitri.com/2019/8/2/chi-tiet-mau-xe-dien-sang-chanh-khien-gioi-tre-me-man-ce07d3.jpg', 1),
(2, 'https://i.vietgiaitri.com/2019/8/2/chi-tiet-mau-xe-dien-sang-chanh-khien-gioi-tre-me-man-418a47.jpg', 1),
(3, 'https://i.vietgiaitri.com/2019/8/2/chi-tiet-mau-xe-dien-sang-chanh-khien-gioi-tre-me-man-ba86e6.jpg', 1),
(4, 'https://i.vietgiaitri.com/2019/8/2/chi-tiet-mau-xe-dien-sang-chanh-khien-gioi-tre-me-man-a3a712.jpg', 1),
(5, 'https://i.vietgiaitri.com/2019/8/2/chi-tiet-mau-xe-dien-sang-chanh-khien-gioi-tre-me-man-b29ac7.jpg', 1),
-- Post 2
(1, 'https://bizweb.dktcdn.net/100/443/045/files/1875856b6e73e42dbd62.webp?v=1759221306154', 2),
(2, 'https://bizweb.dktcdn.net/100/443/045/files/2076474148752938863-904fceb0-fc6d-43f9-849c-c2385d10c1d4.webp?v=1759221333460', 2),
(3, 'https://bizweb.dktcdn.net/100/443/045/files/59565b33b02b3a75633a.webp?v=1759221365927', 2),
(4, 'https://bizweb.dktcdn.net/100/443/045/files/6b846ed485cc0f9256dd.webp?v=1759221388833', 2),
(5, 'https://bizweb.dktcdn.net/100/443/045/files/3dafdadf31c7bb99e2d6.webp?v=1759221412800', 2),
-- Post 3
(1, 'https://bizweb.dktcdn.net/100/443/045/files/44e75224ff8872d62b99.jpg?v=1761713870377', 3),
(2, 'https://bizweb.dktcdn.net/100/443/045/files/vegas-7.jpg?v=1747561764186', 3),
(3, 'https://bizweb.dktcdn.net/100/443/045/files/5da7d3010d1fb841e10e.jpg?v=1747561974108', 3),
(4, 'https://bizweb.dktcdn.net/100/443/045/files/vegas-3.jpg?v=1747561996070', 3),
(5, 'https://bizweb.dktcdn.net/100/443/045/files/79df8b1926b5abebf2a4.jpg?v=1761713893702', 3),
-- Post 4
(1, 'https://bizweb.dktcdn.net/100/443/045/files/sasuki-1.jpg?v=1747559031636', 4),
(2, 'https://bizweb.dktcdn.net/100/443/045/files/sasuki-8.jpg?v=1747559078244', 4),
(3, 'https://bizweb.dktcdn.net/100/443/045/files/4a4fab2b4935fc6ba524.jpg?v=1747559417414', 4),
(4, 'https://bizweb.dktcdn.net/100/443/045/files/sasuki-4.jpg?v=1747559462249', 4),
(5, 'https://bizweb.dktcdn.net/100/443/045/files/sasuki-3.jpg?v=1747559612768', 4),
-- Post 5
(1, 'https://bizweb.dktcdn.net/100/443/045/files/1.jpg?v=1747991192712', 5),
(2, 'https://bizweb.dktcdn.net/100/443/045/files/1.jpg?v=1747991192712', 5),
(3, 'https://bizweb.dktcdn.net/100/443/045/files/5.jpg?v=1747991251599', 5),
(4, 'https://bizweb.dktcdn.net/100/443/045/files/15.jpg?v=1747991446958', 5),
(5, 'https://bizweb.dktcdn.net/100/443/045/files/6.jpg?v=1747991469423', 5),
-- Post 6
(1, 'https://bizweb.dktcdn.net/100/443/045/files/m4.jpg?v=1746849825584', 6),
(2, 'https://bizweb.dktcdn.net/100/443/045/files/m9.jpg?v=1746849927835', 6),
(3, 'https://bizweb.dktcdn.net/100/443/045/files/m10.jpg?v=1746850224512', 6),
(4, 'https://bizweb.dktcdn.net/100/443/045/files/m2.jpg?v=1746849986119', 6),
(5, 'https://bizweb.dktcdn.net/100/443/045/files/m5.jpg?v=1746850524605', 6),
-- Post 7
(1, 'https://bizweb.dktcdn.net/100/443/045/files/cbbae31828bf9ae1c3ae.jpg?v=1746844918673', 7),
(2, 'https://bizweb.dktcdn.net/100/443/045/files/fa5030a67b00c95e9011.jpg?v=1746846275111', 7),
(3, 'https://bizweb.dktcdn.net/100/443/045/files/146045d68e713c2f6560.jpg?v=1746846300535', 7),
(4, 'https://bizweb.dktcdn.net/100/443/045/files/eaf982214986fbd8a297.jpg?v=1746846449427', 7),

-- Post 8
(1, 'https://bizweb.dktcdn.net/100/443/045/files/f2dae65df345791b2054.webp?v=1759221841304', 8),
(2, 'https://bizweb.dktcdn.net/100/443/045/files/e5eeea55ff4d75132c5c.webp?v=1759221859127', 8),
(3, 'https://bizweb.dktcdn.net/100/443/045/files/3246989754616255738.webp?v=1759221877752', 8),
(4, 'https://bizweb.dktcdn.net/100/443/045/files/e9a4963b8323097d5032-e2c9c71e-81f9-4373-aea2-1820b752c187.webp?v=1759221893823', 8),
(5, 'https://bizweb.dktcdn.net/100/443/045/files/e9a4963b8323097d5032-e2c9c71e-81f9-4373-aea2-1820b752c187.webp?v=1759221893823', 8),
-- Post 9
(1, 'https://bizweb.dktcdn.net/100/443/045/files/4226461003078701356.webp?v=1759226782520', 9),
(2, 'https://bizweb.dktcdn.net/100/443/045/files/ff5de141d364593a0075.webp?v=1759226806187', 9),
(3, 'https://bizweb.dktcdn.net/100/443/045/files/3a3a4c227e07f459ad16.webp?v=1759226823883', 9),
(4, 'https://bizweb.dktcdn.net/100/443/045/files/5feaa8ea9acf109149de.webp?v=1759226859858', 9),
(5, 'https://bizweb.dktcdn.net/100/443/045/files/501142071841055588.webp?v=1759226877265', 9),
-- Post 10
(1, 'https://bizweb.dktcdn.net/100/443/045/files/1162c8d9f62f43711a3e.jpg?v=1748058248883', 10),
(2, 'https://bizweb.dktcdn.net/100/443/045/files/9176ab3594c3219d78d2.jpg?v=1748059058754', 10),
(3, 'https://bizweb.dktcdn.net/100/443/045/files/eb9c722e4cd8f986a0c9.jpg?v=1748062701028', 10),
(4, 'https://bizweb.dktcdn.net/100/443/045/files/31068753b8a50dfb54b4.jpg?v=1748061899300', 10),
(5, 'https://bizweb.dktcdn.net/100/443/045/files/10f3e88bd77d62233b6c.jpg?v=1748062137274', 10),
-- Post 11
(1, 'https://banggiavinfast.vn/wp-content/uploads/2023/05/vinfast-evo200-xe-may-dien-ha-noi.jpg ', 11),
(2, 'https://banggiavinfast.vn/wp-content/uploads/2023/05/vinfast-evo200-xe-may-dien-ha-noi-2.jpg', 11),
(3, 'https://banggiavinfast.vn/wp-content/uploads/2023/05/vinfast-evo200-xe-may-dien-ha-noi-3.jpg', 11),
(4, 'https://banggiavinfast.vn/wp-content/uploads/2023/05/Vinfast-Evo200-chi-tiet-xe-2.jpg', 11),
(5, 'https://banggiavinfast.vn/wp-content/uploads/2023/05/Vinfast-evo-200-cong-nghe-pin-moi.jpg', 11),
-- Post 12
(1, 'https://xedienvietthanh.com/wp-content/uploads/2025/09/xe-may-dien-pixar-946-plus-mau-hong-1.jpg', 12),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2025/09/xe-may-dien-pixar-946-plus-mau-hong-2.jpg', 12),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2025/09/xe-may-dien-pixar-946-plus-mau-hong-3.jpg', 12),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2025/09/xe-may-dien-pixar-946-plus-mau-hong-4.jpg', 12),
-- Post 13
(1, 'https://xedienvietthanh.com/wp-content/uploads/2025/04/xe-may-dien-espero-diamond-plus-mau-tra-sua.jpg', 13),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2025/04/xe-may-dien-espero-diamond-plus-mau-tra-sua.jpg', 13),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2025/04/xe-may-dien-espero-diamond-plus-mau-den.jpg', 13),
-- Post 14
(1, 'https://xedienvietthanh.com/wp-content/uploads/2025/07/xe-may-dien-vinfast-feliz-2025-1.jpg', 14),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2025/07/xe-may-dien-vinfast-feliz-2025-2.jpg', 14),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2025/07/xe-may-dien-vinfast-feliz-2025-3.jpg', 14),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2025/07/xe-may-dien-vinfast-feliz-2025-6.jpg', 14),
(5, 'https://xedienvietthanh.com/wp-content/uploads/2025/07/xe-may-dien-vinfast-feliz-2025-5.jpg', 14),
-- Post 15
(1, 'https://xedienvietthanh.com/wp-content/uploads/2025/07/xe-may-dien-vinfast-feliz-2025-5.jpg', 15),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2025/07/xe-may-dien-vinfast-evo-grand-mau-xanh-oliu-4.jpg', 15),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2025/07/xe-may-dien-vinfast-evo-grand-mau-oliu-2.jpg', 15),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2025/07/xe-may-dien-vinfast-evo-grand-mau-xanh-oliu-5.jpg', 15),
(5, 'https://xedienvietthanh.com/wp-content/uploads/2025/07/xe-may-dien-vinfast-evo-grand-mau-vang-cat-3.jpg', 15),
-- Post 16
(1, 'https://xedienvietthanh.com/wp-content/uploads/2025/07/xe-may-dien-vinfast-evo-grand-mau-vang-cat-3.jpg', 16),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2025/05/xe-may-dien-espero-enigma-mau-xanh-su-hao-2.jpg', 16),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2025/05/xe-may-dien-espero-enigma-mau-xanh-su-hao-3.jpg', 16),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2025/05/xe-may-dien-espero-enigma-mau-hong-tra-sua.jpg', 16),
(5, 'https://xedienvietthanh.com/wp-content/uploads/2024/05/xe-ga-50cc-espero-enigma-mau-xanh-reui.jpg', 16),
-- Post 17
(1, 'https://xedienvietthanh.com/wp-content/uploads/2024/05/xe-ga-50cc-espero-enigma-mau-xanh-reui.jpg', 17),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2024/04/xe-may-dien-yadea-oris-2.jpg', 17),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2024/04/xe-may-dien-yadea-oris-3.jpg', 17),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2024/05/xe-may-dien-yadea-oris-2.jpg', 17),
(5, 'https://xedienvietthanh.com/wp-content/uploads/2024/01/xe-may-dien-yadea-oris.jpg', 17),
-- Post 18
(1, 'https://xedienvietthanh.com/wp-content/uploads/2024/01/xe-may-dien-yadea-oris.jpg', 18),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2025/01/xe-may-dien-vinfast-motio-2025-mau-hong-2.jpg', 18),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2025/01/xe-may-dien-vinfast-motio-2025-mau-den.jpg', 18),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2025/01/xe-may-dien-vinfast-motio-2025-mau-do.jpg', 18),
(5, 'https://xedienvietthanh.com/wp-content/uploads/2025/01/xe-may-dien-vinfast-motio-2025-mau-do.jpg', 18),
-- Post 19
(1, 'https://xedienvietthanh.com/wp-content/uploads/2025/10/xe-may-dien-vinfast-flazz-mau-trang.jpg', 19),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2025/10/xe-may-dien-vinfast-flazz-mau-den.jpg', 19),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2025/10/xe-may-dien-vinfast-flazz-mau-do-den.jpg', 19),
-- Post 20
(1, 'https://xedienvietthanh.com/wp-content/uploads/2020/08/xe-may-dien-vinfast-evo-neo.jpg', 20),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2020/08/xe-may-dien-vinfast-evo-neo.jpg', 20),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2020/08/xe-may-dien-vinfast-evo-neo-mau-xanh-reu.jpg', 20),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2020/08/xe-may-dien-vinfast-evo-neo-mau-xanh-reu.jpg', 20),
-- Post 21
(1, 'https://xedienvietthanh.com/wp-content/uploads/2020/08/xe-may-dien-vinfast-evo-neo-mau-xanh-reu.jpg', 21),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2023/02/xe-dien-vespa-espero-classic-pro-2024-xanh-3D.jpg', 21),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2023/02/xe-dien-vespa-espero-classic-pro-2024-hong-tra-sua.jpg', 21),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2023/02/xe-dien-vespa-espero-classic-pro-2024-do-dun.jpg', 21),
(5, 'https://xedienvietthanh.com/wp-content/uploads/2023/02/xe-dien-vespa-espero-classic-pro-2024-xanh-tim.jpg', 21),
-- Post 22
(1, 'https://xedienvietthanh.com/wp-content/uploads/2023/02/xe-dien-vespa-espero-classic-pro-2024-xanh-tim.jpg', 22),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2021/11/xe-may-dien-yadea-odora-s2-chinh-hang-2.jpg', 22),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2021/11/xe-may-dien-yadea-odora-s2-chinh-hang-3.jpg', 22),
-- Post 23
(1, 'https://xedienvietthanh.com/wp-content/uploads/2024/08/xe-may-dien-yadea-vekoo-soobin-1.jpg', 23),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2024/08/xe-may-dien-yadea-vekoo-soobin-2.jpg', 23),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2024/08/xe-may-dien-yadea-vekoo-soobin-3.jpg', 23),
-- Post 24
(1, 'https://xedienvietthanh.com/wp-content/uploads/2024/03/xe-may-dien-yadea-orla-2-1.jpg', 24),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2024/03/xe-may-dien-yadea-orla-2-2.jpg', 24),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2024/03/xe-may-dien-yadea-orla-2-3.jpg', 24),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2023/01/xe-may-dien-yadea-orla-moi-nhat-2023-5.jpg', 24),
-- Post 25
(1, 'https://xedienvietthanh.com/wp-content/uploads/2024/02/xe-may-dien-espero-diamond-ultra-hong.jpg', 25),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2024/02/xe-may-dien-espero-diamond-ultra-xam-chi.jpg', 25),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2024/02/xe-may-dien-espero-diamond-ultra-den-bong.jpg', 25),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2024/02/xe-may-dien-espero-diamond-ultra-trang.jpg', 25),
-- Post 26
(1, 'https://xedienvietthanh.com/wp-content/uploads/2025/01/xe-may-dien-vinfast-motio-2025-mau-hong.jpg', 26),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2025/01/xe-may-dien-vinfast-motio-2025-mau-hong-2.jpg', 26),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2025/01/xe-may-dien-vinfast-motio-2025-mau-do.jpg', 26),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2023/12/xe-may-dien-vinfast-motio-6.jpg', 26),
(5, 'https://xedienvietthanh.com/wp-content/uploads/2023/12/xe-may-dien-vinfast-motio-5.jpg', 26),
-- Post 27
(1, 'https://xedienvietthanh.com/wp-content/uploads/2023/01/xe-dap-dien-133-mini-bo-dien.jpg', 27),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2023/01/xe-dap-dien-133-mini-bo-dien.jpg', 27),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2023/01/xe-dap-dien-133-mini-bo-dien.jpg', 27),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2023/01/xe-dap-dien-133-mini-bo-dien.jpg', 27),
(5, 'https://xedienvietthanh.com/wp-content/uploads/2022/01/3.jpg', 27),
-- Post 28
(1, 'https://xedienvietthanh.com/wp-content/uploads/2022/03/1-3.jpg', 28),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2022/03/2-2.jpg', 28),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2022/03/4-3.jpg', 28),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2022/03/4-3.jpg', 28),
(5, 'https://xedienvietthanh.com/wp-content/uploads/2022/03/6-2.jpg', 28),
-- Post 29
(1, 'https://xedienvietthanh.com/wp-content/uploads/2022/03/6-2.jpg', 29),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2022/03/6-2.jpg', 29),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2019/11/xe-may-dien-evgo-a-do.jpg', 29),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2019/11/xe-may-dien-evgo-a-xam.jpg', 29),
(5, 'https://xedienvietthanh.com/wp-content/uploads/2019/11/xe-may-dien-evgo-a-xanh-than.jpg', 29),
-- Post 30
(1, 'https://xedienvietthanh.com/wp-content/uploads/2024/10/ac-quy-thien-nang-tne-60v-27ah-1.jpg', 30),
--(2, '', 30),
--(3, '', 30),
--(4, '', 30),
--(5, '', 30),
-- Post 31
(1, 'https://xedienvietthanh.com/wp-content/uploads/2022/12/xe-may-dien-tailg-y5-1.jpg', 31),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2022/12/xe-may-dien-tailg-y5-2.jpg', 31),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2022/12/xe-may-dien-tailg-y5-2.jpg', 31),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2022/12/xe-may-dien-tailg-y5-4.jpg', 31),
-- Post 32
(1, 'https://xedienvietthanh.com/wp-content/uploads/2022/12/xe-may-dien-tailg-y5-4.jpg', 32),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2023/01/xe-may-dien-tailg-s35-running-2.jpg', 32),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2023/01/xe-may-dien-tailg-s35-running-3.jpg', 32),
-- Post 33
(1, 'https://xedienvietthanh.com/wp-content/uploads/2022/12/xe-may-dien-tailg-cone-1.jpg', 33),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2022/12/xe-may-dien-tailg-cone-2.jpg', 33),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2022/12/xe-may-dien-tailg-cone-3.jpg', 33),
-- Post 34
(1, 'https://xedienvietthanh.com/wp-content/uploads/2022/12/xe-may-dien-tailg-cone-3.jpg', 34),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2023/01/xe-may-dien-tailg-t36-xam-1.jpg', 34),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2023/01/xe-may-dien-tailg-t36-xam-2.jpg', 34),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2023/01/xe-may-dien-tailg-t36-xanh-1.jpg', 34),
(5, 'https://xedienvietthanh.com/wp-content/uploads/2023/01/xe-may-dien-tailg-t36-xanh-2.jpg', 34),
-- Post 35
(1, 'https://xedienvietthanh.com/wp-content/uploads/2024/10/xe-may-dien-datbike-quantum-s1-mau-trang.jpg', 35),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2024/10/xe-may-dien-datbike-quantum-s1-mau-trang-2.jpg', 35),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2024/10/xe-may-dien-datbike-quantum-s1-mau-xam.jpg', 35),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2024/10/xe-may-dien-datbike-quantum-s1-mau-cam.jpg', 35),
(5, 'https://xedienvietthanh.com/wp-content/uploads/2024/10/xe-may-dien-datbike-quantum-s1-mau-cam-2.jpg', 35),
-- Post 36
(1, 'https://xedienvietthanh.com/wp-content/uploads/2024/10/xe-may-dien-datbike-quantum-s3-mau-xanh-duong.jpg', 36),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2024/10/xe-may-dien-datbike-quantum-s3-mau-xam-3.jpg', 36),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2024/10/xe-may-dien-datbike-quantum-s3-mau-xam-2.jpg', 36),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2024/10/xe-may-dien-datbike-quantum-s3-mau-xam.jpg', 36),
(5, 'https://xedienvietthanh.com/wp-content/uploads/2024/10/xe-may-dien-datbike-quantum-s3-mau-trang.jpg', 36),
-- Post 37
(1, 'https://xedienvietthanh.com/wp-content/uploads/2025/10/xe-may-dien-datbike-quantum-s2-mau-xanh-1.jpg', 37),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2025/10/xe-may-dien-datbike-quantum-s2-mau-xanh-2.jpg', 37),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2025/10/xe-may-dien-datbike-quantum-s2-mau-xanh-2.jpg', 37),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2025/10/xe-may-dien-datbike-quantum-s2-mau-den.jpg', 37),
-- Post 38
(1, 'https://xedienvietthanh.com/wp-content/uploads/2024/06/xe-ga-50cc-dkbike-roma-lite-s-mau-do-dun.jpg', 38),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2024/06/xe-ga-50cc-dkbike-roma-lite-s-mau-den-san.jpg', 38),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2024/06/xe-ga-50cc-dkbike-roma-lite-s-mau-den-san.jpg', 38),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2024/06/xe-ga-50cc-dkbike-roma-lite-s-mau-anh-tim.jpg', 38),
(5, 'https://xedienvietthanh.com/wp-content/uploads/2024/06/xe-ga-50cc-dkbike-roma-lite-s-mau-xanh-ngoc.jpg', 38),
-- Post 39
(1, 'https://xedienvietthanh.com/wp-content/uploads/2023/06/xe-ga-50cc-dk-roma-sx-den-kim-cuong-3.jpg', 39),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2023/06/xe-ga-50cc-dk-roma-sx-den-kim-cuong-4.jpg', 39),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2023/06/xe-ga-50cc-dk-roma-sx-den-kim-cuong-2.jpg', 39),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2023/06/xe-ga-50cc-dk-roma-sx-den-kim-cuong-12.jpg', 39),
(5, 'https://xedienvietthanh.com/wp-content/uploads/2023/06/xe-ga-50cc-dk-roma-sx-den-kim-cuong-11.jpg', 39),
-- Post 40
(1, 'https://xedienvietthanh.com/wp-content/uploads/2023/06/xe-ga-50cc-dk-roma-sx-den-kim-cuong-7.jpg', 40),
(2, 'https://xedienvietthanh.com/wp-content/uploads/2023/06/xe-ga-50cc-dk-roma-sx-den-kim-cuong-8.jpg', 40),
(3, 'https://xedienvietthanh.com/wp-content/uploads/2023/06/xe-ga-50cc-dk-roma-sx-den-kim-cuong-10.jpg', 40),
(4, 'https://xedienvietthanh.com/wp-content/uploads/2023/06/xe-ga-50cc-dk-roma-sx-den-kim-cuong-5.jpg', 40),
(5, 'https://xedienvietthanh.com/wp-content/uploads/2023/06/xe-ga-50cc-dk-roma-sx-den-kim-cuong-5.jpg', 40);

UPDATE product_image
SET image_url = 'https://media-cdn-v2.laodong.vn/storage/newsportal/2024/9/22/1397812/Xe-May-Dien-Re-Dep-2.jpg'
WHERE image_url = '';
-- =========================================================
-- 🖼 WISH-LISTING
-- =========================================================
INSERT INTO wish_listing(created_at, note, priority, buyer_id, post_id)
VALUES (NOW(), 'Để đây và sẽ mua sau', 'LOW', 2, 1),
       (NOW(), 'Sản phẩm này hay nè. Sẽ mua', 'HIGH', 2, 2),
       (NOW(), 'Cũng thích nhưng mà chưa cần lắm', 'LOW', 2, 3),
       (NOW(), 'Cũng ok thôi', 'MEDIUM', 2, 4),
       (NOW(), 'Má ơi hay nha, rất thích', 'HIGH', 2, 5),
       (NOW(), 'Thích vãiiii', 'HIGH', 2, 6),
       (NOW(), 'Để đây và sẽ mua sau', 'LOW', 2, 7),
       (NOW(), 'Để đây và sẽ mua sau', 'MEDIUM', 2, 8),
       (NOW(), 'Không thích cho lắm', 'LOW', 2, 9),
       (NOW(), 'Để đây và sẽ mua sau', 'HIGH', 2, 10),
       (NOW(), 'Cũng ok thôi', 'MEDIUM', 2, 11),
       (NOW(), 'Để đây và sẽ mua sau', 'LOW', 2, 12),
       (NOW(), 'Để đây và sẽ mua sau', 'HIGH', 2, 13),
       (NOW(), 'Thích vãiiii', 'HIGH', 2, 14);

-- =========================================================
-- 🖼 SYSTEM_POLICY
-- =========================================================

INSERT INTO system_policy
(title, content, version, effective_date, expired_date, created_at, updated_at, status, admin_id)
VALUES
-- 🧾 Chính sách 1: Đăng ký tài khoản
('User Registration Policy',
 CONCAT(
         'Khi người dùng đăng ký tài khoản trên nền tảng Green Trade, người dùng cần cung cấp thông tin cá nhân chính xác, đầy đủ và trung thực, bao gồm nhưng không giới hạn ở họ tên, địa chỉ email, số điện thoại và các thông tin nhận dạng khác.', CHAR(10),
         'Việc đăng ký đồng nghĩa với việc người dùng đã đọc, hiểu và đồng ý tuân thủ các điều khoản sử dụng (Terms of Service), chính sách bảo mật (Privacy Policy) và các quy định cộng đồng của hệ thống.', CHAR(10),
         'Người dùng cam kết không sử dụng thông tin sai lệch, mạo danh hoặc tạo nhiều tài khoản nhằm mục đích gian lận, trục lợi hoặc gây rối hoạt động của nền tảng.', CHAR(10),
         'Green Trade có quyền tạm khóa hoặc chấm dứt vĩnh viễn tài khoản nếu phát hiện vi phạm quy định, gian lận trong giao dịch hoặc gây ảnh hưởng tiêu cực đến cộng đồng người dùng khác.', CHAR(10),
         'Ngoài ra, các tài khoản bị nghi ngờ có hành vi lừa đảo, vi phạm pháp luật hoặc bị báo cáo bởi người dùng khác sẽ được xem xét và xử lý theo quy định pháp luật và chính sách nội bộ.', CHAR(10),
         'Người dùng có thể yêu cầu chỉnh sửa hoặc xóa tài khoản theo quy trình hỗ trợ chính thức được đăng tải trên trang chính sách của nền tảng.'
 ),
 1.0,
 NOW(),
 NULL,
 NOW(),
 NOW(),
 'ACTIVE',
 1),

-- 🛍️ Chính sách 2: Nâng cấp tài khoản (Người mua → Người bán)
('Account Upgrade Policy (Buyer to Seller)',
 CONCAT(
         'Người dùng khi nâng cấp tài khoản từ Người mua (Buyer) lên Người bán (Seller) phải hoàn tất quy trình xác minh danh tính, cung cấp giấy tờ cá nhân hoặc pháp lý (CMND/CCCD, giấy phép kinh doanh nếu có) và đồng ý với các điều khoản dành cho người bán.', CHAR(10),
         'Tài khoản Người bán cần đảm bảo các thông tin hiển thị công khai, bao gồm tên cửa hàng, địa chỉ liên hệ, mô tả sản phẩm và cam kết chất lượng hàng hóa, tuân thủ các quy định thương mại điện tử của Việt Nam.', CHAR(10),
         'Trong quá trình hoạt động, Người bán có nghĩa vụ cập nhật thông tin chính xác, không đăng tải sản phẩm vi phạm pháp luật, hàng giả, hàng cấm hoặc hàng hóa không rõ nguồn gốc.', CHAR(10),
         'Green Trade có quyền kiểm duyệt nội dung sản phẩm, tạm ngưng hoặc xóa sản phẩm nếu phát hiện vi phạm tiêu chuẩn đăng bán hoặc có hành vi lừa đảo.', CHAR(10),
         'Trường hợp Người bán vi phạm nghiêm trọng (ví dụ: bán hàng cấm, gian lận giao dịch, spam, đánh giá ảo), tài khoản có thể bị tạm đình chỉ hoặc khóa vĩnh viễn tùy theo mức độ.', CHAR(10),
         'Mọi quyết định xử lý tài khoản được thực hiện bởi Ban quản trị Green Trade dựa trên chứng cứ xác minh, và Người bán có quyền gửi yêu cầu khiếu nại hoặc minh chứng bổ sung qua kênh hỗ trợ chính thức.', CHAR(10),
         'Việc nâng cấp tài khoản đồng nghĩa với việc Người bán hiểu rõ và chấp thuận toàn bộ chính sách hoạt động, nghĩa vụ và trách nhiệm liên quan đến kinh doanh trên nền tảng.'
 ),
 1.0,
 NOW(),
 NULL,
 NOW(),
 NOW(),
 'ACTIVE',
 1);

-- =========================================================
-- 🚚 SHIPPING_PARTNER (ĐỐI TÁC VẬN CHUYỂN)
-- =========================================================
INSERT INTO shipping_partner (email, partner_name, address, website_url, hotline, created_at, updated_at)
VALUES ('support@ghn.vn', 'GHN Express', '20 Đường Tân Sơn, P.15, Q.Tân Bình, TP.HCM', 'https://ghn.vn', '1900636681',
        NOW(), NOW()),
       ('contact@ghtk.vn', 'Giao Hàng Tiết Kiệm', '435 Hoàng Văn Thụ, P.4, Q.Tân Bình, TP.HCM', 'https://ghtk.vn',
        '19008092', NOW(), NOW()),
       ('cs@viettelpost.vn', 'Viettel Post', '01 Giang Văn Minh, Q.Ba Đình, Hà Nội', 'https://viettelpost.com.vn',
        '19008095', NOW(), NOW()),
       ('info@jtexpress.vn', 'J&T Express', '19 Nguyễn Trãi, Q.Thanh Xuân, Hà Nội', 'https://jtexpress.vn', '19001088',
        NOW(), NOW()),
       ('admin@beelogistics.com.vn', 'Bee Logistics', '12 Trần Hưng Đạo, Q.1, TP.HCM', 'https://beelogistics.com.vn',
        '02838222266', NOW(), NOW()) ON DUPLICATE KEY
UPDATE partner_name =
VALUES (partner_name);

-- =========================================================
-- ORDERS
-- =========================================================
INSERT INTO orders (order_code, shipping_address, phone_number, price, shipping_fee, status, created_at, buyer_id,
                    post_id, shipping_partner_id)
VALUES ('XYZ123@', 'Ấp Ngô Quyền, xã Bàu Hàm 2, huyện Thống Nhất, tỉnh Đồng Nai', '0796051911', 30000000.000,
        1000000.000, 'PENDING', NOW(), 2, 1, 1),
       ('XYZ133@', 'Ấp Ngô Quyền, xã Bàu Hàm 2, huyện Thống Nhất, tỉnh Đồng Nai', '0796051911', 30000000.000,
        1000000.000, 'PENDING', NOW(), 2, 2, 1),
       ('XYZ143@', 'Ấp Ngô Quyền, xã Bàu Hàm 2, huyện Thống Nhất, tỉnh Đồng Nai', '0796051911', 30000000.000,
        1000000.000, 'COMPLETED', NOW(), 2, 3, 1),
       ('XYZ153@', 'Ấp Ngô Quyền, xã Bàu Hàm 2, huyện Thống Nhất, tỉnh Đồng Nai', '0796051911', 30000000.000,
        1000000.000, 'PENDING', NOW(), 2, 4, 1),
       ('XYZ163@', 'Ấp Ngô Quyền, xã Bàu Hàm 2, huyện Thống Nhất, tỉnh Đồng Nai', '0796051911', 30000000.000,
        1000000.000, 'COMPLETED', NOW(), 2, 5, 1);

-- ================= Payment Data =================
INSERT INTO payment (description, gateway_name)
VALUES ('Thanh toán khi nhận hàng (COD)', 'COD'),
       ('Thanh toán trực tuyến qua VNPay', 'VNPay');

-- =========================================================
-- TRANSACTION
-- =========================================================
INSERT INTO transactions(amount, created_at, currency, payment_method, status, order_id, payment_id)
VALUES (40000000.00, NOW(), 'VND', 'VNPAY', 'SUCCESS', 1, 2);

-- =========================================================
-- SYSTEM WALLET
-- =========================================================
INSERT INTO system_wallet(balance, buyer_wallet_id, concurrency, created_at, seller_wallet_id, status, admin_id,
                          order_id)
VALUES (40000000.000, 2, 'VND', NOW(), 1, 'ESCROW_HOLD', 1, 1);

-- =========================================================
-- ⚖️ DISPUTE_CATEGORY (DANH MỤC KHIẾU NẠI / TRANH CHẤP)
-- =========================================================
--
INSERT INTO dispute_category (title, reason, description)
VALUES ('Khiếu nại đơn hàng', 'Người mua không nhận được hàng', 'Đơn hàng thất lạc hoặc chưa được giao.'),
       ('Khiếu nại chất lượng sản phẩm', 'Sản phẩm không đúng mô tả', 'Sản phẩm không giống mô tả hoặc hư hại.'),
       ('Khiếu nại thanh toán', 'Thanh toán thất bại nhưng bị trừ tiền', 'Giao dịch bị lỗi nhưng đã bị trừ tiền.'),
       ('Khiếu nại hoàn tiền', 'Chậm xử lý hoàn tiền', 'Yêu cầu hoàn tiền chưa được xử lý.'),
       ('Khiếu nại người bán', 'Người bán không phản hồi', 'Người bán không xác nhận hoặc phản hồi.'),
       ('Khiếu nại vận chuyển', 'Giao hàng chậm hoặc thất lạc', 'Đối tác giao hàng chậm hoặc thất lạc.'),
       ('Khiếu nại chính sách', 'Chính sách hoàn tiền / đổi trả không rõ ràng', 'Người dùng khiếu nại chính sách.'),
       ('Khiếu nại khác', 'Khác (yêu cầu đặc biệt)', 'Các loại khiếu nại khác.') ON DUPLICATE KEY
UPDATE title =
VALUES (title);

-- =========================================================
-- ⚖️ DISPUTE - MẪU TRANH CHẤP / KHIẾU NẠI
-- =========================================================
INSERT INTO dispute(created_at, decision, status, order_id, dispute_category_id)
VALUES (NOW(), 'NOT_HAVE_YET', 'PENDING', 1, 1);

---- =========================================================
---- 🖼 EVIDENCE - ẢNH MINH CHỨNG CHO TRANH CHẤP
---- =========================================================
INSERT INTO evidence(image_url, order_image, dispute_id)
VALUES ('https://media-cdn-v2.laodong.vn/storage/newsportal/2025/9/25/1580851/Xe-Dien-Khong-Giay-9.jpg', 1, 1),
       ('https://media-cdn-v2.laodong.vn/storage/newsportal/2025/9/25/1580851/Xe-Dien-Khong-Giay-9.jpg', 2, 1),
       ('https://thegioixedien.com.vn/datafiles/img_data/images/news/canh-bao-tinh-trang-lay-anh-xe-dien-xe-dap-dien-chinh-hang-de-ban-hang-fake.jpg',
        3, 1);

---- =========================================================
---- CANCEL ORDER REASON
---- =========================================================
INSERT INTO cancel_order_reason (cancel_reason_name)
VALUES ('Người mua thay đổi ý định'),
       ('Giá bán không đúng so với thông tin đăng tải'),
       ('Không thể liên hệ với người mua'),
       ('Người mua yêu cầu huỷ vì giao hàng chậm'),
       ('Khách phát hiện pin không đúng dung lượng mô tả'),
       ('Khách hàng tìm được sản phẩm tương tự với giá tốt hơn'),
       ('Pin không tương thích với dòng xe của khách'),
       ('Khách hàng nhập sai địa chỉ giao hàng');
-- =========================================================
-- ✅ KẾT THÚC FILE DATA.SQL
-- =========================================================
