# Task Decomposition — Java Microservices Wallet

## 1. Mục tiêu

Xây dựng ứng dụng ví tiền mô phỏng:

Đăng nhập → xem số dư → cộng/trừ tiền.

Sau khi cộng tiền và lưu dữ liệu thành công:
Wallet phát event vào Kafka → Subscriber nhận và ghi log.

## 2. Công nghệ

- Backend: Java + Spring Boot.
- Frontend: React + Vite.
- Database: PostgreSQL, dữ liệu riêng cho Login và Wallet.
- Messaging: Kafka.
- Khởi chạy hệ thống: Docker Compose.

## 3. Phạm vi

- Login Service: xác thực tài khoản mẫu và cấp token.
- Wallet Service: xem số dư, cộng tiền và trừ tiền.
- Wallet Subscriber: nhận sự kiện cộng tiền và ghi log.
- Frontend: màn hình đăng nhập và màn hình ví.
- Không tích hợp tiền thật, ngân hàng hoặc chuyển tiền giữa các ví.

## 4. Quy trình thực hiện mỗi task

1. Đọc mục tiêu và task phụ thuộc.
2. Tạo/sửa đúng file theo hướng dẫn triển khai.
3. Chạy kiểm tra và đối chiếu kết quả mong đợi.
4. Cập nhật docs/PROGRESS.md bằng kết quả thực tế.
5. Kiểm tra git diff và commit.
6. Chuyển task tiếp theo khi task hiện tại đã đạt.

Không đánh dấu hoàn thành nếu chưa kiểm tra.
Không commit mật khẩu thực, token, private key hoặc file .env.

---

## T01 — Khởi tạo project

Phụ thuộc: đã đọc ARCHITECTURE.md và CONTRACTS.md.

Mục đích:
Tạo các ứng dụng rỗng để có nền tảng viết code.

Công việc:

- [x] Kiểm tra Java, Node.js, npm, Docker và Git.
- [x] Tạo Spring Boot project trong services/login-service.
- [x] Tạo Spring Boot project trong services/wallet-service.
- [x] Tạo Spring Boot project trong services/wallet-subscriber.
- [x] Tạo React + Vite project trong frontend.
- [x] Ghi phiên bản sử dụng và commit Maven Wrapper/lockfile.

Kiểm tra:

- Các project Java build/test được theo cấu hình khởi tạo.
- Frontend build được.
- Chưa yêu cầu chức năng đăng nhập hoặc ví hoạt động.

Commit:
chore: scaffold Java services and React frontend

## T02 — Chuẩn bị PostgreSQL và Kafka

Phụ thuộc: T01.

Mục đích:
Có database và Kafka để các chương trình kết nối khi phát triển.

Vị trí:
compose.yaml, .env.example, infra/.

Công việc:

- [x] Khai báo PostgreSQL và Kafka trong Compose.
- [x] Tạo login_db và wallet_db với quyền truy cập riêng.
- [x] Cấu hình volume để giữ dữ liệu.
- [x] Cấu hình Kafka cho kết nối từ máy host và container.
- [x] Tạo topic wallet-deposited.
- [x] Ghi lệnh khởi chạy và kiểm tra vào tài liệu.

Kiểm tra:

- PostgreSQL và Kafka chạy thành công.
- Kết nối được từng database bằng đúng tài khoản.
- Gửi và nhận được message thử qua Kafka.

Commit:
chore(infra): configure PostgreSQL and Kafka

## T03 — Xây dựng Login Service

Phụ thuộc: T02.

Mục đích:
Xác thực người dùng và cấp token để gọi Wallet API.

Vị trí:
services/login-service/.

Công việc:

- [x] Kết nối PostgreSQL và tạo bảng users bằng Flyway.
- [x] Tạo User entity, repository và tài khoản demo.
- [x] Lưu mật khẩu dưới dạng hash.
- [x] Tạo API POST /api/auth/login.
- [x] Kiểm tra dữ liệu và thông tin đăng nhập.
- [x] Cấp token có thời hạn khi đăng nhập đúng.
- [x] Không reset tài khoản có sẵn khi khởi động lại.
- [x] Kiểm tra username/password bằng BCrypt.
- [x] Trả JWT ký bằng RSA RS256 khi đăng nhập thành công.
- [x] JWT chứa sub, iss, aud, iat, exp; thời hạn 900 giây.
- [x] Kiểm tra đăng nhập thành công, sai mật khẩu và thiếu dữ liệu.
- [x] Unit test JWT: chữ ký và claims hợp lệ; từ chối public key sai.

Kiểm tra:

- Đúng tài khoản: 200 và token.
- Sai thông tin đăng nhập: 401.
- Thiếu trường bắt buộc: 400.
- Response và log không chứa mật khẩu/hash.

Commit:
feat(auth): implement login and access tokens

## T04 — Xác thực Wallet và lấy số dư

Phụ thuộc: T03.

Mục đích:
Cho người dùng đã đăng nhập xem đúng ví của mình.

Vị trí:
services/wallet-service/.

Công việc:

- [ ] Tạo Wallet entity, repository và ví demo.
- [x] Cấu hình xác thực token.
- [x] Lấy userId từ token đã xác thực.
- [ ] Tạo GET /api/wallet/balance.
- [ ] Không reset số dư khi khởi động lại.

Kiểm tra:

- Token hợp lệ: trả số dư đúng người dùng.
- Token thiếu, sai hoặc hết hạn: 401.
- Người dùng không xem được ví của người khác.

Commit:
feat(wallet): implement authenticated balance endpoint

## T05 — Cộng tiền

Phụ thuộc: T04.

Mục đích:
Cập nhật số dư khi người dùng gửi yêu cầu cộng tiền.

Vị trí:
Controller, DTO, Service, Repository và tests trong Wallet.

Công việc:

- [ ] Tạo POST /api/wallet/deposit.
- [ ] Kiểm tra amount theo CONTRACTS.md.
- [ ] Cập nhật số dư trong transaction an toàn.
- [ ] Trả mã giao dịch và số dư mới.
- [ ] Chưa phát Kafka event trong task này.

Kiểm tra:

- Cộng 100000: số dư tăng đúng 100000.
- Amount không hợp lệ: trả lỗi, không đổi số dư.
- Hai yêu cầu cộng đồng thời không làm mất cập nhật.

Commit:
feat(wallet): implement validated deposits

## T06 — Trừ tiền

Phụ thuộc: T05.

Mục đích:
Cho phép trừ tiền nhưng không làm số dư âm.

Vị trí:
Controller, DTO, Service, Repository và tests trong Wallet.

Công việc:

- [ ] Tạo POST /api/wallet/withdraw.
- [ ] Kiểm tra amount hợp lệ.
- [ ] Kiểm tra đủ tiền và cập nhật trong cùng transaction.
- [ ] Trả số dư mới hoặc lỗi rõ ràng.

Kiểm tra:

- Đủ tiền: trừ đúng amount.
- Không đủ tiền: 409, giữ nguyên số dư.
- Hai yêu cầu trừ đồng thời không gây chi vượt số dư.

Commit:
feat(wallet): implement withdrawals with balance protection

## T07 — Phát sự kiện cộng tiền

Phụ thuộc: T05, T06 và Kafka ở T02.

Mục đích:
Thông báo cho chương trình khác biết tiền đã được cộng thành công.

Vị trí:
event/ và messaging/ trong Wallet Service.

Công việc:

- [ ] Tạo MoneyDepositedEvent theo CONTRACTS.md.
- [ ] Tạo producer gửi vào topic wallet-deposited.
- [ ] Chỉ phát event sau khi transaction DB commit thành công.
- [ ] Ghi nhận lỗi publish để có thể kiểm tra.
- [ ] Ghi rõ giới hạn giữa DB commit và Kafka publish.

Kiểm tra:

- Deposit thành công: Kafka nhận event đúng thông tin.
- Deposit bị từ chối/rollback: không phát event thành công.
- Không tuyên bố chống mất hoặc trùng event tuyệt đối.

Commit:
feat(events): publish deposit events after commit

## T08 — Subscriber nhận sự kiện

Phụ thuộc: T07.

Mục đích:
Thực hành chương trình độc lập nhận và xử lý event từ Kafka.

Vị trí:
services/wallet-subscriber/.

Công việc:

- [ ] Cấu hình Kafka consumer và consumer group.
- [ ] Lắng nghe topic wallet-deposited.
- [ ] Đọc event và ghi log thông tin giao dịch.
- [ ] Có xử lý lỗi cho message không hợp lệ.
- [ ] Subscriber không cập nhật số dư.

Kiểm tra:

- Cộng tiền ở Wallet: subscriber có log tương ứng.
- Tạm dừng subscriber rồi chạy lại: nhận phần chưa xử lý.
- Message lỗi không làm chương trình retry vô hạn.

Commit:
feat(subscriber): consume and log deposit events

## T09 — Giao diện đăng nhập

Phụ thuộc: T03, T04.

Mục đích:
Cho người dùng đăng nhập bằng giao diện thay vì công cụ gọi API.

Vị trí:
frontend/src/ và frontend/vite.config.js.

Công việc:

- [ ] Tạo form username/password.
- [ ] Gọi Login API thật.
- [ ] Hiển thị loading và lỗi.
- [ ] Giữ token trong memory và chuyển sang trang ví.
- [ ] Xử lý đăng xuất và token hết hạn.

Kiểm tra:

- Sai mật khẩu: hiển thị lỗi.
- Đúng mật khẩu: vào trang ví.
- Đăng xuất: xóa token và thông tin người dùng.
- Reload yêu cầu đăng nhập lại theo thiết kế bản đầu.

Commit:
feat(ui): implement login flow and API client

## T10 — Giao diện ví

Phụ thuộc: T06, T08, T09.

Mục đích:
Hoàn thành luồng sử dụng từ giao diện đến backend và Kafka.

Vị trí:
frontend/src/pages/, services/ và styles.

Công việc:

- [ ] Hiển thị số dư lấy từ API.
- [ ] Tạo ô nhập tiền và nút cộng/trừ.
- [ ] Gọi Deposit và Withdraw API thật.
- [ ] Cập nhật giao diện theo response backend.
- [ ] Khóa nút khi đang xử lý và hiển thị lỗi.
- [ ] Không tự gửi lại POST tiền khi chưa rõ kết quả.

Kiểm tra:

- Đăng nhập → xem ví → cộng/trừ hoạt động.
- Không đủ tiền: hiển thị lỗi, số dư không đổi.
- Cộng thành công: số dư đổi và subscriber có log.

Commit:
feat(ui): connect wallet balance and money actions

## T11 — Docker hóa toàn bộ hệ thống

Phụ thuộc: T08, T10.

Mục đích:
Chạy cả ứng dụng bằng Compose mà không cần mở từng backend trong IDE.

Vị trí:
Dockerfile, .dockerignore, compose.yaml và cấu hình frontend proxy.

Công việc:

- [ ] Viết Dockerfile cho ba chương trình Java.
- [ ] Đóng gói frontend.
- [ ] Thêm các ứng dụng vào Compose.
- [ ] Cấu hình network, ports, environment và key.
- [ ] Cấu hình frontend gọi đúng API.
- [ ] Giữ dữ liệu qua volume.

Kiểm tra:

- docker compose up --build chạy được toàn bộ.
- Truy cập giao diện và thực hiện toàn luồng.
- Khởi động lại không xóa volume: số dư vẫn còn.

Commit:
chore(docker): package the complete wallet application

## T12 — Kiểm thử và bàn giao

Phụ thuộc: T11.

Mục đích:
Người khác có thể tải source và làm theo hướng dẫn để chạy lại.

Vị trí:
README.md, docs/ACCEPTANCE.md, docs/PROGRESS.md, scripts/.

Công việc:

- [ ] Chạy các tình huống trong ACCEPTANCE.md.
- [ ] Ghi kết quả thực tế và sửa lỗi.
- [ ] Viết hướng dẫn thiết lập env/key và tài khoản demo.
- [ ] Ghi lệnh build, chạy, xem log và dừng hệ thống.
- [ ] Kiểm tra từ một checkout sạch.
- [ ] Nêu rõ giới hạn của bản demo.

Kiểm tra:

- Người khác làm theo hướng dẫn chạy được.
- Không thiếu file hoặc bước cấu hình chưa được ghi.
- Không có bí mật thực trong repository.

Commit:
docs: add setup instructions and acceptance results
