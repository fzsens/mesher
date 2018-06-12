package srv.handler;

import com.google.protobuf.ByteString;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protocol.dubbo.protobuf.MesherProtoDubbo;
import proxy.core.connect.channel.RequestChannel;
import srv.protocol.dubbo.model.DubboRpcInvocation;
import srv.protocol.dubbo.model.DubboRpcRequest;
import srv.protocol.dubbo.model.DubboRpcResponse;
import srv.protocol.dubbo.model.JsonUtils;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Created by fzsens on 6/3/18.
 */
@ChannelHandler.Sharable
public class SrvHandler extends ChannelInboundHandlerAdapter {

    private RequestChannel channel;

    private Logger log = LoggerFactory.getLogger(SrvHandler.class);

    public SrvHandler(RequestChannel channel) {
        this.channel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof MesherProtoDubbo.Request) {
            MesherProtoDubbo.Request request = (MesherProtoDubbo.Request) msg;
            DubboRpcInvocation invocation = new DubboRpcInvocation();
            invocation.setMethodName(request.getMethod());
            invocation.setAttachment("path", request.getInterfaceName());
            invocation.setParameterTypes(request.getParameterTypesString());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
            JsonUtils.writeObject(request.getParameter(), writer);
            invocation.setArguments(out.toByteArray());
            DubboRpcRequest dubboReq = new DubboRpcRequest();
            dubboReq.setVersion("2.0.0");
            dubboReq.setTwoWay(true);
            dubboReq.setData(invocation);
            channel.sendAsyncRequest(dubboReq, new RequestChannel.Listener() {
                @Override
                public void onRequestSent() {
                    log.debug("sent");
                }
                @Override
                public void onResponseReceived(Object message) {
                    if (message instanceof DubboRpcResponse) {
                        DubboRpcResponse dubboResponse = (DubboRpcResponse) message;
                        MesherProtoDubbo.Response protoDubboResp =
                                MesherProtoDubbo.Response.newBuilder()
                                        .setRequestId(dubboResponse.getRequestId())
                                        .setData(ByteString.copyFrom(dubboResponse.getBytes()))
                                        .build();
                        ctx.writeAndFlush(protoDubboResp);
                    }
                }
                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            });
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }
}
