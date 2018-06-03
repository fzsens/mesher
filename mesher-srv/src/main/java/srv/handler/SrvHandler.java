package srv.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Created by fzsens on 6/3/18.
 */
public class SrvHandler extends ChannelInboundHandlerAdapter {
    static final int IDLE_TIMEOUT = 10;

    public SrvHandler(ChannelPipeline pipeline) {
        initChannelPipeline(pipeline);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        int size = byteBuf.readableBytes();
        byte[] bytes = new byte[size];
        byteBuf.readBytes(bytes);
        String param = new String(bytes);

        byte[] CONTENT = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};
        ChannelFuture future = ctx.writeAndFlush(Unpooled.copiedBuffer(CONTENT));
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    private void initChannelPipeline(ChannelPipeline pipeline) {
        pipeline.addLast("handler", this);
    }
}
