# Scripts

## GenerateJwtKeys.java

Tạo cặp khóa RSA 2048 bit phục vụ JWT RS256.

Yêu cầu: JDK 25.

Chạy từ thư mục gốc ứng dụng, nơi chứa `compose.yml`:

```powershell
java scripts/GenerateJwtKeys.java
```

Kết quả:

- `secrets/jwt-private.pem`: private key dạng PKCS#8 PEM.
- `secrets/jwt-public.pem`: public key dạng X.509 PEM.

Script từ chối chạy nếu một trong hai file đã tồn tại.
Không xóa khóa đang dùng chỉ để chạy lại script.

Không commit các file khóa vào Git.

## Smoke test

Chưa có `smoke-test.ps1`.

Khi bổ sung, script phải:

- Ghi kết quả mong đợi và kết quả thực tế.
- Không in token hoặc mật khẩu.
- Không tự xóa volume.
- Không tự gửi lại POST nạp/rút khi kết quả chưa rõ.
- Chỉ thay đổi số dư trong môi trường test đã được chọn rõ ràng.