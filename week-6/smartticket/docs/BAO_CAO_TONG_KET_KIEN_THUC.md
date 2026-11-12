# BÁO CÁO TỔNG KẾT KIẾN THỨC VÀ DỰ ÁN SMARTTICKET

## 1. Dự án giải quyết bài toán gì?

SmartTicket là ứng dụng quản lý sự kiện, suất diễn và sơ đồ ghế. Người dùng chọn ghế, giữ ghế tối đa 10 phút, rồi xác nhận để tạo booking hoặc hủy lượt giữ. Quản trị viên tạo và quản lý sự kiện, suất diễn.

Bài toán kỹ thuật trọng tâm là **tranh chấp ghế**: nhiều người có thể cùng gửi yêu cầu giữ một ghế. Hệ thống phải ngăn giữ/bán trùng, đồng thời trả ghế về kho khi lượt giữ bị hủy hoặc hết hạn.

## 2. Kiến thức đã học và ứng dụng

| Kiến thức | Ứng dụng trong dự án |
|---|---|
| CSDL quan hệ, SQL, khóa và ràng buộc | Các bảng User, Event, Showtime, Seat, ShowtimeSeat, SeatHold, SeatHoldItem, Booking; khóa ngoại và unique constraint bảo vệ quan hệ và dữ liệu không trùng. |
| OOP, Spring Boot, DI/IoC | Tách Controller nhận HTTP, Service xử lý nghiệp vụ, Repository truy vấn dữ liệu; Spring quản lý và inject các thành phần. |
| REST, DTO, validation, JPA | API JSON cho CRUD và đặt vé; DTO tách dữ liệu API khỏi entity; Hibernate ánh xạ entity Java với MySQL; validation chặn input sai. |
| Transaction và xử lý đồng thời | `@Transactional` giữ các cập nhật cùng thành công hoặc rollback; `PESSIMISTIC_WRITE` khóa dòng ghế đang tranh chấp. `Event` dùng `@Version` cho cập nhật sự kiện đồng thời. |
| Xác thực và phân quyền | BCrypt băm mật khẩu; JWT xác thực request; role `USER`/`ADMIN` giới hạn API; backend lấy user ID từ token và kiểm tra quyền sở hữu hold. |
| Message Queue, Redis/cache | Đã tìm hiểu để biết hướng mở rộng. Hiện không dùng RabbitMQ/Outbox hoặc Redis trong luồng chính; ưu tiên giải quyết tính đúng đắn khi giữ ghế. |

## 3. Kiến trúc ứng dụng

```text
Frontend app.js → Security/JWT → Controller → Service/Transaction
               → Repository/JPA → MySQL
```

Frontend gửi request, không tự quyết định ghế được giữ. `SeatHoldController` lấy user ID từ JWT; `SeatHoldService` kiểm tra nghiệp vụ và quản lý transaction; `ShowtimeSeatRepository` truy vấn/khóa dòng ghế; MySQL lưu trạng thái cuối cùng. `SeatHoldCleanupJob` định kỳ gọi service để giải phóng hold hết hạn.

<!-- PAGEBREAK -->

## 4. Giải pháp cho tranh chấp ghế

### Luồng giữ ghế

Khi nhận `POST /api/holds`, backend:

1. Lấy user từ JWT, kiểm tra user và suất diễn.
2. Sắp xếp ID ghế tăng dần, từ chối ID trùng.
3. Khóa từng dòng `ShowtimeSeat` bằng `PESSIMISTIC_WRITE`.
4. Sau khi có khóa, kiểm tra lại trạng thái và suất diễn của từng ghế.
5. Nếu tất cả ghế còn hợp lệ, tạo `SeatHold`, các `SeatHoldItem`, đổi ghế thành `HELD` và gắn hold hiện hành.
6. Commit tất cả trong cùng transaction; nếu một ghế không dùng được, rollback toàn bộ.

Request thứ hai tranh cùng ghế phải chờ khóa. Khi được cấp khóa, request đọc trạng thái mới: nếu ghế đã `HELD` thì không tạo hold và trả `409 Conflict`. Database cấp khóa trước cho request nào theo lịch thực thi của nó; ứng dụng không bảo đảm FIFO chỉ dựa trên thời điểm người dùng bấm.

Khóa ghế theo ID tăng dần giúp giảm nguy cơ deadlock khi nhiều request giữ nhiều ghế. Đọc hold bằng locking read cũng giúp tránh dùng snapshot cũ trong một số tình huống MySQL `REPEATABLE READ`.

### Các luồng liên quan

- **Xác nhận:** khóa ghế và hold, kiểm tra hold còn hiệu lực và ghế vẫn thuộc hold; tạo `Booking`, chuyển hold thành `CONFIRMED`, ghế thành `SOLD` trong một transaction. Gọi lại xác nhận trả booking đã có.
- **Hủy:** kiểm tra chủ sở hữu, khóa hold/ghế, chuyển trạng thái và chỉ giải phóng ghế còn thuộc hold đó.
- **Hết hạn:** scheduler chạy mỗi 30 giây, lấy tối đa 100 hold hết hạn; service khóa và kiểm tra lại trước khi chuyển thành `EXPIRED` và trả ghế.

### Vai trò các bảng ghế

`ShowtimeSeat` là inventory của một ghế trong một suất diễn, có trạng thái `AVAILABLE`, `HELD`, `SOLD`; đây là dòng được khóa. `SeatHold` là lượt giữ của một người dùng. `SeatHoldItem` lưu danh sách ghế thuộc hold để tìm lại khi xác nhận, hủy hoặc hết hạn. Cột `activeHold` trên `ShowtimeSeat` trỏ tới hold đang giữ ghế và được xóa khi giải phóng hoặc bán.

<!-- PAGEBREAK -->

## 5. Những phần khó và giá trị của project

1. **Tránh giữ trùng:** row lock buộc các transaction tranh cùng ghế lần lượt kiểm tra và cập nhật; chỉ request đầu tiên có thể giữ ghế còn trống.
2. **Giữ nhiều ghế nguyên tử:** transaction bảo đảm giữ toàn bộ hoặc không giữ ghế nào.
3. **Tránh deadlock phổ biến:** khóa ghế theo thứ tự ID nhất quán.
4. **Quản lý vòng đời hold:** xác nhận, hủy, hết hạn đều kiểm tra trạng thái và quyền sở hữu trước khi đổi dữ liệu.
5. **Bảo vệ API:** phân biệt xác thực JWT, role quản trị và quyền thao tác trên hold của từng người.

Demo hai tài khoản gửi request cùng giữ một ghế minh họa kết quả mong đợi: một request thành công, request kia nhận `409`. `403` là vấn đề quyền truy cập; `500` là lỗi ngoài dự kiến cần tra log. Hai request chỉ chứng minh tính đúng đắn ở tình huống tranh chấp, không phải kiểm thử tải.

## 6. Giới hạn hiện tại

- Chưa đo throughput, độ trễ, thời gian chờ khóa hoặc tải tối đa, nên chưa thể kết luận sức chịu tải thực tế.
- Request tranh cùng ghế phải chờ; row lock bảo vệ dữ liệu nhưng không bảo đảm FIFO và không làm năng lực xử lý tăng vô hạn.
- Chưa dùng Redis, RabbitMQ, Outbox hay hàng đợi người mua trong phiên bản hiện tại.
- JWT có thời hạn nhưng logout phía trình duyệt chưa thu hồi token ở backend; schema hiện dùng `ddl-auto=update`.

## 7. Hướng phát triển

- Viết integration test cho hai transaction cùng giữ ghế; kiểm thử thêm hủy, xác nhận, hết hạn, nhiều ghế, deadlock và request lặp.
- Dùng k6/JMeter đo p95/p99 latency, throughput, connection pool, lock wait và tỷ lệ `409`; tối ưu dựa trên kết quả.
- Dùng Flyway/Liquibase để quản lý schema; đưa mật khẩu và JWT secret vào biến môi trường/secret store; bổ sung metric cho lock và hold hết hạn.
- Chỉ thêm Redis nếu dữ liệu đọc như sự kiện/suất diễn gây tải đáng kể; không dùng cache làm nguồn quyết định ghế còn trống.
- Chỉ thêm Outbox/RabbitMQ nếu cần công việc nền sau booking như gửi email; queue xử lý tác vụ phụ, không thay thế transaction và khóa ghế.

## Kết luận

SmartTicket áp dụng kiến thức CSDL, Spring/JPA, REST, transaction và bảo mật để bảo vệ inventory ghế. Phần khó nhất là bảo đảm nhiều request tranh cùng một ghế không tạo dữ liệu trùng; giải pháp chính là khóa bi quan, transaction, kiểm tra trạng thái và constraint ở database. Hệ thống hiện minh họa tính đúng đắn của luồng đồng thời; cần load test có số liệu trước khi khẳng định khả năng chịu tải lớn.
