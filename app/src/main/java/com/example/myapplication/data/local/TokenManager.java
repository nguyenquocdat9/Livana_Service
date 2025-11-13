
package com.example.myapplication.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Quản lý việc lưu trữ và truy xuất JWT token một cách an toàn.
 * Sử dụng EncryptedSharedPreferences để mã hóa token.
 */
public class TokenManager {
    private static final String FILE_NAME = "livana_secure_prefs";
    private static final String KEY_JWT = "jwt_token";

    private SharedPreferences sharedPreferences;

    public TokenManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            // Trong ứng dụng thực tế, bạn nên xử lý lỗi này một cách phù hợp hơn
            // ví dụ như ghi log hoặc hiển thị thông báo cho người dùng.
        }
    }

    /**
     * Lưu JWT token vào SharedPreferences đã mã hóa.
     * @param token JWT token cần lưu.
     */
    public void saveToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_JWT, token);
        editor.apply();
    }

    /**
     * Lấy JWT token đã lưu.
     * @return JWT token, hoặc null nếu không tìm thấy.
     */
    public String getToken() {
        return sharedPreferences.getString(KEY_JWT, null);
    }

    /**
     * Xóa JWT token đã lưu.
     */
    public void deleteToken() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_JWT);
        editor.apply();
    }
}

