package app.server.command;

import app.common.dto.DepositRequest;
import app.common.dto.WalletUpdateResponse;
import app.common.enums.PacketType;
import app.common.exception.ServiceException;
import app.common.mapper.DtoMapper;
import app.common.models.User;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.server.network.ClientHandler;
import app.server.service.UserService;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** DepositCommand. */
public class DepositCommand extends Command {
  private static final Logger logger = LoggerFactory.getLogger(DepositCommand.class);
  private final UserService userService;

  /** DepositCommand. */
  public DepositCommand(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void execute(ClientHandler clientHandler, PacketReq packet) {
    try {
      if (!clientHandler.isAuthenticated()) {
        sendError(clientHandler, "Authentication required");
        return;
      }
      DepositRequest request = packet.getData(DepositRequest.class);
      if (request == null || request.amount() == null) {
        sendError(clientHandler, "Dữ liệu nạp tiền không hợp lệ.");
        return;
      }
      BigDecimal amount = request.amount();
      User user = userService.deposit(clientHandler.getUser().getId(), amount);
      WalletUpdateResponse response = new WalletUpdateResponse(DtoMapper.toUserData(user));
      clientHandler.sendPacket(PacketRes.of(true, PacketType.DEPOSIT, "OK", response));
    } catch (ServiceException e) {
      logger.warn("Deposit failed: {}", e.getMessage());
      sendError(clientHandler, e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected deposit error", e);
      sendError(clientHandler, "Không thể nạp tiền.");
    }
  }

  private void sendError(ClientHandler clientHandler, String message) {
    clientHandler.sendPacket(PacketRes.error(PacketType.DEPOSIT, message));
  }
}
