package srv.protocol.dubbo.model;

public class DubboRpcResponse {
    public static final String HEARTBEAT_EVENT = null;

    private long requestId;
    private byte[] bytes;
    private boolean mEvent = false;
    private Object mResult;

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public boolean isHeartbeat() {
        return mEvent && HEARTBEAT_EVENT == mResult;
    }

    public void setHeartbeat(boolean isHeartbeat) {
        if (isHeartbeat) {
            setEvent(HEARTBEAT_EVENT);
        }
    }
    public void setEvent(String event) {
        mEvent = true;
        mResult = event;
    }
}
