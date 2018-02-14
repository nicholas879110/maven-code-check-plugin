package org.jetbrains.io.jsonRpc;




import java.util.EventListener;
import java.util.List;
import java.util.Map;

public interface ClientListener extends EventListener {
  void connected( Client client,  Map<String, List<String>> parameters);

  void disconnected( Client client);
}
