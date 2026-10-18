# Tuần 3: Authentication & Authorization

---

## 1. Khái niệm

### 1.1. Authentication (Xác thực) là gì?

**Authentication** là quá trình xác minh danh tính của người dùng.

**Nói đơn giản:** *Authentication trả lời câu hỏi: “Bạn là ai?”*

Ví dụ khi đăng nhập vào một hệ thống:

```text
Username: chien
Password: ********
```

Hệ thống kiểm tra **username/password** có đúng hay không.

- Đúng → người dùng được xác thực thành công.
- Sai → không được đăng nhập.

Ngoài **username/password, Authentication** còn có thể dùng:

- OTP
- Vân tay
- Face ID
- Google/Facebook login
- JWT Token
- Access Token

Ví dụ trong ứng dụng **Spring Boot**: `POST /login`

Người dùng gửi:

```json
{
    "username": "chien",
    "password": "123456"
}
```

Server kiểm tra thông tin và có thể trả về JWT:

```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Token** này được dùng để chứng minh rằng người dùng đã đăng nhập.

### 1.2. Authorization (Phân quyền) là gì?

**Authorization** là quá trình kiểm tra xem người dùng đã xác thực được phép làm những gì.

**Nói đơn giản:** *Authorization trả lời câu hỏi: “Bạn được phép làm gì?”*

**Ví dụ hệ thống có hai quyền:**

- `USER`
- `ADMIN`

**Người dùng có quyền `USER` có thể:**

- GET /students
- GET /students/{id}

**Nhưng chỉ `ADMIN` mới được:**

- POST /students
- PUT /students/{id}
- DELETE /students/{id}

**Nếu một `USER` gửi `DELETE /students/1`**, thì dù người đó đã đăng nhập thành công, server vẫn từ chối vì không có quyền.

**Thông thường server sẽ trả về:** `403 Forbidden`

### 1.3. Phân biệt 2 khái niệm này

| Tiêu chí | Authentication | Authorization |
| :--: | :--: | :--: |
| Định nghĩa | Xác thực | Phân quyền |
| Câu hỏi | Bạn là ai | Bạn được phép làm gì |
| Mục đích | Xác minh danh tính | Kiểm tra quyền truy cập |
| Thời điểm | Thực hiện trước Authentication | Thực hiện sau Authentication |
| Thất bại thường gặp | `401 Unauthorized` | `403 Forbidden` |

## 2. Phân biệt cơ chế xác thực

### 2.1. Session-based: flow hoạt động, cookie, server lưu session

**Session-based Authentication** là cơ chế mà sau khi người dùng đăng nhập thành công, server tạo và lưu thông tin phiên đăng nhập (session).

Server giữ trạng thái đăng nhập của người dùng.

#### 2.1.1. Flow hoạt động

**Ví dụ người dùng đăng nhập:**

```text
Client
  |
  | POST /login
  | username + password
  v
Server
```

**Server kiểm tra tài khoản. Nếu đúng:**

```text
Server tạo Session
sessionId = abc123
```

**Server lưu:**

```text
abc123 -> userId = 10
          role = ADMIN
```

**Sau đó server trả về cho client:** `Set-Cookie: JSESSIONID=abc123`

**Trình duyệt lưu cookie. Các request tiếp theo:**

```text
GET /students

Cookie: JSESSIONID=abc123
```

**Server lấy `abc123`, tìm trong nơi lưu session và biết:**

```text
abc123
  ↓
userId = 10
role = ADMIN
```

**Luồng tổng quát:**

```text
1. Client gửi username/password
              ↓
2. Server xác thực
              ↓
3. Server tạo Session
              ↓
4. Server lưu Session
              ↓
5. Server gửi Session ID qua Cookie
              ↓
6. Browser lưu Cookie
              ↓
7. Request sau tự gửi Cookie
              ↓
8. Server tra Session ID
              ↓
9. Xác định người dùng
```

#### 2.1.2. Cookie

**Cookie** thường không chứa toàn bộ thông tin người dùng mà chỉ chứa một ID đại diện cho session:

```text
Cookie
JSESSIONID=abc123
```

**Thông tin thật nằm ở server:**

```text
Server

abc123:
{
    userId: 10,
    username: "chien",
    role: "ADMIN"
}
```

**Do đó:** Client giữ Session ID, server giữ Session Data.

Trong Spring Boot/Spring Security thường có thể bắt gặp `JSESSIONID` được gửi trong cookie.

### 2.2. Token-based: flow hoạt động, stateless, client giữ token

Với **Token-based Authentication**, sau khi đăng nhập thành công, server tạo một token và gửi cho client.

Client sẽ giữ token và gửi token theo mỗi request.

**Một dạng phổ biến là:** `JWT - JSON Web Token`.

#### 2.2.1. Flow hoạt động

**Client đăng nhập: `POST /login`.**

```json
{
    "username": "chien",
    "password": "123456"
}
```

**Server xác thực thành công và tạo token:** `eyJhbGciOiJIUzI1NiJ9...`

Sau đó trả token cho client. Client lưu token.

**Ví dụ request tiếp theo:**

```http
GET /students
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Server đọc token:**

```text
Token
  ↓
Kiểm tra chữ ký
  ↓
Kiểm tra thời hạn
  ↓
Lấy thông tin user
  ↓
Cho phép request
```

#### 2.2.2. Stateless

Đây là điểm quan trọng của **Token-based Authentication**.

Trong **Session-based**, Server phải nhớ `sessionId -> user`

**Ví dụ:**

```text
abc123 -> user 1
xyz456 -> user 2
def789 -> user 3
```

Có nghĩa server đang giữ state.

Trong **Token-based**, với **JWT** token có thể chứa các thông tin cần thiết:

```json
{
    "sub": "chien",
    "role": "ADMIN",
    "exp": 1790000000
}
```

Server không nhất thiết phải lưu: `token -> user`

Mỗi request chỉ cần:

```text
Token
 ↓
Verify
 ↓
Đọc thông tin
```

Do đó gọi **Stateless** là việc server không cần lưu trạng thái đăng nhập của từng client.

**Lưu ý:** Stateless không có nghĩa là server không có database. Server vẫn có thể truy vấn database khi cần. Nó chỉ có nghĩa là server không cần duy trì session đăng nhập cho từng client.

### 2.3. So sánh: khi nào dùng session, khi nào dùng token

| Tiêu chí | Session-based | Token-based |
| :--: | :--: | :--: |
| Thông tin đăng nhập | Server lưu | Client giữ token |
| Client thường giữ | Session ID | Token |
| Thường gửi qua | Cookie | `Authorization header` |
| Server có lưu trạng thái? | Có | Thường không |
| Tiêu chí quả lý trạng thái | Stateful | Stateless |
| Scale nhiều server | Phức tạp | Dễ |
| Logout | Dễ | Phức tạp |

#### 2.3.1. Khi nào nên dùng Session?

**Session** phù hợp với các ứng dụng web truyền thống, đặc biệt khi:

- Frontend và Backend nằm cùng một hệ thống.
- Ứng dụng chạy chủ yếu trên browser.
- Muốn logout hoặc vô hiệu hóa tài khoản ngay lập tức.
- Không cần phục vụ nhiều loại client.
- Không có kiến trúc microservices quá phức tạp.

#### 2.3.2. Khi nào nên dùng Token?

**Token** phù hợp khi xây dựng:

- REST API.
- SPA như React, Vue, Angular.
- Mobile application.
- Backend phục vụ nhiều client.
- Microservices.
- Hệ thống phân tán.
- API cho bên thứ ba.

## 3. JWT (JSON Web Token)

### 3.1. JWT là gì?

**JWT (JSON Web Token)** là một chuẩn dùng để truyền thông tin giữa các bên dưới dạng một chuỗi token nhỏ gọn và có thể xác minh được.

**JWT** thường được dùng trong **Authentication** và **Authorization**.

**Ví dụ sau khi đăng nhập thành công:**

```text
POST /login
username + password
        ↓
Server xác thực
        ↓
Tạo JWT
        ↓
Trả JWT cho Client
```

**Sau đó client gửi token ở các request tiếp theo:** `Authorization: Bearer <JWT>`

**Server kiểm tra token để biết:**

- Người dùng là ai?
- Có role gì?
- Token còn hạn không?

**Một JWT thường có dạng:** `xxxxx.yyyyy.zzzzz`

**Gồm 3 phần:** `Header.Payload.Signature`

### 3.2. Cấu trúc JWT

#### 3.2.1. Header

**Header** chứa thông tin về token, đặc biệt là thuật toán được dùng để ký.

**Ví dụ:**

```json
{
    "alg": "HS256",
    "typ": "JWT"
}
```

**Trong đó:**

- alg = thuật toán ký
- typ = loại token

Ví dụ HS256 nghĩa là sử dụng: `HMAC + SHA-256`.

**Header** sau đó được Base64URL encode.

#### 3.2.2. Payload

**Payload** chứa các thông tin gọi là **claims**.

**Ví dụ:**

```json
{
    "sub": "chien",
    "role": "ADMIN",
    "iat": 1792160000,
    "exp": 1792163600
}
```

**Ý nghĩa:**

```text
sub = Subject: người dùng mà token đại diện

role = quyền của người dùng

iat = Issued At: thời điểm token được tạo

exp = Expiration Time: thời điểm token hết hạn
```

**Một số claim phổ biến:**

| Claim | Ý nghĩa |
|---|---|
| `sub` | Subject - chủ thể của token |
| `iss` | Issuer - ai phát hành token |
| `aud` | Audience - token dành cho ai |
| `iat` | thời gian token được tạo |
| `exp` | thời gian token hết hạn |
| `nbf` | token chỉ hợp lệ sau thời điểm này |

**Điểm rất quan trọng:** Payload của JWT thường không được mã hóa.

**Nó chỉ được:** Base64URL Encode.

Vì vậy bất kỳ ai lấy được **JWT** đều có thể decode **Header** và **Payload**.

Do đó không nên lưu thông tin nhạy cảm trong **JWT payload** như:

- password
- OTP
- secret key
- số thẻ ngân hàng

#### 3.2.3. Signature

**Signature dùng để kiểm tra:** Token có thực sự do server tạo ra không và nội dung có bị sửa không?

**Ví dụ đơn giản với HS256:**

```text
HMACSHA256(
    base64Url(header)
    +
    "."
    +
    base64Url(payload),
    secretKey
)
```

**Ta có:**

```text
Header + Payload
       ↓
     Secret
       ↓
   Signature
```

**JWT cuối cùng:** Header.Payload.Signature

**Nếu attacker sửa:** role từ `USER` thành `ADMIN` thì Payload thay đổi.

Signature cũ sẽ không còn khớp, Server sẽ từ chối token.

### 3.3. Lợi ích của JWT

#### 3.3.1. Stateless

Đây là lợi ích nổi bật nhất.

Server không cần lưu: `sessionId -> user`

Mỗi request đã mang token chứa thông tin cần thiết. Điều này thuận tiện khi scale hệ thống.

#### 3.3.2. JWT rất phù hợp với REST API

- Spring Boot REST API
- React
- Vue
- Angular
- Android
- iOS

Các client chỉ cần gửi: `Authorization: Bearer <token>`

#### 3.3.3. Phù hợp với hệ thống nhiều server

Các server có thể tự kiểm tra token mà không nhất thiết phải dùng chung session store.

#### 3.3.4. Nhỏ gọn và dễ truyền

JWT chỉ là một chuỗi: `xxxxx.yyyyy.zzzzz` nên dễ truyền qua HTTP header.

#### 3.3.5. Chứa được thông tin phân quyền

**Ví dụ:**

```json
{
    "sub": "chien",
    "role": "ADMIN"
}
```

Server có thể dùng claim này để kiểm tra authorization.

### 3.3. Hạn chế và rủi ro bảo mật

**JWT** không phải lúc nào cũng tốt hơn **Session**.

#### 3.3.1. Khó thu hồi token

Giả sử token có thời hạn **1 giờ**, Người dùng logout sau **5 phút**.

Nếu hệ thống hoàn toàn **stateless** thì **JWT** đó về lý thuyết vẫn hợp lệ trong thời gian còn lại.

Nếu muốn vô hiệu hóa ngay, hệ thống có thể cần:

- Token blacklist
- Redis
- Token version

Nhưng khi đó hệ thống không còn hoàn toàn **stateless** nữa.

#### 3.3.2. Token có thể bị đánh cắp

Nếu attacker lấy được token `eyJhbGciOiJIUzI1NiJ9...`

Họ có thể gửi `Authorization: Bearer <stolen-token>`

Và giả mạo người dùng cho tới khi token hết hạn hoặc bị thu hồi.

Đây là lý do token cần được bảo vệ cẩn thận.

#### 3.3.3. JWT thường lớn hơn Session ID

**Session cookie** có thể chỉ là: `JSESSIONID=abc123`

Trong khi **JWT** có thể dài vài trăm ký tự: `eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOi...`

**JWT** được gửi lại với nhiều request nên có thêm overhead.

#### 3.3.4. Các rủi ro bảo mật quan trọng

- Không nên lưu dữ liệu nhạy cảm trong Payload
- Không nên sử dụng secret key yếu
- Không nên tạo Access Token quá lâu
- Không nên lưu token ở nơi không an toàn

## 4. Password Hashing

### 4.1. Tại sao không lưu password dạng plain text?

**Plain text** nghĩa là lưu mật khẩu đúng nguyên bản mà người dùng nhập.

**Ví dụ user đăng ký:**

```text
username: chien
password: 123456
```

**Nếu lưu trực tiếp trong database:** thì đây là cách làm rất nguy hiểm.

| id | username | password |
| :--: | :--: | :--: |
| 1 | chien | 123456 |

**Nếu database bị lộ**, attacker sẽ thấy ngay toàn bộ mật khẩu:

```text
chien -> 123456
admin -> admin123
user1 -> qwerty
```

Vấn đề còn nghiêm trọng hơn vì nhiều người thường sử dụng lại một mật khẩu cho nhiều dịch vụ khác nhau.

**Do đó nguyên tắc là:** Không bao giờ lưu password dưới dạng plain text.

**Thay vào đó:**

```text
Password
   ↓
Hash function
   ↓
Password Hash
```

**Ví dụ**

```text
123456
   ↓
bcrypt
   ↓
$2a$10$N9qo8uLOickgx2ZMRZoMye...
```

**Database chỉ lưu hash:**

| id | username | password_hash |
| :--: | :--: | :--: |
| 1 | chien | $2a$10$... |

### 4.2. Password Hashing hoạt động như thế nào?

**Khi đăng ký:**

```text
User nhập password
        ↓
Hash password
        ↓
Lưu hash vào database
```

**Khi đăng nhập:**

```text
User nhập password
        ↓
Hash/check password
        ↓
So sánh với hash trong database
        ↓
Đúng → đăng nhập
Sai → từ chối
```

Server không cần biết lại mật khẩu gốc.

### 4.3. Tại sao không dùng MD5/SHA? (brute force, rainbow table)

**Có một điểm cần nói chính xác:** MD5 và SHA không hoàn toàn “xấu”, nhưng không phù hợp để hash password trực tiếp.

**Ví dụ:** password = "123456"

- MD5: e10adc3949ba59abbe56e057f20f883e
- SHA-256: 8d969eef6ecad3c29a3a629280e686cff8ca...

**Vấn đề lớn** là các thuật toán này được thiết kế để chạy rất nhanh. Nhưng đối với password, chạy nhanh lại là một nhược điểm.

#### 4.3.1. Vấn đề 1: Brute Force

**Brute force** nghĩa là attacker thử rất nhiều password:

**Mỗi lần:**

```text
candidate password
      ↓
SHA-256
      ↓
So sánh hash
```

Vì **SHA-256** rất nhanh nên attacker có thể thử một lượng password rất lớn trong thời gian ngắn, đặc biệt khi sử dụng GPU hoặc phần cứng chuyên dụng.

**Đối với password hashing, ta lại muốn:** Mỗi lần thử password phải tốn một lượng tài nguyên đáng kể.

#### 4.3.2. Vấn đề 2: Rainbow Table

**Giả sử** password "123456" luôn tạo cùng một MD5: "e10adc3949ba59abbe56e057f20f883e"

**Attacker có thể chuẩn bị trước một bảng:**

| Password | Hash |
| :--: | :--: |
| 123456 | e10adc3949... |
| password | 5f4dcc3b5... |
| qwerty | d8578edf8... |
| admin | 21232f297... |

Đây là ý tưởng của các bảng hash được tính trước như **rainbow table**.

**Khi database bị lộ:** e10adc3949..., attacker chỉ việc tìm: e10adc3949... → 123456 thay vì phải tính từ đầu.

#### 4.3.3. Dùng bcrypt/argon2: salt, cost factor

##### 4.3.3.1. Salt là gì?

**Salt** là một giá trị ngẫu nhiên được thêm vào quá trình hash password.

**Ví dụ hai người đều dùng:** password = "123456"

**Nếu không có salt:**

```text
User A → HASH_ABC
User B → HASH_ABC
```

Vì hash giống nhau. Attacker có thể dễ dàng nhận thấy: Hai tài khoản có cùng password.

**Nếu có salt:**

```text
User A
password = 123456
salt = X8A2
        ↓
      Hash A

User B
password = 123456
salt = K91P
        ↓
      Hash B
```

**Kết quả:** Hash A != Hash B mặc dù password giống nhau.

##### 4.3.3.2. Salt có cần giữ bí mật không?

**Salt** không cần giữ bí mật. 

**Salt** có thể được lưu cùng password hash.

**Ví dụ bcrypt hash:** `$2b$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`

**Trong chuỗi này đã chứa các thông tin cần thiết như:**

- algorithm
- cost
- salt
- hash

**Điều quan trọng là:** Salt phải ngẫu nhiên và khác nhau cho mỗi password.

##### 4.3.3.3. Salt chống Rainbow Table như thế nào?

**Không salt:** password → hash cố định

**Attacker có thể chuẩn bị:** password → hash từ trước.

**Có salt:**

```text
password + salt1 → hash1
password + salt2 → hash2
password + salt3 → hash3
```

Attacker không thể dùng một bảng hash duy nhất cho tất cả tài khoản.

Họ phải brute-force riêng theo từng salt.

##### 4.3.3.4. Tại sao dùng bcrypt hoặc Argon2?

**Các thuật toán như:**

- bcrypt
- Argon2
- scrypt
- PBKDF2

được thiết kế dành riêng cho password hashing.

**Khác với:**

- MD5
- SHA-1
- SHA-256

**bcrypt/argon2** cố tình được thiết kế để: **chậm hơn + có salt + điều chỉnh độ khó**.

##### 4.3.3.5. bcrypt

**bcrypt** là một thuật toán password hashing rất phổ biến.

**Ví dụ:** `password = "123456"`. **bcrypt** có thể tạo: `$2a$10$WrgDdrYQ...`

##### 4.3.3.6. Cost Factor

**Cost factor** kiểm soát mức độ tốn tài nguyên để hash password.

**Ví dụ bcrypt:** `$2a$10$WrgDdrYQ...` có cost = 10.

Số vòng xử lý về cơ bản tăng theo: 2^10

**Nếu:** cost = 12 thì công việc tăng lên khoảng: 2^12

**Do đó:**

```text
cost tăng
   ↓
hash chậm hơn
   ↓
brute-force khó hơn
```

Nhưng không thể tăng vô hạn.

**Ví dụ:**

- Hash mất 50ms có thể chấp nhận được
- Nhưng hash mất 10s sẽ làm người dùng đăng nhập rất chậm và làm server quá tải.

##### 4.3.3.7. Vì sao Cost Factor quan trọng?

Máy tính ngày càng mạnh. Một mức cost phù hợp hôm nay có thể trở nên quá nhanh sau vài năm.

**bcrypt** cho phép tăng: cost = 10 thành cost = 12 mà không cần thay đổi hoàn toàn thuật toán.

##### 4.3.3.8. Argon2

**Argon2** là một password hashing algorithm hiện đại.

**Các biến thể chính:**

- Argon2d
- Argon2i
- Argon2id

**Trong thực tế, thường gặp:** Argon2id

**Argon2** không chỉ điều chỉnh CPU cost mà còn có thể điều chỉnh:

- Memory cost
- Time cost
- Parallelism

**Điểm mạnh của Argon2:** là có thể buộc việc thử password phải tiêu tốn nhiều RAM, khiến brute-force bằng GPU/ASIC trở nên khó và đắt hơn.

## 5. Thực hành — Phát triển từ project tuần 2

### 5.1. Đăng ký USER

![Đăng ký USER](screenshots/register.png)

### 5.2. Demo password hashing

![Demo password hashing](screenshots/password_hasing.png)

### 5.3. Đăng nhập

![Đăng nhập](screenshots/login_dung.png)

### 5.4. Demo login sai

![Đăng nhập sai](screenshots/login_sai.png)

### 5.5. Demo API protected khi không có JWT

![Demo API protected khi không có JWT](screenshots/api_protected.png)

### 5.6. Demo USER gọi API protected thành công

![Demo USER gọi API protected thành công](screenshots/goi_api_protected_success.png)

### 5.7. Demo USER không có quyền ADMIN

![Demo USER không có quyền ADMIN](screenshots/user_khong_co_quyen_admin.png)

### 5.8. Tạo ADMIN để demo

![Tạo ADMIN để demo](screenshots/tao_admin.png)

### 5.9. Tạo role ADMIN

![Tạo role ADMIN](screenshots/tao_role_admin.png)

### 5.10. Demo ADMIN gọi POST thành công

![Demo ADMIN gọi POST thành công](screenshots/admin_goi_post_success.png)

### 5.11. Demo Refresh Token

![Demo Refresh Token](screenshots/demo_refreshtoken.png)

### 5.12. Demo Refresh Token Rotation

![Demo Refresh Token Rotation](screenshots/demo_refreshtoken_rotation.png)

### 5.13. Dùng Access Token mới gọi API

![Dùng Access Token mới gọi API](screenshots/accesstoken_moi_hoat_dong.png)