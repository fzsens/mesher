package proxy.registry;

import java.util.Map;

public interface IRegistry {

    Map<Endpoint,Integer> find(String serviceName);

}
