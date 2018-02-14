package org.jetbrains.builtInWebServer;

import com.gome.maven.execution.filters.TextConsoleBuilder;
import com.gome.maven.execution.filters.TextConsoleBuilderFactory;
import com.gome.maven.execution.ui.ConsoleView;
import com.gome.maven.openapi.actionSystem.ActionGroup;
import com.gome.maven.openapi.actionSystem.ActionManager;
import com.gome.maven.openapi.actionSystem.ActionPlaces;
import com.gome.maven.openapi.actionSystem.ActionToolbar;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.ui.SimpleToolWindowPanel;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.wm.ToolWindow;
import com.gome.maven.openapi.wm.ToolWindowAnchor;
import com.gome.maven.openapi.wm.ToolWindowManager;
import com.gome.maven.ui.content.Content;
import com.gome.maven.ui.content.ContentFactory;


public final class ConsoleManager {
  private ConsoleView console;


  public ConsoleView getConsole( NetService netService) {
    if (console == null) {
      createConsole(netService);
    }
    return console;
  }

  private void createConsole( final NetService netService) {
    TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(netService.project);
    netService.configureConsole(consoleBuilder);
    console = consoleBuilder.getConsole();

    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        ActionGroup actionGroup = netService.getConsoleToolWindowActions();
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, actionGroup, false);

        SimpleToolWindowPanel toolWindowPanel = new SimpleToolWindowPanel(false, true);
        toolWindowPanel.setContent(console.getComponent());
        toolWindowPanel.setToolbar(toolbar.getComponent());

        ToolWindow toolWindow = ToolWindowManager.getInstance(netService.project)
          .registerToolWindow(netService.getConsoleToolWindowId(), false, ToolWindowAnchor.BOTTOM, netService.project, true);
        toolWindow.setIcon(netService.getConsoleToolWindowIcon());

        Content content = ContentFactory.SERVICE.getInstance().createContent(toolWindowPanel, "", false);
        Disposer.register(content, console);

        toolWindow.getContentManager().addContent(content);
      }
    }, netService.project.getDisposed());
  }
}