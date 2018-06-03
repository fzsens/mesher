package proxy.loadbalance;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by thierry.fu on 2018/5/31.
 */
public class ProxyLoadBalance<T> {

    private final SecureRandom random = new SecureRandom();

    public T select(List<T> channels) {
        int position = random.nextInt(channels.size());
        return channels.get(position);
    }

    public T select(Map<T, Integer> channelsMap) {
        Set<T> keySet = channelsMap.keySet();
        List<T> channels = new ArrayList<>();
        for (T channel : keySet) {
            Integer weight = channelsMap.get(channel);
            for (int i = 0; i < weight; i++) {
                channels.add(channel);
            }
        }
        // Collections.shuffle(channels);
        return select(channels);
    }
}
