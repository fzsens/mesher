package proxy.core.connect.channel;

import io.netty.buffer.ByteBuf;

/**
 * Created by fzsens on 2018/6/8.
 */
public interface RequestChannel {

    void sendAsyncRequest(final Object request, final Listener listener) throws Exception;


    void close();

    interface Listener {

        void onRequestSent() ;

        void onResponseReceived(ByteBuf message);

        void onError(Exception ex);
    }
}
