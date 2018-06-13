package proxy.connect;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import protocol.dubbo.DubboClientChannel;

/**
 * Created by thierry.fu on 2018/6/13.
 */
public class RegistryableDubboClientChannel extends DubboClientChannel {

    protected RegistryableDubboClientChannel(Channel nettyChannel) {
        super(nettyChannel);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // registry
        log.info("registry");
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // un-registry
        log.info("un-registry");
        super.channelInactive(ctx);
    }
}
