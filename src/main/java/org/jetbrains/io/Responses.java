/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.io;

import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ex.ApplicationInfoEx;
import com.gome.maven.openapi.util.text.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;



import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Calendar;

public final class Responses {
  private static String SERVER_HEADER_VALUE;

  public static FullHttpResponse response(HttpResponseStatus status) {
    return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.EMPTY_BUFFER);
  }


  public static FullHttpResponse response( String contentType,  ByteBuf content) {
    FullHttpResponse response = content == null
                                ? new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
                                : new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
    if (contentType != null) {
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
    }
    return response;
  }

  public static void setDate( HttpResponse response) {
    if (!response.headers().contains(HttpHeaderNames.DATE)) {
      HttpHeaders.setDateHeader(response, HttpHeaderNames.DATE, Calendar.getInstance().getTime());
    }
  }

  public static void addNoCache( HttpResponse response) {
    response.headers().add(HttpHeaderNames.CACHE_CONTROL, "no-cache, no-store, must-revalidate, max-age=0");
    response.headers().add(HttpHeaderNames.PRAGMA, "no-cache");
  }


  public static String getServerHeaderValue() {
    if (SERVER_HEADER_VALUE == null) {
      Application app = ApplicationManager.getApplication();
      if (app != null && !app.isDisposed()) {
        SERVER_HEADER_VALUE = ApplicationInfoEx.getInstanceEx().getFullApplicationName();
      }
    }
    return SERVER_HEADER_VALUE;
  }

  public static void addServer( HttpResponse response) {
    if (getServerHeaderValue() != null) {
      response.headers().add(HttpHeaderNames.SERVER, getServerHeaderValue());
    }
  }

  public static void send( HttpResponse response, Channel channel,  HttpRequest request) {
    if (response.status() != HttpResponseStatus.NOT_MODIFIED && !HttpHeaderUtil.isContentLengthSet(response)) {
      HttpHeaderUtil.setContentLength(response,
                                   response instanceof FullHttpResponse ? ((FullHttpResponse)response).content().readableBytes() : 0);
    }

    addCommonHeaders(response);
    send(response, channel, request != null && !addKeepAliveIfNeed(response, request));
  }

  public static boolean addKeepAliveIfNeed(HttpResponse response, HttpRequest request) {
    if (HttpHeaderUtil.isKeepAlive(request)) {
      HttpHeaderUtil.setKeepAlive(response, true);
      return true;
    }
    return false;
  }

  public static void addCommonHeaders( HttpResponse response) {
    addServer(response);
    setDate(response);
    if (!response.headers().contains("X-Frame-Options")) {
      response.headers().set("X-Frame-Options", "SameOrigin");
    }
    response.headers().set("X-Content-Type-Options", "nosniff");
    response.headers().set("x-xss-protection", "1; mode=block");
  }

  public static void send(CharSequence content, Channel channel,  HttpRequest request) {
    send(content, CharsetUtil.US_ASCII, channel, request);
  }

  public static void send(CharSequence content, Charset charset, Channel channel,  HttpRequest request) {
    send(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(content, charset)), channel, request);
  }

  public static void send( HttpResponse response,  Channel channel, boolean close) {
    if (!channel.isActive()) {
      return;
    }

    ChannelFuture future = channel.write(response);
    if (!(response instanceof FullHttpResponse)) {
      channel.write(LastHttpContent.EMPTY_LAST_CONTENT);
    }
    channel.flush();
    if (close) {
      future.addListener(ChannelFutureListener.CLOSE);
    }
  }

  public static void sendStatus(HttpResponseStatus responseStatus, Channel channel) {
    sendStatus(responseStatus, channel, null);
  }

  public static void sendStatus(HttpResponseStatus responseStatus, Channel channel,  HttpRequest request) {
    sendStatus(responseStatus, channel, null, request);
  }

  public static HttpResponseStatus okInSafeMode( HttpResponseStatus status) {
    Application app = ApplicationManager.getApplication();
    return app != null && app.isUnitTestMode() ? status : HttpResponseStatus.OK;
  }

  public static void sendStatus( HttpResponseStatus responseStatus, Channel channel,  String description,  HttpRequest request) {
    send(createStatusResponse(responseStatus, request, description), channel, request);
  }

  private static HttpResponse createStatusResponse(HttpResponseStatus responseStatus,  HttpRequest request,  String description) {
    if (request != null && request.method() == HttpMethod.HEAD) {
      return response(responseStatus);
    }

    StringBuilder builder = new StringBuilder();
    String message = responseStatus.toString();
    builder.append("<!doctype html><title>").append(message).append("</title>").append("<h1 style=\"text-align: center\">").append(message).append("</h1>");
    if (description != null) {
      builder.append("<p>").append(description).append("</p>");
    }
    builder.append("<hr/><p style=\"text-align: center\">").append(StringUtil.notNullize(getServerHeaderValue(), "")).append("</p>");

    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, responseStatus, ByteBufUtil.encodeString(ByteBufAllocator.DEFAULT, CharBuffer.wrap(builder), CharsetUtil.UTF_8));
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
    return response;
  }
}