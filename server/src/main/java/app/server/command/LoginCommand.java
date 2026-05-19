package app.server.command;

import app.common.dto.LoginRequest;
import app.common.dto.LoginResponse;
import app.common.enums.PacketType;
import app.common.exception.ServiceException;
import app.common.mapper.DtoMapper;
import app.common.models.User;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.network.Server;
import app.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** LoginCommand. */
public class LoginCommand extends Command {
  private static final Logger logger = LoggerFactory.getLogger(LoginCommand.class);
  private final UserService userService;

  /** LoginCommand. */
  public LoginCommand(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      LoginRequest request = packet.getData(LoginRequest.class);
      if (request == null) {
        sendError(clientHandler, "Dữ liệu đăng nhập không hợp lệ.");
        return;
      }
      String username = request.username();
      String password = request.password();
      if (username == null || username.isBlank() || password == null || password.isBlank()) {
        sendError(clientHandler, "Tên đăng nhập và mật khẩu không được để trống.");
        return;
      }
      username = username.trim();
      User user = userService.login(username, password);
      if (user == null) {
        sendError(clientHandler, "Sai tên tài khoản hoặc mật khẩu.");
        return;
      }
      clientHandler.getSession().authenticate(user);
      Server.registerClient(user.getId(), clientHandler);
      logger.info("[SERVER] User {} logged in", username);
      LoginResponse response = new LoginResponse(DtoMapper.toUserData(user));
      clientHandler.sendPacket(
          PacketRes.of(true, PacketType.LOGIN, "Đăng nhập thành công!", response));
    } catch (ServiceException e) {
      logger.warn("[SERVER] Login failed: {}", e.getMessage());
      sendError(clientHandler, "Sai tên tài khoản hoặc mật khẩu.");
    } catch (Exception e) {
      logger.error("[SERVER] Login error", e);
      sendError(clientHandler, "Lỗi hệ thống.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(PacketType.LOGIN, message));
  }
}
