package srv.handler;

import com.alibaba.fastjson.JSON;
import com.sun.org.apache.bcel.internal.ExceptionConstants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateHandler;
import proxy.core.connect.channel.RequestChannel;
import srv.protocol.dubbo.model.JsonUtils;
import srv.protocol.dubbo.model.RpcInvocation;
import srv.protocol.dubbo.model.RpcRequest;
import srv.protocol.dubbo.model.RpcResponse;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fzsens on 6/3/18.
 */
@ChannelHandler.Sharable
public class SrvHandler extends ChannelInboundHandlerAdapter {

    private RequestChannel channel;

    public SrvHandler(RequestChannel channel) {
        this.channel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        int size = byteBuf.readableBytes();
        byte[] bytes = new byte[size];
        byteBuf.readBytes(bytes);
        String param = new String(bytes);
        HashMap<String, String> paramMap = JSON.parseObject(param, HashMap.class);
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName(paramMap.get("method"));
        invocation.setAttachment("path", paramMap.get("interface"));
        invocation.setParameterTypes(paramMap.get("parameterTypesString"));    // Dubbo内部用"Ljava/lang/String"来表示参数类型是String
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        JsonUtils.writeObject(paramMap.get("parameter"), writer);
        invocation.setArguments(out.toByteArray());

        RpcRequest request = new RpcRequest();
        request.setVersion("2.0.0");
        request.setTwoWay(true);
        request.setData(invocation);
        channel.sendAsyncRequest(request, new RequestChannel.Listener() {
            @Override
            public void onRequestSent() {
                System.out.println("sned");
            }

            @Override
            public void onResponseReceived(Object resp) {
                if (resp instanceof RpcResponse) {
                    RpcResponse response = (RpcResponse) resp;
                    ChannelFuture future = ctx.writeAndFlush(Unpooled.copiedBuffer(response.getBytes()));
                    System.out.println(future);
                }
            }
            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
            }
        });
//        byte[] CONTENT = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};
//        ChannelFuture future =;
//        System.out.println(future.get());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }
}
