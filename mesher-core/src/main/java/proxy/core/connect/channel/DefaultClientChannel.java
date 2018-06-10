package proxy.core.connect.channel;

import io.netty.channel.Channel;

/**
 * Created by fzsens on 2018/6/8.
 */
public class DefaultClientChannel extends AbstractClientChannel {

    public DefaultClientChannel(Channel nettyChannel) {
        super(nettyChannel);
    }

    @Override
    protected long extractSequenceId(Object message) throws Exception {
        return 0;
    }

}
