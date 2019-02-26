package com.iaruchkin.deepbreath.network;

import androidx.annotation.NonNull;
import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface WeatherEndpoint {

    @GET("v1/{forecast}.json")
    Single<WeatherResponse> get(@Path("forecast") @NonNull String forecast);
}


