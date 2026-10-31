# Hợp đồng API và event — chốt trước khi code

Tất cả API trả JSON. Đường dẫn frontend dùng prefix `/api`. Balance và amount là số nguyên VND trong giới hạn ARCHITECTURE.md. Request lỗi dùng cùng một cấu trúc.

## POST /api/auth/login

Request: `{"username":"demo1","password":"<local-demo-password>"}`

200:
```json
{"accessToken":"<signed-jwt>","tokenType":"Bearer","expiresIn":900,"user":{"id":"<uuid>","username":"demo1"}}
```

400 nếu thiếu trường; 401 nếu tài khoản/mật khẩu sai. Không trả hash hoặc password. JWT có sub=userId, iss=`wallet-login`, aud=`wallet-api`, iat và exp.

## GET /api/wallet/balance

Header `Authorization: Bearer <token>`.

200: `{"userId":"<uuid>","balance":200000,"currency":"VND"}`

401 nếu thiếu/sai/hết hạn token; 404 nếu ví không tồn tại. Không nhận userId từ client để lựa chọn ví.

## POST /api/wallet/deposit và POST /api/wallet/withdraw

Cùng header Bearer. Request: `{"amount":100000}`.

200: `{"transactionId":"<uuid>","type":"DEPOSIT","amount":100000,"balance":300000,"currency":"VND"}`

Với withdraw, type=`WITHDRAW`. transactionId tạo một lần cho thao tác thành công và dùng lại trong event của deposit.

400: amount sai kiểu, <=0, có phần thập phân hoặc vượt giới hạn; 401: xác thực không hợp lệ; 404: không có ví; 409: không đủ tiền hoặc vượt giới hạn balance. 500 chỉ dùng cho lỗi backend chưa phân loại, không lộ stacktrace.

Mẫu lỗi:
```json
{"code":"INSUFFICIENT_BALANCE","message":"Số dư không đủ."}
```

Các code khác: VALIDATION_ERROR, INVALID_CREDENTIALS, UNAUTHORIZED, WALLET_NOT_FOUND, BALANCE_LIMIT_EXCEEDED, INTERNAL_ERROR.

## Kafka event

Topic: `wallet-deposited`; key: userId; consumer group: `wallet-log-group`.

```json
{
  "eventId":"<uuid>",
  "eventType":"MoneyDeposited",
  "schemaVersion":1,
  "transactionId":"<uuid>",
  "userId":"<uuid>",
  "amount":100000,
  "balanceAfter":300000,
  "currency":"VND",
  "occurredAt":"2026-10-30T15:00:00Z"
}
```

occurredAt là UTC ISO 8601. Không đưa password/token vào event hoặc log. Consumer có thể nhận lại một event; với bản đầu chỉ ghi log nên không ảnh hưởng số dư. Nếu sau này có tác động nghiệp vụ, cần chống xử lý trùng theo eventId.

Đặt offset reset earliest cho group mới; dùng commit offset sau xử lý thành công. Không đồng nghĩa mọi lần restart đều đọc lại toàn bộ topic. Event không hợp lệ phải có log rõ và chính sách không retry vô hạn.
