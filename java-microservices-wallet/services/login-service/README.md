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

## API đăng nhập

POST /api/auth/login

Request mẫu:
```json
{
  "username": "demo1",
  "password": "Demo123!"
}
```

Đăng nhập thành công trả HTTP 200 với:
- accessToken: JWT ký bằng RSA RS256.
- tokenType: Bearer.
- expiresIn: 900 giây.
- user: id và username; không chứa passwordHash.

JWT claims:
- sub: UUID của tài khoản.
- iss: wallet-login.
- aud: wallet-api.
- iat: thời điểm cấp.
- exp: hết hạn sau 15 phút.

Sai thông tin đăng nhập trả HTTP 401.
Thiếu thông tin trả HTTP 400 với code VALIDATION_ERROR.

## Khóa JWT local

Chạy từ thư mục gốc dự án:
```powershell
java .\scripts\GenerateJwtKeys.java
```

Script tạo cặp khóa trong secrets/ và không ghi đè khóa đã tồn tại.
Thư mục secrets/ được bỏ qua bởi Git.

Login đọc khóa qua app.jwt.private-key và app.jwt.public-key.
Có thể cấu hình đường dẫn bằng JWT_PRIVATE_KEY_PATH và JWT_PUBLIC_KEY_PATH.

## Kiểm tra JWT

Chạy tại services/login-service:
```powershell
.\mvnw.cmd "-Dtest=JwtServiceTest" clean test
```

Đã chạy thành công 2 test:
- Xác minh chữ ký, user, issuer, audience và thời hạn 900 giây.
- Từ chối token khi xác minh bằng public key khác.

Test sử dụng khóa tạm trong bộ nhớ, không cần database.
