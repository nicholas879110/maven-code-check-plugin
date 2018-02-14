package org.jetbrains.jps.incremental.messages;

import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.NotNullFunction;

import org.jetbrains.jps.builders.BuildTarget;

import java.util.Collection;

/**
 * @author nik
 */
public class BuildingTargetProgressMessage extends BuildMessage {
  private final Collection<? extends BuildTarget<?>> myTargets;
   private final Event myEventType;

  public enum Event {
    STARTED, FINISHED
  }

  public BuildingTargetProgressMessage( Collection<? extends BuildTarget<?>> targets,  Event event) {
    super(composeMessageText(targets, event), Kind.PROGRESS);
    myTargets = targets;
    myEventType = event;
  }

  private static String composeMessageText(Collection<? extends BuildTarget<?>> targets, Event event) {
    String targetsString = StringUtil.join(targets, new NotNullFunction<BuildTarget<?>, String>() {

      @Override
      public String fun(BuildTarget<?> dom) {
        return dom.getPresentableName();
      }
    }, ", ");
    return (event == Event.STARTED ? "Started" : "Finished") + " building " + targetsString;
  }


  public Collection<? extends BuildTarget<?>> getTargets() {
    return myTargets;
  }


  public Event getEventType() {
    return myEventType;
  }
}
