package appnet.hstp.labs.util.http;

import appnet.hstp.engine.util.JSON;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HstpHttpClient {
    public static String doGet(String url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
        urlConnection.setRequestProperty("user-agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0");
        InputStream inputStream = urlConnection.getInputStream();
        byte[] bytes = new byte[65536];
        int c;
        StringBuilder stringBuffer = new StringBuilder();
        while((c=inputStream.read(bytes))>0){
            stringBuffer.append(new String(bytes,0,c));
        }
        inputStream.close();

        return stringBuffer.toString();
    }

    public static String doPost(String url, Map<String, String> headers, Map<String, String> params,Object body) throws Exception {
        String bodyString = JSON.toJson(body);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("user-agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0");
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Content-Length", String.valueOf(bodyString.length()));
        if(headers!=null&&!headers.isEmpty()) for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(),entry.getValue());
        }
        conn.setDoOutput(true);
        conn.getOutputStream().write(bodyString.getBytes(StandardCharsets.UTF_8));
        InputStream inputStream;
        if(conn.getResponseCode()>=400)
            inputStream = conn.getErrorStream();
        else
            inputStream = conn.getInputStream();
        byte[] bytes = new byte[65536];
        int c;
        StringBuilder stringBuffer = new StringBuilder();
        while((c=inputStream.read(bytes))>0){
            stringBuffer.append(new String(bytes,0,c));
        }
        inputStream.close();

        return stringBuffer.toString();
    }
    public static String hstpRequest(String url,String hstpUsl,String hstpAuth,Object body) throws Exception {
        return doPost(url, Map.of("hstp-usl",hstpUsl==null?"":hstpUsl,"hstp-auth",hstpAuth==null?"":hstpAuth),null,body);
    }
    public static <T> T hstpRequest(String url,String hstpUsl,String hstpAuth,Object body,Class<T> type) throws Exception {
        return JSON.fromJson(hstpRequest(url,hstpUsl,hstpAuth,body),type);
    }

    public static <T> T hstpRequest(String url, String hstpUsl, String hstpAuth, Object body, TypeReference<T> type) throws Exception {
        return JSON.fromJson(hstpRequest(url,hstpUsl,hstpAuth,body),type);
    }
}
