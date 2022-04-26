package com.myapp.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MeiTuAIUtil {
    public static final String APPKEY = "fae7b4b2d85c47b3a55fead1ed38c7e2";

    public static final String SECRETID = "ad63efd5629c43c1b10c011031906ffc";

    public static String ImageToBase64(String imagePath) {
        InputStream in = null;
        byte[] data = null;
        try {
            in = new FileInputStream(imagePath);
            data = new byte[in.available()];
            in.read(data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try{
                if(in != null){
                    in.close();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
        return encoder.encodeToString(data);
    }

    public static void doPost(String imagePath, Integer alpha, String extensionName, Handler handler) throws MalformedURLException {
        String imgStr = ImageToBase64(imagePath);
        //String json = "test";
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        //String jsonStr = "{\"parameter\":{\"beautyAlpha\":"+alpha+"},\"extra\":{},\"media_info_list\":[{\"media_data\":\""+ imgStr +"\",\"media_profiles\":{\"media_data_type\":\""+extensionName+"\"}}]}";
        //System.out.println(jsonStr);
        String jsonStr = "{\"parameter\":{\"beautyAlpha\":70},\"extra\":{},\"media_info_list\":[{\"media_data\":\""+ imgStr +"\",\"media_profiles\":{\"media_data_type\":\"jpg\"}}]}";

        RequestBody body = RequestBody.create(JSON, jsonStr);
        URL url = new URL("https://openapi.mtlab.meitu.com/v1/beauty?api_key="+APPKEY+"&api_secret="+SECRETID);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Message message = new Message();
                message.what = 0;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try {
                    Message message = new Message();
                    ResponseBody responseBody = response.body();
                    if(responseBody==null){
                        return;
                    }
                    BufferedReader in = new BufferedReader(new InputStreamReader(responseBody.byteStream(), "UTF-8"));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null){
                        result.append(line);
                        System.out.println(line);
                    }
                    String res = result.toString();

                    JsonElement jsonElement = JsonParser.parseString(res);
                    JsonObject asJsonObject = jsonElement.getAsJsonObject();
                    JsonArray media_info_list = asJsonObject.getAsJsonArray("media_info_list");
                    JsonElement element = null;
                    if(media_info_list!=null){
                        element = media_info_list.get(0);
                    }
                    if(element==null){
                        message.what = 0;
                        handler.sendMessage(message);
                        return;
                    }
                    JsonObject media_data = element.getAsJsonObject();

                    String base = media_data.get("media_data").toString().replace("\r\n", "");




                    byte[] decode = Base64.decode(base,Base64.DEFAULT);

                    Bitmap bitmap = BitmapFactory.decodeByteArray(decode, 0, decode.length);

                    message.what = 1;
                    message.obj = bitmap;
                    handler.sendMessage(message);
                } catch (Exception e) {
                    Message message = new Message();
                    message.what = 0;
                    handler.sendMessage(message);
                    e.printStackTrace();
                }
            }
        });

    }

}
