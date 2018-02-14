package org.jetbrains.io.jsonRpc;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.SimpleTimer;
import com.gome.maven.openapi.util.SimpleTimerTask;
import gnu.trove.THashSet;
import gnu.trove.TObjectProcedure;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;


import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.io.webSocket.WebSocketServerOptions;

import java.util.List;

public class ClientManager implements Disposable {
  public static final AttributeKey<Client> CLIENT = AttributeKey.valueOf("SocketHandler.client");

  private final SimpleTimerTask heartbeatTimer;


  private final ClientListener listener;


  public final ExceptionHandler exceptionHandler;

  private final THashSet<Client> clients = new THashSet<Client>();

  public ClientManager() {
    this(null, new ExceptionHandlerImpl());
  }

  public ClientManager( ClientListener listener,  ExceptionHandler exceptionHandler) {
    this(null, exceptionHandler, listener);
  }

  public ClientManager( WebSocketServerOptions options,  ExceptionHandler exceptionHandler,  ClientListener listener) {
    this.exceptionHandler = exceptionHandler;
    this.listener = listener;

    heartbeatTimer = SimpleTimer.getInstance().setUp(new Runnable() {
      @Override
      public void run() {
        synchronized (clients) {
          if (clients.isEmpty()) {
            return;
          }

          clients.forEach(new TObjectProcedure<Client>() {
            @Override
            public boolean execute(Client client) {
              if (client.channel.isActive()) {
                client.sendHeartbeat();
              }
              return true;
            }
          });
        }
      }
    }, (options == null ? new WebSocketServerOptions() : options).heartbeatDelay);
  }

  public void addClient( Client client) {
    synchronized (clients) {
      clients.add(client);
    }
  }

  public int getClientCount() {
    synchronized (clients) {
      return clients.size();
    }
  }

  public boolean hasClients() {
    return getClientCount() > 0;
  }

  @Override
  public void dispose() {
    try {
      heartbeatTimer.cancel();
    }
    finally {
      synchronized (clients) {
        clients.clear();
      }
    }
  }

  public <T> void send(final int messageId,  final ByteBuf message,  final List<AsyncPromise<Pair<Client, T>>> results) {
    forEachClient(new TObjectProcedure<Client>() {
      private boolean first;

      @Override
      public boolean execute(final Client client) {
        try {
          AsyncPromise<Pair<Client, T>> result = client.send(messageId, first ? message : message.duplicate());
          first = false;
          if (results != null) {
            results.add(result);
          }
        }
        catch (Throwable e) {
          exceptionHandler.exceptionCaught(e);
        }
        return true;
      }
    });
  }

  public boolean disconnectClient( ChannelHandlerContext context,  Client client, boolean closeChannel) {
    synchronized (clients) {
      if (!clients.remove(client)) {
        return false;
      }
    }

    try {
      context.attr(CLIENT).remove();

      if (closeChannel) {
        context.channel().close();
      }

      client.rejectAsyncResults(exceptionHandler);
    }
    finally {
      if (listener != null) {
        listener.disconnected(client);
      }
    }
    return true;
  }

  public void forEachClient( TObjectProcedure<Client> procedure) {
    synchronized (clients) {
      if (clients.isEmpty()) {
        return;
      }

      clients.forEach(procedure);
    }
  }
}