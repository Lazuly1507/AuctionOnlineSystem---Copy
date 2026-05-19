package app.client.command;

import app.client.manager.UserManager;
import app.common.dto.WalletUpdateResponse;
import app.common.mapper.DtoMapper;
import app.common.protocol.PacketRes;

/** WalletUpdateCommand. */
public class WalletUpdateCommand extends Command {
  @Override
  public void execute(PacketRes packet) {
    if (packet.isSuccess()) {
      WalletUpdateResponse response = packet.getData(WalletUpdateResponse.class);
      if (response != null && response.user() != null) {
        UserManager.getInstance().setCurrentUser(DtoMapper.toUser(response.user()));
      }
      notifyUpdate();
    }
    notifyMessage(packet == null ? "" : packet.getMessage());
  }
}
