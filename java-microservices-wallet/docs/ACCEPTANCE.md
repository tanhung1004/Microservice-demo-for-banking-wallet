# Nghiệm thu — chưa chạy

Không đổi cột kết quả thành PASS khi chưa có bằng chứng. Dùng user demo, không tiền thật. Khi test tiền, ghi số dư trước/sau; reset dữ liệu chỉ trong môi trường test được chủ động chọn.

| ID | Thao tác | Kết quả mong đợi | Thực tế |
|---|---|---|---|
| A01 | Login đúng/sai/thiếu trường | 200 + token / 401 / 400 | Chưa chạy |
| A02 | Balance thiếu, sai, hết hạn token | 401 | Chưa chạy |
| A03 | Hai user gọi balance | Chỉ thấy ví của mình | Chưa chạy |
| A04 | Cộng 100000 | Số dư tăng đúng 100000, response khớp DB | Chưa chạy |
| A05 | Trừ hợp lệ | Số dư giảm đúng amount | Chưa chạy |
| A06 | Trừ quá số dư | 409, số dư giữ nguyên | Chưa chạy |
| A07 | Amount 0, âm, lẻ, sai kiểu, vượt giới hạn | 400, số dư giữ nguyên | Chưa chạy |
| A08 | Hai request trừ 80000 từ balance 100000 đồng thời | Một thành công, một 409, cuối 20000 | Chưa chạy |
| A09 | Deposit thành công | Event đúng schema; consumer log cùng transactionId | Chưa chạy |
| A10 | Deposit bị reject/rollback | Không có MoneyDeposited cho thao tác đó | Chưa chạy |
| A11 | Dừng subscriber, deposit, chạy lại subscriber | Deposit không chờ subscriber, event được nhận sau | Chưa chạy |
| A12 | Gửi message hỏng trong môi trường test | Có log lỗi, consumer không kẹt vô hạn | Chưa chạy |
| A13 | Broker lỗi khi deposit | Ghi rõ trạng thái DB và lỗi publish; không tuyên bố chưa cộng nếu DB đã commit | Chưa chạy |
| A14 | Restart Compose không xóa volumes | Số dư giữ nguyên, seed không reset | Chưa chạy |
| A15 | UI login/cộng/trừ/logout | API thật, có loading/error, hiển thị đúng | Chưa chạy |
| A16 | Checkout sạch và làm theo README | Build/chạy/demo được không cần file bí mật không được hướng dẫn | Chưa chạy |

Với bản cơ bản A13 là kiểm tra và công bố giới hạn, KHÔNG chứng minh delivery bảo đảm. Chỉ tuyên bố phục hồi event sau lỗi khi đã triển khai và kiểm thử outbox.
