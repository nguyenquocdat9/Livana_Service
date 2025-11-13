package com.example.myapplication.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;


import com.example.myapplication.R;
import com.example.myapplication.data.Enum.Role;
import com.example.myapplication.data.Repository.User.UserRepository;
import com.example.myapplication.ui.auth.LoginActivity;
import com.example.myapplication.ui.fragments.ExploreFragment;
import com.example.myapplication.ui.fragments.MessagesFragment;
import com.example.myapplication.ui.fragments.NewExploreFragment;
import com.example.myapplication.ui.fragments.ProfileFragment;
import com.example.myapplication.ui.fragments.TripsFragment;
import com.example.myapplication.ui.fragments.WishlistFragment;
import com.example.myapplication.ui.misc.WishlistManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.core.splashscreen.SplashScreen;

public class MainActivity extends AppCompatActivity {
    private UserRepository userRepository;
    private WishlistManager wishlistManager;
    private boolean isFragmentTransactionInProgress = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Install splash screen
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // --- Sửa lỗi: Di chuyển checkUserAndRedirect() lên trước --- 
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // User chưa đăng nhập, chuyển ngay về LoginActivity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return; // Dừng thực thi onCreate tại đây để tránh lỗi
        }
        
        // Nếu đã đăng nhập, mới tiếp tục set content view và các việc khác
        setContentView(R.layout.activity_main);

        // Khởi tạo các repository và manager
        userRepository = new UserRepository(this);
        wishlistManager = WishlistManager.getInstance();

        // Kiểm tra quyền thông báo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        // --- Sửa lỗi: Chỉ load wishlist sau khi đã chắc chắn có user ---
        loadUserWishlist(currentUser.getUid());

        // Kiểm tra xem user có phải là HOST không và chuyển hướng nếu cần
        checkIfUserIsHost(currentUser.getUid());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new NewExploreFragment())
                    .commit();
        }

        setupFooterNavigation();

        // Xử lý intent điều hướng fragment
        handleFragmentNavigation(getIntent());
    }

    private void updateButtonStates(int selectedButtonId) {
        int[] buttonIds = {
                R.id.button_explore,
                R.id.button_wishlists,
                R.id.button_trips,
                R.id.button_messages,
                R.id.button_profile
        };

        for (int id : buttonIds) {
            findViewById(id).setSelected(id == selectedButtonId);
        }
    }

    private void setupFooterNavigation() {
        findViewById(R.id.button_explore).setOnClickListener(v -> {
            loadFragment(new NewExploreFragment());
            updateButtonStates(R.id.button_explore);
        });
        findViewById(R.id.button_wishlists).setOnClickListener(v -> {
            loadFragment(new WishlistFragment());
            updateButtonStates(R.id.button_wishlists);
        });
        findViewById(R.id.button_trips).setOnClickListener(v -> {
            loadFragment(new TripsFragment());
            updateButtonStates(R.id.button_trips);
        });
        findViewById(R.id.button_messages).setOnClickListener(v -> {
            loadFragment(new MessagesFragment());
            updateButtonStates(R.id.button_messages);
        });
        findViewById(R.id.button_profile).setOnClickListener(v -> {
            loadFragment(new ProfileFragment());
            updateButtonStates(R.id.button_profile);
        });

        // Set initial state
        updateButtonStates(R.id.button_explore);
    }

    private void loadFragment(Fragment fragment) {
        if (isFragmentTransactionInProgress) return;
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) return;

        isFragmentTransactionInProgress = true;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss();
        new android.os.Handler().postDelayed(() -> isFragmentTransactionInProgress = false, 300);
    }

    // --- Sửa lỗi: Chấp nhận uid làm tham số ---
    private void loadUserWishlist(String userUID) {
        Log.d("WishlistLoad", "Loading wishlist for user: " + userUID);
        wishlistManager.loadUserWishlist(
                userUID,
                userRepository,
                unused -> Log.d("WishlistLoad", "Wishlist loaded successfully."),
                e -> Log.e("WishlistLoad", "Lỗi khi load wishlist", e)
        );
    }

    // --- Sửa lỗi: Tách riêng logic kiểm tra Host ---
    private void checkIfUserIsHost(String userUID) {
        userRepository.getUserByUid(userUID,
                user -> {
                    if (user != null && user.role == Role.HOST) {
                        startActivity(new Intent(this, HostMainActivity.class));
                        finish();
                    }
                },
                e -> {
                    // Lỗi, không cần xử lý đặc biệt, cứ ở lại MainActivity
                }
        );
    }
    
    private void handleFragmentNavigation(Intent intent) {
        String fragmentToLoad = intent.getStringExtra("FRAGMENT_TO_LOAD");
        if (fragmentToLoad != null) {
            switch (fragmentToLoad) {
                case "wishlists":
                    loadFragment(new WishlistFragment());
                    updateButtonStates(R.id.button_wishlists);
                    break;
                case "trips":
                    loadFragment(new TripsFragment());
                    updateButtonStates(R.id.button_trips);
                    break;
                case "profile":
                    loadFragment(new ProfileFragment());
                    updateButtonStates(R.id.button_profile);
                    break;
                case "messages":
                    MessagesFragment messagesFragment = new MessagesFragment();
                    Bundle extras = getIntent().getExtras();
                    if (extras != null) {
                        messagesFragment.setArguments(extras);
                    }
                    loadFragment(messagesFragment);
                    updateButtonStates(R.id.button_messages);
                    break;
                default:
                    loadFragment(new ExploreFragment());
                    updateButtonStates(R.id.button_explore);
                    break;
            }
        }
    }
}
