package proxy.registry;

import java.util.List;

public interface IRegistry {

    void register(String serviceName, int port, String weight) throws Exception;

    List<Endpoint> find(String serviceName) throws Exception;

}
