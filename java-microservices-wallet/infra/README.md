# Hạ tầng
T02 tạo init PostgreSQL và Compose tại root. T11 bổ sung application containers. Dùng version tag cụ thể và Docker volumes; cấu hình Kafka cả host và container listeners.

## Chạy hạ tầng

Từ thư mục gốc:
- Khởi động: `docker compose up -d`
- Kiểm tra: `docker compose ps -a`
- Dừng, giữ dữ liệu: `docker compose down`

## Địa chỉ kết nối từ máy
- Login DB: localhost:15433 — database login_db
- Wallet DB: localhost:5434 — database wallet_db
- Kafka: localhost:9092

## Trong mạng Docker
- Login DB: login-db:5432
- Wallet DB: wallet-db:5432
- Kafka: kafka:19092

kafka-init cấp quyền volume trước khi Kafka khởi động.
Exited (0) của kafka-init là trạng thái hoàn thành bình thường.

Đã kiểm tra truy vấn hai database và gửi/nhận thông điệp
trên topic infra-smoke-test bằng công cụ Kafka trong container.