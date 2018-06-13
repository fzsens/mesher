package proxy.loadbalance;

import org.junit.Test;
import proxy.registry.Endpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by thierry.fu on 2018/6/1.
 */
public class ProxyLoadBalanceTests {


    @Test
    public void testSelect() {
        ProxyLoadBalance loadBalance = new ProxyLoadBalance();
        List<Endpoint> endpoints = new ArrayList<>();
        endpoints.add(new Endpoint("a",1,2));
        endpoints.add(new Endpoint("b",1,3));
        endpoints.add(new Endpoint("c",1,5));
        int aTimes = 0;
        int bTimes = 0;
        int cTimes = 0;
        loadBalance.init(endpoints);
        for(int i = 0 ; i < 1000000;i++){
            Endpoint s = loadBalance.select();
            if(s.getHost().equals("a")) aTimes++;
            if(s.getHost().equals("b")) bTimes++;
            if(s.getHost().equals("c")) cTimes++;
        }
        System.out.println("aTime : " + aTimes );
        System.out.println("bTimes : " + bTimes );
        System.out.println("cTimes : " + cTimes );
    }
}
