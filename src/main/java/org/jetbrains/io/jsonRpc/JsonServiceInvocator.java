package org.jetbrains.io.jsonRpc;


import org.jetbrains.io.JsonReaderEx;

import java.io.IOException;

public interface JsonServiceInvocator {
  void invoke( String command,  Client client,  JsonReaderEx reader, int messageId) throws IOException;
}
