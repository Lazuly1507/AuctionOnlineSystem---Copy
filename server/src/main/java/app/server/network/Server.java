package app.server.network;

import app.common.dto.AuctionSummariesResponse;
import app.common.dto.WalletUpdateResponse;
import app.common.enums.PacketType;
import app.common.mapper.DtoMapper;
import app.common.protocol.PacketRes;
import app.server.dao.*;
import app.server.dao.impl.*;
import app.server.database.TransactionManager;
import app.server.service.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Server. */
public class Server {
  private static final Logger logger = LoggerFactory.getLogger(Server.class);
  public static final int PORT = 5000;
  private static volatile Server instance;
  private ServerSocket serverSocket;
  private volatile boolean running = true;
  private static final Map<Integer, ClientHandler> authenticatedClients = new ConcurrentHashMap<>();
  private static final ExecutorService clientPool = Executors.newCachedThreadPool();
  private static final ExecutorService broadcastPool = Executors.newCachedThreadPool();
  private final ScheduledExecutorService auctionMaintenancePool =
      Executors.newSingleThreadScheduledExecutor();
  private AuctionService auctionService;
  private BidService bidService;
  private UserService userService;
  private ItemService itemService;

  private Server() {
    try {
      serverSocket = new ServerSocket();
      serverSocket.setReuseAddress(true);
      serverSocket.bind(new InetSocketAddress(PORT));
      logger.info("[SERVER] Running on port {}", PORT);
      initService();
    } catch (IOException e) {
      logger.error("[SERVER] Failed to start on port {}", PORT, e);
      throw new RuntimeException("Cannot start server", e);
    }
  }

  /** getInstance. */
  public static synchronized Server getInstance() {
    if (instance == null) {
      instance = new Server();
    }
    return instance;
  }

  private void initService() {
    logger.info("[SERVER] Initializing database...");
    UserDAO userDAO = new MySqlUserDAO();
    ItemDAO itemDAO = new MySqlItemDAO();
    AuctionDAO auctionDAO = new MySqlAuctionDAO();
    AutoBidDAO autoBidDAO = new MySqlAutoBidDAO();
    BidDAO bidDAO = new MySqlBidDAO();
    TransactionManager transactionManager = new TransactionManager();
    BidValidator bidValidator = new BidValidator();
    AntiSnipeService antiSnipeService = new AntiSnipeService();
    userService = new UserService(userDAO, transactionManager);
    itemService = new ItemService(itemDAO, auctionDAO, transactionManager);
    bidService =
        new BidService(
            bidDAO, auctionDAO, userDAO, transactionManager, bidValidator, antiSnipeService);
    auctionService = new AuctionService(auctionDAO, bidDAO, itemDAO, userDAO, transactionManager);
    startAuctionMaintenance();
  }

  private void startAuctionMaintenance() {
    auctionMaintenancePool.scheduleAtFixedRate(
        () -> {
          try {
            var completions = auctionService.completeExpiredAuctionCompletions();
            if (!completions.isEmpty()) {
              logger.info(
                  "[SERVER] Completed expired auctions: {}",
                  completions.stream().map(completion -> completion.auctionId()).toList());
              broadcastAuctionList();
              sendWalletUpdates(completions);
            }
          } catch (Exception e) {
            logger.error("[SERVER] Auction maintenance failed", e);
          }
        },
        5,
        5,
        TimeUnit.SECONDS);
  }

  private void broadcastAuctionList() {
    try {
      var response = new AuctionSummariesResponse(auctionService.getAuctionSummaries());
      broadcast(PacketRes.of(PacketType.AUCTION_SUMMARIES_UPDATED, "OK", response), -1);
    } catch (Exception e) {
      logger.error("[SERVER] Failed to broadcast auction list", e);
    }
  }

  private void sendWalletUpdates(List<AuctionCompletion> completions) {
    Set<Integer> userIds = new LinkedHashSet<>();
    for (AuctionCompletion completion : completions) {
      userIds.addAll(completion.settledUserIds());
    }
    for (Integer userId : userIds) {
      sendWalletUpdate(userId);
    }
  }

  private void sendWalletUpdate(int userId) {
    try {
      var user = userService.getById(userId);
      sendPacketToUser(
          userId,
          PacketRes.of(
              PacketType.WALLET_UPDATED,
              "OK",
              new WalletUpdateResponse(DtoMapper.toUserData(user))));
    } catch (Exception e) {
      logger.warn("[SERVER] Failed to send wallet update to user {}", userId, e);
    }
  }

  /** start. */
  public void start() {
    logger.info("[SERVER] Waiting for clients...");
    try {
      while (running && !serverSocket.isClosed() && !Thread.currentThread().isInterrupted()) {
        try {
          Socket socket = serverSocket.accept();
          if (!running) {
            socket.close();
            break;
          }
          socket.setTcpNoDelay(true);
          socket.setKeepAlive(true);
          socket.setSoTimeout(0);
          logger.info("[SERVER] Client connected: {}", socket.getRemoteSocketAddress());
          ClientHandler clientHandler =
              new ClientHandler(socket, auctionService, bidService, userService, itemService);
          clientPool.execute(clientHandler);
        } catch (SocketException e) {
          if (serverSocket.isClosed()) {
            logger.info("[SERVER] Server socket closed");
            break;
          }
          logger.error("[SERVER] Socket error", e);
        }
      }
    } catch (IOException e) {
      if (running) {
        logger.error("[SERVER] Error accepting client", e);
      }
    }
  }

  /** shutdown. */
  public void shutdown() {
    logger.info("[SERVER] Shutting down...");
    running = false;
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
      authenticatedClients.values().stream().toList().forEach(ClientHandler::close);
      authenticatedClients.clear();
      shutdownExecutor(clientPool, "clientPool");
      shutdownExecutor(broadcastPool, "broadcastPool");
      shutdownExecutor(auctionMaintenancePool, "auctionMaintenancePool");
      logger.info("[SERVER] Shutdown complete");
      instance = null;
    } catch (IOException e) {
      logger.error("[SERVER] Shutdown IO error", e);
    }
  }

  private void shutdownExecutor(ExecutorService executor, String name) {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        logger.warn("[SERVER] Force shutdown {}", name);
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("[SERVER] Shutdown interrupted: {}", name, e);
      executor.shutdownNow();
    }
  }

  /** registerClient. */
  public static void registerClient(int userId, ClientHandler handler) {
    ClientHandler old = authenticatedClients.put(userId, handler);
    if (old != null && old != handler) {
      logger.info("[SERVER] Replacing old auction for user {}", userId);
      authenticatedClients.remove(userId, old);
      old.close();
    }
  }

  /** removeClient. */
  public static void removeClient(int userId, ClientHandler handler) {
    authenticatedClients.remove(userId, handler);
  }

  /** broadcast. */
  public static void broadcast(PacketRes packet, int excludeUser) {
    sendToClients(packet, user -> user.getId() != excludeUser, "Broadcast failed to user {}");
  }

  /** sendPacketToUser. */
  public static void sendPacketToUser(int userId, PacketRes packet) {
    ClientHandler handler = authenticatedClients.get(userId);
    if (packet == null || !isReady(handler)) {
      return;
    }
    asyncSend(handler, packet, "Packet send failed to user {}", userId);
  }

  /** broadcastToAuctionViewers. */
  public static void broadcastToAuctionViewers(int auctionId, PacketRes packet, int excludeUser) {
    if (auctionId <= 0) {
      return;
    }
    sendToClients(
        packet,
        user -> user.getId() != excludeUser,
        handler -> {
          Integer viewingAuctionId = handler.getSession().getViewingAuctionId();
          return viewingAuctionId != null && viewingAuctionId == auctionId;
        },
        "Auction viewer broadcast failed to user {} for auction {}",
        auctionId);
  }

  private static void sendToClients(
      PacketRes packet, Predicate<app.common.models.User> userFilter, String errorLog) {
    sendToClients(packet, userFilter, handler -> true, errorLog);
  }

  private static void sendToClients(
      PacketRes packet,
      Predicate<app.common.models.User> userFilter,
      Predicate<ClientHandler> handlerFilter,
      String errorLog,
      Object... logArgs) {
    if (packet == null) {
      return;
    }
    authenticatedClients
        .values()
        .forEach(
            handler -> {
              if (!isReady(handler) || !handlerFilter.test(handler)) {
                return;
              }
              var user = handler.getUser();
              if (!userFilter.test(user)) {
                return;
              }
              Object[] args = new Object[logArgs.length + 1];
              args[0] = user.getId();
              System.arraycopy(logArgs, 0, args, 1, logArgs.length);
              asyncSend(handler, packet, errorLog, args);
            });
  }

  private static boolean isReady(ClientHandler handler) {
    return handler != null && handler.isAuthenticated() && handler.getUser() != null;
  }

  private static void asyncSend(
      ClientHandler handler, PacketRes packet, String errorLog, Object... args) {
    broadcastPool.execute(
        () -> {
          try {
            handler.sendPacket(packet);
          } catch (Exception e) {
            logger.warn("[SERVER] " + errorLog, appendThrowable(args, e));
          }
        });
  }

  private static Object[] appendThrowable(Object[] args, Throwable throwable) {
    Object[] finalArgs = new Object[args.length + 1];
    System.arraycopy(args, 0, finalArgs, 0, args.length);
    finalArgs[args.length] = throwable;
    return finalArgs;
  }

  public static int getOnlineUserCount() {
    return authenticatedClients.size();
  }

  /** isUserOnline. */
  public static boolean isUserOnline(int userId) {
    return authenticatedClients.containsKey(userId);
  }
}
