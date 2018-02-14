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
package com.gome.maven.util;

import com.gome.maven.ide.FileIconPatcher;
import com.gome.maven.ide.FileIconProvider;
import com.gome.maven.ide.presentation.VirtualFilePresentation;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.IconLoader;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.vfs.VFileProperty;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.WritingAccessProvider;
import com.gome.maven.ui.IconDeferrer;
import com.gome.maven.ui.LayeredIcon;
import com.gome.maven.ui.RowIcon;
import com.gome.maven.util.ui.EmptyIcon;
import com.gome.maven.util.ui.JBImageIcon;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;


/**
 * @author max
 * @author Konstantin Bulenkov
 */
public class IconUtil {
    private static final Key<Boolean> PROJECT_WAS_EVER_INITIALIZED = Key.create("iconDeferrer:projectWasEverInitialized");

    private static boolean wasEverInitialized( Project project) {
        Boolean was = project.getUserData(PROJECT_WAS_EVER_INITIALIZED);
        if (was == null) {
            if (project.isInitialized()) {
                was = Boolean.valueOf(true);
                project.putUserData(PROJECT_WAS_EVER_INITIALIZED, was);
            }
            else {
                was = Boolean.valueOf(false);
            }
        }

        return was.booleanValue();
    }

    
    public static Icon cropIcon( Icon icon, int maxWidth, int maxHeight) {
        if (icon.getIconHeight() <= maxHeight && icon.getIconWidth() <= maxWidth) {
            return icon;
        }

        final int w = Math.min(icon.getIconWidth(), maxWidth);
        final int h = Math.min(icon.getIconHeight(), maxHeight);

        final BufferedImage image = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .createCompatibleImage(icon.getIconWidth(), icon.getIconHeight(), Transparency.TRANSLUCENT);
        final Graphics2D g = image.createGraphics();
        icon.paintIcon(new JPanel(), g, 0, 0);
        g.dispose();

        final BufferedImage img = UIUtil.createImage(w, h, Transparency.TRANSLUCENT);
        final int offX = icon.getIconWidth() > maxWidth ? (icon.getIconWidth() - maxWidth) / 2 : 0;
        final int offY = icon.getIconHeight() > maxHeight ? (icon.getIconHeight() - maxHeight) / 2 : 0;
        for (int col = 0; col < w; col++) {
            for (int row = 0; row < h; row++) {
                img.setRGB(col, row, image.getRGB(col + offX, row + offY));
            }
        }

        return new ImageIcon(img);
    }

    public static Icon cropIcon( Icon icon, Rectangle area) {
        if (!new Rectangle(icon.getIconWidth(), icon.getIconHeight()).contains(area)) {
            return icon;
        }
        return new CropIcon(icon, area);
    }

    
    public static Icon flip( Icon icon, boolean horizontal) {
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();
        BufferedImage first = UIUtil.createImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = first.createGraphics();
        icon.paintIcon(new JPanel(), g, 0, 0);
        g.dispose();

        BufferedImage second = UIUtil.createImage(w, h, BufferedImage.TYPE_INT_ARGB);
        g = second.createGraphics();
        if (horizontal) {
            g.drawImage(first, 0, 0, w, h, w, 0, 0, h, null);
        }
        else {
            g.drawImage(first, 0, 0, w, h, 0, h, w, 0, null);
        }
        g.dispose();
        return new ImageIcon(second);
    }

    private static final NullableFunction<FileIconKey, Icon> ICON_NULLABLE_FUNCTION = new NullableFunction<FileIconKey, Icon>() {
        @Override
        public Icon fun(final FileIconKey key) {
            final VirtualFile file = key.getFile();
            final int flags = key.getFlags();
            final Project project = key.getProject();

            if (!file.isValid() || project != null && (project.isDisposed() || !wasEverInitialized(project))) return null;

            final Icon providersIcon = getProvidersIcon(file, flags, project);
            Icon icon = providersIcon == null ? VirtualFilePresentation.getIconImpl(file) : providersIcon;

            final boolean dumb = project != null && DumbService.getInstance(project).isDumb();
            for (FileIconPatcher patcher : getPatchers()) {
                if (dumb && !DumbService.isDumbAware(patcher)) {
                    continue;
                }

                icon = patcher.patchIcon(icon, file, flags, project);
            }

            if ((flags & Iconable.ICON_FLAG_READ_STATUS) != 0 &&
                    (!file.isWritable() || !WritingAccessProvider.isPotentiallyWritable(file, project))) {
                icon = new LayeredIcon(icon, PlatformIcons.LOCKED_ICON);
            }
            if (file.is(VFileProperty.SYMLINK)) {
                icon = new LayeredIcon(icon, PlatformIcons.SYMLINK_ICON);
            }

            Iconable.LastComputedIcon.put(file, icon, flags);

            return icon;
        }
    };

    public static Icon getIcon( final VirtualFile file, @Iconable.IconFlags final int flags,  final Project project) {
        Icon lastIcon = Iconable.LastComputedIcon.get(file, flags);

        final Icon base = lastIcon != null ? lastIcon : VirtualFilePresentation.getIconImpl(file);
        return IconDeferrer.getInstance().defer(base, new FileIconKey(file, project, flags), ICON_NULLABLE_FUNCTION);
    }

    
    private static Icon getProvidersIcon( VirtualFile file, @Iconable.IconFlags int flags, Project project) {
        for (FileIconProvider provider : getProviders()) {
            final Icon icon = provider.getIcon(file, flags, project);
            if (icon != null) return icon;
        }
        return null;
    }

    
    public static Icon getEmptyIcon(boolean showVisibility) {
        RowIcon baseIcon = new RowIcon(2);
        baseIcon.setIcon(createEmptyIconLike(PlatformIcons.CLASS_ICON_PATH), 0);
        if (showVisibility) {
            baseIcon.setIcon(createEmptyIconLike(PlatformIcons.PUBLIC_ICON_PATH), 1);
        }
        return baseIcon;
    }

    
    private static Icon createEmptyIconLike( String baseIconPath) {
        Icon baseIcon = IconLoader.findIcon(baseIconPath);
        if (baseIcon == null) {
            return EmptyIcon.ICON_16;
        }
        return new EmptyIcon(baseIcon.getIconWidth(), baseIcon.getIconHeight());
    }

    private static class FileIconProviderHolder {
        private static final FileIconProvider[] myProviders = Extensions.getExtensions(FileIconProvider.EP_NAME);
    }

    private static FileIconProvider[] getProviders() {
        return FileIconProviderHolder.myProviders;
    }

    private static class FileIconPatcherHolder {
        private static final FileIconPatcher[] ourPatchers = Extensions.getExtensions(FileIconPatcher.EP_NAME);
    }

    private static FileIconPatcher[] getPatchers() {
        return FileIconPatcherHolder.ourPatchers;
    }

    public static Image toImage( Icon icon) {
        if (icon instanceof ImageIcon) {
            return ((ImageIcon)icon).getImage();
        }
        else {
            final int w = icon.getIconWidth();
            final int h = icon.getIconHeight();
            final BufferedImage image = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(w, h, Transparency.TRANSLUCENT);
            final Graphics2D g = image.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            return image;
        }
    }

    public static Icon getAddIcon() {
        return getToolbarDecoratorIcon("add.png");
    }

    public static Icon getRemoveIcon() {
        return getToolbarDecoratorIcon("remove.png");
    }

    public static Icon getMoveUpIcon() {
        return getToolbarDecoratorIcon("moveUp.png");
    }

    public static Icon getMoveDownIcon() {
        return getToolbarDecoratorIcon("moveDown.png");
    }

    public static Icon getEditIcon() {
        return getToolbarDecoratorIcon("edit.png");
    }

    public static Icon getAddClassIcon() {
        return getToolbarDecoratorIcon("addClass.png");
    }

    public static Icon getAddPatternIcon() {
        return getToolbarDecoratorIcon("addPattern.png");
    }

    public static Icon getAddJiraPatternIcon() {
        return getToolbarDecoratorIcon("addJira.png");
    }

    public static Icon getAddYouTrackPatternIcon() {
        return getToolbarDecoratorIcon("addYouTrack.png");
    }

    public static Icon getAddBlankLineIcon() {
        return getToolbarDecoratorIcon("addBlankLine.png");
    }

    public static Icon getAddPackageIcon() {
        return getToolbarDecoratorIcon("addPackage.png");
    }

    public static Icon getAddLinkIcon() {
        return getToolbarDecoratorIcon("addLink.png");
    }

    public static Icon getAddFolderIcon() {
        return getToolbarDecoratorIcon("addFolder.png");
    }

    public static Icon getAnalyzeIcon() {
        return getToolbarDecoratorIcon("analyze.png");
    }

    public static void paintInCenterOf( Component c, Graphics g, Icon icon) {
        final int x = (c.getWidth() - icon.getIconWidth()) / 2;
        final int y = (c.getHeight() - icon.getIconHeight()) / 2;
        icon.paintIcon(c, g, x, y);
    }

    public static Icon getToolbarDecoratorIcon(String name) {
        return IconLoader.getIcon(getToolbarDecoratorIconsFolder() + name);
    }

    private static String getToolbarDecoratorIconsFolder() {
        return "/toolbarDecorator/" + (SystemInfo.isMac ? "mac/" : "");
    }

    /**
     * Result icons look like original but have equal (maximum) size
     */
    
    public static Icon[] getEqualSizedIcons( Icon... icons) {
        Icon[] result = new Icon[icons.length];
        int width = 0;
        int height = 0;
        for (Icon icon : icons) {
            width = Math.max(width, icon.getIconWidth());
            height = Math.max(height, icon.getIconHeight());
        }
        for (int i = 0; i < icons.length; i++) {
            result[i] = new IconSizeWrapper(icons[i], width, height);
        }
        return result;
    }

    public static Icon toSize( Icon icon, int width, int height) {
        return new IconSizeWrapper(icon, width, height);
    }

    private static class IconSizeWrapper implements Icon {
        private final Icon myIcon;
        private final int myWidth;
        private final int myHeight;

        private IconSizeWrapper( Icon icon, int width, int height) {
            myIcon = icon;
            myWidth = width;
            myHeight = height;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            x += (myWidth - myIcon.getIconWidth()) / 2;
            y += (myHeight - myIcon.getIconHeight()) / 2;
            myIcon.paintIcon(c, g, x, y);
        }

        @Override
        public int getIconWidth() {
            return myWidth;
        }

        @Override
        public int getIconHeight() {
            return myHeight;
        }
    }

    private static class CropIcon implements Icon {
        private final Icon mySrc;
        private final Rectangle myCrop;

        private CropIcon( Icon src, Rectangle crop) {
            mySrc = src;
            myCrop = crop;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            mySrc.paintIcon(c, g, x - myCrop.x, y - myCrop.y);
        }

        @Override
        public int getIconWidth() {
            return myCrop.width;
        }

        @Override
        public int getIconHeight() {
            return myCrop.height;
        }
    }

    public static Icon scale( final Icon source, double _scale) {
        final int hiDPIscale;
        if (source instanceof ImageIcon) {
            Image image = ((ImageIcon)source).getImage();
            hiDPIscale =   image instanceof JBHiDPIScaledImage ? 2 : 1;
        } else {
            hiDPIscale = 1;
        }
        final double scale = Math.min(32, Math.max(.1, _scale));
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D)g.create();
                try {
                    AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
                    transform.preConcatenate(g2d.getTransform());
                    g2d.setTransform(transform);
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    source.paintIcon(c, g2d, x, y);
                } finally {
                    g2d.dispose();
                }
            }

            @Override
            public int getIconWidth() {
                return (int)(source.getIconWidth() * scale) / hiDPIscale;
            }

            @Override
            public int getIconHeight() {
                return (int)(source.getIconHeight() * scale) / hiDPIscale;
            }
        };

    }

    
    public static Icon colorize( final Icon source,  Color color) {
        return colorize(source, color, false);
    }

    
    public static Icon colorize( final Icon source,  Color color, boolean keepGray) {
        float[] base = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

        final BufferedImage image = UIUtil.createImage(source.getIconWidth(), source.getIconHeight(), Transparency.TRANSLUCENT);
        final Graphics2D g = image.createGraphics();
        source.paintIcon(null, g, 0, 0);
        g.dispose();

        final BufferedImage img = UIUtil.createImage(source.getIconWidth(), source.getIconHeight(), Transparency.TRANSLUCENT);
        int[] rgba = new int[4];
        float[] hsb = new float[3];
        for (int y = 0; y < image.getRaster().getHeight(); y++) {
            for (int x = 0; x < image.getRaster().getWidth(); x++) {
                image.getRaster().getPixel(x, y, rgba);
                if (rgba[3] != 0) {
                    Color.RGBtoHSB(rgba[0], rgba[1], rgba[2], hsb);
                    int rgb = Color.HSBtoRGB(base[0], base[1] * (keepGray ? hsb[1] : 1f), base[2] * hsb[2]);
                    img.getRaster().setPixel(x, y, new int[]{rgb >> 16 & 0xff, rgb >> 8 & 0xff, rgb & 0xff, rgba[3]});
                }
            }
        }

        return new JBImageIcon(img) {
            @Override
            public int getIconWidth() {
                return getImage() instanceof JBHiDPIScaledImage ? super.getIconWidth() / 2 : super.getIconWidth();
            }

            @Override
            public int getIconHeight() {
                return getImage() instanceof JBHiDPIScaledImage ? super.getIconHeight() / 2: super.getIconHeight();
            }
        };
    }
}