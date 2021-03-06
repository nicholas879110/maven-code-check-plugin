/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.openapi.actionSystem;

import com.gome.maven.ide.DataManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.ui.PlaceProvider;
//import org.gome.maven.lang.annotations.JdkConstants;

import java.awt.event.InputEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Container for the information necessary to execute or update an {@link AnAction}.
 *
 * @see AnAction#actionPerformed(AnActionEvent)
 * @see AnAction#update(AnActionEvent)
 */

public class AnActionEvent implements PlaceProvider<String> {
    private final InputEvent myInputEvent;
     private final ActionManager myActionManager;
     private final DataContext myDataContext;
     private final String myPlace;
     private final Presentation myPresentation;
    /*@JdkConstants.InputEventMask*/ private final int myModifiers;
    private boolean myWorksInInjected;
     private static final String ourInjectedPrefix = "$injected$.";
    private static final Map<String, String> ourInjectedIds = new HashMap<String, String>();

    /**
     * @throws IllegalArgumentException if <code>dataContext</code> is <code>null</code> or
     * <code>place</code> is <code>null</code> or <code>presentation</code> is <code>null</code>
     *
     * @see ActionManager#getInstance()
     */
    public AnActionEvent(InputEvent inputEvent,
                          DataContext dataContext,
                           String place,
                          Presentation presentation,
                          ActionManager actionManager,
                         /*@JdkConstants.InputEventMask*/ int modifiers) {
        // TODO[vova,anton] make this constructor package local. No one is allowed to create AnActionEvents
        myInputEvent = inputEvent;
        myActionManager = actionManager;
        myDataContext = dataContext;
        myPlace = place;
        myPresentation = presentation;
        myModifiers = modifiers;
    }

    @Deprecated
    
    public static AnActionEvent createFromInputEvent( AnAction action,  InputEvent event,  String place) {
        DataContext context = event == null ? DataManager.getInstance().getDataContext() : DataManager.getInstance().getDataContext(event.getComponent());
        return createFromAnAction(action, event, place, context);
    }

    
    public static AnActionEvent createFromAnAction( AnAction action,
                                                    InputEvent event,
                                                    String place,
                                                    DataContext dataContext) {
        int modifiers = event == null ? 0 : event.getModifiers();
        Presentation presentation = action.getTemplatePresentation().clone();
        AnActionEvent anActionEvent = new AnActionEvent(event, dataContext, place, presentation, ActionManager.getInstance(), modifiers);
        anActionEvent.setInjectedContext(action.isInInjectedContext());
        return anActionEvent;
    }

    
    public static AnActionEvent createFromDataContext( String place,
                                                       Presentation presentation,
                                                       DataContext dataContext) {
        return new AnActionEvent(null, dataContext, place, presentation == null ? new Presentation() : presentation, ActionManager.getInstance(), 0);
    }


    
    public static AnActionEvent createFromInputEvent( InputEvent event,
                                                      String place,
                                                      Presentation presentation,
                                                      DataContext dataContext) {
        return new AnActionEvent(event, dataContext, place, presentation, ActionManager.getInstance(),
                event == null ? 0 : event.getModifiers());
    }

    /**
     * Returns the <code>InputEvent</code> which causes invocation of the action. It might be
     * <code>KeyEvent</code>, <code>MouseEvent</code>.
     * @return the <code>InputEvent</code> instance.
     */
    public InputEvent getInputEvent() {
        return myInputEvent;
    }

    /**
     * @return Project from the context of this event.
     */
    
    public Project getProject() {
        return getData(CommonDataKeys.PROJECT);
    }

    
    public static String injectedId(String dataId) {
        synchronized(ourInjectedIds) {
            String injected = ourInjectedIds.get(dataId);
            if (injected == null) {
                injected = ourInjectedPrefix + dataId;
                ourInjectedIds.put(dataId, injected);
            }
            return injected;
        }
    }

    
    public static String uninjectedId( String dataId) {
        return StringUtil.trimStart(dataId, ourInjectedPrefix);
    }

    public static DataContext getInjectedDataContext(final DataContext context) {
        return new DataContextWrapper(context) {
            
            @Override
            public Object getData( String dataId) {
                Object injected = super.getData(injectedId(dataId));
                if (injected != null) return injected;
                return super.getData(dataId);
            }
        };
    }

    /**
     * Returns the context which allows to retrieve information about the state of IDEA related to
     * the action invocation (active editor, selection and so on).
     *
     * @return the data context instance.
     */
    
    public DataContext getDataContext() {
        return myWorksInInjected ? getInjectedDataContext(myDataContext) : myDataContext;
    }

    
    public <T> T getData( DataKey<T> key) {
        return key.getData(getDataContext());
    }

    /**
     * Returns not null data by a data key. This method assumes that data has been checked for null in AnAction#update method.
     *<br/><br/>
     * Example of proper usage:
     *
     * <pre>
     *
     * public class MyAction extends AnAction {
     *   public void update(AnActionEvent e) {
     *     //perform action if and only if EDITOR != null
     *     boolean enabled = e.getData(CommonDataKeys.EDITOR) != null;
     *     e.getPresentation.setEnabled(enabled);
     *   }
     *
     *   public void actionPerformed(AnActionEvent e) {
     *     //if we're here then EDITOR != null
     *     Document doc = e.getRequiredData(CommonDataKeys.EDITOR).getDocument();
     *     doSomething(doc);
     *   }
     * }
     *
     * </pre>
     */
    
    public <T> T getRequiredData( DataKey<T> key) {
        T data = getData(key);
        assert data != null;
        return data;
    }

    /**
     * Returns the identifier of the place in the IDEA user interface from where the action is invoked
     * or updated.
     *
     * @return the place identifier
     * @see ActionPlaces
     */
    @Override
    
    public String getPlace() {
        return myPlace;
    }

    /**
     * Returns the presentation which represents the action in the place from where it is invoked
     * or updated.
     *
     * @return the presentation instance.
     */
    
    public Presentation getPresentation() {
        return myPresentation;
    }

    /**
     * Returns the modifier keys held down during this action event.
     * @return the modifier keys.
     */
    /*@JdkConstants.InputEventMask*/
    public int getModifiers() {
        return myModifiers;
    }

    
    public ActionManager getActionManager() {
        return myActionManager;
    }

    public void setInjectedContext(boolean worksInInjected) {
        myWorksInInjected = worksInInjected;
    }

    public boolean isInInjectedContext() {
        return myWorksInInjected;
    }

    public void accept( AnActionEventVisitor visitor) {
        visitor.visitEvent(this);
    }
}
