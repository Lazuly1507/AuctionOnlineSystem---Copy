# Design Patterns - Hệ thống Đấu giá Trực tuyến

Tài liệu này chỉ liệt kê các design pattern thực sự rõ ràng đang được áp dụng trong dự án. Các kỹ thuật như thread pool, transaction, concurrency control, client-server layering, exception hierarchy... không được đưa vào danh sách chính vì chúng nghiêng về kiến trúc hoặc kỹ thuật triển khai hơn là design pattern OOP.

---

## 1. Singleton Pattern
**Ý nghĩa:** Đảm bảo một class chỉ có một instance dùng chung trong vòng đời ứng dụng.

**Nơi áp dụng:**
* `app.server.database.DatabaseConnection`: Quản lý duy nhất một `HikariDataSource` dùng chung cho toàn bộ ứng dụng.
* `app.observer.Client`: Mỗi client JavaFX dùng một instance socket chính để kết nối server và quản lý listener.
* `app.observer.Server`: Quản lý server socket, service graph và thread pool thông qua `Server.getInstance()`.
* `app.models.DataStore`: Lưu state cục bộ phía client như user hiện tại, danh sách phiên đấu giá và phiên đang xem.
* `app.client.manager.NavigationManager`: Quản lý `primaryStage`, controller hiện tại và điều hướng màn hình JavaFX.

**Ghi chú:** `JsonUtil`, `PasswordUtils`, `AlertUtils` là static utility classes, không phải Singleton đúng nghĩa vì không quản lý một instance có state.

---

## 2. Factory Pattern / Simple Factory
**Ý nghĩa:** Đóng gói logic tạo object, giúp code gọi không cần biết class con cụ thể.

**Nơi áp dụng:**
* `app.common.models.ItemFactory`: Tạo subclass của `Item` theo `ItemType`, ví dụ `Electronics`, `Art`, `Vehicle`.
* `app.common.models.UserFactory`: Tạo subclass của `User` theo `UserRole`, ví dụ `Admin`, `Seller`, `Bidder`.
* `app.common.models.PacketReq` và `app.common.models.PacketRes`: Dùng static factory methods như `of(...)`, `success(...)`, `error(...)` để chuẩn hóa cách tạo packet request/response.

**Lợi ích trong dự án:**
* Giảm việc rải `new Electronics(...)`, `new Bidder(...)` ở nhiều nơi.
* Khi thêm loại item hoặc role mới, logic tạo object tập trung tại factory.

---

## 3. Command Pattern
**Ý nghĩa:** Đóng gói mỗi request thành một object có thể thực thi qua cùng một contract.

**Nơi áp dụng:**
* `app.observer.Command` định nghĩa method `execute(ClientHandler clientHandler, PacketReq packet)`.
* Các command cụ thể như `LoginCommand`, `RegisterCommand`, `PlaceBidCommand`, `DepositCommand`, `CreateAuctionCommand`, `CancelAuctionCommand`, `ChatCommand`, `FetchAuctionsCommand`.
* `app.observer.ClientHandler` tạo bảng đăng ký command theo `PacketType`, sau đó dispatch request đến command tương ứng.

**Lợi ích trong dự án:**
* Server không bị dồn logic xử lý request vào một `switch` hoặc `if-else` lớn.
* Thêm request mới chỉ cần thêm DTO, `PacketType`, command mới và đăng ký command.

---

## 4. Observer Pattern / Pub-Sub Variant
**Ý nghĩa:** Cho phép nhiều thành phần đăng ký lắng nghe sự kiện. Khi packet tương ứng xuất hiện, publisher thông báo đến các listener đã subscribe.

**Nơi áp dụng:**
* `app.common.observer.PacketListener<T>` là callback interface.
* `app.observer.Client` quản lý `Map<PacketType, CopyOnWriteArrayList<PacketListener<?>>>`.
* `Client.subscribe(...)`, `Client.unsubscribe(...)`, `Client.notifyListeners(...)` tạo cơ chế pub-sub phía client.
* Các controller như `FirstScene`, `LiveController`, `MyHistoryController`, `DepositController`, `RegisterController`, `LoginController`, `MessController`, `AuctionController` đăng ký listener theo từng `PacketType`.
* `app.observer.Server.broadcast(...)` gửi packet đến nhiều client đang online để cập nhật realtime giá thầu, chat, danh sách phiên đấu giá và ví.

**Lợi ích trong dự án:**
* Controller có thể phản ứng với packet realtime mà không cần polling liên tục.
* Các màn hình khác nhau có thể lắng nghe cùng một loại packet theo nhu cầu riêng.

---

## 5. DAO Pattern
**Ý nghĩa:** Tách logic truy cập database khỏi business logic.

**Nơi áp dụng:**
* Interface trong `app.server.dao`: `UserDAO`, `ItemDAO`, `AuctionDAO`, `BidDAO`, `AutoBidDAO`, `ChatDAO`, `NotificationDAO`.
* Implementation trong `app.server.dao.impl`: `MySqlUserDAO`, `MySqlItemDAO`, `MySqlAuctionDAO`, `MySqlBidDAO`, `MySqlAutoBidDAO`.
* `app.server.dao.BaseDAO` gồm helper dùng chung như `withConnection(...)`, `executeUpdate(...)`, `setParameters(...)`, `runInTransaction(...)`.

**Lợi ích trong dự án:**
* Service không chứa SQL trực tiếp.
* Có thể thay đổi cách lưu trữ dữ liệu bằng cách thay implementation DAO.
* Các thao tác database được gom theo từng nhóm domain rõ ràng.

---

## 6. DTO Pattern
**Ý nghĩa:** Đóng gói dữ liệu để truyền qua mạng hoặc giữa các tầng mà không kéo theo behavior nghiệp vụ.

**Nơi áp dụng:**
* Package `app.data` chứa các record làm request/response DTO, ví dụ `LoginRequest`, `LoginResponse`, `CreateAuctionRequest`, `AuctionDetailResponse`, `PlaceBidRequest`, `PlaceBidResponse`, `ChatRequest`, `ChatResponse`.
* Marker interfaces `Request` và `Response` phân biệt dữ liệu vào/ra.
* `PacketType` ánh xạ mỗi packet type với class request/response tương ứng.
* `PacketReq` và `PacketRes` serialize/deserialize payload bằng Gson.

**Lợi ích trong dự án:**
* Client-server trao đổi JSON nhất quán.
* Domain model không bị gửi nguyên trạng qua socket.
* Response trả về client chỉ gồm dữ liệu cần thiết.

---

## 7. Mapper Pattern
**Ý nghĩa:** Chuyển đổi giữa domain model và DTO/view model để giảm phụ thuộc trực tiếp giữa tầng nghiệp vụ và dữ liệu trả về client.

**Nơi áp dụng:**
* `app.server.service.AuctionMapper`: Chuyển `Auction` thành `AuctionSummary` và `AuctionDetail`.
* `app.data.UserData`: Tạo DTO từ `User`.
* `app.data.ItemData`: Tạo DTO từ `Item`.

**Lợi ích trong dự án:**
* Màn hình danh sách không cần nhận toàn bộ dữ liệu chi tiết.
* Response không expose trực tiếp mọi field nội bộ của domain object.
* Logic tính/chọn dữ liệu hiển thị được tập trung tại mapper.

---

## 8. MVC Pattern
**Ý nghĩa:** Tách giao diện, dữ liệu/domain model và logic điều khiển màn hình.

**Nơi áp dụng:**
* **View:** `src/main/resources/app/views/` chứa các file FXML.
* **Controller:** `app.client.controllers` chứa controller JavaFX như `FirstScene`, `LiveController`, `LoginController`, `RegisterController`, `DepositController`, `MessController`.
* **Model:** `app.models` chứa domain object như `Auction`, `Item`, `Wallet`, `User`, `BidTransaction`, `Session`, `DataStore`.
* **Điều hướng:** `NavigationManager` load FXML, gắn CSS và thay scene trên `Stage`.

**Lợi ích trong dự án:**
* UI, state/domain model và logic điều khiển màn hình được tách riêng.
* Controller không truy cập database trực tiếp mà gửi request qua client-server protocol.

---

## 9. Registry / Dispatcher Pattern
**Ý nghĩa:** Dùng một bảng đăng ký để ánh xạ key sang handler tương ứng, từ đó dispatch request mà không cần chuỗi `if-else` dài.

**Nơi áp dụng:**
* `ClientHandler.createCommands(...)` tạo `EnumMap<PacketType, Command>`.
* `ClientHandler.handlePacket(...)` lấy command bằng `commands.get(type)` rồi gọi `execute(...)`.
* `PacketType` đóng vai trò key chung cho protocol client-server.

**Lợi ích trong dự án:**
* Dễ nhìn toàn bộ request server hỗ trợ.
* Mỗi command độc lập, giảm rủi ro sửa request này ảnh hưởng request khác.