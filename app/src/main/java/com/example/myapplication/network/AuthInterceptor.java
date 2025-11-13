package com.example.myapplication.network;

import android.content.Context;

import com.example.myapplication.data.local.TokenManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Interceptor để tự động thêm JWT vào header của mỗi yêu cầu.
 */
public class AuthInterceptor implements Interceptor {

    private final TokenManager tokenManager;

    public AuthInterceptor(Context context) {
        this.tokenManager = new TokenManager(context.getApplicationContext());
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // --- SỬA LỖI ---
        // Nếu request là để đổi token, không thêm header và cho đi luôn.
        if (originalRequest.url().encodedPath().endsWith("/auth/exchange-token")) {
            return chain.proceed(originalRequest);
        }

        // Với tất cả các request khác, lấy token đã lưu và thêm vào header
        String token = tokenManager.getToken();

        if (token != null) {
            Request.Builder builder = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + token);
            Request newRequest = builder.build();
            return chain.proceed(newRequest);
        }

        // Nếu không có token, thực hiện request gốc
        return chain.proceed(originalRequest);
    }
}
