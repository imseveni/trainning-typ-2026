# Kịch bản thuyết trình SmartTicket (10–15 phút)

## Mục tiêu

Trình bày cách hệ thống ngăn hai người cùng giữ thành công một ghế khi hai request đến gần như đồng thời. Cơ chế cốt lõi là transaction, pessimistic row lock và kiểm tra trạng thái ghế trong MySQL.

## Chuẩn bị

1. Chạy ứng dụng theo [Hướng dẫn chạy](HUONG_DAN_CHAY.md), mở `http://localhost:8080/`.
2. Đăng nhập `ADMIN`, chuẩn bị sự kiện, layout ghế và suất diễn.
3. Tạo hai tài khoản `USER` riêng. Đăng nhập User 1 trên giao diện.
4. Mở **Sơ đồ & giữ ghế**, chọn suất diễn và một ghế đang `AVAILABLE`.

## Kịch bản 10–15 phút

| Thời gian | Nội dung |
|---|---|
| 0–2 phút | Giới thiệu overselling: nhiều người cùng tranh một ghế |
| 2–4 phút | Giải thích `ShowtimeSeat`, `SeatHold`, `SeatHoldItem`, `Booking` và trạng thái ghế |
| 4–8 phút | Đi qua `@Transactional`, `PESSIMISTIC_WRITE`, khóa ghế theo ID tăng dần và rollback |
| 8–11 phút | Gửi hai request giữ cùng một ghế, quan sát một request thành công và request còn lại nhận `409 Conflict` |
| 11–15 phút | Xác nhận booking, hủy/để hết hạn hold, giải thích index, constraint và giới hạn demo |

## Demo hai user tranh cùng một ghế

1. Tạo hai user khác nhau trước. Đăng nhập User 1 và để phiên đó mở.
2. Chọn đúng một ghế trống.
3. Trong khung **Hai request, một ghế**, nhập email và mật khẩu User 2 rồi bấm **Đăng nhập User 2 và gửi đồng thời**.
4. Giao diện đăng nhập User 2 để lấy JWT, sau đó gửi hai `POST /api/holds` gần như đồng thời, cùng suất diễn/ghế nhưng với JWT của hai user.
5. Kết quả đúng: một request tạo hold; request kia nhận `409 Conflict` vì ghế đã được giữ. User nào thắng phụ thuộc vào request nào lấy được khóa database trước; hệ thống không cam kết FIFO.
6. Mở hold thành công để xác nhận booking. Ghế chuyển `HELD` sang `SOLD`.

Nếu thấy `401`, kiểm tra đăng nhập/JWT. Nếu thấy `403`, kiểm tra quyền và backend đang chạy. Đây không phải kết quả tranh chấp ghế; tranh chấp đúng phải trả `409`.

## Luồng backend cần chỉ ra

1. `SeatHoldController` lấy `userId` từ JWT, không tin `userId` do client tự gửi.
2. `SeatHoldService.createSeatHold()` mở transaction, xác thực suất diễn/ghế và sắp xếp ID ghế.
3. `ShowtimeSeatRepository.findByShowtimeSeatIdForUpdate()` lấy khóa ghi trên từng dòng ghế. Request còn lại phải đợi transaction giữ khóa hoàn tất.
4. Request lấy khóa trước kiểm tra ghế còn trống, tạo `SeatHold`/`SeatHoldItem` và cập nhật ghế thành `HELD` trong cùng transaction.
5. Request lấy khóa sau đọc trạng thái hiện tại. Nếu ghế đã được giữ và hold còn hiệu lực, service trả `409`; không tạo hold thứ hai.
6. Nếu xác nhận hold, `confirmSeatHold()` khóa lại ghế và hold, kiểm tra chủ sở hữu/trạng thái/thời hạn, tạo `Booking`, đổi ghế thành `SOLD` trong transaction.

## Ý nghĩa các cơ chế

- **Transaction:** mọi ghế trong một yêu cầu được giữ toàn bộ hoặc không ghế nào được giữ.
- **Pessimistic row lock:** tuần tự hóa các request tranh cùng ghế; ghế khác nhau vẫn có thể xử lý đồng thời.
- **Khóa theo ID tăng dần:** giảm nguy cơ deadlock khi một request giữ nhiều ghế.
- **Unique constraint và index:** bảo vệ tính duy nhất của inventory và hỗ trợ truy vấn; chúng bổ sung cho lock, không thay thế lock.
- **JWT và phân quyền:** xác định user thực hiện thao tác và giới hạn quyền quản trị.