package proxy.handler;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proxy.codec.RequestParser;
import proxy.loadbalance.ProxyLoadBalance;
import proxy.registry.ETCDRegistry;
import proxy.registry.Endpoint;
import proxy.registry.IRegistry;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * tackle request and dispatcher to {@link ProxyToServerChannel}
 * Created by fzsens on 2018/5/30.
 */
@ChannelHandler.Sharable
public class ClientToProxyChannel extends AbstractProxyChannel {

    private Logger log = LoggerFactory.getLogger(ClientToProxyChannel.class);

    private ProxyLoadBalance<Endpoint> proxyLoadBalance = new ProxyLoadBalance<>();

    private static Map<Endpoint, ProxyToServerChannel> servers = new HashMap<>();

    public ClientToProxyChannel(ChannelPipeline pipeline) {
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
            IRegistry registry = new ETCDRegistry();
            Map<Endpoint, Integer> endpointMap = registry.find(paramMap.get("interface"));
            Endpoint endpoint = proxyLoadBalance.select(endpointMap);
            ProxyToServerChannel serverChannel = servers.get(endpoint);
            if (serverChannel == null || !serverChannel.isConnected()) {
                serverChannel =
                        new ProxyToServerChannel(this, new InetSocketAddress(endpoint.getHost(), endpoint.getPort()));
                servers.put(endpoint, serverChannel);
            }
            byte[] bytes = JSON.toJSONString(paramMap).getBytes();
            ChannelFuture future = serverChannel.doWrite(Unpooled.wrappedBuffer(bytes));
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
