# Java Microservices Wallet

Demo học microservices: đăng nhập → xem số dư → cộng/trừ tiền; cộng thành công phát event qua Kafka và một subscriber nhận event.

## Trạng thái hiện tại

Đã có khung thư mục và kế hoạch triển khai. CHƯA có ứng dụng chạy được, Dockerfile, compose.yaml hay nghiệp vụ Java/React. Không đánh dấu các task triển khai là hoàn thành chỉ vì có thư mục.

## Đọc theo thứ tự

1. [Thiết kế và các quyết định](docs/ARCHITECTURE.md).
2. [Hợp đồng API và event](docs/CONTRACTS.md).
3. [Task decomposition](docs/TASK_DECOMPOSITION.md): làm tuần tự T01–T12.
4. [Kịch bản nghiệm thu](docs/ACCEPTANCE.md).

## Phạm vi

- Hai backend nghiệp vụ Java: Login Service, Wallet Service.
- Một Java worker: Wallet Subscriber, chỉ nhận event và ghi log.
- React + Vite: màn hình đăng nhập và ví tiền.
- PostgreSQL: database và tài khoản truy cập riêng cho Login/Wallet.
- Kafka: topic `wallet-deposited`, một broker local.
- Docker Compose: khởi chạy toàn bộ khi hoàn thành T11.
- Tài khoản demo được tạo sẵn; không làm đăng ký, quên mật khẩu, chuyển tiền hay thanh toán thật.

## Bản đồ thư mục

| Đường dẫn | Nội dung |
|---|---|
| `services/login-service/` | Spring Boot: đăng nhập, cấp token |
| `services/wallet-service/` | Spring Boot: số dư, cộng/trừ, phát event |
| `services/wallet-subscriber/` | Spring Boot: nhận event Kafka, ghi log |
| `frontend/` | React: form đăng nhập, trang ví |
| `infra/` | Khởi tạo database; cấu hình hạ tầng bổ sung nếu cần |
| `scripts/` | Script smoke test ở T12 |
| `docs/` | Thiết kế, hợp đồng, task và nghiệm thu |
| `compose.yaml` | Sẽ tạo ở T02, mở rộng ở T11 |

## Quy tắc làm việc

- Mỗi task có đầu vào, file, bước làm, tiêu chí kiểm tra và commit.
- Chỉ commit task khi tiêu chí đạt; lưu lệnh và kết quả trong `docs/PROGRESS.md`.
- Không commit `.env`, private key, mật khẩu thực, `node_modules` hoặc thư mục `target`.
- Dùng Maven Wrapper cho Java và commit package-lock.json cho frontend.
- Chốt và ghi phiên bản cụ thể tại T01/T02; không dùng Docker tag `latest`.
- Chưa chạy thử thì ghi “chưa kiểm tra”, không ghi PASS.

## Bắt đầu trên Windows PowerShell

Giải nén, mở terminal tại thư mục chứa README này, sau đó:

```powershell
git init -b main
git add .
git commit -m "docs: define wallet architecture and task decomposition"
java -version
node --version
npm --version
docker --version
docker compose version
git --version
```

Tiếp theo làm T01. Các lệnh Maven, Docker và frontend trong tài liệu task chỉ chạy SAU KHI đã tạo các file tương ứng.

## Tài liệu chính thức để tra cứu khi triển khai

- Spring Initializr: https://start.spring.io/
- Spring Kafka: https://docs.spring.io/spring-kafka/reference/
- Apache Kafka: https://kafka.apache.org/documentation/
- Docker Compose: https://docs.docker.com/compose/
- Vite: https://vite.dev/guide/

Không coi các URL này là bằng chứng đã chạy/kiểm thử project. Phiên bản và cấu hình thực tế được xác minh trong T01/T02.
