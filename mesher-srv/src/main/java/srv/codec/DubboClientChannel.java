package srv.codec;

import io.netty.channel.Channel;
import proxy.core.connect.channel.AbstractClientChannel;
import srv.codec.model.RpcRequest;
import srv.codec.model.RpcResponse;

/**
 * Created by fzsens on 6/11/18.
 */
public class DubboClientChannel extends AbstractClientChannel {

    protected DubboClientChannel(Channel nettyChannel) {
        super(nettyChannel);
    }

    @Override
    protected long extractSequenceId(Object message) throws Exception {
        if(message instanceof RpcRequest) {
            return ((RpcRequest)message).getId();
        } else if(message instanceof RpcResponse) {
            return ((RpcResponse)message).getRequestId();
        }
        return 0;
    }
}
