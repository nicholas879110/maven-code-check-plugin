package org.jetbrains.builtInWebServer;

import com.gome.maven.execution.ExecutionException;
import com.gome.maven.execution.filters.TextConsoleBuilder;
import com.gome.maven.execution.process.OSProcessHandler;
import com.gome.maven.execution.process.ProcessAdapter;
import com.gome.maven.execution.process.ProcessEvent;
import com.gome.maven.execution.ui.ConsoleViewContentType;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.ActionGroup;
import com.gome.maven.openapi.actionSystem.DefaultActionGroup;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.net.NetUtils;


import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.AsyncValueLoader;
import org.jetbrains.concurrency.Promise;

import javax.swing.*;
import java.io.IOException;

public abstract class NetService implements Disposable {
  protected static final Logger LOG = Logger.getInstance(NetService.class);

  protected final Project project;
  private final ConsoleManager consoleManager;

  protected final AsyncValueLoader<OSProcessHandler> processHandler = new AsyncValueLoader<OSProcessHandler>() {
    @Override
    protected boolean isCancelOnReject() {
      return true;
    }


    private OSProcessHandler doGetProcessHandler(int port) {
      try {
        return createProcessHandler(project, port);
      }
      catch (ExecutionException e) {
        LOG.error(e);
        return null;
      }
    }


    @Override
    protected Promise<OSProcessHandler> load( final AsyncPromise<OSProcessHandler> promise) throws IOException {
      final int port = NetUtils.findAvailableSocketPort();
      final OSProcessHandler processHandler = doGetProcessHandler(port);
      if (processHandler == null) {
        promise.setError(Promise.createError("rejected"));
        return promise;
      }

      promise.rejected(new Consumer<Throwable>() {
        @Override
        public void consume(Throwable error) {
          processHandler.destroyProcess();
          if (!(error instanceof Promise.MessageError)) {
            LOG.error(error);
          }
        }
      });

      final MyProcessAdapter processListener = new MyProcessAdapter();
      processHandler.addProcessListener(processListener);
      processHandler.startNotify();

      if (promise.getState() == Promise.State.REJECTED) {
        return promise;
      }

      ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
        @Override
        public void run() {
          if (promise.getState() != Promise.State.REJECTED) {
            try {
              connectToProcess(promise, port, processHandler, processListener);
            }
            catch (Throwable e) {
              if (!promise.setError(e)) {
                LOG.error(e);
              }
            }
          }
        }
      });
      return promise;
    }

    @Override
    protected void disposeResult( OSProcessHandler processHandler) {
      try {
        closeProcessConnections();
      }
      finally {
        processHandler.destroyProcess();
      }
    }
  };

  protected NetService( Project project) {
    this(project, new ConsoleManager());
  }

  protected NetService( Project project,  ConsoleManager consoleManager) {
    this.project = project;
    this.consoleManager = consoleManager;
  }


  protected abstract OSProcessHandler createProcessHandler( Project project, int port) throws ExecutionException;

  protected void connectToProcess( AsyncPromise<OSProcessHandler> promise,
                                  int port,
                                   OSProcessHandler processHandler,
                                   Consumer<String> errorOutputConsumer) {
    promise.setResult(processHandler);
  }

  protected abstract void closeProcessConnections();

  @Override
  public void dispose() {
    processHandler.reset();
  }

  protected void configureConsole( TextConsoleBuilder consoleBuilder) {
  }


  protected abstract String getConsoleToolWindowId();


  protected abstract Icon getConsoleToolWindowIcon();


  public ActionGroup getConsoleToolWindowActions() {
    return new DefaultActionGroup();
  }

  private final class MyProcessAdapter extends ProcessAdapter implements Consumer<String> {
    @Override
    public void onTextAvailable(ProcessEvent event, Key outputType) {
      print(event.getText(), ConsoleViewContentType.getConsoleViewType(outputType));
    }

    private void print(String text, ConsoleViewContentType contentType) {
      consoleManager.getConsole(NetService.this).print(text, contentType);
    }

    @Override
    public void processTerminated(ProcessEvent event) {
      processHandler.reset();
      print(getConsoleToolWindowId() + " terminated\n", ConsoleViewContentType.SYSTEM_OUTPUT);
    }

    @Override
    public void consume(String message) {
      print(message, ConsoleViewContentType.ERROR_OUTPUT);
    }
  }
}