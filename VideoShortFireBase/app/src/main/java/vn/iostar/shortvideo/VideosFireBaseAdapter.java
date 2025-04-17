package vn.iostar.shortvideo;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class VideosFireBaseAdapter extends FirebaseRecyclerAdapter<Video1Model, VideosFireBaseAdapter.MyHolder> {
    public VideosFireBaseAdapter(@NonNull FirebaseRecyclerOptions<Video1Model> options) {
        super(options);
    }

    public class MyHolder extends RecyclerView.ViewHolder {
        private VideoView videoView;
        private ProgressBar videoProgressBar;
        private TextView textVideoTitle;
        private TextView textVideoDescription;
        private ImageView inPerson, favorites, imShare, imMore, imLike, imDislike, userAvatarImageView;
        private TextView likeCountTextView, dislikeCountTextView, userEmailTextView;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.videoView);
            videoProgressBar = itemView.findViewById(R.id.videoProgressBar);
            textVideoTitle = itemView.findViewById(R.id.textVideoTitle);
            textVideoDescription = itemView.findViewById(R.id.textVideoDescription);
            inPerson = itemView.findViewById(R.id.imPerson);
            favorites = itemView.findViewById(R.id.imFavorite);
            imShare = itemView.findViewById(R.id.imShare);
            imMore = itemView.findViewById(R.id.imMore);
            imLike = itemView.findViewById(R.id.imLike);
            imDislike = itemView.findViewById(R.id.imDislike);
            likeCountTextView = itemView.findViewById(R.id.likeCountTextView);
            dislikeCountTextView = itemView.findViewById(R.id.dislikeCountTextView);
            userAvatarImageView = itemView.findViewById(R.id.userAvatarImageView);
            userEmailTextView = itemView.findViewById(R.id.userEmailTextView);
            itemView.setTag(this);
        }

        public void pauseVideo() {
            if (videoView != null && videoView.isPlaying()) {
                videoView.pause();
            }
        }
    }

    @Override
    protected void onBindViewHolder(@NonNull MyHolder holder, int position, @NonNull Video1Model model) {
        holder.textVideoTitle.setText(model.getTitle());
        holder.textVideoDescription.setText(model.getDesc());
        try {
            holder.videoView.setVideoURI(Uri.parse(model.getUrl()));
        } catch (Exception e) {
            holder.videoProgressBar.setVisibility(View.GONE);
            holder.textVideoDescription.setText("Lỗi tải video");
            return;
        }

        holder.videoView.setOnPreparedListener(mp -> {
            holder.videoProgressBar.setVisibility(View.GONE);
            mp.setLooping(true);
            mp.start();
            float videoRatio = mp.getVideoWidth() / (float) mp.getVideoHeight();
            float screenRatio = holder.videoView.getWidth() / (float) holder.videoView.getHeight();
            if (screenRatio != 0) {
                float scale = videoRatio / screenRatio;
                if (scale >= 1f) {
                    holder.videoView.setScaleX(scale);
                } else {
                    holder.videoView.setScaleY(1f / scale);
                }
            }
        });

        holder.videoView.setOnErrorListener((mp, what, extra) -> {
            holder.videoProgressBar.setVisibility(View.GONE);
            holder.textVideoDescription.setText("Lỗi phát video");
            return true;
        });

        // Tải ảnh đại diện và email
        String userId = model.getUserId();
        if (userId != null && !userId.isEmpty()) {
            FirebaseDatabase.getInstance().getReference("users").child(userId)
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            String profileImage = task.getResult().child("profileImage").getValue(String.class);
                            String email = task.getResult().child("email").getValue(String.class);
                            if (profileImage != null && !profileImage.isEmpty()) {
                                Glide.with(holder.itemView.getContext())
                                        .load(profileImage)
                                        .into(holder.userAvatarImageView);
                            }
                            if (email != null && !email.isEmpty()) {
                                holder.userEmailTextView.setText(email);
                            }
                        }
                    });
        }

        // Favorite button
        String videoId = getRef(position).getKey();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        holder.favorites.setImageResource(model.isFavorite() ? R.drawable.ic_fill_favorite : R.drawable.ic_favorite);
        holder.favorites.setOnClickListener(v -> {
            boolean newFavState = !model.isFavorite();
            FirebaseDatabase.getInstance().getReference("videos").child(videoId)
                    .child("isFavorite").setValue(newFavState)
                    .addOnSuccessListener(aVoid -> {
                        model.setFavorite(newFavState);
                        holder.favorites.setImageResource(newFavState ? R.drawable.ic_fill_favorite : R.drawable.ic_favorite);
                        Toast.makeText(holder.itemView.getContext(), newFavState ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    });
        });

        // Like button
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        boolean isLiked = model.getLikes() != null && model.getLikes().containsKey(currentUserId);
        boolean isDisliked = model.getDislikes() != null && model.getDislikes().containsKey(currentUserId);
        holder.imLike.setImageResource(isLiked ? R.drawable.ic_fill_favorite : R.drawable.ic_favorite);
        holder.imDislike.setImageResource(isDisliked ? R.drawable.ic_fill_favorite : R.drawable.ic_favorite);
        holder.likeCountTextView.setText(String.valueOf(model.getLikeCount()));
        holder.dislikeCountTextView.setText(String.valueOf(model.getDislikeCount()));

        holder.imLike.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(holder.itemView.getContext(), "Vui lòng đăng nhập để thích video", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> updates = new HashMap<>();
            if (isLiked) {
                updates.put("likes/" + currentUserId, null);
                updates.put("likeCount", model.getLikeCount() - 1);
            } else {
                updates.put("likes/" + currentUserId, true);
                updates.put("likeCount", model.getLikeCount() + 1);
                if (isDisliked) {
                    updates.put("dislikes/" + currentUserId, null);
                    updates.put("dislikeCount", model.getDislikeCount() - 1);
                }
            }
            FirebaseDatabase.getInstance().getReference("videos").child(videoId)
                    .updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(holder.itemView.getContext(), isLiked ? "Đã bỏ thích" : "Đã thích", Toast.LENGTH_SHORT).show();
                    });
        });

        // Dislike button
        holder.imDislike.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(holder.itemView.getContext(), "Vui lòng đăng nhập để không thích video", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> updates = new HashMap<>();
            if (isDisliked) {
                updates.put("dislikes/" + currentUserId, null);
                updates.put("dislikeCount", model.getDislikeCount() - 1);
            } else {
                updates.put("dislikes/" + currentUserId, true);
                updates.put("dislikeCount", model.getDislikeCount() + 1);
                if (isLiked) {
                    updates.put("likes/" + currentUserId, null);
                    updates.put("likeCount", model.getLikeCount() - 1);
                }
            }
            FirebaseDatabase.getInstance().getReference("videos").child(videoId)
                    .updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(holder.itemView.getContext(), isDisliked ? "Đã bỏ không thích" : "Đã không thích", Toast.LENGTH_SHORT).show();
                    });
        });

        // Share button
        holder.imShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, model.getUrl());
            holder.itemView.getContext().startActivity(Intent.createChooser(shareIntent, "Chia sẻ video"));
        });

        // Person button
        holder.inPerson.setOnClickListener(v -> {
            Toast.makeText(holder.itemView.getContext(), "Chuyển đến trang cá nhân", Toast.LENGTH_SHORT).show();
        });
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_video_row, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onViewRecycled(@NonNull MyHolder holder) {
        super.onViewRecycled(holder);
        holder.pauseVideo();
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        notifyDataSetChanged();
    }
}