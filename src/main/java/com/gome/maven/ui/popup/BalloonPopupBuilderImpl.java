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
package com.gome.maven.ui.popup;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.openapi.ui.popup.Balloon;
import com.gome.maven.openapi.ui.popup.BalloonBuilder;
import com.gome.maven.openapi.ui.popup.JBPopupAdapter;
import com.gome.maven.openapi.ui.popup.LightweightWindowEvent;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.ui.BalloonImpl;
import com.gome.maven.ui.Gray;
import com.gome.maven.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BalloonPopupBuilderImpl implements BalloonBuilder {
     private final Map<Disposable, List<Balloon>> myStorage;
     private Disposable myAnchor;

    private final JComponent myContent;

    private Color   myBorder             = new JBColor(JBColor.GRAY, Gray._200);
     private Insets myBorderInsets = null;
    private Color   myFill               = MessageType.INFO.getPopupBackground();
    private boolean myHideOnMouseOutside = true;
    private boolean myHideOnKeyOutside   = true;
    private long    myFadeoutTime        = -1;
    private boolean myShowCallout        = true;
    private boolean myCloseButtonEnabled = false;
    private boolean myHideOnFrameResize  = true;
    private boolean myHideOnLinkClick    = false;

    private ActionListener myClickHandler;
    private boolean        myCloseOnClick;
    private int myAnimationCycle = 500;

    private int myCalloutShift;
    private int myPositionChangeXShift;
    private int myPositionChangeYShift;
    private boolean myHideOnAction = true;
    private boolean myDialogMode;
    private String  myTitle;
    private Insets  myContentInsets = new Insets(2, 2, 2, 2);
    private boolean myShadow        = false;
    private boolean mySmallVariant  = false;

    private Balloon.Layer myLayer;
    private boolean myBlockClicks = false;

    public BalloonPopupBuilderImpl( Map<Disposable, List<Balloon>> storage,  final JComponent content) {
        myStorage = storage;
        myContent = content;
    }

    @Override
    public boolean isHideOnAction() {
        return myHideOnAction;
    }

    
    @Override
    public BalloonBuilder setHideOnAction(boolean hideOnAction) {
        myHideOnAction = hideOnAction;
        return this;
    }

    
    @Override
    public BalloonBuilder setDialogMode(boolean dialogMode) {
        myDialogMode = dialogMode;
        return this;
    }

    
    @Override
    public BalloonBuilder setPreferredPosition(final Balloon.Position position) {
        return this;
    }

    
    @Override
    public BalloonBuilder setBorderColor( final Color color) {
        myBorder = color;
        return this;
    }

    @Override
    public BalloonBuilder setBorderInsets( Insets insets) {
        myBorderInsets = insets;
        return this;
    }

    
    @Override
    public BalloonBuilder setFillColor( final Color color) {
        myFill = color;
        return this;
    }

    
    @Override
    public BalloonBuilder setHideOnClickOutside(final boolean hide) {
        myHideOnMouseOutside  = hide;
        return this;
    }

    
    @Override
    public BalloonBuilder setHideOnKeyOutside(final boolean hide) {
        myHideOnKeyOutside = hide;
        return this;
    }

    
    @Override
    public BalloonBuilder setShowCallout(final boolean show) {
        myShowCallout = show;
        return this;
    }

    
    @Override
    public BalloonBuilder setFadeoutTime(long fadeoutTime) {
        myFadeoutTime = fadeoutTime;
        return this;
    }

    
    @Override
    public BalloonBuilder setBlockClicksThroughBalloon(boolean block) {
        myBlockClicks = block;
        return this;
    }

    
    @Override
    public BalloonBuilder setAnimationCycle(int time) {
        myAnimationCycle = time;
        return this;
    }

    
    @Override
    public BalloonBuilder setHideOnFrameResize(boolean hide) {
        myHideOnFrameResize = hide;
        return this;
    }

    
    @Override
    public BalloonBuilder setHideOnLinkClick(boolean hide) {
        myHideOnLinkClick = hide;
        return this;
    }

    
    @Override
    public BalloonBuilder setPositionChangeXShift(int positionChangeXShift) {
        myPositionChangeXShift = positionChangeXShift;
        return this;
    }

    
    @Override
    public BalloonBuilder setPositionChangeYShift(int positionChangeYShift) {
        myPositionChangeYShift = positionChangeYShift;
        return this;
    }

    
    @Override
    public BalloonBuilder setCloseButtonEnabled(boolean enabled) {
        myCloseButtonEnabled = enabled;
        return this;
    }

    
    @Override
    public BalloonBuilder setClickHandler(ActionListener listener, boolean closeOnClick) {
        myClickHandler = listener;
        myCloseOnClick = closeOnClick;
        return this;
    }

    
    @Override
    public BalloonBuilder setCalloutShift(int length) {
        myCalloutShift = length;
        return this;
    }

    
    @Override
    public BalloonBuilder setTitle( String title) {
        myTitle = title;
        return this;
    }

    
    @Override
    public BalloonBuilder setContentInsets(Insets insets) {
        myContentInsets = insets;
        return this;
    }

    
    @Override
    public BalloonBuilder setShadow(boolean shadow) {
        myShadow = shadow;
        return this;
    }

    
    @Override
    public BalloonBuilder setSmallVariant(boolean smallVariant) {
        mySmallVariant = smallVariant;
        return this;
    }

    
    @Override
    public BalloonBuilder setLayer(Balloon.Layer layer) {
        myLayer = layer;
        return this;
    }

    
    @Override
    public BalloonBuilder setDisposable( Disposable anchor) {
        myAnchor = anchor;
        return this;
    }

    
    @Override
    public Balloon createBalloon() {
        final BalloonImpl result = new BalloonImpl(
                myContent, myBorder, myBorderInsets, myFill, myHideOnMouseOutside, myHideOnKeyOutside, myHideOnAction, myShowCallout, myCloseButtonEnabled,
                myFadeoutTime, myHideOnFrameResize, myHideOnLinkClick, myClickHandler, myCloseOnClick, myAnimationCycle, myCalloutShift,
                myPositionChangeXShift, myPositionChangeYShift, myDialogMode, myTitle, myContentInsets, myShadow, mySmallVariant, myBlockClicks,
                myLayer);

        if (myStorage != null && myAnchor != null) {
            List<Balloon> balloons = myStorage.get(myAnchor);
            if (balloons == null) {
                myStorage.put(myAnchor, balloons = new ArrayList<Balloon>());
                Disposer.register(myAnchor, new Disposable() {
                    @Override
                    public void dispose() {
                        List<Balloon> toDispose = myStorage.remove(myAnchor);
                        if (toDispose != null) {
                            for (Balloon balloon : toDispose) {
                                if (!balloon.isDisposed()) {
                                    Disposer.dispose(balloon);
                                }
                            }
                        }
                    }
                });
            }
            balloons.add(result);
            result.addListener(new JBPopupAdapter() {
                @Override
                public void onClosed(LightweightWindowEvent event) {
                    if (!result.isDisposed()) {
                        Disposer.dispose(result);
                    }
                }
            });
        }

        return result;
    }
}
