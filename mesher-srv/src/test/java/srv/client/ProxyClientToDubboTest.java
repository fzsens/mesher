package srv.client;

import org.junit.Test;
import proxy.core.ClientConfig;
import proxy.core.ProxyClient;
import proxy.core.connect.ClientConnector;
import proxy.core.connect.channel.ClientChannel;
import proxy.core.connect.channel.RequestChannel;
import srv.protocol.dubbo.DubboClientConnector;
import srv.protocol.dubbo.model.JsonUtils;
import srv.protocol.dubbo.model.DubboRpcInvocation;
import srv.protocol.dubbo.model.DubboRpcRequest;
import srv.protocol.dubbo.model.DubboRpcResponse;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.HashMap;
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
        for(int i = 0; i< 2000000;i++) {

            DubboRpcInvocation invocation = new DubboRpcInvocation();
            invocation.setMethodName("hash");
            invocation.setAttachment("path", "com.alibaba.dubbo.performance.demo.provider.IHelloService");
            invocation.setParameterTypes("Ljava/lang/String;");    // Dubbo内部用"Ljava/lang/String"来表示参数类型是String

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
            JsonUtils.writeObject("1234" + i, writer);
            invocation.setArguments(out.toByteArray());

            DubboRpcRequest request = new DubboRpcRequest();
            request.setVersion("2.0.0");
            request.setTwoWay(true);
            request.setData(invocation);
            channel.sendAsyncRequest(request, new RequestChannel.Listener() {
                @Override
                public void onRequestSent() {
                    System.out.println("sent");
                }

                @Override
                public void onResponseReceived(Object resp) {
                    if (resp instanceof DubboRpcResponse) {
                        DubboRpcResponse response = (DubboRpcResponse) resp;
                        String s = new String(response.getBytes());
                        System.out.println("received resp " + s);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
        System.out.println(System.currentTimeMillis() - start);
        TimeUnit.SECONDS.sleep(5);

    }
}
