package proxy.codec;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by fzsens on 6/2/18.
 */
public class RequestParser {

    /**
     * 解析请求参数
     *
     * @return 包含所有请求参数的键值对, 如果没有参数, 则返回空Map
     */
    public static Map<String, String> parse(FullHttpRequest fullReq) {
        HttpMethod method = fullReq.getMethod();
        Map<String, String> paramMap = new HashMap<>();
        if (HttpMethod.GET == method) {
            QueryStringDecoder decoder = new QueryStringDecoder(fullReq.getUri());
            decoder.parameters().entrySet().forEach(entry -> paramMap.put(entry.getKey(), entry.getValue().get(0)));
        } else if (HttpMethod.POST == method) {
            HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(fullReq);
            try {
                decoder.offer(fullReq);
                List<InterfaceHttpData> paramList = decoder.getBodyHttpDatas();
                for (InterfaceHttpData param : paramList) {
                    Attribute data = (Attribute) param;

                    paramMap.put(data.getName(), data.getValue());

                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                decoder.destroy();
            }
        }
        return paramMap;
    }
}