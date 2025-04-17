package vn.iostar.shortvideo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UploadVideoActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EditText titleEditText, descEditText;
    private Button chooseVideoButton, uploadButton;
    private ProgressBar progressBar;
    private Uri videoUri;
    private ActivityResultLauncher<Intent> videoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_video);

        mAuth = FirebaseAuth.getInstance();
        titleEditText = findViewById(R.id.titleEditText);
        descEditText = findViewById(R.id.descEditText);
        chooseVideoButton = findViewById(R.id.chooseVideoButton);
        uploadButton = findViewById(R.id.uploadButton);
        progressBar = findViewById(R.id.progressBar);

        videoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        videoUri = result.getData().getData();
                        uploadButton.setEnabled(true);
                        Toast.makeText(this, "Đã chọn video", Toast.LENGTH_SHORT).show();
                    }
                });

        chooseVideoButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            videoPickerLauncher.launch(intent);
        });

        uploadButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString().trim();
            String desc = descEditText.getText().toString().trim();

            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tiêu đề và mô tả", Toast.LENGTH_SHORT).show();
                return;
            }
            if (videoUri == null) {
                Toast.makeText(this, "Vui lòng chọn video", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadVideoToCloudinary(title, desc);
        });
    }

    private void uploadVideoToCloudinary(String title, String description) {
        progressBar.setVisibility(View.VISIBLE);
        uploadButton.setEnabled(false);
        String videoId = UUID.randomUUID().toString();
        String userId = mAuth.getCurrentUser().getUid();

        Log.d("UploadVideo", "Tạo videoId: " + videoId);
        MediaManager.get().upload(videoUri)
                .unsigned("shortvideo_unsigned")
                .option("resource_type", "video")
                .option("folder", "short_videos")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d("UploadVideo", "Bắt đầu upload: " + requestId);
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        double progress = (double) bytes / totalBytes * 100;
                        Log.d("UploadVideo", "Tiến trình: " + progress + "%");
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String videoUrl = (String) resultData.get("secure_url");
                        Log.d("UploadVideo", "Upload thành công, URL: " + videoUrl);

                        Map<String, Object> videoData = new HashMap<>();
                        videoData.put("title", title);
                        videoData.put("desc", description);
                        videoData.put("url", videoUrl);
                        videoData.put("isFavorite", false);
                        videoData.put("userId", userId);
                        videoData.put("videoId", videoId); // Lưu videoId
                        videoData.put("likeCount", 0); // Khởi tạo lượt thích
                        videoData.put("dislikeCount", 0); // Khởi tạo lượt không thích

                        FirebaseDatabase.getInstance()
                                .getReference("videos")
                                .child(videoId)
                                .setValue(videoData)
                                .addOnCompleteListener(task -> {
                                    progressBar.setVisibility(View.GONE);
                                    uploadButton.setEnabled(true);
                                    if (task.isSuccessful()) {
                                        Log.d("UploadVideo", "Lưu video thành công, videoId: " + videoId);
                                        Toast.makeText(UploadVideoActivity.this, "Tải video thành công", Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        Log.e("UploadVideo", "Lỗi lưu dữ liệu", task.getException());
                                        Toast.makeText(UploadVideoActivity.this, "Lỗi lưu dữ liệu: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        progressBar.setVisibility(View.GONE);
                        uploadButton.setEnabled(true);
                        Log.e("UploadVideo", "Lỗi upload: " + error.getDescription());
                        Toast.makeText(UploadVideoActivity.this, "Lỗi upload: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.d("UploadVideo", "Tái thử upload: " + requestId);
                    }
                })
                .dispatch();
    }
}