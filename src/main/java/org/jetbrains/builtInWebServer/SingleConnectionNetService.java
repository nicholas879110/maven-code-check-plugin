package org.jetbrains.builtInWebServer;

import com.gome.maven.execution.process.OSProcessHandler;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.net.NetUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;

import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.io.NettyUtil;

import java.net.InetSocketAddress;

public abstract class SingleConnectionNetService extends NetService {
  protected volatile Channel processChannel;

  protected SingleConnectionNetService( Project project) {
    super(project);
  }

  protected abstract void configureBootstrap( Bootstrap bootstrap,  Consumer<String> errorOutputConsumer);

  @Override
  protected void connectToProcess( AsyncPromise<OSProcessHandler> promise, int port,  OSProcessHandler processHandler,  Consumer<String> errorOutputConsumer) {
    Bootstrap bootstrap = NettyUtil.oioClientBootstrap();
    configureBootstrap(bootstrap, errorOutputConsumer);
    Channel channel = NettyUtil.connect(bootstrap, new InetSocketAddress(NetUtils.getLoopbackAddress(), port), promise);
    if (channel != null) {
      processChannel = channel;
      promise.setResult(processHandler);
    }
  }

  @Override
  protected void closeProcessConnections() {
    Channel currentProcessChannel = processChannel;
    if (currentProcessChannel != null) {
      processChannel = null;
      NettyUtil.closeAndReleaseFactory(currentProcessChannel);
    }
  }
}