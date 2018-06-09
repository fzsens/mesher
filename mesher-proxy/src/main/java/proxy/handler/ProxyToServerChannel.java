package proxy.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import proxy.client.bootstrap.ClientBootstrap;
import proxy.client.bootstrap.DefaultClientBootstrap;

import java.net.SocketAddress;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * connection to mesher server
 * Created by fzsens on 2018/5/30.
 */
public class ProxyToServerChannel extends AbstractProxyChannel {

    private ClientToProxyChannel clientChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    void doRead(Object res) {
        byte[] CONTENT = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(CONTENT));
        response.headers().set(CONTENT_TYPE, "text/plain");
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        ChannelFuture f = clientChannel.doWrite(response);
    }

    public ProxyToServerChannel(ClientToProxyChannel clientChannel, SocketAddress remoteAddress) {
        this.clientChannel = clientChannel;
        ClientBootstrap clientBootstrap = new DefaultClientBootstrap();
        clientBootstrap.group(new NioEventLoopGroup());
        //        what different
        //        clientBootstrap.handler(..);
        ChannelFuture future = clientBootstrap.connect(remoteAddress);
        try {
            future.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.nettyChannel = future.channel();
        this.nettyChannel.pipeline().addLast(this);
    }
}
