# Tuần 2: Framework, RESTful API và ORM

---

## Phần 1: Framework & Xây dựng RESTful API

### 1. Lựa chọn Framework (chọn 1)
- Java: Spring Boot
- NodeJS: Express.js hoặc NestJS
- Python: Django

### 2. Tạo Project & Cấu trúc
- Khởi tạo project:
  - Spring Boot → Spring Initializr
  - NodeJS → `npm init`
  - Django → `django-admin startproject`
- Tìm hiểu cấu trúc thư mục: controllers, models, routes, services, migrations
- Tìm hiểu file cấu hình: `application.properties` / `.env` / `settings.py`

### 3. Xây dựng RESTful API
- HTTP Methods: GET, POST, PUT/PATCH, DELETE
- Mapping URL đến hàm xử lý
- Xử lý Request:
  - Path Variable: `/users/{id}`
  - Query Param: `/users?age=20`
  - Request Body: JSON payload
- Xử lý Response:
  - Trả về JSON
  - HTTP Status Code phù hợp (200, 201, 400, 404, 500)
- Error Handling:
  - Cấu trúc error response thống nhất (ví dụ: `{code, message, details}`)
  - Phân biệt client error (4xx) vs server error (5xx)

### 4. Pagination trong API
- Áp dụng kiến thức phân trang tuần 1 vào API
- Response format: `{data, page, size, totalPages, totalElements}`
- Implement `GET /items?page=1&size=10`

### 5. Thực hành
- Xây dựng API CRUD cho 1 đối tượng (Student, Book, Product)
- `GET /items` (có phân trang), `GET /items/{id}`, `POST`, `PUT`, `DELETE`
- Dữ liệu lưu tạm trong List/Array (chưa cần DB)
- Test bằng Postman hoặc cURL

---

## Phần 2: Tích hợp Database (ORM)

### 1. ORM là gì? Lợi ích?

### 2. Sử dụng ORM theo Framework
- Java: JPA + Hibernate, Spring Data JPA, Entity mapping, Repository
- NodeJS: Prisma / TypeORM / Sequelize
- Python: Django ORM

### 3. Thực hành
- Thay thế CRUD in-memory bằng ORM kết nối DB thật
- Pagination query từ DB thật (LIMIT/OFFSET hoặc cursor)
- Relationship giữa các entity (OneToMany, ManyToOne)

---

## Output

- Trình bày lý thuyết
- Project backend chạy được, kết nối DB thật
- Tối thiểu 5 API CRUD hoạt động, có phân trang, error response chuẩn
- Test bằng Postman
