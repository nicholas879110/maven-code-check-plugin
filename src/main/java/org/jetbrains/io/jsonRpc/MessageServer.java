package org.jetbrains.io.jsonRpc;



import java.io.IOException;

public interface MessageServer {
  void messageReceived( Client client,  CharSequence message, boolean isBinary) throws IOException;
}