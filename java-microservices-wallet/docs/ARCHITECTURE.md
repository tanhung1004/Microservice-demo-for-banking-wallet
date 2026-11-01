# Thiết kế đã chọn

## 1. Trách nhiệm

- Login chỉ truy cập `login_db`. User có UUID ổn định, username duy nhất và password_hash.
- Wallet chỉ truy cập `wallet_db`. Wallet có user_id duy nhất và balance.
- user_id là tham chiếu nghiệp vụ, không tạo foreign key xuyên database.
- Subscriber không có database ở bản đầu; nhận event và ghi log, KHÔNG cập nhật số dư.
- Frontend không truy cập database/Kafka và không tự quyết định số dư.

## 2. Luồng đồng bộ

Frontend gọi REST login → Login kiểm tra password hash và trả JWT → frontend gọi Wallet kèm Bearer token → Wallet kiểm tra chữ ký, issuer, audience, thời hạn rồi lấy userId từ sub.

Chọn JWT ký bất đối xứng: private key chỉ ở Login; public key ở Wallet. Không tin userId do frontend gửi. Token lưu trong bộ nhớ frontend ở bản đầu; refresh trang sẽ yêu cầu đăng nhập lại. Token hết hạn 15 phút; chưa làm refresh token.

Hai user demo có UUID cố định khác nhau và hai ví số dư ban đầu 0. Seed theo upsert, chỉ tạo nếu chưa tồn tại; restart KHÔNG reset số dư. Mật khẩu demo được khai báo rõ là dữ liệu local, không dùng mật khẩu cá nhân.

## 3. Luồng bất đồng bộ

Cộng tiền → transaction Wallet cập nhật balance → commit thành công → producer phát MoneyDeposited vào topic wallet-deposited → subscriber nhận và ghi log.

Key Kafka = userId; value = JSON theo CONTRACTS.md. Bản đầu dùng một partition, một consumer group `wallet-log-group`. Subscriber ngừng chạy không làm nghiệp vụ cộng tiền phải đợi nó.

Giới hạn cần ghi rõ: commit DB và publish Kafka là hai thao tác khác nhau. Chỉ dùng after-commit chưa bảo đảm không mất event nếu ứng dụng chết ở giữa. Bản cơ bản cần log rõ lỗi publish, không rollback giả hoặc trả thông báo khiến người dùng tưởng chưa cộng. Outbox là task nâng cao tùy chọn sau T12 nếu cần phát event tin cậy khi broker lỗi; không được tuyên bố exactly-once.

## 4. Quy tắc tiền

- Chỉ VND nguyên đồng; dùng Java long và PostgreSQL BIGINT, không dùng float/double.
- Amount hợp lệ: 1–1.000.000.000 VND mỗi thao tác. Balance tối đa demo: 1.000.000.000.000 VND.
- Validate ở backend; frontend validate để hỗ trợ người dùng.
- Cập nhật nguyên tử/khóa hàng trong transaction; không read-then-write thiếu khóa.
- Trừ quá số dư: 409, không đổi database. Lệnh cộng/trừ lỗi không phát event.
- Khóa nút khi request đang chạy. Không tự retry POST tiền nếu mất phản hồi; gọi GET balance để kiểm tra. Idempotency key là mở rộng sau bản cơ bản.

## 5. Hạ tầng local dự kiến

| Thành phần | Địa chỉ trên máy | Địa chỉ nội bộ Compose |
|---|---|---|
| Login | localhost:8081 | login-service:8080 |
| Wallet | localhost:8082 | wallet-service:8080 |
| Login DB | localhost:15433 | login-db:5432 |
| Wallet DB | localhost:5434 | wallet-db:5432 |
| Frontend dev | localhost:5173 | không áp dụng |
| Frontend đóng gói | localhost:8080 | frontend:80 |
| Kafka | localhost:9092 | kafka:19092 |

Kafka cần cấu hình advertised listeners cho cả client trên host và trong container. Trong container, localhost là chính container, không phải máy host. Browser dùng URL public hoặc đường dẫn cùng origin, không dùng hostname Docker.

Trong dev, Vite proxy `/api/auth` tới Login, `/api/wallet` tới Wallet. Khi đóng gói dùng Nginx proxy hai prefix tương tự và giữ nguyên đường dẫn API. Browser luôn gọi `/api/...`.

Dùng hai PostgreSQL container riêng: login-db và wallet-db.
Mỗi service chỉ kết nối database của mình bằng tài khoản riêng.
Đặt volume riêng cho Login DB, Wallet DB và Kafka.
Không chạy `docker compose down -v` trong bài test giữ dữ liệu.

## 6. Phạm vi bảo đảm

Demo local, không xử lý tiền thật, không đưa cấu hình local trực tiếp lên production. Không thêm Gateway service, Kubernetes, CQRS, chuyển tiền giữa ví hoặc gửi email thật.
