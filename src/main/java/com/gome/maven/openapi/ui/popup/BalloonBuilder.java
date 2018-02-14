/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.gome.maven.openapi.ui.popup;

import com.gome.maven.openapi.Disposable;

import java.awt.*;
import java.awt.event.ActionListener;

/**
 * @see JBPopupFactory#createBalloonBuilder(javax.swing.JComponent)
 */
public interface BalloonBuilder {
    /** @deprecated use {@link Balloon#show(com.gome.maven.ui.awt.RelativePoint, Balloon.Position)} (to remove in IDEA 14) */
    @SuppressWarnings("UnusedDeclaration")
    
    BalloonBuilder setPreferredPosition(Balloon.Position position);

    
    BalloonBuilder setBorderColor( Color color);

    
    BalloonBuilder setBorderInsets( Insets insets);

    
    BalloonBuilder setFillColor( Color color);

    
    BalloonBuilder setHideOnClickOutside(boolean hide);

    
    BalloonBuilder setHideOnKeyOutside(boolean hide);

    
    BalloonBuilder setShowCallout(boolean show);

    
    BalloonBuilder setCloseButtonEnabled(boolean enabled);

    
    BalloonBuilder setFadeoutTime(long fadeoutTime);

    
    BalloonBuilder setAnimationCycle(int time);

    
    BalloonBuilder setHideOnFrameResize(boolean hide);

    
    BalloonBuilder setHideOnLinkClick(boolean hide);

    
    BalloonBuilder setClickHandler(ActionListener listener, boolean closeOnClick);

    
    BalloonBuilder setCalloutShift(int length);

    
    BalloonBuilder setPositionChangeXShift(int positionChangeXShift);

    
    BalloonBuilder setPositionChangeYShift(int positionChangeYShift);

    /** @deprecated to remove in IDEA 14 */
    @SuppressWarnings("UnusedDeclaration")
    boolean isHideOnAction();

    
    BalloonBuilder setHideOnAction(boolean hideOnAction);

    
    BalloonBuilder setDialogMode(boolean dialogMode);

    
    BalloonBuilder setTitle( String title);

    
    BalloonBuilder setContentInsets(Insets insets);

    
    BalloonBuilder setShadow(boolean shadow);

    
    BalloonBuilder setSmallVariant(boolean smallVariant);

    
    BalloonBuilder setLayer(Balloon.Layer layer);

    
    BalloonBuilder setBlockClicksThroughBalloon(boolean block);

    /**
     * Links target balloon life cycle to the given object. I.e. current balloon will be auto-hide and collected as soon
     * as given anchor is disposed.
     * <p/>
     * <b>Note:</b> given disposable anchor is assumed to correctly implement {@link #hashCode()} and {@link #equals(Object)}.
     *
     * @param anchor  target anchor to link to
     * @return        balloon builder which produces balloon linked to the given object life cycle
     */
    
    BalloonBuilder setDisposable( Disposable anchor);

    
    Balloon createBalloon();
}