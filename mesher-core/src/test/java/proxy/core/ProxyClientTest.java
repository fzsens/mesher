package proxy.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import proxy.core.connect.ClientFuture;
import proxy.core.connect.DefaultClientConnector;
import proxy.core.connect.channel.ClientChannel;
import proxy.core.connect.channel.RequestChannel;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by fzsens on 6/9/18.
 */
public class ProxyClientTest {

    private ProxyClient client ;

    @Before
    public void setUp() {
        ClientConfig config = new ClientConfig(new HashMap<>(),new InetSocketAddress("127.0.0.1", 20001),2);

        client = new ProxyClient(config);
    }

    @Test
    public void assertNull() throws Exception {
        ClientFuture<ClientChannel> future =
                client.connectAsync(new DefaultClientConnector(new InetSocketAddress("127.0.0.1",20001)));
        ClientChannel channel = future.get();
        Assert.assertNotNull(channel);
        channel.sendAsyncRequest(Unpooled.wrappedBuffer("aaa".getBytes()), new RequestChannel.Listener() {
            @Override
            public void onRequestSent() {
                System.out.println("msg sent");
            }
            @Override
            public void onResponseReceived(Object response) {
                if(response instanceof ByteBuf) {
                    ByteBuf message = (ByteBuf)response;
                    int size = message.readableBytes();
                    byte [] bytes = new byte[size];
                    message.readBytes(bytes);
                    String str = new String(bytes);
                    System.out.println("response received : " + str);
                }

            }
            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
                System.out.println("error");
            }
        });

        TimeUnit.SECONDS.sleep(3);
    }

    @After
    public void after(){
        client.close();
    }
}
