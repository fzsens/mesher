package srv.client;

import io.netty.buffer.ByteBuf;
import org.junit.Test;
import proxy.core.ClientConfig;
import proxy.core.ProxyClient;
import proxy.core.connect.ClientConnector;
import proxy.core.connect.channel.ClientChannel;
import proxy.core.connect.channel.RequestChannel;
import srv.codec.DubboClientConnector;
import srv.codec.model.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Arrays;
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


        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("hash");
        invocation.setAttachment("path", "com.alibaba.dubbo.performance.demo.provider.IHelloService");
        invocation.setParameterTypes("Ljava/lang/String;");    // Dubbo内部用"Ljava/lang/String"来表示参数类型是String

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        JsonUtils.writeObject("1234", writer);
        invocation.setArguments(out.toByteArray());

        Request request = new Request();
        request.setVersion("2.0.0");
        request.setTwoWay(true);
        request.setData(invocation);

        channel.sendAsyncRequest(request, new RequestChannel.Listener() {
            @Override
            public void onRequestSent() {
                System.out.println("sned");
            }

            @Override
            public void onResponseReceived(ByteBuf byteBuf) {
                byte[] data = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(data);

                byte[] subArray = Arrays.copyOfRange(data, 16 + 1, data.length);

                String s = new String(subArray);

                byte[] requestIdBytes = Arrays.copyOfRange(data,4,12);
                long requestId = Bytes.bytes2long(requestIdBytes, 0);
                RpcResponse response = new RpcResponse();
                response.setRequestId(String.valueOf(requestId));
                response.setBytes(subArray);
                System.out.println("reces" + byteBuf);
            }

            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
            }
        });
        System.out.println("1234".hashCode());
        TimeUnit.SECONDS.sleep(5);

    }
}
