package com.gome.maven.plugin.code.pmd.inspection;

public class CompilerMessage {

    /**
     * The kind of message.
     */
    private Kind kind;

    /**
     * The start line number of the offending program text
     */
    private int startline;

    /**
     * The start column number of the offending program text
     */
    private int startcolumn;

    /**
     * The end line number of the offending program text
     */
    private int endline;

    /**
     * The end column number of the offending program text
     */
    private int endcolumn;

    /**
     * The name of the file containing the offending program text
     */
    private String file;

    /**
     * The actual message text produced by the language processor
     */
    private String message;


    /**
     * Constructs a compiler message.
     *
     * @param file        The name of the file containing the offending program text
     * @param error       <code>true</code> if this is a error message, or <code>false</code> if it
     *                    is a warning message
     * @param startline   The start line number of the offending program text
     * @param startcolumn The start column number of the offending program text
     * @param endline     The end line number of the offending program text
     * @param endcolumn   The end column number of the offending program text
     * @param message     The actual message text produced by the language processor
     * @deprecated Use {@link #CompilerMessage(String, Kind, int, int, int, int, String)} instead
     */
    @Deprecated
    public CompilerMessage( String file, boolean error, int startline, int startcolumn, int endline, int endcolumn,
                            String message )
    {
        this.file = file;
        this.kind = error ? Kind.ERROR : Kind.WARNING;
        this.startline = startline;
        this.startcolumn = startcolumn;
        this.endline = endline;
        this.endcolumn = endcolumn;
        this.message = message;
    }

    /**
     * Constructs a compiler message.
     *
     * @param file        The name of the file containing the offending program text
     * @param kind        The kind of message
     * @param startline   The start line number of the offending program text
     * @param startcolumn The start column number of the offending program text
     * @param endline     The end line number of the offending program text
     * @param endcolumn   The end column number of the offending program text
     * @param message     The actual message text produced by the language processor
     */
    public CompilerMessage( String file, Kind kind, int startline, int startcolumn, int endline, int endcolumn,
                            String message )
    {
        this.file = file;
        this.kind = kind;
        this.startline = startline;
        this.startcolumn = startcolumn;
        this.endline = endline;
        this.endcolumn = endcolumn;
        this.message = message;
    }

    /**
     * The warning message constructor.
     *
     * @param message The actual message text produced by the language processor
     * @deprecated Use {@link #CompilerMessage(String, Kind)} instead
     */
    @Deprecated
    public CompilerMessage( String message )
    {
        this.message = message;
        this.kind = Kind.WARNING;
    }

    /**
     * Constructs a compiler message.
     *
     * @param message The actual message text produced by the language processor
     * @param error   <code>true</code> if this is a error message, or <code>false</code> if it
     *                is a warning message
     * @deprecated Use {@link #CompilerMessage(String, Kind)} instead
     */
    @Deprecated
    public CompilerMessage( String message, boolean error )
    {
        this.message = message;
        this.kind = error ? Kind.ERROR : Kind.WARNING;
    }

    /**
     * Constructs a compiler message.
     *
     * @param message The actual message text produced by the language processor
     * @param kind    The kind of message
     * @since 2.0
     */
    public CompilerMessage( String message, Kind kind )
    {
        this.message = message;
        this.kind = kind;
    }

    /**
     * Returns the filename associated with this compiler message.
     *
     * @return The filename associated with this compiler message
     */
    public String getFile()
    {
        return file;
    }

    /**
     * Asserts whether this is an error message or not.
     *
     * @return Whether the message is an error message
     */
    public boolean isError()
    {
        return kind == Kind.ERROR;
    }

    /**
     * Returns the starting line number of the program text originating this compiler
     * message.
     *
     * @return The starting line number of the program text originating this message
     */
    public int getStartLine()
    {
        return startline;
    }

    /**
     * Returns the starting column number of the program text originating this
     * compiler message.
     *
     * @return The starting column number of the program text originating this
     *         message
     */
    public int getStartColumn()
    {
        return startcolumn;
    }

    /**
     * Return the ending line number of the program text originating this compiler
     * message.
     *
     * @return The ending line number of the program text originating this message
     */
    public int getEndLine()
    {
        return endline;
    }

    /**
     * Returns the ending column number of the program text originating this
     * compiler message.
     *
     * @return The ending column number of the program text originating this
     *         message
     */
    public int getEndColumn()
    {
        return endcolumn;
    }

    /**
     * Returns the message produced by the language processor.
     *
     * @return The message produced by the language processor
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Returns the kind of the compiler message.
     *
     * @return the kind of the message
     * @since 2.0
     */
    public Kind getKind()
    {
        return kind;
    }

    public String toString()
    {
        if ( file != null )
        {
            if ( startline != 0 )
            {
                if ( startcolumn != 0 )
                {
                    return file + ":" + "[" + startline + "," + startcolumn + "] " + message;
                }
                else
                {
                    return file + ":" + "[" + startline + "] " + message;
                }
            }
            else
            {
                return file + ": " + message;
            }
        }
        else
        {
            return message;
        }
    }

    /**
     * As we are still 1.5 required we use a wrapper to Diagnostic.Kind and some compilers don't know jdk constants.
     *
     * @since 2.0
     */
    public enum Kind
    {
        /**
         * Problem which prevents the tool's normal completion.
         */
        ERROR( "error" ),
        /**
         * Problem similar to a warning, but is mandated by the tool's specification.
         */
        MANDATORY_WARNING( "mandatory_warning" ),
        /**
         * Informative message from the tool.
         */
        NOTE( "note" ),
        /**
         * Diagnostic which does not fit within the other kinds.
         */
        OTHER( "other" ),
        /**
         * Problem which does not usually prevent the tool from completing normally.
         */
        WARNING( "warning" );

        private String type;

        private Kind( String type )
        {
            this.type = type;
        }
    }
}
