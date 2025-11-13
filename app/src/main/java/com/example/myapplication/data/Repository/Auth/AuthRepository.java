
package com.example.myapplication.data.Repository.Auth;

import android.content.Context;
import android.util.Log;

import com.example.myapplication.data.Model.Auth.AuthLogin;
import com.example.myapplication.data.Model.Auth.AuthRegister;
import com.example.myapplication.data.Model.User.User;
import com.example.myapplication.data.Repository.FirebaseService;
import com.example.myapplication.data.Repository.Notification.NotificationRepository;
import com.example.myapplication.data.Repository.User.UserRepository;
import com.example.myapplication.data.local.TokenManager;
import com.example.myapplication.network.AuthApiService;
import com.example.myapplication.network.ApiClient;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {
    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AuthApiService authApiService;
    private final TokenManager tokenManager;

    public AuthRepository(Context context) {
        this.firebaseAuth = FirebaseService.getInstance(context).getAuth();
        this.userRepository = new UserRepository(context);
        this.notificationRepository = new NotificationRepository(context);
        this.authApiService = ApiClient.getClient(context).create(AuthApiService.class);
        this.tokenManager = new TokenManager(context);
    }

    public void register(AuthRegister authInformation, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        firebaseAuth.createUserWithEmailAndPassword(authInformation.email, authInformation.password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();
                            User newUser = new User(uid, authInformation.full_name, authInformation.phone_number);

                            userRepository.createUser(
                                    newUser,
                                    unused -> onSuccess.onSuccess(null),
                                    e -> onFailure.onFailure(new Exception("Đăng ký thành công nhưng lưu user thất bại: " + e.getMessage()))
                            );
                        } else {
                            onFailure.onFailure(new Exception("Đăng ký thành công nhưng không lấy được FirebaseUser"));
                        }
                    } else {
                        onFailure.onFailure(task.getException());
                    }
                });
    }

    public void login(AuthLogin authInformation, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        firebaseAuth.signInWithEmailAndPassword(authInformation.email, authInformation.password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            exchangeToken(user, onSuccess, onFailure);
                        } else {
                            onFailure.onFailure(new Exception("Không thể lấy thông tin người dùng sau khi đăng nhập."));
                        }
                    } else {
                        onFailure.onFailure(task.getException());
                    }
                });
    }

    public void resetPassword(String email, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public String getUserUid() {
        return this.firebaseAuth.getUid();
    }

    public FirebaseUser getCurrentUser() {
        return this.firebaseAuth.getCurrentUser();
    }
    public boolean checkLogin() {
        return this.firebaseAuth.getCurrentUser() != null;
    }

    // Sửa lại phương thức này để tương thích với LoginActivity
    public void loginWithGoogleAccount(GoogleSignInAccount account, OnSuccessListener<AuthResult> onSuccess, OnFailureListener onFailure) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    AuthResult authResult = task.getResult();
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        // Thực hiện đổi token ngay tại đây
                        user.getIdToken(true).addOnCompleteListener(idTokenTask -> {
                            if (idTokenTask.isSuccessful()) {
                                String idToken = idTokenTask.getResult().getToken();
                                Call<JsonObject> call = authApiService.exchangeToken("Bearer " + idToken);
                                call.enqueue(new Callback<JsonObject>() {
                                    @Override
                                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                        if (response.isSuccessful() && response.body() != null) {
                                            String jwt = response.body().get("jwt").getAsString();
                                            tokenManager.saveToken(jwt);
                                            Log.d("AuthRepository", "JWT đã được lưu an toàn.");
                                            
                                            // Sau khi lưu JWT, gọi callback thành công với AuthResult
                                            onSuccess.onSuccess(authResult);
                                        } else {
                                            onFailure.onFailure(new Exception("Không thể đổi token. Response code: " + response.code()));
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<JsonObject> call, Throwable t) {
                                        onFailure.onFailure(new Exception("Lỗi mạng khi đổi token: " + t.getMessage()));
                                    }
                                });
                            } else {
                                onFailure.onFailure(idTokenTask.getException());
                            }
                        });
                    } else {
                        onFailure.onFailure(new Exception("Không thể lấy thông tin người dùng sau khi đăng nhập Google."));
                    }
                } else {
                    onFailure.onFailure(task.getException());
                }
            });
    }

    private void exchangeToken(FirebaseUser firebaseUser, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        firebaseUser.getIdToken(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String idToken = task.getResult().getToken();
                Call<JsonObject> call = authApiService.exchangeToken("Bearer " + idToken);
                call.enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String jwt = response.body().get("jwt").getAsString();
                            tokenManager.saveToken(jwt);
                            Log.d("AuthRepository", "JWT đã được lưu an toàn.");
                            onSuccess.onSuccess(null);
                        } else {
                            onFailure.onFailure(new Exception("Không thể đổi token. Response code: " + response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        onFailure.onFailure(new Exception("Lỗi mạng khi đổi token: " + t.getMessage()));
                    }
                });
            } else {
                onFailure.onFailure(task.getException());
            }
        });
    }

    public void logout() {
        firebaseAuth.signOut();
        if (this.getUserUid() != null) {
            this.notificationRepository.deleteFCMToken(this.getUserUid());
        }
        tokenManager.deleteToken();
        Log.d("AuthRepository", "JWT đã được xóa.");
    }
}
