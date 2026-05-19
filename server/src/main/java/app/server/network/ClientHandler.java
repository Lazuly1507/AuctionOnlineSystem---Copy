package app.server.network;

import app.common.enums.PacketType;
import app.common.models.User;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.common.utils.JsonUtil;
import app.server.command.*;
import app.server.service.AuctionService;
import app.server.service.BidService;
import app.server.service.ItemService;
import app.server.service.UserService;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** ClientHandler. */
public class ClientHandler implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

  private final Socket socket;
  private BufferedWriter writer;
  private BufferedReader reader;
  private final Object writeLock = new Object();
  private final Session session = new Session();
  private final Map<PacketType, Command> commands;
  private volatile boolean closed = false;

  /** ClientHandler. */
  public ClientHandler(
      Socket socket,
      AuctionService auctionService,
      BidService bidService,
      UserService userService,
      ItemService itemService) {
    this.socket = socket;
    this.commands = createCommands(auctionService, bidService, userService, itemService);
  }

  private Map<PacketType, Command> createCommands(
      AuctionService auctionService,
      BidService bidService,
      UserService userService,
      ItemService itemService) {
    Map<PacketType, Command> registry = new EnumMap<>(PacketType.class);
    registry.put(PacketType.CHAT, new ChatCommand());
    registry.put(PacketType.LOGIN, new LoginCommand(userService));
    registry.put(PacketType.REGISTER, new RegisterCommand(userService));
    registry.put(PacketType.CREATE_AUCTION, new CreateAuctionCommand(auctionService));
    registry.put(
        PacketType.FETCH_AUCTION_SUMMARIES, new FetchAuctionSummariesCommand(auctionService));
    registry.put(PacketType.FETCH_AUCTION_HISTORY, new FetchAuctionHistoryCommand(auctionService));
    registry.put(PacketType.FETCH_AUCTION_DETAIL, new FetchAuctionDetailCommand(auctionService));
    registry.put(
        PacketType.FETCH_AUCTION_RESULT,
        new FetchAuctionResultCommand(auctionService, userService));
    registry.put(PacketType.FETCH_SELLER_ITEMS, new FetchSellerItemsCommand(itemService));
    registry.put(PacketType.UPDATE_ITEM, new UpdateItemCommand(itemService));
    registry.put(PacketType.DELETE_ITEM, new DeleteItemCommand(itemService));
    registry.put(PacketType.FETCH_USER_LIST, new FetchUserListCommand(userService));
    registry.put(PacketType.CANCEL_AUCTION, new CancelAuctionCommand(auctionService));
    registry.put(
        PacketType.PLACE_BID, new PlaceBidCommand(bidService, userService, auctionService));
    registry.put(PacketType.DEPOSIT, new DepositCommand(userService));
    return registry;
  }

  @Override
  public void run() {
    try {
      socket.setSoTimeout(0);
      writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      listen();
    } catch (IOException e) {
      logger.error("Error initializing client command", e);
    } finally {
      cleanup();
    }
  }

  private void listen() {
    try {
      String line;
      while (!closed && (line = reader.readLine()) != null) {
        try {
          PacketReq packet = JsonUtil.fromJson(line, PacketReq.class);
          if (packet == null || packet.getType() == null) {
            sendPacket(PacketRes.error(PacketType.ERROR, "Packet type is required"));
            continue;
          }
          session.touch();
          handlePacket(packet);
        } catch (Exception e) {
          logger.error("Invalid packet received", e);
          sendPacket(PacketRes.error(PacketType.ERROR, "Invalid packet received"));
        }
      }
      logger.info("Client disconnected: {}", socket.getRemoteSocketAddress());
    } catch (SocketTimeoutException e) {
      logger.warn("Client timeout: {}", socket.getRemoteSocketAddress());
    } catch (IOException e) {
      if (!closed) {
        logger.error("Error while listening to client", e);
      }
    }
  }

  private void handlePacket(PacketReq packet) {
    PacketType type = packet.getType();
    logger.info("Processing network: {}", type);
    if (requiresAuthentication(type) && !requireLogin(type)) {
      return;
    }
    Command command = commands.get(type);
    if (command == null) {
      logger.warn("Unrecognized network type: {}", type);
      sendPacket(PacketRes.error(PacketType.ERROR, "Unrecognized network type"));
      return;
    }
    try {
      command.execute(this, packet);
    } catch (Exception e) {
      logger.error("Error executing network: {}", type, e);
      sendPacket(PacketRes.error(PacketType.ERROR, "Error executing network"));
    }
  }

  private boolean requiresAuthentication(PacketType type) {
    return switch (type) {
      case PLACE_BID,
          CREATE_AUCTION,
          FETCH_AUCTION_HISTORY,
          FETCH_SELLER_ITEMS,
          UPDATE_ITEM,
          DELETE_ITEM,
          FETCH_USER_LIST,
          CANCEL_AUCTION,
          DEPOSIT ->
          true;
      default -> false;
    };
  }

  private boolean requireLogin(PacketType type) {
    if (!session.isAuthenticated()) {
      sendPacket(PacketRes.error(type, "Authentication required"));
      return false;
    }
    return true;
  }

  /** sendPacket. */
  public void sendPacket(PacketRes packet) {
    send(packet);
  }

  private void send(Object packet) {
    if (packet == null || writer == null || closed) {
      return;
    }
    try {
      synchronized (writeLock) {
        writer.write(JsonUtil.toJson(packet));
        writer.newLine();
        writer.flush();
      }
      User user = session.getUser();
      logger.debug("Sent message to {}", user != null ? user.getName() : "unknown");
    } catch (IOException e) {
      logger.error("Failed to send message", e);
      close();
    }
  }

  /** close. */
  public synchronized void close() {
    if (closed) {
      return;
    }
    closed = true;
    try {
      if (socket != null && !socket.isClosed()) {
        socket.shutdownInput();
        socket.shutdownOutput();
      }
    } catch (IOException ignored) {
      // Socket may already be half-closed by the peer.
    }
    try {
      if (reader != null) {
        reader.close();
      }
    } catch (IOException e) {
      logger.error("Error closing reader", e);
    }
    try {
      if (writer != null) {
        writer.close();
      }
    } catch (IOException e) {
      logger.error("Error closing writer", e);
    }
    try {
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
    } catch (IOException e) {
      logger.error("Error closing socket", e);
    }
  }

  private void cleanup() {
    User user = session.getUser();
    if (user != null) {
      Server.removeClient(user.getId(), this);
    }
    session.logout();
    close();
  }

  public boolean isAuthenticated() {
    return session.isAuthenticated();
  }

  public Session getSession() {
    return session;
  }

  public User getUser() {
    return session.getUser();
  }

  public Socket getSocket() {
    return socket;
  }

  public BufferedWriter getWriter() {
    return writer;
  }

  public BufferedReader getReader() {
    return reader;
  }
}
