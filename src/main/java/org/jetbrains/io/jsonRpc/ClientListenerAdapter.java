package org.jetbrains.io.jsonRpc;




import java.util.List;
import java.util.Map;

public abstract class ClientListenerAdapter implements ClientListener {
  @Override
  public void connected( Client client,  Map<String, List<String>> parameters) {
  }

  @Override
  public void disconnected( Client client) {
  }
}
