package proxy.registry;

import junit.framework.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by thierry.fu on 2018/6/13.
 */
public class ETCDRegistryTest {

    @Test
    public void testConnect() throws Exception {
        ETCDRegistry etcdRegistry = new ETCDRegistry("http://127.0.0.1:2379");
        etcdRegistry.register("aaa", 90, "1");
        List<Endpoint> list = etcdRegistry.find("aaa");
        Assert.assertEquals(1,list.size());
    }


}
