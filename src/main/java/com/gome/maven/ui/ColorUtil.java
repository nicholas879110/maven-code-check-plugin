//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gome.maven.ui;

import com.gome.maven.util.ui.UIUtil;
import java.awt.Color;
import java.lang.annotation.Annotation;

public class ColorUtil {
    private ColorUtil() {
    }

    public static Color softer( Color color) {
        if (color.getBlue() > 220 && color.getRed() > 220 && color.getGreen() > 220) {
            return color;
        } else {
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), (float[])null);
            return Color.getHSBColor(hsb[0], 0.6F * hsb[1], hsb[2]);
        }
    }

    public static Color darker( Color color, int tones) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), (float[])null);
        float brightness = hsb[2];

        for(int i = 0; i < tones; ++i) {
            brightness = Math.max(0.0F, brightness / 1.1F);
            if (brightness == 0.0F) {
                break;
            }
        }

        return Color.getHSBColor(hsb[0], hsb[1], brightness);
    }

    public static Color brighter( Color color, int tones) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), (float[])null);
        float brightness = hsb[2];

        for(int i = 0; i < tones; ++i) {
            brightness = Math.min(1.0F, brightness * 1.1F);
            if (brightness == 1.0F) {
                break;
            }
        }

        return Color.getHSBColor(hsb[0], hsb[1], brightness);
    }

    public static Color dimmer( Color color) {
        float[] rgb = color.getRGBColorComponents((float[])null);
        float alpha = 0.8F;
        float rem = 1.0F - alpha;
        return new Color(rgb[0] * alpha + rem, rgb[1] * alpha + rem, rgb[2] * alpha + rem);
    }

    private static int shift(int colorComponent, double d) {
        int n = (int)((double)colorComponent * d);
        return n > 255 ? 255 : (n < 0 ? 0 : n);
    }

    public static Color shift(Color c, double d) {
        return new Color(shift(c.getRed(), d), shift(c.getGreen(), d), shift(c.getBlue(), d), c.getAlpha());
    }

    public static Color withAlpha(Color c, double a) {
        return toAlpha(c, (int)(255.0D * a));
    }

    public static Color toAlpha(Color color, int a) {
        Color c = color != null ? color : Color.black;
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    public static Color withAlphaAdjustingDarkness(Color c, double d) {
        return shift(withAlpha(c, d), d);
    }

    public static String toHex( Color c) {
        String R = Integer.toHexString(c.getRed());
        String G = Integer.toHexString(c.getGreen());
        String B = Integer.toHexString(c.getBlue());
        return (R.length() < 2 ? "0" : "") + R + (G.length() < 2 ? "0" : "") + G + (B.length() < 2 ? "0" : "") + B;
    }

    public static Color fromHex(String str) {
        if (str.startsWith("#")) {
            str = str.substring(1);
        }

        if (str.length() == 3) {
            return new Color(17 * Integer.valueOf(String.valueOf(str.charAt(0)), 16).intValue(), 17 * Integer.valueOf(String.valueOf(str.charAt(1)), 16).intValue(), 17 * Integer.valueOf(String.valueOf(str.charAt(2)), 16).intValue());
        } else if (str.length() == 6) {
            return Color.decode("0x" + str);
        } else {
            throw new IllegalArgumentException("Should be String of 3 or 6 chars length.");
        }
    }

    
    public static Color fromHex(String str,  Color defaultValue) {
        try {
            return fromHex(str);
        } catch (Exception var3) {
            return defaultValue;
        }
    }

    
    public static Color getColor( Class<?> cls) {
        Annotation annotation = cls.getAnnotation(Colored.class);
        if (annotation instanceof Colored) {
            Colored colored = (Colored)annotation;
            return fromHex(UIUtil.isUnderDarcula() ? colored.darkVariant() : colored.color(), (Color)null);
        } else {
            return null;
        }
    }

    public static boolean isDark( Color c) {
        return 1.0D - (0.299D * (double)c.getRed() + 0.587D * (double)c.getGreen() + 0.114D * (double)c.getBlue()) / 255.0D >= 0.5D;
    }

    
    public static Color mix( Color c1,  Color c2, double balance) {
        balance = Math.min(1.0D, Math.max(0.0D, balance));
        return new Color((int)((1.0D - balance) * (double)c1.getRed() + (double)c2.getRed() * balance + 0.5D), (int)((1.0D - balance) * (double)c1.getGreen() + (double)c2.getGreen() * balance + 0.5D), (int)((1.0D - balance) * (double)c1.getBlue() + (double)c2.getBlue() * balance + 0.5D), (int)((1.0D - balance) * (double)c1.getAlpha() + (double)c2.getAlpha() * balance + 0.5D));
    }
}
