package vn.iostar.shortvideo;

import java.io.Serializable;
import java.util.Map;

public class Video1Model implements Serializable {
    private String title;
    private String desc;
    private String url;
    private boolean isFavorite;
    private String userId;
    private String videoId;
    private long likeCount;
    private long dislikeCount;
    private Map<String, Boolean> likes;
    private Map<String, Boolean> dislikes;

    public Video1Model() {
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc != null ? desc : "";
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getUrl() {
        return url != null ? url : "";
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public String getUserId() {
        return userId != null ? userId : "";
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getVideoId() {
        return videoId != null ? videoId : "";
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(long likeCount) {
        this.likeCount = likeCount;
    }

    public long getDislikeCount() {
        return dislikeCount;
    }

    public void setDislikeCount(long dislikeCount) {
        this.dislikeCount = dislikeCount;
    }

    public Map<String, Boolean> getLikes() {
        return likes;
    }

    public void setLikes(Map<String, Boolean> likes) {
        this.likes = likes;
    }

    public Map<String, Boolean> getDislikes() {
        return dislikes;
    }

    public void setDislikes(Map<String, Boolean> dislikes) {
        this.dislikes = dislikes;
    }
}