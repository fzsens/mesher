package proxy.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import protocol.dubbo.protobuf.MesherProtoDubbo;
import proxy.core.ClientConfig;
import proxy.core.connect.AbstractClientConnector;
import proxy.core.connect.channel.ClientChannel;
import proxy.core.connect.channel.DefaultClientChannel;

import java.net.SocketAddress;

/**
 * Created by thierry.fu on 2018/6/12.
 */
public class ProtobufClientConnector  extends AbstractClientConnector<ClientChannel> {

    public ProtobufClientConnector(SocketAddress address) {
        super(address);
    }

    @Override
    public ClientChannel newClientChannel(Channel nettyChannel, ClientConfig clientConfig) {
        DefaultClientChannel channel = new DefaultClientChannel(nettyChannel);
        channel.getNettyChannel().pipeline().addLast("default", channel);
        return channel;
    }

    @Override
    public ChannelInitializer<Channel> newChannelInitializer(ClientConfig clientConfig) {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                // init initializer
                ChannelPipeline p = channel.pipeline();
                p.addLast(new ProtobufVarint32FrameDecoder());
                p.addLast(new ProtobufDecoder(MesherProtoDubbo.Response.getDefaultInstance()));
                p.addLast(new ProtobufVarint32LengthFieldPrepender());
                p.addLast(new ProtobufEncoder());
            }
        };
    }
}
