# Login Service
Khung, chưa có code. Triển khai T01 và T03 theo ../../docs/TASK_DECOMPOSITION.md. Sở hữu login_db; cấp JWT; không quản lý balance.

## Kết nối database và chạy local

Yêu cầu:
- Java 25.
- Docker Desktop đang chạy.
- Login DB trong Compose đã healthy, truy cập từ máy qua localhost:15433.

Chạy các lệnh sau trong PowerShell, tại thư mục services/login-service:

```powershell
$env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS -Duser.timezone=UTC"
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Chỉ chạy ứng dụng sau khi build báo BUILD SUCCESS.

Cấu hình UTC áp dụng cho terminal hiện tại. Khi mở terminal mới,
chạy lại dòng cấu hình trước khi build hoặc chạy ứng dụng.
Cấu hình này tránh lỗi PostgreSQL từ chối múi giờ Asia/Saigon.

Kiểm tra sức khỏe: http://localhost:8081/actuator/health

## Migration database
Flyway tự chạy các file SQL trong src/main/resources/db/migration.
V1__create_users.sql tạo bảng users.
Đã kiểm tra migration V1 có success = true trong flyway_schema_history.
Không sửa migration đã chạy thành công.
Thay đổi cấu trúc tiếp theo cần tạo migration V2.

## Tài khoản demo local

| Username | Password | UUID |
|---|---|---|
| demo1 | Demo123! | 11111111-1111-4111-8111-111111111111 |
| demo2 | Demo456! | 22222222-2222-4222-8222-222222222222 |

Đây là tài khoản mẫu dùng cho local.

Chạy tại thư mục services/login-service:
```powershell
$env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS -Duser.timezone=UTC"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Seeder chỉ hoạt động với profile local.
Mật khẩu được băm bằng BCrypt; tài khoản đã tồn tại không bị ghi đè.

Đã kiểm tra restart: vẫn hai tài khoản, UUID và fingerprint của
password_hash giữ nguyên.
