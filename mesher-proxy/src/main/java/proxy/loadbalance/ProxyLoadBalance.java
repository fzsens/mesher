package proxy.loadbalance;

import proxy.registry.Endpoint;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by thierry.fu on 2018/5/31.
 */
public class ProxyLoadBalance {

    AtomicInteger cursor = new AtomicInteger(0);

    List<Endpoint> proxyList = new ArrayList<>();

    public Endpoint select() {
        if(proxyList == null || proxyList.size() == 0) return null;
        return proxyList.get(cursor.addAndGet(1) % proxyList.size());
    }

    public void init(List<Endpoint> endpoints) {
        List <Endpoint> channels = new ArrayList<>();
        for (Endpoint endpoint : endpoints) {
            Integer weight = endpoint.getWeight();
            for (int i = 0; i < weight; i++) {
                channels.add(endpoint);
            }
        }
        Collections.shuffle(channels);
        proxyList = channels;
    }
}
