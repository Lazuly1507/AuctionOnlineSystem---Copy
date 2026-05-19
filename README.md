# Online Auction System (Hệ thống Đấu giá Trực tuyến)
  Một ứng dụng client-server hỗ trợ đấu giá trực tuyến theo thời gian thực. Dự án này được xây dựng để áp dụng các nguyên lý lập trình hướng đối tượng (OOP), quản lý luồng dữ liệu qua Socket và thiết kế theo kiến trúc chuẩn.
  ## Mục lục
- [Tính năng chính](#-tính-năng-chính)
- [Kiến trúc & Design Patterns](#-kiến-trúc--design-patterns)
- [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
- [Cài đặt & Chạy dự án](#-cài-đặt--chạy-dự-án)
- [Chuẩn mực Code](#-chuẩn-mực-code)
  ## Tính năng chính
* **Quản lý phiên đấu giá:** Mở, đóng và theo dõi các phiên đấu giá theo thời gian thực.
* **Client-Server Communication:** Xử lý kết nối đồng thời từ nhiều client tham gia trả giá thông qua Java Socket.
* **Cập nhật Real-time:** Broadcast mức giá mới nhất đến tất cả người tham gia ngay lập tức.
  ##  Kiến trúc & Design Patterns
Dự án được phân chia rõ ràng theo mô hình **MVC (Model-View-Controller)** nhằm tách biệt logic nghiệp vụ và giao diện.
Các Design Patterns được áp dụng:
* **Singleton:** Quản lý kết nối database và các instance duy nhất của hệ thống (ví dụ: AuctionManager).
* **Observer:** Lắng nghe và cập nhật trạng thái giá thầu tới các client đang theo dõi phòng đấu giá.
* **Factory Method:** Khởi tạo các loại tài sản/vật phẩm đấu giá khác nhau một cách linh hoạt.
  ## Yêu cầu hệ thống
Để biên dịch và chạy dự án này, máy của bạn cần cài đặt:
* **Java Development Kit (JDK) 25** trở lên.
* **Apache Maven** (để quản lý dependencies và build project).
* Hệ điều hành: Linux/Windows/macOS.
  ## Cài đặt & Chạy dự án
  1. **Clone repository:**
  ```bash
  git clone [https://github.com/username/online-auction-system.git](https://github.com/username/online-auction-system.git)
  cd online-auction-system
