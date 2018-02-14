package org.jetbrains.io.jsonRpc;

import com.gome.maven.openapi.util.UserDataHolderBase;
import com.gome.maven.util.containers.ConcurrentIntObjectMap;
import com.gome.maven.util.containers.ContainerUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;


import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

import java.util.Enumeration;

public abstract class Client extends UserDataHolderBase {
  static final RuntimeException REJECTED = Promise.createError("rejected");

  protected final Channel channel;

  final ConcurrentIntObjectMap<AsyncPromise<Object>> messageCallbackMap = ContainerUtil.createConcurrentIntObjectMap();

  protected Client( Channel channel) {
    this.channel = channel;
  }


  public final EventLoop getEventLoop() {
    return channel.eventLoop();
  }


  public final ByteBufAllocator getByteBufAllocator() {
    return channel.alloc();
  }

  protected abstract ChannelFuture send( ByteBuf message);

  public abstract void sendHeartbeat();


  final <T> AsyncPromise<T> send(int messageId,  ByteBuf message) {
    ChannelFuture channelFuture = send(message);
    if (messageId == -1) {
      return null;
    }

    ChannelFutureAwarePromise<T> promise = new ChannelFutureAwarePromise<T>(messageId, messageCallbackMap);
    channelFuture.addListener(promise);
    //noinspection unchecked
    messageCallbackMap.put(messageId, (AsyncPromise<Object>)promise);
    return promise;
  }

  final void rejectAsyncResults( ExceptionHandler exceptionHandler) {
    if (!messageCallbackMap.isEmpty()) {
      Enumeration<AsyncPromise<Object>> elements = messageCallbackMap.elements();
      while (elements.hasMoreElements()) {
        try {
          elements.nextElement().setError(REJECTED);
        }
        catch (Throwable e) {
          exceptionHandler.exceptionCaught(e);
        }
      }
    }
  }

  private static final class ChannelFutureAwarePromise<T> extends AsyncPromise<T> implements ChannelFutureListener {
    private final int messageId;
    private final ConcurrentIntObjectMap<?> messageCallbackMap;

    public ChannelFutureAwarePromise(int messageId, ConcurrentIntObjectMap<?> messageCallbackMap) {
      this.messageId = messageId;
      this.messageCallbackMap = messageCallbackMap;
    }

    @Override
    public boolean setError( Throwable error) {
      boolean result = super.setError(error);
      messageCallbackMap.remove(messageId);
      return result;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
      if (!future.isSuccess()) {
        Throwable cause = future.cause();
        setError(cause == null ? Promise.createError("No success") : cause);
      }
    }
  }
}