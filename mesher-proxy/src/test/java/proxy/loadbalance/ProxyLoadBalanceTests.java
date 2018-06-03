package proxy.loadbalance;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by thierry.fu on 2018/6/1.
 */
public class ProxyLoadBalanceTests {


    @Test
    public void testSelect() {
        ProxyLoadBalance<String> loadBalance = new ProxyLoadBalance<>();
        Map<String,Integer> map = new HashMap<>();
        map.put("a",2);
        map.put("b",3);
        map.put("c",5);
        int aTimes = 0;
        int bTimes = 0;
        int cTimes = 0;
        for(int i = 0 ; i < 1000000;i++){
           String s = loadBalance.select(map);
            if(s.equals("a")) aTimes++;
            if(s.equals("b")) bTimes++;
            if(s.equals("c")) cTimes++;
        }
        System.out.println("aTime : " + aTimes );
        System.out.println("bTimes : " + bTimes );
        System.out.println("cTimes : " + cTimes );
    }
}
