package appnet.hstp.labs.util.http;

import appnet.hstp.engine.util.JSON;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpClient {
    public static String doRequest(String method,String url, Map<String, String> header,Object body) throws Exception {
        String requestBody = null;

        if(method==null||(method=method.trim().toUpperCase()).isEmpty()){
            method = "GET";
        }
        switch (method){
            case "POST":
            case "DELETE":
            case "PUT":
            case "PATCH":
                if(body!=null) requestBody = body instanceof String?body.toString():JSON.toJson(body);
                break;
            case "GET":
                break;
            default:
                throw new IllegalArgumentException(method);
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("user-agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0");
        conn.setRequestMethod("POST");

        if(header!=null&&!header.isEmpty()) for (Map.Entry<String, String> entry : header.entrySet()) {
            conn.setRequestProperty(entry.getKey(),entry.getValue());
        }
        conn.setDoOutput(true);
        if(requestBody!=null) {
            conn.setRequestProperty("Content-Length", String.valueOf(requestBody.length()));
            conn.getOutputStream().write(requestBody.getBytes(StandardCharsets.UTF_8));
        }
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
    public static String doGet(String url) throws IOException {
        return doGet(url,null);
    }
    public static String doGet(String url,Map<String, String> header) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("user-agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0");
        if(header!=null) for (Map.Entry<String, String> entry : header.entrySet()) {
            conn.setRequestProperty(entry.getKey(),entry.getValue());
        }
        InputStream inputStream = conn.getInputStream();
        byte[] bytes = new byte[65536];
        int c;
        StringBuilder stringBuffer = new StringBuilder();
        while((c=inputStream.read(bytes))>0){
            stringBuffer.append(new String(bytes,0,c));
        }
        inputStream.close();

        return stringBuffer.toString();
    }

    public static String doDelete(String url,Map<String, String> header) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("user-agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0");
        conn.setRequestMethod("DELETE");
        if(header!=null) for (Map.Entry<String, String> entry : header.entrySet()) {
            conn.setRequestProperty(entry.getKey(),entry.getValue());
        }
        InputStream inputStream = conn.getInputStream();
        byte[] bytes = new byte[65536];
        int c;
        StringBuilder stringBuffer = new StringBuilder();
        while((c=inputStream.read(bytes))>0){
            stringBuffer.append(new String(bytes,0,c));
        }
        inputStream.close();

        return stringBuffer.toString();
    }

    public static String doPost(String url, Map<String, String> headers, Object body) throws Exception {

        String bodyString = body instanceof String?body.toString():JSON.toJson(body);
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
}
