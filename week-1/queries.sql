-- DDL
CREATE TABLE BanChuyenMon(
    MaBan varchar(10) PRIMARY KEY,
    TenBan varchar(100) NOT NULL
);

CREATE TABLE ThanhVien(
    MSV varchar(20) PRIMARY KEY,
    HoVaTen varchar(100) NOT NULL,
    NgaySinh date,
    GioiTinh varchar(10),
    Email varchar(100),
    SDT varchar(10),
    NgayGiaNhap date,
    TrangThaiHD varchar(30) DEFAULT 'Hoạt động',
    ChucVu varchar(50) DEFAULT 'Thành viên',
    MaBan varchar(10),
    FOREIGN KEY (MaBan) REFERENCES BanChuyenMon(MaBan)
);

CREATE TABLE SuKien(
    MaSK varchar(10) PRIMARY KEY,
    TenSK varchar(150) NOT NULL,
    DiaDiem varchar(150),
    NgayBatDau DATE,
    NgayKetThuc DATE
);

CREATE TABLE ThamGia_SuKien(
    MSV varchar(20),
    MaSK varchar(10),
    VaiTro varchar(50),
    PRIMARY KEY (MSV, MaSK),
    FOREIGN KEY (MSV) REFERENCES ThanhVien(MSV),
    FOREIGN KEY (MaSK) REFERENCES SuKien(MaSK)
);

CREATE TABLE ThuChi(
    MaGD varchar(10) PRIMARY KEY,
    LoaiGD varchar(10) CHECK (LoaiGD IN ('Thu', 'Chi')),
    SoTien decimal(12, 2) NOT NULL,
    ThoiGian datetime DEFAULT CURRENT_TIMESTAMP,
    MucDich text,
    PhuongThucThanhToan varchar(50),
    MSV varchar(20),
    MaSK varchar(10),
    FOREIGN KEY (MSV) REFERENCES ThanhVien(MSV),
    FOREIGN KEY (MaSK) REFERENCES SuKien(MaSK)
);

ALTER TABLE SuKien
ADD COLUMN GhiChu text;

ALTER TABLE SuKien
DROP COLUMN GhiChu;

CREATE TABLE BangTam(
    ID int PRIMARY KEY,
    MoTa text
);

DROP TABLE BangTam;

-- DML

-- INSERT: Ban chuyên môn
INSERT INTO BanChuyenMon (MaBan, TenBan) VALUES
('BAN_HC', 'Ban Hậu Cần'),
('BAN_TT', 'Ban Truyền thông'),
('BAN_ND', 'Ban Nội dung'),
('BAN_HR', 'Ban Nhân sự');

-- INSERT: Thành viên
INSERT INTO ThanhVien (MSV, HoVaTen, NgaySinh, GioiTinh, Email, SDT, NgayGiaNhap, TrangThaiHD, ChucVu, MaBan) VALUES
('B21DCCN001', 'Nguyễn Văn A', '2003-05-12', 'Nam', 'a.nv@gmail.com', '0912345678', '2023-10-01', 'Hoạt động', 'Trưởng ban', 'BAN_HC'),
('B21DCCN002', 'Trần Thị B', '2003-08-20', 'Nữ', 'b.tt@gmail.com', '0923456789', '2023-10-01', 'Hoạt động', 'Phó ban', 'BAN_TT'),
('B22DCCN003', 'Lê Văn C', '2004-01-15', 'Nam', 'c.lv@gmail.com', '0934567890', '2024-03-15', 'Hoạt động', 'Thành viên', 'BAN_HC'),
('B22DCCN004', 'Phạm Thị D', '2004-11-02', 'Nữ', 'd.pt@gmail.com', '0945678901', '2024-03-15', 'Nghỉ sinh hoạt', 'Thành viên', 'BAN_ND'),
('B23DCCN005', 'Võ Văn E', '2005-09-09', 'Nam', 'e.vv@gmail.com', '0956789012', '2025-01-10', 'Hoạt động', 'Cộng tác viên', NULL); -- Chưa vào ban nào

-- INSERT: Sự kiện
INSERT INTO SuKien (MaSK, TenSK, DiaDiem, NgayBatDau, NgayKetThuc) VALUES
('SK01', 'Chào tân sinh viên 2025', 'HTA2', '2025-10-15', '2025-10-15'),
('SK02', 'Workshop AI', 'Phòng máy', '2025-11-20', '2025-11-20'),
('SK03', 'Teambuilding 2026', 'Ngoại thành', '2026-06-10', '2026-06-12');

-- INSERT: Tham gia sự kiện
INSERT INTO ThamGia_SuKien (MSV, MaSK, VaiTro) VALUES
('B21DCCN001', 'SK01', 'Trưởng ban tổ chức'),
('B21DCCN002', 'SK01', 'Phụ trách truyền thông'),
('B22DCCN003', 'SK01', 'Hỗ trợ kỹ thuật'),
('B21DCCN001', 'SK02', 'Diễn giả chính'),
('B22DCCN003', 'SK02', 'Trợ giảng');

-- INSERT: Thu/Chi
INSERT INTO ThuChi (MaGD, LoaiGD, SoTien, ThoiGian, MucDich, PhuongThucThanhToan, MSV, MaSK) VALUES
('GD01', 'Chi', 3500000.00, '2025-10-10 09:00:00', 'In backdrop và banner', 'Chuyển khoản', 'B21DCCN002', 'SK01'),
('GD02', 'Chi', 1200000.00, '2025-10-14 15:30:00', 'Mua nước và bánh teabreak', 'Tiền mặt', 'B21DCCN001', 'SK01'),
('GD03', 'Thu', 5000000.00, '2025-10-05 10:00:00', 'Nhà tài trợ A rót vốn', 'Chuyển khoản', 'B21DCCN001', 'SK01'),
('GD04', 'Chi', 800000.00, '2025-11-19 14:00:00', 'Quà tặng diễn giả', 'Chuyển khoản', 'B21DCCN001', 'SK02'),
('GD05', 'Thu', 2000000.00, '2025-11-15 08:30:00', 'Vé tham gia Workshop', 'Chuyển khoản', 'B22DCCN003', 'SK02');

SELECT * FROM ThuChi;

-- UPDATE: Cập nhật địa điểm cho sự kiện
UPDATE SuKien
SET DiaDiem = 'HTA1'
WHERE MaSK = 'SK02';

-- DELETE: Xóa dữ liệu mẫu
DELETE FROM SuKien 
WHERE NgayBatDau <= '2025-10-20';

-- QUERY

-- WHERE
SELECT MSV, HoVaTen, ChucVu, NgayGiaNhap
FROM ThanhVien
WHERE TrangThaiHD = 'Hoạt động'

-- INNER JOIN
SELECT ThanhVien.HoVaTen, BanChuyenMon.TenBan
FROM ThanhVien
INNER JOIN BanChuyenMon ON ThanhVien.MaBan = BanChuyenMon.MaBan

-- LEFT JOIN
SELECT ThanhVien.HoVaTen, BanChuyenMon.TenBan
FROM ThanhVien
LEFT JOIN BanChuyenMon ON ThanhVien.MaBan = BanChuyenMon.MaBan

-- RIGHT JOIN
SELECT ThanhVien.HoVaTen, BanChuyenMon.TenBan
FROM ThanhVien
RIGHT JOIN BanChuyenMon ON ThanhVien.MaBan = BanChuyenMon.MaBan

-- GROUP BY
SELECT MaBan, COUNT(*) AS SoLuongThanhVien
FROM ThanhVien
GROUP BY MaBan;

-- HAVING
SELECT MaBan, COUNT(*) AS SoLuongThanhVien
FROM ThanhVien
GROUP BY MaBan
HAVING SoLuongThanhVien = 2;

-- ORDER BY
SELECT * FROM ThuChi
ORDER BY SoTien DESC, MaGD ASC;

-- GROUP BY, HAVING kết hợp Hàm tổng hợp (COUNT, SUM, AVG, MIN, MAX)
SELECT 
    sk.MaSK,
    sk.TenSK,
    COUNT(tc.MaGD) AS SoKhoanChi,
    SUM(tc.SoTien) AS TongTienChi,
    ROUND(AVG(tc.SoTien), 2) AS ChiTrungBinh,
    MIN(tc.SoTien) AS KhoanChiThapNhat,
    MAX(tc.SoTien) AS KhoanChiCaoNhat
FROM SuKien sk
INNER JOIN ThuChi tc ON sk.MaSK = tc.MaSK
WHERE tc.LoaiGD = 'Chi'
GROUP BY sk.MaSK
HAVING TongTienChi >= 1000000;
