package proxy.core.connect.channel;

import io.netty.channel.Channel;

/**
 * Created by thierry.fu on 2018/6/8.
 */
public interface ClientChannel extends RequestChannel {

    boolean hasError();

    Exception getError();

    /**
     * Executes the given {@link Runnable} on the I/O thread that manages reads/writes for this
     * channel.
     */
    void executeInIoThread(Runnable runnable);

    Channel getNettyChannel();

}
