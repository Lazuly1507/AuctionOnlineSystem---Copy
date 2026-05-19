# Kiến trúc Design Patterns - Hệ thống Đấu giá Trực tuyến

Tài liệu này tổng hợp các mẫu thiết kế và mẫu kiến trúc đang được áp dụng trong dự án Java OOP Đấu giá trực tuyến. Nội dung được đối chiếu theo cấu trúc code hiện tại trong `src/main/java/app`.

---

## 1. Singleton Pattern

**Ý nghĩa:** Đảm bảo một class chỉ có duy nhất một instance trong vòng đời ứng dụng và cung cấp điểm truy cập chung tới instance đó.

**Nơi áp dụng:**

* `app.server.database.DatabaseConnection`: Quản lý duy nhất một `HikariDataSource` dùng chung cho toàn bộ ứng dụng. Class có constructor private, `static volatile HikariDataSource`, API `getDataSource()` và `resetDataSource()` phục vụ test/re-init.
* `app.observer.Client`: Mỗi client JavaFX dùng một instance socket duy nhất để kết nối server, gửi request và quản lý danh sách listener. Dùng double-checked locking trong `getInstance()`.
* `app.observer.Server`: Server socket, service graph và thread pool được khởi tạo qua `Server.getInstance()`, tránh mở nhiều server cùng port `5000`.
* `app.models.DataStore`: Lưu state cục bộ phía client như user hiện tại, danh sách phiên đấu giá và phiên đang xem. Dùng `getInstance()` và đăng ký listener để đồng bộ dữ liệu.
* `app.client.manager.NavigationManager`: Singleton eager initialization, giữ `primaryStage` và controller hiện tại để điều hướng màn hình JavaFX.

**Ghi chú:** `JsonUtil`, `PasswordUtils`, `AlertUtils` là static utility classes, không phải Singleton đúng nghĩa vì không quản lý một instance có state.

---

## 2. Factory Pattern / Simple Factory

**Ý nghĩa:** Đóng gói logic khởi tạo object, giúp code gọi không cần biết class con cụ thể.

**Nơi áp dụng:**

* `app.common.models.ItemFactory`: Tạo đúng subclass của `Item` theo `ItemType`: `Electronics`, `Art`, `Vehicle`.
* `app.common.models.UserFactory`: Tạo đúng subclass của `User` theo `UserRole`: `Admin`, `Seller`, `Bidder`.
* `app.common.models.PacketReq` và `app.common.models.PacketRes`: Có các static factory methods như `of(...)`, `success(...)`, `error(...)` để chuẩn hóa cách tạo packet request/response trước khi serialize JSON.

**Lợi ích trong dự án:**

* Controller/Command/DAO không cần rải `new Electronics(...)`, `new Bidder(...)` ở nhiều nơi.
* Khi thêm loại item hoặc role mới, điểm sửa chính nằm tại enum và factory tương ứng.

---

## 3. Command Pattern

**Ý nghĩa:** Đóng gói một request thành một object có thể gọi `execute()`, giúp server xử lý nhiều loại request theo cùng một contract.

**Nơi áp dụng:**

* Interface `app.observer.Command` định nghĩa `execute(ClientHandler clientHandler, PacketReq packet)`.
* Các command cụ thể trong `app.observer`: `LoginCommand`, `RegisterCommand`, `PlaceBidCommand`, `DepositCommand`, `CreateAuctionCommand`, `CancelAuctionCommand`, `ChatCommand`, `FetchAuctionsCommand`, `FetchAuctionDetailCommand`, `SettleWalletCommand`, v.v.
* `app.observer.ClientHandler` tạo registry `EnumMap<PacketType, Command>` trong `createCommands(...)`, sau đó dispatch request theo `PacketType`.

**Luồng xử lý:**

1. Client gửi `PacketReq` qua socket.
2. `ClientHandler` deserialize JSON thành `PacketReq`.
3. `ClientHandler` kiểm tra đăng nhập nếu request cần authentication.
4. `ClientHandler` lấy `Command` từ registry theo `PacketType`.
5. `Command.execute(...)` gọi service tương ứng và gửi `PacketRes` về client.

**Lợi ích trong dự án:**

* Server không bị dồn toàn bộ logic request vào một `switch` lớn.
* Thêm request mới chỉ cần thêm DTO, `PacketType`, command mới và đăng ký vào registry.

---

## 4. Observer Pattern / Pub-Sub Variant

**Ý nghĩa:** Cho phép nhiều thành phần đăng ký lắng nghe một loại sự kiện. Khi có packet tương ứng, publisher thông báo tới toàn bộ listener đã subscribe.

**Nơi áp dụng:**

* `app.common.observer.PacketListener<T>` là callback interface.
* `app.observer.Client` giữ `Map<PacketType, CopyOnWriteArrayList<PacketListener<?>>> listenersMap`.
* `Client.subscribe(...)`, `Client.unsubscribe(...)`, `Client.notifyListeners(...)` tạo cơ chế pub-sub phía client.
* Các controller như `FirstScene`, `LiveController`, `MyHistoryController`, `DepositController`, `RegisterController`, `LoginController`, `MessController`, `AuctionController` đăng ký listener theo từng `PacketType`.
* `app.observer.Server.broadcast(...)` gửi packet tới nhiều client đang online, phục vụ realtime update giá thầu, chat, danh sách phiên đấu giá và cập nhật ví.

**Điểm triển khai quan trọng:**

* UI update được đưa về JavaFX Application Thread bằng `Platform.runLater(...)`.
* Controller triển khai `Cleanable` hoặc tự unsubscribe khi rời màn hình để tránh callback cũ tiếp tục chạy.

---

## 5. DAO (Data Access Object) Pattern

**Ý nghĩa:** Tách code truy cập database khỏi business logic, giúp service không chứa raw SQL.

**Nơi áp dụng:**

* Interface trong `app.server.dao`: `UserDAO`, `ItemDAO`, `AuctionDAO`, `BidDAO`, `AutoBidDAO`, `ChatDAO`, `NotificationDAO`.
* Implementation trong `app.server.dao.impl`: `MySqlUserDAO`, `MySqlItemDAO`, `MySqlAuctionDAO`, `MySqlBidDAO`, `MySqlAutoBidDAO`.
* `app.server.dao.BaseDAO` gom helper dùng chung như `withConnection(...)`, `executeUpdate(...)`, `setParameters(...)`, `runInTransaction(...)`.

**Đặc điểm đáng chú ý:**

* Nhiều DAO có hai dạng method: method tự mở connection và method nhận `Connection` từ transaction bên ngoài.
* Các service thao tác qua interface DAO, không gọi SQL trực tiếp.
* `lockRow(...)` trong `UserDAO` và `AuctionDAO` hỗ trợ xử lý đấu giá đồng thời.

---

## 6. Service Layer Pattern

**Ý nghĩa:** Tách business logic khỏi controller/network và DAO. Service là nơi kiểm tra nghiệp vụ, điều phối transaction và gọi DAO.

**Nơi áp dụng:**

* `app.server.service.UserService`: Đăng nhập, đăng ký, nạp/rút tiền, reserve/settle tiền trong ví, kiểm tra quyền admin.
* `app.server.service.ItemService`: Thêm/sửa/xóa mềm sản phẩm, kiểm tra quyền quản lý sản phẩm, chặn sửa/xóa item đang đấu giá.
* `app.server.service.BidService`: Xử lý đặt giá, khóa auction/user, validate bid, đóng băng tiền, cập nhật giá cao nhất.
* `app.server.service.AuctionService`: Tạo phiên, lấy danh sách/detail/result, hủy phiên, hoàn tất phiên hết hạn, settle/release ví.
* `app.server.service.BidValidator`: Tách các rule validate giá đấu và trạng thái phiên.
* `app.server.service.AntiSnipeService`: Tách logic gia hạn phiên khi có bid ở những giây cuối.

**Lợi ích trong dự án:**

* Command chỉ nhận request và trả response, không chứa quá nhiều nghiệp vụ.
* DAO chỉ lưu/truy vấn dữ liệu, không quyết định rule đấu giá.
* Unit test có thể tập trung vào service và DAO.

---

## 7. Transaction Manager / Unit of Work Variant

**Ý nghĩa:** Gom nhiều thao tác database vào cùng một transaction. Nếu có lỗi thì rollback, thành công thì commit.

**Nơi áp dụng:**

* `app.server.database.TransactionManager` cung cấp `runInTransaction(...)` và `runWithoutResult(...)`.
* `UserService`, `ItemService`, `BidService`, `AuctionService` dùng `TransactionManager` để đảm bảo các thao tác liên quan cùng commit/rollback.
* DAO có method nhận `Connection` để cùng tham gia một transaction, ví dụ:
    * `auctionDAO.lockRow(conn, auctionId)`
    * `auctionDAO.findById(conn, auctionId)`
    * `bidDAO.insertBid(conn, auctionId, userId, bidAmount, false)`
    * `userDAO.update(conn, bidder)`
    * `auctionDAO.update(conn, auction)`

**Ví dụ nghiệp vụ đặt giá:**

Trong `BidService.placeBid(...)`, hệ thống khóa phiên đấu giá, khóa user, validate giá, đóng băng tiền, ghi bid và cập nhật auction trong cùng một transaction. Điều này giảm nguy cơ lost update khi nhiều bidder đặt giá cùng lúc.

---

## 8. MVC Layering (Model - View - Controller)

**Ý nghĩa:** Tách giao diện, dữ liệu/domain model và logic điều khiển màn hình.

**Nơi áp dụng:**

* **View:** `src/main/resources/app/views/` chứa các file FXML như `firstscene.fxml`, `live_auction.fxml`, `login_scene.fxml`, `register_account.fxml`, `deposit.fxml`, `mess_chat.fxml`.
* **Controller:** `app.client.controllers` chứa controller JavaFX như `FirstScene`, `LiveController`, `LoginController`, `RegisterController`, `DepositController`, `MyHistoryController`, `MessController`, `UserProfileController`.
* **Model:** `app.models` chứa domain object như `Auction`, `Item`, `Wallet`, `User`, `BidTransaction`, `Session`, `DataStore`.
* **Điều hướng view:** `NavigationManager` load FXML, gắn CSS và thay scene trên `Stage`.

**Tách tầng client-server:**

* Controller phía client không truy cập database trực tiếp.
* Controller gửi `PacketReq` qua `Client`.
* Server nhận packet qua `ClientHandler`, dispatch sang Command, rồi Command gọi Service/DAO.

---

## 9. DTO (Data Transfer Object) Pattern

**Ý nghĩa:** Đóng gói dữ liệu để truyền qua mạng hoặc giữa các tầng mà không kéo theo behavior nghiệp vụ.

**Nơi áp dụng:**

* Package `app.data` chứa nhiều `record` làm request/response DTO:
    * Auth: `LoginRequest`, `LoginResponse`, `RegisterRequest`, `RegisterResponse`
    * Auction: `AuctionsRequest`, `AuctionsResponse`, `AuctionDetailRequest`, `AuctionDetailResponse`, `AuctionResultRequest`, `AuctionResultResponse`, `CreateAuctionRequest`, `CreateAuctionResponse`
    * Bid: `PlaceBidRequest`, `PlaceBidResponse`, `BidRequest`, `BidResponse`, `BidResult`
    * Item/User/Wallet/Chat: `ItemData`, `ItemResponse`, `ItemListResponse`, `UserData`, `UsersResponse`, `WalletUpdateResponse`, `ChatRequest`, `ChatResponse`
* Marker interfaces `Request` và `Response` phân biệt dữ liệu vào/ra.
* `PacketType` ánh xạ mỗi packet type với class request/response tương ứng thông qua `reqClass` và `resClass`.
* `PacketReq` và `PacketRes` serialize/deserialize payload bằng Gson.

**Lợi ích trong dự án:**

* Client-server trao đổi JSON nhất quán.
* Domain model như `User`, `Wallet`, `Item` không cần gửi nguyên trạng qua socket.
* Dễ thêm response mới mà không làm vỡ protocol hiện tại.

---

## 10. Mapper Pattern

**Ý nghĩa:** Chuyển đổi giữa domain model và DTO/view model để giảm phụ thuộc trực tiếp giữa tầng nghiệp vụ và dữ liệu trả về client.

**Nơi áp dụng:**

* `app.server.service.AuctionMapper` chuyển `Auction` thành:
    * `AuctionSummary` dùng cho danh sách phiên.
    * `AuctionDetail` dùng cho màn hình chi tiết/live auction.
* `app.data.UserData` có constructor nhận `User` để tạo DTO an toàn hơn khi trả về client.
* `app.data.ItemData` có constructor nhận `Item` để tạo DTO item.

**Lợi ích trong dự án:**

* Màn hình danh sách không cần toàn bộ dữ liệu chi tiết.
* Response không expose trực tiếp mọi field nội bộ của domain object.
* Logic tính `currentPrice` được tập trung trong mapper thay vì rải trong nhiều command/controller.

---

## 11. Dependency Injection thủ công

**Ý nghĩa:** Class nhận dependency qua constructor thay vì tự tạo dependency bên trong, giúp giảm coupling và dễ test hơn.

**Nơi áp dụng:**

* `Server.initService()` tạo object graph:
    * DAO: `MySqlUserDAO`, `MySqlItemDAO`, `MySqlAuctionDAO`, `MySqlAutoBidDAO`, `MySqlBidDAO`
    * Infrastructure/service helper: `TransactionManager`, `BidValidator`, `AntiSnipeService`
    * Service: `UserService`, `ItemService`, `BidService`, `AuctionService`
* Service nhận DAO qua constructor, ví dụ `UserService(UserDAO, TransactionManager)`, `BidService(BidDAO, AuctionDAO, UserDAO, TransactionManager, BidValidator, AntiSnipeService)`.
* Command nhận service qua constructor, ví dụ `LoginCommand(UserService)`, `PlaceBidCommand(BidService, UserService, AuctionService)`, `CreateAuctionCommand(AuctionService, ItemService)`.

**Ghi chú:** Đây là dependency injection thủ công, chưa dùng DI container như Spring.

---

## 12. Registry / Dispatcher Pattern

**Ý nghĩa:** Dùng một bảng đăng ký để ánh xạ key sang handler tương ứng, từ đó dispatch request mà không cần chuỗi `if-else` dài.

**Nơi áp dụng:**

* `ClientHandler.createCommands(...)` tạo `EnumMap<PacketType, Command>`.
* `ClientHandler.handlePacket(...)` lấy command bằng `commands.get(type)` rồi gọi `execute(...)`.
* `PacketType` đóng vai trò key chung cho protocol client-server.

**Lợi ích trong dự án:**

* Dễ nhìn toàn bộ các request server hỗ trợ.
* Mỗi command độc lập, giảm rủi ro sửa một request làm ảnh hưởng request khác.

---

## 13. Concurrency Control Patterns

**Ý nghĩa:** Bảo vệ dữ liệu khi nhiều client thao tác đồng thời, đặc biệt trong đặt giá và duyệt/hủy phiên đấu giá.

**Nơi áp dụng:**

* **Pessimistic Locking:** `MySqlAuctionDAO.lockRow(...)` và `MySqlUserDAO.lockRow(...)` dùng `SELECT ... FOR UPDATE` trong transaction để khóa hàng đang thao tác.
* **Optimistic Locking:** `Auction` có field `version`; `MySqlAuctionDAO.updateIfVersionMatches(...)` update với điều kiện `WHERE id = ? AND version = ?`. `CancelAuctionRequest` mang `expectedVersion` từ client lên server.
* **Thread-safe collections:** `Client.listenersMap` dùng `ConcurrentHashMap`, listener list dùng `CopyOnWriteArrayList`; `Server.authenticatedClients` dùng `ConcurrentHashMap`.
* **Lock cục bộ:** `Wallet` dùng `ReentrantLock` để bảo vệ `availableBalance` và `frozenFunds` trong object.

**Lợi ích trong dự án:**

* Giảm rủi ro hai bidder cùng ghi giá thắng.
* Tránh mất cập nhật khi admin thao tác trên dữ liệu auction đã cũ.
* Giữ state ví nhất quán khi reserve/release/commit tiền.

---

## 14. Thread Pool / Background Worker

**Ý nghĩa:** Tách xử lý nền khỏi thread chính và tái sử dụng thread cho các tác vụ lặp lại hoặc tác vụ song song.

**Nơi áp dụng:**

* `Server.clientPool`: `Executors.newCachedThreadPool()` xử lý nhiều `ClientHandler`.
* `Server.broadcastPool`: gửi broadcast tới nhiều client mà không chặn luồng accept/request chính.
* `Server.auctionMaintenancePool`: `ScheduledExecutorService` chạy định kỳ để hoàn tất các phiên đã hết hạn.
* `LiveController.scheduler`: cập nhật countdown trên màn hình live auction mỗi giây.

**Lợi ích trong dự án:**

* Server chấp nhận nhiều client đồng thời.
* Broadcast realtime không làm nghẽn xử lý request.
* Phiên đấu giá có thể tự kết thúc theo thời gian.

---

## 15. State Management / Enum State Machine

**Ý nghĩa:** Quản lý vòng đời của domain object bằng trạng thái rõ ràng, tránh thao tác sai trạng thái.

**Nơi áp dụng:**

* `app.common.enums.AuctionStatus`: `OPEN`, `RUNNING`, `FINISHED`, `PAID`, `CANCELED`.
* `app.common.models.Auction` có các method thay đổi trạng thái như `start()`, `finish()`, `markPaid()`, `cancel()`.
* `AuctionService` kiểm tra trạng thái trước khi hủy, hoàn tất, settle ví hoặc trả kết quả.
* `app.common.enums.ItemStatus` quản lý trạng thái item như active/delete tùy theo schema và DAO.

**Ghi chú:** Đây là state machine bằng enum, chưa phải GoF State Pattern đầy đủ vì chưa có các class state riêng biệt.

---

## 16. Lifecycle Callback / Cleanup Interface

**Ý nghĩa:** Chuẩn hóa thao tác dọn tài nguyên khi controller không còn được hiển thị.

**Nơi áp dụng:**

* `app.client.controllers.Cleanable` định nghĩa `cleanup()`.
* `NavigationManager.navigateTo(...)` gọi `cleanup()` nếu controller hiện tại implement `Cleanable`.
* Các controller như `FirstScene`, `LiveController`, `MyHistoryController`, `DepositController` dừng scheduler/timeline và unsubscribe listener trong `cleanup()`.

**Lợi ích trong dự án:**

* Tránh leak listener khi đổi màn hình.
* Tránh countdown/timeline cũ tiếp tục chạy sau khi view đã rời khỏi scene.

---

## 17. Exception Hierarchy / Layered Exceptions

**Ý nghĩa:** Phân loại lỗi theo tầng để message xử lý rõ ràng hơn.

**Nơi áp dụng:**

* `app.common.exception.AppException`: base runtime exception của ứng dụng.
* `app.common.exception.DatabaseException`: lỗi database/DAO.
* `app.common.exception.ServiceException`: lỗi nghiệp vụ ở service.
* `app.common.exception.ConnectException`: lỗi kết nối client-server.

**Lợi ích trong dự án:**

* Command có thể bắt `ServiceException` để trả lỗi nghiệp vụ thân thiện về client.
* DAO ném `DatabaseException` thay vì để lộ `SQLException` lên controller/network.

---

## 18. Client-Server Layering

**Ý nghĩa:** Tách ứng dụng thành client GUI và server xử lý nghiệp vụ/database.

**Nơi áp dụng:**

* Client JavaFX:
    * Controller nhận event UI.
    * `Client` gửi `PacketReq` JSON qua socket.
    * Listener nhận `PacketRes` và cập nhật UI.
* Server:
    * `Server` nhận socket.
    * `ClientHandler` đọc packet và dispatch command.
    * `Command` gọi service.
    * `Service` gọi DAO và quản lý transaction.
    * DAO thao tác MySQL qua `DatabaseConnection`.

**Luồng tổng quát:**

```text
FXML View
  -> JavaFX Controller
  -> Client / PacketReq
  -> Socket JSON
  -> Server / ClientHandler
  -> Command
  -> Service
  -> DAO
  -> MySQL
  -> PacketRes
  -> PacketListener
  -> UI update
```

---

## Những điểm không nên ghi quá mức

* **Strategy Pattern:** Dự án có các service tách thuật toán như `BidValidator` và `AntiSnipeService`, nhưng chưa có interface strategy để hoán đổi nhiều thuật toán cùng contract. Vì vậy chỉ nên mô tả là service/helper tách nghiệp vụ, không nên khẳng định là Strategy Pattern hoàn chỉnh.
* **State Pattern:** Dự án có state machine bằng enum cho `AuctionStatus`, nhưng chưa triển khai state object riêng cho từng trạng thái.
* **Repository Pattern:** DAO trong dự án đang đúng hơn với tên DAO vì implementation chứa SQL trực tiếp và interface mô tả thao tác persistence.