
package com.example.myapplication.network;

import com.google.gson.JsonObject;

import retrofit2.Call;
//import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AuthApiService {

    /**
     * Gửi Firebase ID Token lên server để đổi lấy JWT tùy chỉnh.
     * @param idToken Firebase ID Token
     * @return Một Call object chứa JWT từ server.
     */
    @POST("auth/exchange-token")
    Call<JsonObject> exchangeToken(@Header("Authorization") String idToken);

}
