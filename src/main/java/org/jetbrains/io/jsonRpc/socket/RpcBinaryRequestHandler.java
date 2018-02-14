package org.jetbrains.io.jsonRpc.socket;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.AtomicNotNullLazyValue;
import com.gome.maven.openapi.util.Disposer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;


import org.jetbrains.ide.BinaryRequestHandler;
import org.jetbrains.ide.BuiltInServerManager;
import org.jetbrains.io.MessageDecoder;
import org.jetbrains.io.jsonRpc.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

class RpcBinaryRequestHandler extends BinaryRequestHandler implements ExceptionHandler, ClientListener {
  private static final Logger LOG = Logger.getInstance(RpcBinaryRequestHandler.class);

  private static final UUID ID = UUID.fromString("69957EEB-AFB8-4036-A9A8-00D2D022F9BD");

  private final AtomicNotNullLazyValue<ClientManager> clientManager = new AtomicNotNullLazyValue<ClientManager>() {

    @Override
    protected ClientManager compute() {
      ClientManager result = new ClientManager(RpcBinaryRequestHandler.this, RpcBinaryRequestHandler.this);
      Disposable serverDisposable = BuiltInServerManager.getInstance().getServerDisposable();
      assert serverDisposable != null;
      Disposer.register(serverDisposable, result);

      rpcServer = new JsonRpcServer(result);
      return result;
    }
  };

  private JsonRpcServer rpcServer;


  @Override
  public UUID getId() {
    return ID;
  }


  @Override
  public ChannelHandler getInboundHandler( ChannelHandlerContext context) {
    SocketClient client = new SocketClient(context.channel());
    context.attr(ClientManager.CLIENT).set(client);
    clientManager.getValue().addClient(client);
    connected(client, null);
    return new MyDecoder(client);
  }

  @Override
  public void exceptionCaught( Throwable e) {
    LOG.error(e);
  }

  @Override
  public void connected( Client client,  Map<String, List<String>> parameters) {
  }

  @Override
  public void disconnected( Client client) {
  }

  private enum State {
    LENGTH, CONTENT
  }

  private class MyDecoder extends MessageDecoder {
    private State state = State.LENGTH;
    private int contentLength;

    private final SocketClient client;

    public MyDecoder( SocketClient client) {
      this.client = client;
    }

    @Override
    protected void messageReceived( ChannelHandlerContext context,  ByteBuf input) throws Exception {
      while (true) {
        switch (state) {
          case LENGTH: {
            ByteBuf buffer = getBufferIfSufficient(input, 4, context);
            if (buffer == null) {
              return;
            }

            state = State.CONTENT;
            contentLength = buffer.readInt();
          }

          case CONTENT: {
            CharSequence content = readChars(input);
            if (content == null) {
              return;
            }

            try {
              rpcServer.messageReceived(client, content, true);
            }
            catch (Throwable e) {
              clientManager.getValue().exceptionHandler.exceptionCaught(e);
            }
            finally {
              contentLength = 0;
              state = State.LENGTH;
            }
          }
        }
      }
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
      Client client = context.attr(ClientManager.CLIENT).get();
      // if null, so, has already been explicitly removed
      if (client != null) {
        clientManager.getValue().disconnectClient(context, client, false);
      }
    }
  }
}
