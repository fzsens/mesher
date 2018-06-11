package proxy.registry;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fzsens on 6/3/18.
 */
public class ETCDRegistry implements IRegistry {

    @Override
    public Map<Endpoint, Integer> find(String serviceName) {
        Map<Endpoint,Integer> map = new HashMap<>();
        Endpoint endpoint = new Endpoint("127.0.0.1",20001,1);
        map.put(endpoint,1);
        return map;
    }
}
