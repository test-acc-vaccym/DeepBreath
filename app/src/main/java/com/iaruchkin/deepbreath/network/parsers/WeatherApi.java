package com.iaruchkin.deepbreath.network.parsers;

import androidx.annotation.NonNull;

import com.iaruchkin.deepbreath.network.endpoints.WeatherEndpoint;
import com.iaruchkin.deepbreath.network.interceptors.WeatherApiKeyInterceptor;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class WeatherApi {

    private static WeatherApi networkSilngleton;
    private static final String URL = "https://api.apixu.com/";
    private WeatherEndpoint weatherEndpoint;
    Proxy proxyTest = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("96.9.69.133", 42956));


    public static synchronized WeatherApi getInstance(){
        if (networkSilngleton == null){
            networkSilngleton = new WeatherApi();
        }
        return networkSilngleton;
    }

    private WeatherApi(){
        final OkHttpClient client = builtClient();
        final Retrofit retrofit = builtRertofit(client);

        weatherEndpoint = retrofit.create(WeatherEndpoint.class);
    }

    private Retrofit builtRertofit(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }

    private OkHttpClient builtClient(){

        final HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(interceptor)
                .addInterceptor(WeatherApiKeyInterceptor.create())
                .proxy(proxyTest)
                .build();
    }

    @NonNull
    public WeatherEndpoint weatherEndpoint() {
        return weatherEndpoint;
    }
}