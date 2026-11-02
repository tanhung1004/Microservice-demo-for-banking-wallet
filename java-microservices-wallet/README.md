# Java Microservices Wallet

Ứng dụng mô phỏng ví tiền để học microservices:

Đăng nhập → xem số dư → nạp/rút tiền. Khi nạp tiền thành công,
Wallet Service phát sự kiện qua Kafka để Wallet Subscriber nhận và ghi log.

Không sử dụng tiền thật.

## Trạng thái

- Đã triển khai backend, frontend và Kafka subscriber.
- Đã chạy toàn bộ hệ thống bằng Docker Compose.
- Đã kiểm tra dữ liệu ví được giữ nguyên khi tạo lại container database.
- Đang hoàn thiện nghiệm thu và hướng dẫn chạy từ checkout sạch ở T12.
- Chưa triển khai Cloudflare.

Kết quả kiểm tra chi tiết nằm trong `docs/ACCEPTANCE.md`.
Không coi các trường hợp chưa kiểm tra là đã đạt.

## Thành phần

| Thành phần | Chức năng |
|---|---|
| Login Service | Xác thực tài khoản, cấp JWT |
| Wallet Service | Lấy số dư, nạp/rút tiền, phát sự kiện |
| Wallet Subscriber | Nhận MoneyDeposited và ghi log |
| Frontend + Nginx | Giao diện React và chuyển tiếp API |
| Login PostgreSQL | Lưu dữ liệu đăng nhập |
| Wallet PostgreSQL | Lưu dữ liệu ví |
| Kafka | Lưu và phân phối sự kiện nạp tiền |

Nginx chuyển tiếp:

- `/api/auth/` → `login-service:8081`
- `/api/wallet/` → `wallet-service:8082`

Frontend gọi API bằng đường dẫn tương đối trên cùng địa chỉ truy cập.

## Công nghệ

- Java 25, Spring Boot.
- Spring Security OAuth2 Resource Server, JWT RS256.
- PostgreSQL 17.6, JDBC, Flyway.
- Apache Kafka 3.9.1, Spring Kafka.
- React, Vite, Nginx.
- Docker Compose.

Phiên bản thư viện cụ thể được khai báo trong các file `pom.xml`,
`frontend/package.json` và `frontend/package-lock.json`.
Image sử dụng được khai báo trong Dockerfile và Compose.

## Cấu trúc thư mục

| Đường dẫn | Nội dung |
|---|---|
| `services/login-service/` | Backend đăng nhập |
| `services/wallet-service/` | Backend ví tiền |
| `services/wallet-subscriber/` | Kafka consumer |
| `frontend/` | React, Dockerfile và cấu hình Nginx |
| `secrets/` | Khóa JWT trên máy chạy, không commit khóa |
| `scripts/` | Công cụ hỗ trợ, gồm GenerateJwtKeys.java |
| `docs/` | Thiết kế, hợp đồng, checklist và nghiệm thu |
| `compose.yml` | PostgreSQL, Kafka và volume |
| `compose.override.yml` | Các ứng dụng và cấu hình chạy trong Docker |

## Yêu cầu

Để chạy bằng Docker:

- Docker Desktop đang hoạt động, sử dụng Linux containers.
- Docker Compose.
- Git nếu tải dự án từ repository.
- Kết nối Internet cho lần build đầu tiên.

Java, Node và Maven dùng để build ứng dụng được cung cấp trong Docker image.
Không cần chạy riêng backend bằng IDE hoặc frontend bằng Vite.

Khâu tạo khóa JWT có yêu cầu riêng được mô tả bên dưới.

## Chuẩn bị khóa JWT

Login Service cần hai file:

- `secrets/jwt-private.pem`
- `secrets/jwt-public.pem`

Wallet Service chỉ sử dụng public key.

Khóa được mount vào container ở chế độ chỉ đọc, không đóng vào image.
Không commit private key hoặc gửi nội dung private key cho người khác.

Repository có `scripts/GenerateJwtKeys.java`.
Hướng dẫn gọi script đang được xác minh trong T12.
Checkout mới chưa có khóa cần hoàn thành bước này trước khi khởi động.

Nếu máy đã có cặp khóa đang sử dụng thì giữ nguyên, không tạo đè.

## Chạy trên Windows PowerShell

Mở terminal tại thư mục chứa cả `compose.yml` và `compose.override.yml`.

Kiểm tra cấu hình:

```powershell
docker compose config --quiet
```

Build và chạy:

```powershell
docker compose up -d --build
```

Lần đầu có thể mất vài phút để tải image và Maven dependencies.

Kiểm tra trạng thái:

```powershell
docker compose ps -a
```

Mở:

http://localhost:8080

Các service có healthcheck cần chuyển sang `healthy`.
Subscriber cần ở trạng thái `Up`; kiểm tra log để xác nhận consumer hoạt động.

`kafka-init` và `kafka-topics` là các job khởi tạo.
Trạng thái `Exited (0)` của hai job này là bình thường.

## Tài khoản demo

| Username | Password |
|---|---|
| `demo1` | `Demo123!` |
| `demo2` | `Demo456!` |

Số dư phụ thuộc dữ liệu hiện có trong volume.
Không mặc định số dư luôn bằng 0 sau mỗi lần khởi động.

Các tài khoản này chỉ dành cho bản demo học tập.

## Thao tác trên giao diện

1. Đăng nhập.
2. Xem số dư.
3. Nhập số tiền nguyên, không dùng dấu phân cách hàng nghìn.
4. Chọn cộng tiền hoặc trừ tiền.
5. Kiểm tra số dư và thông báo trả về.
6. Đăng xuất khi hoàn tất.

Quy tắc:

- Mỗi thao tác từ 1 đến 1.000.000.000 VND.
- Số dư tối đa 1.000.000.000.000 VND.
- Không được rút quá số dư.
- Ví được xác định từ JWT đã xác thực.
- Token được giữ trong bộ nhớ frontend; tải lại trang cần đăng nhập lại.
- Khi kết quả giao dịch chưa rõ, không tự gửi lại yêu cầu nạp/rút.

## Xem log

```powershell
docker compose logs --tail 100 login-service
docker compose logs --tail 100 wallet-service
docker compose logs --tail 100 wallet-subscriber
docker compose logs --tail 100 frontend
```

Tìm sự kiện subscriber đã nhận:

```powershell
docker compose logs --since 30m wallet-subscriber |
    Select-String -SimpleMatch "MoneyDeposited received"
```

Đối chiếu `transactionId` trong response nạp tiền với log subscriber.

## Dừng và chạy lại

Dừng hệ thống:

```powershell
docker compose stop
```

Chạy lại:

```powershell
docker compose up -d
```

Sau khi sửa source hoặc Dockerfile:

```powershell
docker compose up -d --build
```

Đóng VS Code không tự dừng container chạy bằng chế độ `-d`.

## Lưu trữ dữ liệu

Dự án dùng ba named volume:

- `java-wallet_login_data`
- `java-wallet_wallet_data`
- `java-wallet_kafka_data`

Giữ nguyên project name `java-wallet` và cấu hình volume để tiếp tục dùng
dữ liệu hiện tại.

Tạo lại container với cùng volume không xóa dữ liệu trong volume.
Không chạy `docker compose down -v` nếu muốn giữ dữ liệu.

Volume không thay thế backup. Xóa volume, reset Docker Desktop hoặc hỏng
ổ đĩa có thể làm mất dữ liệu.

## Backup PostgreSQL

Chạy khi hai database đang hoạt động:

```powershell
$backupDir = Join-Path $env:USERPROFILE (
    "wallet-backups\" + (Get-Date -Format "yyyyMMdd-HHmmss")
)
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null

docker compose exec -T login-db pg_dump `
    -U login_user -d login_db -Fc -f /tmp/login-backup.dump
if ($LASTEXITCODE -ne 0) { throw "Backup login-db thất bại." }

docker compose cp login-db:/tmp/login-backup.dump "$backupDir\login.dump"
if ($LASTEXITCODE -ne 0) { throw "Copy backup login-db thất bại." }

docker compose exec -T wallet-db pg_dump `
    -U wallet_user -d wallet_db -Fc -f /tmp/wallet-backup.dump
if ($LASTEXITCODE -ne 0) { throw "Backup wallet-db thất bại." }

docker compose cp wallet-db:/tmp/wallet-backup.dump "$backupDir\wallet.dump"
if ($LASTEXITCODE -ne 0) { throw "Copy backup wallet-db thất bại." }

Get-ChildItem $backupDir
```

Backup trên chỉ gồm hai database PostgreSQL, không gồm Kafka hoặc khóa JWT.
Cần kiểm tra phục hồi backup trên môi trường riêng trước khi coi quy trình
backup/restore đã được nghiệm thu.

## Xử lý lỗi thường gặp

### no configuration file provided: not found

Terminal đang đứng sai thư mục.
Chuyển đến thư mục chứa `compose.yml` và `compose.override.yml`.

### localhost:8080 từ chối kết nối

Kiểm tra build đã hoàn tất và frontend đã chạy:

```powershell
docker compose ps -a
docker compose logs --tail 100 frontend
```

### Backend không khởi động

Kiểm tra log Login hoặc Wallet, trạng thái database và các file khóa JWT.

### Không thấy sự kiện mới

Kiểm tra Kafka, subscriber và log publish của Wallet Service.
Nếu subscriber local trong IDE vẫn chạy cùng consumer group,
nó có thể nhận sự kiện thay cho subscriber Docker.

## Giới hạn của bản demo

- Không phải hệ thống thanh toán thực tế.
- Chưa có đăng ký, quên mật khẩu, chuyển tiền hoặc lịch sử giao dịch bền vững.
- Chưa có idempotency key cho API nạp/rút.
- Không tự động retry giao dịch khi kết quả chưa rõ.
- Sự kiện được gửi sau khi transaction database commit.
- Chưa có transactional outbox: DB có thể cập nhật thành công nhưng sự kiện
  không đến Kafka khi publish lỗi.
- Producer idempotence không bảo đảm exactly-once cho toàn bộ luồng nghiệp vụ.
- Subscriber chỉ kiểm tra và ghi log; message sai được ghi nhận rồi bỏ qua.
- Kafka chạy một broker, replication factor 1; chưa có khả năng chịu lỗi cao.
- Cấu hình hiện tại dùng tài khoản và mật khẩu demo.
- Chưa nghiệm thu cho triển khai production hoặc truy cập công khai.

## Tài liệu

- [Thiết kế](docs/ARCHITECTURE.md)
- [Hợp đồng API và event](docs/CONTRACTS.md)
- [Task decomposition](docs/TASK_DECOMPOSITION.md)
- [Kết quả nghiệm thu](docs/ACCEPTANCE.md)