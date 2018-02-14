package org.jetbrains.io;

import com.gome.maven.util.text.CharArrayCharSequence;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.ByteBufUtilEx;



import java.io.IOException;
import java.nio.CharBuffer;

public final class ChannelBufferToString {

  public static CharSequence readChars( ByteBuf buffer) throws IOException {
    return new MyCharArrayCharSequence(readIntoCharBuffer(buffer, buffer.readableBytes(), null));
  }

  @SuppressWarnings("unused")

  public static CharSequence readChars( ByteBuf buffer, int byteCount) throws IOException {
    return new MyCharArrayCharSequence(readIntoCharBuffer(buffer, byteCount, null));
  }


  public static CharBuffer readIntoCharBuffer( ByteBuf buffer, int byteCount,  CharBuffer charBuffer) throws IOException {
    if (charBuffer == null) {
      charBuffer = CharBuffer.allocate(byteCount);
    }
    ByteBufUtilEx.readUtf8(buffer, byteCount, charBuffer);
    return charBuffer;
  }

  public static void writeIntAsAscii(int value,  ByteBuf buffer) {
    ByteBufUtil.writeAscii(buffer, new StringBuilder().append(value));
  }

  // we must return string on subSequence() - JsonReaderEx will call toString in any case
  public static final class MyCharArrayCharSequence extends CharArrayCharSequence {
    public MyCharArrayCharSequence( CharBuffer charBuffer) {
      super(charBuffer.array(), charBuffer.arrayOffset(), charBuffer.position());
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      return start == 0 && end == length() ? this : new String(myChars, myStart + start, end - start);
    }
  }
}