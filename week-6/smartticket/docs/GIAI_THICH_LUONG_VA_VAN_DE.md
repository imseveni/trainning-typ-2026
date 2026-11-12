# SmartTicket: giải quyết tranh chấp đặt ghế

## 1. Bài toán

Một suất diễn có số ghế hữu hạn. Khi nhiều request cùng chọn một ghế, hai request có thể cùng đọc thấy ghế đang trống rồi cùng ghi nhận đặt chỗ. Kết quả sai này gọi là **overselling**. Hệ thống cần kiểm tra và cập nhật trạng thái ghế theo cách không cho hai transaction cùng thành công trên một row ghế.

SmartTicket minh họa vấn đề đó bằng Spring Boot, Spring Data JPA và MySQL. Ứng dụng cũng có đăng ký/đăng nhập JWT và phân quyền `USER`/`ADMIN`; phần cốt lõi của bài toán đồng thời là transaction, pessimistic row lock, index và constraint.

```text
Browser → JWT Bearer token → SecurityFilterChain
                           → SeatHoldController → SeatHoldService (@Transactional)
                                  ↓
                         Repository / JPA
                                  ↓
                                MySQL
```

## 2. Mô hình dữ liệu liên quan

- `showtime_seats`: một row đại diện cho một ghế trong một suất diễn; trạng thái là `AVAILABLE`, `HELD` hoặc `SOLD`.
- `seat_holds`: lượt giữ tạm thời, có chủ sở hữu, suất diễn, trạng thái và thời gian hết hạn.
- `seat_hold_items`: liên kết hold với các ghế đã giữ.
- `bookings`: booking được tạo khi hold được xác nhận.

Luồng trạng thái thường là:

```text
AVAILABLE ──giữ ghế──→ HELD ──xác nhận──→ SOLD
    ↑                     │
    └──── huỷ / hết hạn ──┘
```

## 3. Luồng giữ ghế và transaction

Client đăng nhập và gửi access token trong header `Authorization: Bearer ...`. Sau đó client gửi `POST /api/holds` với `showtimeId` và một hoặc nhiều `showtimeSeatIds`. Server lấy User ID từ claim `sub` của JWT; client không được tự chỉ định user sở hữu hold.

1. `SeatHoldController` nhận request và chuyển cho `SeatHoldService.createSeatHold()`.
2. `@Transactional` mở transaction database. Service tìm user theo ID lấy từ JWT và xác nhận suất diễn tồn tại.
3. Service sắp xếp ID ghế tăng dần để mọi request nhiều ghế lấy lock theo cùng thứ tự.
4. `ShowtimeSeatRepository.findByShowtimeSeatIdForUpdate()` truy vấn từng row với `PESSIMISTIC_WRITE`, tương đương khóa row ở MySQL (`SELECT ... FOR UPDATE`).
5. Sau khi lấy lock, service kiểm tra ghế thuộc đúng suất diễn và vẫn có thể giữ.
6. Nếu tất cả ghế hợp lệ, service tạo `SeatHold`, các `SeatHoldItem` và đổi trạng thái các ghế thành `HELD`.
7. Transaction commit rồi API trả lượt giữ. Nếu ghế nào không hợp lệ/đã có người giữ, API trả lỗi và transaction rollback.

Vì mọi thao tác ghi của một lượt giữ cùng thuộc transaction, hệ thống không để request giữ được một phần danh sách ghế rồi thất bại ở phần còn lại.

## 4. Hai người tranh cùng một ghế

Giả sử User A và User B cùng gửi request giữ ghế X:

1. Cả hai transaction tìm row `showtime_seats` của X để lấy khóa ghi.
2. MySQL cấp row lock cho transaction lấy được khóa trước. Transaction còn lại phải chờ.
3. Request đang giữ khóa kiểm tra `AVAILABLE`, tạo hold, đổi X thành `HELD` rồi commit.
4. Request đang chờ tiếp tục và đọc trạng thái mới nhất là `HELD`; service từ chối với `409 Conflict`.

Ứng dụng không xác định ai bấm chuột trước tuyệt đối. Kết quả theo thứ tự database cấp khóa. Điều đảm bảo đúng là chỉ một transaction cập nhật được ghế từ trạng thái còn trống; transaction còn lại thấy kết quả đã commit.

## 5. Tại sao khóa theo ID tăng dần?

Nếu một request giữ ghế 10 rồi 20, request khác giữ ghế 20 rồi 10, cả hai có thể giữ mỗi request một khóa và chờ khóa còn lại. Đây là deadlock. Sắp xếp ghế tăng dần trước khi lock khiến các request xin khóa cùng thứ tự, giảm nguy cơ chờ lẫn nhau.

## 6. Vai trò transaction, lock, index và constraint

| Kiến thức | Cách dùng trong dự án | Vai trò |
|---|---|---|
| Transaction | `@Transactional` trong `SeatHoldService` | Gom tạo hold, hold items và cập nhật ghế; commit hoặc rollback cùng nhau |
| Pessimistic lock | `PESSIMISTIC_WRITE` trên row `showtime_seats` | Tuần tự hóa request tranh cùng một ghế |
| Unique constraint | `uk_showtime_seat(showtime_id, seat_id)` | Ngăn tạo trùng row inventory cho cùng ghế và suất diễn |
| Unique constraint | `bookings.seat_hold_id` | Ngăn tạo nhiều booking cho cùng một hold |
| Index | `showtime_seats(showtime_id, status)` | Hỗ trợ tìm/lọc ghế trong suất diễn; không tự đảm bảo tính đúng đắn concurrent |
| REST status | `409 Conflict` khi ghế không khả dụng | Thông báo rõ cho client request không thể giữ ghế |

Unique constraint trên `showtime_id, seat_id` và index theo `showtime_id, status` đã được khai báo trong entity `ShowtimeSeat`. `Booking` có unique foreign key trên `seat_hold_id`. Với database đang tồn tại, Hibernate `ddl-auto=update` có thể bổ sung constraint/index thiếu tùy tình trạng schema; không xóa dữ liệu cũ.

## 7. Confirm, cancel và tự giải phóng ghế

### Confirm

`POST /api/holds/{seatHoldId}/confirm` lấy danh tính từ JWT, xác minh chủ sở hữu, khóa các row ghế và hold, kiểm tra trạng thái/thời hạn, rồi trong một transaction:

1. Tạo booking.
2. Đổi hold sang `CONFIRMED`.
3. Đổi ghế từ `HELD` sang `SOLD`.

Nếu cùng hold được confirm lần nữa, service đọc lại booking hiện có thay vì tạo bản ghi mới.

### Cancel và hết hạn

- Cancel chỉ giải phóng những ghế còn ở trạng thái `HELD` và vẫn tham chiếu đúng hold hiện tại.
- Hold hết hạn sau 10 phút. `SeatHoldCleanupJob` chạy định kỳ mỗi 30 giây, tìm hold hết hạn theo batch rồi giải phóng ghế có kiểm tra quyền sở hữu tương tự.

## 8. Demo request đồng thời

Trong màn **Sơ đồ & giữ ghế**, đăng nhập User 1, chọn đúng một ghế trống, nhập thông tin User 2 rồi bấm **Đăng nhập User 2 và gửi đồng thời**. Frontend lấy JWT riêng của User 2, sau đó gửi hai request `POST /api/holds` song song: cùng suất diễn/ghế nhưng mỗi request có JWT và user ID khác nhau. Kết quả dự kiến là một hold thành công và một `409 Conflict`. `401`/`403` là lỗi xác thực/phân quyền, không phải kết quả của row lock.

Đây là minh họa tranh chấp; không phải bài benchmark tải. Để đo throughput/độ trễ dưới nhiều request, cần công cụ tải và một kịch bản đo riêng.

## 9. Đăng nhập và phân quyền

- `POST /api/auth/register`: tạo user mới. Server luôn gán role `USER`, không nhận role từ body; mật khẩu được mã hóa bằng BCrypt.
- `POST /api/auth/login`: kiểm tra email/mật khẩu rồi cấp JWT access token có thời hạn 30 phút.
- `GET /api/auth/me`: trả hồ sơ user của JWT hiện tại.
- Các API đọc event, suất diễn, sơ đồ ghế được công khai. API tạo/sửa/xóa event, suất diễn, layout chỉ dành cho `ADMIN`.
- Các API `/api/holds/**` cần đăng nhập. Service kiểm tra hold thuộc đúng User ID trong JWT trước khi tra cứu, xác nhận hoặc hủy.
- Để cấp ADMIN cho demo, đăng ký `admin@local.test`, chạy `src/main/resources/db/manual/promote-local-admin.sql`, sau đó đăng xuất và đăng nhập lại để nhận token có role mới.

## 11. Phạm vi và giới hạn

- Khóa row ngăn overselling trên cùng dữ liệu MySQL; nó không làm vô hạn năng lực xử lý. Request tranh cùng ghế phải chờ nhau.
- Giao dịch nên ngắn và số row cần khóa nên tối thiểu.
- Index giảm thời gian tìm dữ liệu, nhưng lock và transaction mới bảo vệ trạng thái khỏi ghi tranh chấp.
- JWT được lưu trong local storage để đơn giản hóa demo. Secret mặc định chỉ dùng local; không dùng cấu hình này cho production.
- Đăng xuất xóa token khỏi trình duyệt; token đã phát hành không bị thu hồi phía server và còn dùng được tới lúc hết hạn (tối đa 30 phút). Dự án chưa có refresh token hoặc danh sách thu hồi.
- Redis, FIFO xếp hàng người mua, thanh toán và message queue chưa được triển khai. Quyết định giữ/bán ghế được xử lý đồng bộ trong transaction MySQL.
