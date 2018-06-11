package proxy.handler;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private ProxyLoadBalance<Endpoint> proxyLoadBalance = new ProxyLoadBalance<>();

    public ClientProxyHandler(ChannelPipeline pipeline) {
        initChannelPipeline(pipeline);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    void doRead(Object request) {
        if (request instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) request;
            Map<String, String> paramMap = RequestParser.parse(fullHttpRequest);
            byte[] bytes = JSON.toJSONString(paramMap).getBytes();
            IRegistry registry = new ETCDRegistry();
            Map<Endpoint, Integer> endpointMap = registry.find(paramMap.get("interface"));
            proxyLoadBalance.init(endpointMap);
            Endpoint endpoint = proxyLoadBalance.select();
            ClientConfig config = new ClientConfig(new InetSocketAddress(endpoint.getHost(), endpoint.getPort()));
            ProxyClient client = new ProxyClient(config);
            try {
                client.connectAsync().get().sendAsyncRequest(Unpooled.copiedBuffer(bytes), new RequestChannel.Listener() {
                    @Override
                    public void onRequestSent() {
                        // statistic
                    }
                    @Override
                    public void onResponseReceived(Object msg) {
                        if (msg instanceof ByteBuf) {
                            ByteBuf message = (ByteBuf) msg;
                            byte[] CONTENT = new byte[message.readableBytes()];
                            message.readBytes(CONTENT);
                            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(CONTENT));
                            response.headers().set(CONTENT_TYPE, "application/json");
                            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
                            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                            ctx.writeAndFlush(response);
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
    }

    private void initChannelPipeline(ChannelPipeline pipeline) {
        log.info("init nettyChannel pipeline");
        pipeline.addLast("codec", new HttpServerCodec());
        pipeline.addLast("aggregator", new HttpObjectAggregator(CONTENT_LENGTH_D));
        pipeline.addLast("idle", new IdleStateHandler(0, 0, IDLE_TIMEOUT));
        pipeline.addLast("handler", this);
    }
}
