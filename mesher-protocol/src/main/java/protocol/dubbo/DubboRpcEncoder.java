package protocol.dubbo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import protocol.dubbo.model.DubboRpcInvocation;
import protocol.dubbo.model.DubboRpcRequest;
import protocol.dubbo.model.JsonUtils;

import java.io.*;

/**
 *
 */
public class DubboRpcEncoder extends MessageToByteEncoder {
    // header length.
    protected static final int HEADER_LENGTH = 16;
    // magic header.
    protected static final short MAGIC = (short) 0xdabb;
    // message flag.
    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_TWOWAY = (byte) 0x40;
    protected static final byte FLAG_EVENT = (byte) 0x20;

    /**
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf buffer) throws Exception {
        DubboRpcRequest req = (DubboRpcRequest) msg;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteBuf bodyBuf;
        if (!req.isHeartbeat()) {
            encodeRequestData(bos, req.getData());
        } else {
            encodeHeartBeatData(bos, req.getData());
        }
        bodyBuf = Unpooled.wrappedBuffer(bos.toByteArray());
        ByteBuf headerBuf = ctx.alloc().ioBuffer(HEADER_LENGTH);
        headerBuf.writeShort(MAGIC);
        headerBuf.writeByte(getFlag(req));
        headerBuf.writeByte(20);
        headerBuf.writeLong(req.getId());
        headerBuf.writeInt(bodyBuf.readableBytes());

        ((CompositeByteBuf) buffer).addComponent(headerBuf);
        ((CompositeByteBuf) buffer).addComponent(bodyBuf);
        ((CompositeByteBuf) buffer).writerIndex(headerBuf.readableBytes() + bodyBuf.readableBytes());
    }

    private byte getFlag(DubboRpcRequest req) {
        byte flag = FLAG_REQUEST | 6;
        if (req.isTwoWay()) flag |= FLAG_TWOWAY;
        if (req.isEvent()) flag |= FLAG_EVENT;
        return flag;
    }

    @Override
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, Object msg, boolean preferDirect) {
        return ctx.alloc().compositeDirectBuffer(2);
    }

    public void encodeRequestData(OutputStream out, Object data) throws Exception {
        DubboRpcInvocation inv = (DubboRpcInvocation) data;
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        JsonUtils.writeObject(inv.getAttachment("dubbo", "2.0.1"), writer);
        JsonUtils.writeObject(inv.getAttachment("path"), writer);
        JsonUtils.writeObject(inv.getAttachment("version"), writer);
        JsonUtils.writeObject(inv.getMethodName(), writer);
        JsonUtils.writeObject(inv.getParameterTypes(), writer);
        JsonUtils.writeBytes(inv.getArguments(), writer);
        JsonUtils.writeObject(inv.getAttachments(), writer);
    }

    public void encodeHeartBeatData(OutputStream out, Object data) throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        JsonUtils.writeObject(data, writer);
    }

}

