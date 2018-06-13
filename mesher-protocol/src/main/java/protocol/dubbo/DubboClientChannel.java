package protocol.dubbo;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protocol.dubbo.model.DubboRpcRequest;
import protocol.dubbo.model.DubboRpcResponse;
import proxy.core.connect.channel.AbstractClientChannel;

/**
 * Created by fzsens on 6/11/18.
 */
public class DubboClientChannel extends AbstractClientChannel {

    protected Logger log = LoggerFactory.getLogger(DubboClientChannel.class);

    public DubboClientChannel(Channel nettyChannel) {
        super(nettyChannel);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DubboRpcResponse) {
            // TODO 独立成HeartBeat handler
            DubboRpcResponse response = (DubboRpcResponse) msg;
            if (response.isHeartbeat()) {
                DubboRpcRequest heartBeat = new DubboRpcRequest();
                heartBeat.setId(response.getRequestId());
                heartBeat.setTwoWay(false);
                heartBeat.setHeartbeat(true);
                this.sendAsyncRequest(heartBeat, new Listener() {
                    @Override
                    public void onRequestSent() {
                        log.debug("heartbeat sent .");
                    }

                    @Override
                    public void onResponseReceived(Object response) {
                        log.debug("heartbeat respond " + response);
                    }

                    @Override
                    public void onError(Exception ex) {
                        log.warn("heartbeat unexpected exception {} ", ex);
                    }
                });
                return;
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    protected long extractSequenceId(Object message) throws Exception {
        if (message instanceof DubboRpcRequest) {
            return ((DubboRpcRequest) message).getId();
        } else if (message instanceof DubboRpcResponse) {
            return ((DubboRpcResponse) message).getRequestId();
        }
        return 0;
    }
}
