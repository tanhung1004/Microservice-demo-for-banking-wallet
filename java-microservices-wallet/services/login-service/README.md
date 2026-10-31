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
