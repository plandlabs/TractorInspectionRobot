package kr.re.kitech.tractorinspectionrobot.okhttp;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpConnection {
    private OkHttpClient client;
    private static Request.Builder builder;

    public HttpConnection(){
        this.client = new OkHttpClient();
        this.builder = new Request.Builder();
    }

    private Request getRequest(String url) {
        return builder.url(url).get().build();
    }

    private Request getRequest(String url, RequestBody requestBody) {
        return builder.url(url).post(requestBody).build();
    }
    private Call getOkClient(Request request) {
        return client.newCall(request);
    }

    // 비동기 통신을 수행 get
    private CompletableFuture<String> getResult(String url){
        CompletableFuture<String> future = new CompletableFuture<>();

        getOkClient(getRequest(url)).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                System.err.println("Error Occurred");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (response.isSuccessful() && body != null) {
                    future.complete(body.string());
                }
            }
        });

        return future;
    }

    // 비동기 통신을 수행 post
    private CompletableFuture<String> getResult(String url, RequestBody requestBody){
        CompletableFuture<String> future = new CompletableFuture();

        getOkClient(getRequest(url, requestBody)).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                System.err.println("Error Occurred");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException{
                ResponseBody body = response.body();
                if (response.isSuccessful() && body != null) {
                    future.complete(body.string());
                }
            }
        });

        return future;
    }

    // 비동기 통신을 수행 get String 반환
    public String getParamRequest(String url) {

        CompletableFuture<String> future = this.getResult(url);
        try {
            return future.thenApply(s -> s).get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // 비동기 통신을 수행 post String 반환
    public String postParamRequest(String url, RequestBody requestBody) {

        CompletableFuture<String> future = this.getResult(url, requestBody);
        try {
            return future.thenApply(s -> s).get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
