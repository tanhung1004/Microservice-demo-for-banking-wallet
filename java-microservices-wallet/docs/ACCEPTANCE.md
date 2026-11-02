# Nghiệm thu Java Microservices Wallet

Kết quả tổng hợp từ các lần kiểm tra local và Docker Compose.
Dùng tài khoản demo, không sử dụng tiền thật.

“Local” là ứng dụng Java chạy ngoài Docker, kết nối PostgreSQL/Kafka trong Docker.
“Docker” là toàn bộ ứng dụng chạy bằng Compose, truy cập qua Nginx cổng 8080.

| ID | Thao tác | Kết quả mong đợi | Thực tế |
|---|---|---|---|
| A01 | Login đúng/sai/thiếu trường | 200 + token / 401 / 400 | Login đúng và sai đã kiểm tra. Cần bổ sung bằng chứng trường hợp thiếu trường trả 400. |
| A02 | Balance thiếu, sai, hết hạn token | 401 | Đã kiểm tra local; token hết hạn trả 401 với code UNAUTHORIZED. |
| A03 | Hai user gọi balance | Chỉ thấy ví của mình | Đã kiểm tra local với demo1 và demo2, trả đúng userId tương ứng. |
| A04 | Cộng 100000 | Số dư tăng đúng 100000, response khớp DB | Đã quan sát local: response balance 100000 và SELECT database trả 100000. |
| A05 | Trừ hợp lệ | Số dư giảm đúng amount | Đã kiểm tra local; phép trừ 80000 từ 100000 trả balance 20000. Người thực hiện đã xác nhận thao tác giao diện hoạt động. |
| A06 | Trừ quá số dư | 409, số dư giữ nguyên | Đã quan sát local: INSUFFICIENT_BALANCE, HTTP 409; số dư cuối không âm. |
| A07 | Amount 0, âm, lẻ, sai kiểu, vượt giới hạn | 400, số dư giữ nguyên | Deposit local đã kiểm tra 0, -1, 1.5, chuỗi, 1000000001, null và thiếu amount: đều 400; balance giữ 100000. Chưa lưu bằng chứng đầy đủ cùng bộ input cho Withdraw. |
| A08 | Hai request trừ 80000 từ balance 100000 đồng thời | Một thành công, một 409, cuối 20000 | PASS local: một HTTP 200, một HTTP 409, GET balance cuối 20000. |
| A09 | Deposit thành công | Event đúng schema; consumer log cùng transactionId | PASS local: response và subscriber trùng transactionId. Docker đã ghi nhận các event mới tại offset 6–9. |
| A10 | Deposit bị reject/rollback | Không có MoneyDeposited cho thao tác đó | Chưa hoàn tất: chưa có kiểm thử chủ động gây rollback sau khi đăng ký sự kiện. |
| A11 | Dừng subscriber, deposit, chạy lại subscriber | Deposit không chờ subscriber, event được nhận sau | PASS local: deposit khi subscriber dừng, subscriber khởi động lại nhận event tại offset 3. |
| A12 | Gửi message hỏng trong môi trường test | Có log lỗi, consumer không kẹt vô hạn | PASS local: not-json tại offset 4 được ghi INVALID_JSON; event hợp lệ tiếp theo tại offset 5 được nhận. |
| A13 | Broker lỗi khi deposit | Ghi rõ trạng thái DB và lỗi publish; không tuyên bố chưa cộng nếu DB đã commit | Đã kiểm tra local: API trả thành công, balance được cập nhật; publisher ghi lỗi AFTER DB COMMIT. Không bảo đảm giao sự kiện. |
| A14 | Restart Compose không xóa volumes | Số dư giữ nguyên, seed không reset | PASS Docker: tạo lại hai container PostgreSQL với volume cũ; dữ liệu ví trước/sau trùng nhau. Đăng nhập và giao diện hoạt động sau khi bật lại ứng dụng. |
| A15 | UI login/cộng/trừ/logout | API thật, có loading/error, hiển thị đúng | Người thực hiện xác nhận test giao diện local hoàn tất. Docker đã xác nhận đăng nhập, xem số dư, nạp tiền và subscriber nhận event; chưa lưu riêng bằng chứng đầy đủ mọi nhánh UI trên Docker. |
| A16 | Checkout sạch và làm theo README | Build/chạy/demo được không cần file bí mật không được hướng dẫn | Chưa chạy. Đang hoàn thiện hướng dẫn tạo khóa và chuẩn bị kiểm tra checkout sạch. |

## Bằng chứng tiêu biểu

### Nạp tiền và subscriber local

- transactionId: `878a0fe3-8a6b-430e-9a69-145234866582`
- amount: 1000 VND
- balanceAfter: 33001 VND
- Subscriber ghi cùng transactionId tại partition 0, offset 2.

### Subscriber nhận tiếp sau message hỏng

- Message hỏng: partition 0, offset 4, reason INVALID_JSON.
- Event hợp lệ tiếp theo:
  transactionId `ef60d6f3-7353-46ea-a865-d1c34eddd066`.
- Subscriber nhận tại offset 5, balanceAfter 35001 VND.

### Publish lỗi sau DB commit

Publisher ghi:

`Deposit event publish failed AFTER DB COMMIT`

Với transactionId:

`10c87600-98d4-499b-849f-13be159d7603`

Kết quả này chứng minh nhánh ghi nhận lỗi publish đã chạy,
không chứng minh sự kiện được phục hồi sau khi Kafka hoạt động lại.

### Nạp tiền trong Docker

Subscriber ghi nhận:

- transactionId: `24291ead-d49f-4797-a6aa-3dcea46c806a`
- amount: 1000000000 VND
- balanceAfter: 1000018123 VND
- partition: 0
- offset: 9

### Kiểm tra volume trong Docker

Sau khi dừng ứng dụng và tạo lại hai container PostgreSQL:

| User | Số dư trước | Số dư sau |
|---|---:|---:|
| demo1 | 1000018123 | 1000018123 |
| demo2 | 0 | 0 |

Script so sánh trước/sau trả PASS.
Giao diện hiển thị số dư demo1 là 1.000.018.123 VND sau khi ứng dụng chạy lại.

## Backup

Đã tạo và copy backup hai database ra ngoài repository:

- login.dump: 4100 bytes.
- wallet.dump: 3808 bytes.

Đây là bằng chứng tạo/copy backup thành công.
Chưa kiểm tra phục hồi backup trên môi trường riêng.

## Các việc còn lại

- Bổ sung bằng chứng Login thiếu trường trả 400.
- Kiểm tra đầy đủ validation của Withdraw.
- Kiểm tra rollback sau khi đăng ký sự kiện ở A10.
- Hoàn thiện hướng dẫn tạo khóa JWT.
- Chạy từ checkout sạch ở A16.
- Kiểm tra không có private key hoặc bí mật thực được Git theo dõi.

Không tự reset số dư hoặc xóa volume hiện tại để chạy các kiểm thử còn lại.
Các kiểm thử cần trạng thái dữ liệu đặc biệt phải dùng môi trường test riêng.

## Giới hạn giao sự kiện

Bản hiện tại dùng publish sau DB commit, chưa triển khai transactional outbox.

A13 là kiểm tra và công bố giới hạn, không chứng minh delivery bảo đảm.
Chỉ tuyên bố phục hồi sự kiện sau lỗi khi đã triển khai và kiểm thử cơ chế đó.