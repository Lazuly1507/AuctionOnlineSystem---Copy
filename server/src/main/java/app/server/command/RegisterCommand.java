package app.server.command;

import app.common.dto.RegisterRequest;
import app.common.dto.RegisterResponse;
import app.common.enums.PacketType;
import app.common.enums.UserRole;
import app.common.exception.ServiceException;
import app.common.mapper.DtoMapper;
import app.common.models.*;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** RegisterCommand. */
public class RegisterCommand extends Command {
  private static final Logger logger = LoggerFactory.getLogger(RegisterCommand.class);
  private final UserService userService;

  /** RegisterCommand. */
  public RegisterCommand(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    RegisterRequest request = packet.getData(RegisterRequest.class);
    if (request == null) {
      sendError(clientHandler, "Dữ liệu đăng ký không hợp lệ.");
      return;
    }
    String name = request.name();
    String username = request.account();
    String password = request.password();
    if (name == null
        || name.isBlank()
        || username == null
        || username.isBlank()
        || password == null
        || password.isBlank()) {
      sendError(clientHandler, "Thông tin đăng ký không được để trống.");
      return;
    }
    if (request.role() != null
        && request.role() != UserRole.BIDDER
        && request.role() != UserRole.SELLER) {
      sendError(clientHandler, "Vai trò không hợp lệ.");
      return;
    }
    UserRole role = request.role() != null ? request.role() : UserRole.BIDDER;
    try {
      User newUser =
          UserFactory.createUser(name, new Account(username, password), new Wallet(), role);
      User created = userService.register(newUser);
      logger.info("[SERVER] User {} registered successfully.", username);
      RegisterResponse response = new RegisterResponse(DtoMapper.toUserData(created));
      clientHandler.sendPacket(PacketRes.of(PacketType.REGISTER, "Đăng ký thành công!", response));
    } catch (ServiceException e) {
      logger.warn("[SERVER] Register failed for user {}", username);
      sendError(clientHandler, "Tài khoản đã tồn tại hoặc dữ liệu không hợp lệ.");
    } catch (Exception e) {
      logger.error("[SERVER] Register error", e);
      sendError(clientHandler, "Lỗi hệ thống.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(PacketType.REGISTER, message));
  }
}
