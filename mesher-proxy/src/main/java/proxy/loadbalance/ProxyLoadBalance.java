package proxy.loadbalance;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by thierry.fu on 2018/5/31.
 */
public class ProxyLoadBalance<T> {

    AtomicInteger cursor = new AtomicInteger(0);

    List<T> proxyList = new ArrayList<>();

    public T select() {
        return proxyList.get(cursor.addAndGet(1) % proxyList.size());
    }

    public void init(Map<T, Integer> channelsMap) {
        Set<T> keySet = channelsMap.keySet();
        List<T> channels = new ArrayList<>();
        for (T channel : keySet) {
            Integer weight = channelsMap.get(channel);
            for (int i = 0; i < weight; i++) {
                channels.add(channel);
            }
        }
        Collections.shuffle(channels);
        proxyList = channels;
    }
}
