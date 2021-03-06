package org.jetbrains.io.jsonRpc.socket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import org.jetbrains.io.jsonRpc.Client;

import java.nio.channels.ClosedChannelException;

public class SocketClient extends Client {
  protected SocketClient( Channel channel) {
    super(channel);
  }

  @Override
  public ChannelFuture send( ByteBuf message) {
    if (channel.isOpen()) {
      ByteBuf lengthBuffer = channel.alloc().buffer(4);
      lengthBuffer.writeInt(message.readableBytes());
      channel.write(lengthBuffer);
      return channel.writeAndFlush(message);
    }
    else {
      return channel.newFailedFuture(new ClosedChannelException());
    }
  }

  @Override
  public void sendHeartbeat() {
  }
}