# Hướng dẫn chạy SmartTicket

## Yêu cầu

- JDK 17 trở lên.
- Docker Desktop và Docker Compose.
- PowerShell trên Windows hoặc Bash.

## 1. Khởi động MySQL

Từ thư mục dự án:

```powershell
cd D:\CODE\TYP-WEB\week-6\smartticket
docker compose up -d mysql
docker compose ps
```

Ứng dụng kết nối MySQL tại `localhost:3307`; database `smartticket_db`, tài khoản local `root/123456`. Chờ container MySQL chạy ổn định trước khi khởi động backend.

## 2. Khởi động backend và giao diện

```powershell
.\mvnw.cmd spring-boot:run
```

Trên macOS/Linux:

```bash
./mvnw spring-boot:run
```

Mở `http://localhost:8080/`. REST API và giao diện demo dùng cùng origin. Project hiện chỉ cần MySQL; luồng booking không phụ thuộc RabbitMQ.

## 3. Đăng ký và phân quyền

1. Mở giao diện, bấm **Đăng nhập / Đăng ký** rồi tạo tài khoản. Tài khoản đăng ký mới luôn có quyền `USER`; mật khẩu được lưu dưới dạng BCrypt hash.
2. Để quản lý sự kiện, suất diễn và layout, đăng ký tài khoản `admin@local.test`.
3. Chạy `src/main/resources/db/manual/promote-local-admin.sql` bằng MySQL Workbench để nâng tài khoản đó lên `ADMIN`.
4. Đăng xuất rồi đăng nhập lại để nhận JWT chứa role mới.

JWT access token có hạn 30 phút và được giao diện lưu trong local storage để gửi kèm các API cần xác thực. Khóa ký mặc định trong properties chỉ dành cho demo local; khi chạy môi trường khác, đặt biến `JWT_SECRET` là chuỗi Base64 biểu diễn tối thiểu 32 byte ngẫu nhiên.

Các API đọc sự kiện, suất diễn và sơ đồ ghế được mở công khai. API giữ/tra cứu/xác nhận/hủy hold yêu cầu đăng nhập và chỉ chủ hold được thao tác. API tạo/sửa/xóa sự kiện, suất diễn và layout yêu cầu `ADMIN`.

## 4. Dừng ứng dụng

Dừng Spring Boot bằng `Ctrl+C`, sau đó dừng MySQL:

```powershell
docker compose down
```

Lệnh này giữ volume MySQL. Không thêm `-v` nếu cần giữ dữ liệu.
