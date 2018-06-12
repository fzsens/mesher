package srv.protocol.dubbo;

import io.netty.channel.Channel;
import proxy.core.connect.channel.AbstractClientChannel;
import srv.protocol.dubbo.model.DubboRpcRequest;
import srv.protocol.dubbo.model.DubboRpcResponse;

/**
 * Created by fzsens on 6/11/18.
 */
public class DubboClientChannel extends AbstractClientChannel {

    protected DubboClientChannel(Channel nettyChannel) {
        super(nettyChannel);
    }

    @Override
    protected long extractSequenceId(Object message) throws Exception {
        if(message instanceof DubboRpcRequest) {
            return ((DubboRpcRequest)message).getId();
        } else if(message instanceof DubboRpcResponse) {
            return ((DubboRpcResponse)message).getRequestId();
        }
        return 0;
    }
}
