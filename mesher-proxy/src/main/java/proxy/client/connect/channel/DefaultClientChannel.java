package proxy.client.connect.channel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.Timer;

/**
 * Created by thierry.fu on 2018/6/8.
 */
public class DefaultClientChannel extends AbstractClientChannel {

    public DefaultClientChannel(Channel nettyChannel) {
        super(nettyChannel);
    }

    @Override
    protected ByteBuf extractResponse(Object message) {
        if (!(message instanceof ByteBuf)) {
            return null;
        }

        ByteBuf buffer = (ByteBuf) message;
        if (!buffer.isReadable()) {
            return null;
        }

        return buffer;
    }

    @Override
    protected int extractSequenceId(ByteBuf messageBuffer) throws Exception {
        return 0;
    }

    @Override
    protected ChannelFuture writeRequest(ByteBuf request) {
        return getNettyChannel().write(request);
    }
}
