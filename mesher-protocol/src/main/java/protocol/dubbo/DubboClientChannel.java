package protocol.dubbo;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import proxy.core.connect.channel.AbstractClientChannel;
import protocol.dubbo.model.DubboRpcRequest;
import protocol.dubbo.model.DubboRpcResponse;

/**
 * Created by fzsens on 6/11/18.
 */
public class DubboClientChannel extends AbstractClientChannel {

    protected DubboClientChannel(Channel nettyChannel) {
        super(nettyChannel);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof DubboRpcResponse) {
            // TODO 独立成为一个HeartBeat handler
            DubboRpcResponse response = (DubboRpcResponse) msg;
            if(response.isHeartbeat()) {
                DubboRpcRequest heartBeat = new DubboRpcRequest();
                heartBeat.setId(response.getRequestId());
                heartBeat.setTwoWay(false);
                heartBeat.setHeartbeat(true);
                this.sendAsyncRequest(heartBeat, new Listener() {
                    @Override
                    public void onRequestSent() {

                        System.out.println("heartbeat sent");
                    }
                    @Override
                    public void onResponseReceived(Object response) {
                        System.out.println("heartbeat sent" +response);
                    }

                    @Override
                    public void onError(Exception ex) {
                        ex.printStackTrace();
                    }
                });
                return;
            }
        }
        super.channelRead(ctx, msg);
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
