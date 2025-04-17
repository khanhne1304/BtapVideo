package vn.iostar.shortvideo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private ImageView profileImageView;
    private Button changeImageButton;
    private TextView videoCountTextView;
    private ProgressBar progressBar;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        profileImageView = findViewById(R.id.profileImageView);
        changeImageButton = findViewById(R.id.changeImageButton);
        videoCountTextView = findViewById(R.id.videoCountTextView);
        progressBar = findViewById(R.id.progressBar);

        // Khởi tạo launcher chọn ảnh
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        Glide.with(this).load(imageUri).into(profileImageView);
                        uploadImageToCloudinary();
                    }
                });

        // Tải ảnh đại diện
        loadProfileImage();

        // Đếm số video
        countUserVideos();

        // Xử lý nút thay ảnh
        changeImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });
    }

    private void loadProfileImage() {
        String userId = mAuth.getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("users").child(userId)
                .child("profileImage").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().getValue() != null) {
                        String imageUrl = task.getResult().getValue(String.class);
                        if (!imageUrl.isEmpty()) {
                            Glide.with(this).load(imageUrl).into(profileImageView);
                        }
                    }
                });
    }

    private void countUserVideos() {
        String userId = mAuth.getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("videos")
                .orderByChild("userId").equalTo(userId)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        long count = task.getResult().getChildrenCount();
                        videoCountTextView.setText("Số video: " + count);
                    } else {
                        Toast.makeText(this, "Lỗi đếm video: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadImageToCloudinary() {
        if (imageUri == null) return;
        progressBar.setVisibility(View.VISIBLE);
        changeImageButton.setEnabled(false);

        MediaManager.get().upload(imageUri)
                .unsigned("shortvideo_unsigned") // Dùng preset từ Cloudinary
                .option("folder", "profile_images")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d("ProfileImage", "Bắt đầu upload ảnh");
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // Có thể hiển thị tiến trình nếu cần
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        saveImageUrlToFirebase(imageUrl);
                        progressBar.setVisibility(View.GONE);
                        changeImageButton.setEnabled(true);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        progressBar.setVisibility(View.GONE);
                        changeImageButton.setEnabled(true);
                        Toast.makeText(ProfileActivity.this, "Lỗi upload ảnh: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.d("ProfileImage", "Tái thử upload ảnh");
                    }
                })
                .dispatch();
    }

    private void saveImageUrlToFirebase(String imageUrl) {
        String userId = mAuth.getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("users").child(userId)
                .child("profileImage").setValue(imageUrl)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Cập nhật ảnh đại diện thành công", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Lỗi lưu ảnh: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}