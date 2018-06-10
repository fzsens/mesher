package srv.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import srv.codec.model.Bytes;
import srv.codec.model.RpcResponse;

import java.util.Arrays;
import java.util.List;

public class DubboRpcDecoder extends ByteToMessageDecoder {
    // header length.
    protected static final int HEADER_LENGTH = 16;

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        decode2(byteBuf, list);
    }

    private void decode2(ByteBuf byteBuf, List<Object> list) {
        byte[] data = new byte[byteBuf.readableBytes()];
        int index = 0;
//        while (index < data.length) {
//            byte[] requestIdBytes = Arrays.copyOfRange(data, index + 4, index + 12);
//            long requestId = Bytes.bytes2long(requestIdBytes, 0);
//            byte[] dataLenBytes = Arrays.copyOfRange(data, index + 12, index + 16);
//            int dataLen = Bytes.bytesToInt(dataLenBytes, 0);
//            byte[] dataBytes = Arrays.copyOfRange(data, index + 16, index + 16 + dataLen);
//            RpcResponse response = new RpcResponse();
//            response.setRequestId(requestId);
//            response.setBytes(dataBytes);
//            index = index + 16 + dataLen;
//            list.add(response);
//        }
//
        byteBuf.readBytes(data);

        byte[] subArray = Arrays.copyOfRange(data,HEADER_LENGTH + 1, data.length);

        String s = new String(subArray);

        byte[] requestIdBytes = Arrays.copyOfRange(data,4,12);
        byte[] lenBytes = Arrays.copyOfRange(data,12,16);
        int len = Bytes.bytesToInt(lenBytes,0);
        long requestId = Bytes.bytes2long(requestIdBytes,0);

        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setBytes(subArray);
        list.add(response);

    }
}
