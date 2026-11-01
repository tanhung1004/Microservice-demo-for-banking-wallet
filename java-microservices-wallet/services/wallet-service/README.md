# Wallet Service
Khung, chưa có code. Triển khai T01, T04–T07 theo ../../docs/TASK_DECOMPOSITION.md. Sở hữu wallet_db; xác thực JWT, xử lý tiền, phát event deposit sau commit.

## Tiến độ hiện tại
- Xác thực JWT bằng public key RSA, kiểm tra issuer, audience và thời gian hiệu lực.
- Thiếu token hoặc token sai trả HTTP 401 với JSON UNAUTHORIZED.
- Đã thử token từ Login qua endpoint local /api/wallet/auth-check.
- Kết nối wallet_db tại localhost:5434.
- Flyway V1 tạo bảng wallets, giới hạn balance từ 0 đến 1.000.000.000.000 VND.
- Seeder chỉ chạy với profile local, tạo hai ví demo số dư ban đầu 0.
- Seeder dùng ON CONFLICT DO NOTHING để không ghi đè ví đã tồn tại.

Đã kiểm tra database có hai ví demo, đúng UUID, balance = 0 và currency = VND.
Chưa triển khai API balance, deposit và withdraw.
Chưa kiểm tra giữ số dư khác 0 sau restart.
