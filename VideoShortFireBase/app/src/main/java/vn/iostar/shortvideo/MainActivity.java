package vn.iostar.shortvideo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private ViewPager2 viewPager2;
    private VideosFireBaseAdapter videosAdapter;
    private FirebaseAuth mAuth;
    private ImageView avatarImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        // Cấu hình Cloudinary
        try {
            Map config = new HashMap();
            config.put("cloud_name", "dmcg6uu1f");
            config.put("api_key", "629166749329541");
            config.put("api_secret", "Z-YwseZDMnV1EZxLb3Fry-CKGFU"); // Optional cho unsigned
            MediaManager.init(this, config);
        } catch (Exception e) {
            Log.e("CloudinaryInit", "Lỗi khởi tạo Cloudinary", e);
            Toast.makeText(this, "Lỗi Cloudinary: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }



        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Kiểm tra trạng thái đăng nhập
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        viewPager2 = findViewById(R.id.vpager);
        avatarImageView = findViewById(R.id.avatarImageView);
        viewPager2.setOffscreenPageLimit(1);
        getVideos();

        // Tải ảnh đại diện
        loadAvatar();

        // Mở ProfileActivity khi nhấp vào avatar
        avatarImageView.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        // Xử lý nút upload video
        Button uploadVideoButton = findViewById(R.id.uploadVideoButton);
        uploadVideoButton.setOnClickListener(v -> {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                startActivity(new Intent(MainActivity.this, UploadVideoActivity.class));
            } else {
                Toast.makeText(this, "Vui lòng đăng nhập để tải video", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });

        // Tạm dừng video khi chuyển trang
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                try {
                    RecyclerView.ViewHolder holder = ((RecyclerView) viewPager2.getChildAt(0))
                            .findViewHolderForAdapterPosition(position);
                    if (holder instanceof VideosFireBaseAdapter.MyHolder) {
                        VideosFireBaseAdapter.MyHolder videoHolder = (VideosFireBaseAdapter.MyHolder) holder;
                        videoHolder.pauseVideo();
                    }
                } catch (Exception e) {
                    Log.e("ViewPagerError", "Lỗi khi tạm dừng video", e);
                }
            }
        });
    }

    private void loadAvatar() {
        String userId = mAuth.getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("users").child(userId)
                .child("profileImage").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().getValue() != null) {
                        String imageUrl = task.getResult().getValue(String.class);
                        if (!imageUrl.isEmpty()) {
                            Glide.with(this).load(imageUrl).into(avatarImageView);
                        }
                    }
                });
    }

    private void getVideos() {
        DatabaseReference mDataBase = FirebaseDatabase.getInstance().getReference("videos");
        FirebaseRecyclerOptions<Video1Model> options = new FirebaseRecyclerOptions.Builder<Video1Model>()
                .setQuery(mDataBase, Video1Model.class).build();
        videosAdapter = new VideosFireBaseAdapter(options);
        viewPager2.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        viewPager2.setAdapter(videosAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (videosAdapter != null) {
            videosAdapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (videosAdapter != null) {
            videosAdapter.stopListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videosAdapter != null) {
            videosAdapter.stopListening();
        }
        viewPager2.setAdapter(null);
    }
}