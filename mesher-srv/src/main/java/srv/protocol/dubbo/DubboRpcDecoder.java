package srv.protocol.dubbo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import srv.protocol.dubbo.model.Bytes;
import srv.protocol.dubbo.model.DubboRpcResponse;

import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class DubboRpcDecoder extends ByteToMessageDecoder {

    protected static final int HEADER_LENGTH = 16;

    static Object NEED_MORE_INPUT = new Object();

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        try {
            do {
                int savedReaderIndex = byteBuf.readerIndex();
                Object msg;
                msg = decode2(byteBuf);
                if (msg == NEED_MORE_INPUT) {
                    byteBuf.readerIndex(savedReaderIndex);
                    break;
                }
                list.add(msg);
            } while (byteBuf.isReadable());
        } finally {
            if (byteBuf.isReadable()) {
                byteBuf.discardReadBytes();
            }
        }
    }

    private Object decode2(ByteBuf byteBuf) {
        int savedReaderIndex = byteBuf.readerIndex();
        int readable = byteBuf.readableBytes();
        if (readable < HEADER_LENGTH) {
            return NEED_MORE_INPUT;
        }
        byte[] header = new byte[HEADER_LENGTH];
        byteBuf.readBytes(header);
        int len = Bytes.bytes2int(header, 12);
        int tt = len + HEADER_LENGTH;
        if (readable < tt) {
            return NEED_MORE_INPUT;
        }
        byteBuf.readerIndex(savedReaderIndex);
        byte[] data = new byte[tt];
        byteBuf.readBytes(data);
        // HEADER_LENGTH + 1，忽略header & Response value type的读取，直接读取实际Return value
        // dubbo返回的body中，前后各有一个换行，去掉
        long requestId = Bytes.bytes2long(data, 4);
        byte[] dataBytes = Arrays.copyOfRange(data, HEADER_LENGTH + 2, data.length - 1);
        DubboRpcResponse response = new DubboRpcResponse();
        response.setRequestId(requestId);
        response.setBytes(dataBytes);
        return response;
    }

}
