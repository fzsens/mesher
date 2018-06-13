package proxy.connect;

import io.netty.channel.Channel;
import protocol.dubbo.protobuf.MesherProtoDubbo;
import proxy.core.connect.channel.AbstractClientChannel;

/**
 * Created by thierry.fu on 2018/6/13.
 */
public class ProtobufClientChannel extends AbstractClientChannel {

    protected ProtobufClientChannel(Channel nettyChannel) {
        super(nettyChannel);
    }

    @Override
    protected long extractSequenceId(Object message) throws Exception {
        if(message instanceof MesherProtoDubbo.Response) {
            return ((MesherProtoDubbo.Response)message).getRequestId();
        } else if (message instanceof MesherProtoDubbo.Request){
            return ((MesherProtoDubbo.Request)message).getRequestId();
        }
        return 0;
    }


}
