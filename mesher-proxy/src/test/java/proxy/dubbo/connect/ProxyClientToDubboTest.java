package proxy.dubbo.connect;

import org.junit.Test;
import protocol.dubbo.DubboClientConnector;
import protocol.dubbo.model.DubboRpcInvocation;
import protocol.dubbo.model.DubboRpcRequest;
import protocol.dubbo.model.DubboRpcResponse;
import protocol.dubbo.model.JsonUtils;
import proxy.core.ClientConfig;
import proxy.core.ProxyClient;
import proxy.core.connect.ClientConnector;
import proxy.core.connect.channel.ClientChannel;
import proxy.core.connect.channel.RequestChannel;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by fzsens on 6/10/18.
 */
public class ProxyClientToDubboTest {

    @Test
    public void testConnect() throws Exception {
        ClientConfig config = new ClientConfig(new HashMap<>(), new InetSocketAddress("127.0.0.1", 20880), 2);
        ProxyClient client = new ProxyClient(config);
        ClientConnector<ClientChannel> defaultConnector = new DubboClientConnector(new InetSocketAddress("127.0.0.1", 20880));
        RequestChannel channel = client.connectAsync(defaultConnector).get();
        long start = System.currentTimeMillis();
        int size = 10000;
        final CountDownLatch countDownLatch = new CountDownLatch(size);
        for(int i = 0; i< size;i++) {
            final String param = "1234" + i;
            DubboRpcInvocation invocation = new DubboRpcInvocation();
            invocation.setMethodName("hash");
            invocation.setAttachment("path", "com.alibaba.dubbo.performance.demo.provider.IHelloService");
            invocation.setParameterTypes("Ljava/lang/String;");    // Dubbo内部用"Ljava/lang/String"来表示参数类型是String
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
            JsonUtils.writeObject(param, writer);
            invocation.setArguments(out.toByteArray());
            DubboRpcRequest request = new DubboRpcRequest();
            request.setVersion("2.0.0");
            request.setTwoWay(true);
            request.setData(invocation);
            channel.sendAsyncRequest(request, new RequestChannel.Listener() {
                @Override
                public void onRequestSent() {
                   // System.out.println("sent");
                }

                @Override
                public void onResponseReceived(Object resp) {
                    if (resp instanceof DubboRpcResponse) {
                        DubboRpcResponse response = (DubboRpcResponse) resp;
                        String s = new String(response.getBytes());
                        if ((param.hashCode() + "").equals(s.trim())) {

                        } else {
                            System.out.println("error  ... " + s);
                        }
                        countDownLatch.countDown();
                    }
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
        countDownLatch.await();
        System.out.println(System.currentTimeMillis() - start);
        TimeUnit.SECONDS.sleep(5);

    }
}