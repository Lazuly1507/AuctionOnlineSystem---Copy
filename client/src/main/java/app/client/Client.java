package app.client;

import app.client.command.*;
import app.common.enums.PacketType;
import app.common.exception.ConnectException;
import app.common.protocol.PacketReq;
import app.common.protocol.PacketRes;
import app.common.utils.JsonUtil;
import java.io.*;
import java.net.Socket;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client. */
public class Client {
  private static final Logger logger = LoggerFactory.getLogger(Client.class);
  private static volatile Client instance;
  private static final String HOST = "127.0.0.1";
  private static final int PORT = 5000;
  private Socket socket;
  private BufferedWriter writer;
  private BufferedReader reader;
  private volatile boolean connected = false;
  private volatile boolean closed = true;
  private int retryCount = 0;

  private final Map<PacketType, Command> commands;

  private Client() {
    this.commands = createCommands();
  }

  private Map<PacketType, Command> createCommands() {
    Map<PacketType, Command> registry = new EnumMap<>(PacketType.class);
    registry.put(PacketType.CHAT, new ChatCommand());
    registry.put(PacketType.LOGIN, new LoginCommand());
    registry.put(PacketType.REGISTER, new RegisterCommand());
    registry.put(PacketType.CREATE_AUCTION, new CreateAuctionCommand());
    registry.put(PacketType.FETCH_AUCTION_SUMMARIES, new FetchAuctionSummariesCommand());
    registry.put(PacketType.FETCH_AUCTION_HISTORY, new FetchAuctionHistoryCommand());
    registry.put(PacketType.FETCH_AUCTION_DETAIL, new FetchAuctionDetailCommand());
    registry.put(PacketType.FETCH_AUCTION_RESULT, new FetchAuctionResultCommand());
    registry.put(PacketType.UPDATE_ITEM, new UpdateItemCommand());
    registry.put(PacketType.DELETE_ITEM, new DeleteItemCommand());
    registry.put(PacketType.CANCEL_AUCTION, new CancelAuctionCommand());
    registry.put(PacketType.PLACE_BID, new PlaceBidCommand());
    registry.put(PacketType.DEPOSIT, new DepositCommand());
    registry.put(PacketType.CHAT_MESSAGE, new ChatCommand());
    registry.put(PacketType.AUCTION_CREATED, new CreateAuctionCommand());
    registry.put(PacketType.BID_PLACED, new PlaceBidCommand());
    registry.put(PacketType.AUCTION_CANCELLED, new CancelAuctionCommand());
    registry.put(PacketType.AUCTION_SUMMARIES_UPDATED, new FetchAuctionSummariesCommand());
    registry.put(PacketType.AUCTION_DETAIL_UPDATED, new FetchAuctionDetailCommand());
    registry.put(PacketType.WALLET_UPDATED, new WalletUpdateCommand());
    return registry;
  }

  /** getInstance. */
  public static Client getInstance() {
    if (instance == null) {
      synchronized (Client.class) {
        if (instance == null) {
          instance = new Client();
        }
      }
    }
    return instance;
  }

  /** connect. */
  public synchronized void connect() throws IOException {
    if (connected) {
      return;
    }
    logger.info("[CLIENT] Connecting to server...");
    closed = false;
    socket = new Socket(HOST, PORT);
    socket.setKeepAlive(true);
    socket.setTcpNoDelay(true);
    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    connected = true;
    startListener();
    logger.info("[CLIENT] Connected successfully");
  }

  private void startListener() {
    Thread listenerThread = new Thread(this::listen, "client-listener");
    listenerThread.setDaemon(true);
    listenerThread.start();
  }

  private void listen() {
    try {
      while (!closed && connected && socket != null && !socket.isClosed()) {
        String line = reader.readLine();
        if (line == null) {
          logger.warn("[CLIENT] Server disconnected");
          break;
        }
        handleIncoming(line);
      }
    } catch (IOException e) {
      if (!closed) {
        logger.warn("[CLIENT] Socket closed");
      }
    } finally {
      closeResources();
    }
  }

  private void handleIncoming(String line) {
    try {
      PacketRes packet = JsonUtil.fromJson(line, PacketRes.class);
      if (packet == null || packet.getType() == null) {
        logger.error("[CLIENT] Packet type is required");
        return;
      }
      handlePacket(packet);
    } catch (Exception e) {
      logger.error("[CLIENT] Invalid packet received", e);
    }
  }

  private void handlePacket(PacketRes packet) {
    PacketType type = packet.getType();
    logger.info("Processing packet: {}", type);
    Command command = commands.get(type);
    if (command == null) {
      logger.warn("Unrecognized packet type: {}", type);
      return;
    }
    try {
      command.execute(packet);
    } catch (Exception e) {
      logger.error("Error executing packet: {}", type, e);
    }
  }

  /** sendRequest. */
  public synchronized void sendRequest(PacketReq packet) throws IOException {
    if (!connected || closed || writer == null) {
      throw new ConnectException("Chưa kết nối tới server");
    }
    if (packet == null) {
      throw new IllegalArgumentException("Packet cannot be null");
    }
    String json = JsonUtil.toJson(packet);
    writer.write(json);
    writer.newLine();
    writer.flush();
  }

  /** closeResources. */
  public synchronized void closeResources() {
    if (closed) {
      return;
    }
    logger.info("[CLIENT] Closing resources");
    closed = true;
    connected = false;
    try {
      if (socket != null && !socket.isClosed()) {
        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();
      }
    } catch (IOException e) {
      logger.warn("[CLIENT] Error closing socket", e);
    }
    try {
      if (reader != null) {
        reader.close();
      }
    } catch (IOException e) {
      logger.warn("[CLIENT] Error closing reader", e);
    }
    try {
      if (writer != null) {
        writer.close();
      }
    } catch (IOException e) {
      logger.warn("[CLIENT] Error closing writer", e);
    }
    socket = null;
    reader = null;
    writer = null;
  }

  public boolean isConnected() {
    return connected;
  }

  public boolean isClosed() {
    return closed;
  }
}
