/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.ide;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.io.NettyUtil;

import java.io.IOException;

public abstract class HttpRequestHandler {
    // Your handler will be instantiated on first user request
    public static final ExtensionPointName<HttpRequestHandler> EP_NAME = ExtensionPointName.create("com.gome.maven.httpRequestHandler");

    protected static boolean checkPrefix( String uri,  String prefix) {
        if (uri.length() > prefix.length() && uri.charAt(0) == '/' && uri.regionMatches(true, 1, prefix, 0, prefix.length())) {
            if ((uri.length() - prefix.length()) == 1) {
                return true;
            }
            else {
                char c = uri.charAt(prefix.length() + 1);
                return c == '/' || c == '?';
            }
        }
        return false;
    }

    @SuppressWarnings("SpellCheckingInspection")
    /**
     * Write request from browser without Origin will be always blocked regardles of your implementation.
     */
    public boolean isAccessible( HttpRequest request) {
        String host = NettyUtil.host(request);
        // If attacker.com DNS rebound to 127.0.0.1 and user open site directly — no Origin or Referer headers.
        // So we should check Host header.
        return host != null && NettyUtil.isLocalOrigin(request) && NettyUtil.parseAndCheckIsLocalHost("http://" + host);
    }

    public boolean isSupported( FullHttpRequest request) {
        return request.method() == HttpMethod.GET || request.method() == HttpMethod.HEAD;
    }

    /**
     * @return true if processed successfully, false to pass processing to other handlers.
     */
    public abstract boolean process( QueryStringDecoder urlDecoder,  FullHttpRequest request,  ChannelHandlerContext context)
            throws IOException;
}