package app.client.command;

import app.client.manager.UserManager;
import app.client.store.AuctionStore;
import app.common.dto.LoginResponse;
import app.common.mapper.DtoMapper;
import app.common.protocol.PacketRes;

/** LoginCommand. */
public class LoginCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      LoginResponse response = packet.getData(LoginResponse.class);
      if (response != null && response.user() != null) {
        AuctionStore.getInstance().clearHistory();
        UserManager.getInstance().setCurrentUser(DtoMapper.toUser(response.user()));
      }
      notifyUpdate();
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
