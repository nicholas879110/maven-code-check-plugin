//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gome.maven.lexer;

import com.gome.maven.psi.tree.IElementType;

public abstract class Lexer {
    public Lexer() {
    }

    public abstract void start( CharSequence var1, int var2, int var3, int var4);

    public final void start( CharSequence buf, int start, int end) {
        this.start(buf, start, end, 0);
    }

    public final void start( CharSequence buf) {
        this.start(buf, 0, buf.length(), 0);
    }

    
    public CharSequence getTokenSequence() {
        return this.getBufferSequence().subSequence(this.getTokenStart(), this.getTokenEnd());
    }

    
    public String getTokenText() {
        return this.getTokenSequence().toString();
    }

    public abstract int getState();

    
    public abstract IElementType getTokenType();

    public abstract int getTokenStart();

    public abstract int getTokenEnd();

    public abstract void advance();

    
    public abstract LexerPosition getCurrentPosition();

    public abstract void restore( LexerPosition var1);

    
    public abstract CharSequence getBufferSequence();

    public abstract int getBufferEnd();
}
