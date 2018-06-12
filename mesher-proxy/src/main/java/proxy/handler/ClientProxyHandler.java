package proxy.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protocol.dubbo.protobuf.MesherProtoDubbo;
import proxy.client.ProtobufClientConnector;
import proxy.codec.RequestParser;
import proxy.core.ClientConfig;
import proxy.core.ProxyClient;
import proxy.core.connect.channel.RequestChannel;
import proxy.loadbalance.ProxyLoadBalance;
import proxy.registry.ETCDRegistry;
import proxy.registry.Endpoint;
import proxy.registry.IRegistry;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * tackle request and dispatcher to {@link proxy.core.ProxyClient}
 * Created by fzsens on 2018/5/30.
 */
@ChannelHandler.Sharable
public class ClientProxyHandler extends AbstractProxyHandler {

    private Logger log = LoggerFactory.getLogger(ClientProxyHandler.class);

    private static AtomicLong SEQ_REQ_ID = new AtomicLong(0);

    private ProxyLoadBalance<Endpoint> proxyLoadBalance = new ProxyLoadBalance<>();

    private RequestChannel clientChannel;

    public ClientProxyHandler(ChannelPipeline pipeline) {
        initChannelPipeline(pipeline);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    void doRead(FullHttpRequest request) {
        Map<String, String> paramMap = RequestParser.parse(request);
        MesherProtoDubbo.Request protoDubboReq =
                MesherProtoDubbo.Request.newBuilder().setRequestId(SEQ_REQ_ID.addAndGet(1))
                        .setInterfaceName(paramMap.get("interface"))
                        .setMethod(paramMap.get("method"))
                        .setParameterTypesString(paramMap.get("parameterTypesString"))
                        .setParameter(paramMap.get("parameter")).build();
        asyncCall(protoDubboReq);
    }

    private void asyncCall(MesherProtoDubbo.Request protoDubboReq) {
        try {
            if (clientChannel == null) {
                IRegistry registry = new ETCDRegistry();
                Map<Endpoint, Integer> endpointMap = registry.find(protoDubboReq.getInterfaceName());
                proxyLoadBalance.init(endpointMap);
                Endpoint endpoint = proxyLoadBalance.select();
                ClientConfig config = new ClientConfig(new InetSocketAddress(endpoint.getHost(), endpoint.getPort()));
                ProxyClient client = new ProxyClient(config);
                clientChannel = client.connectAsync(new ProtobufClientConnector(config.getDefaultSocksProxyAddress())).get();
            }
            clientChannel.sendAsyncRequest(protoDubboReq, new RequestChannel.Listener() {
                @Override
                public void onRequestSent() {
                    // statistic
                }

                @Override
                public void onResponseReceived(Object msg) {
                    if (msg instanceof MesherProtoDubbo.Response) {
                        MesherProtoDubbo.Response response = (MesherProtoDubbo.Response) msg;
                        byte[] CONTENT = response.getData().toByteArray();
                        FullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(CONTENT));
                        httpResponse.headers().set(CONTENT_TYPE, "application/json");
                        httpResponse.headers().set(CONTENT_LENGTH, httpResponse.content().readableBytes());
                        httpResponse.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                        ctx.writeAndFlush(httpResponse);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    // statistic
                }
            });
        } catch (Exception e) {
            log.warn("send request failed! cause {}", e);
        }
    }

    private void initChannelPipeline(ChannelPipeline pipeline) {
        log.info("init nettyChannel pipeline");
        pipeline.addLast("codec", new HttpServerCodec());
        pipeline.addLast("aggregator", new HttpObjectAggregator(CONTENT_LENGTH_D));
        pipeline.addLast("idle", new IdleStateHandler(0, 0, IDLE_TIMEOUT));
        pipeline.addLast("handler", this);
    }
}
