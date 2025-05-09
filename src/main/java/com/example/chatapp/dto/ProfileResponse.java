package com.example.chatapp.dto;

public class ProfileResponse {
    private String isim;
    private String soyad;
    private String userName;
    private String email;
    private String profileImageUrl;
    private String bio;
    private boolean isOnline;

    // Boş constructor
    public ProfileResponse() {
    }

    // Parametreli constructor
    public ProfileResponse(String isim, String soyad, String userName, String email, String profileImageUrl, String bio, boolean isOnline) {
        this.isim = isim;
        this.soyad = soyad;
        this.userName = userName;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
        this.isOnline = isOnline;
    }

    // ✅ Manuel builder metodu
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String isim;
        private String soyad;
        private String userName;
        private String email;
        private String profileImageUrl;
        private String bio;
        private boolean isOnline;

        public Builder isim(String isim) {
            this.isim = isim;
            return this;
        }

        public Builder soyad(String soyad) {
            this.soyad = soyad;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder profileImageUrl(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
            return this;
        }

        public Builder bio(String bio) {
            this.bio = bio;
            return this;
        }
        
        public Builder isOnline(boolean isOnline) {
            this.isOnline = isOnline;
            return this;
        }

        public ProfileResponse build() {
            return new ProfileResponse(isim, soyad, userName, email, profileImageUrl, bio, isOnline);
        }
    }

    // Getter ve setter'lar
    public String getIsim() { return isim; }
    public void setIsim(String isim) { this.isim = isim; }
    public String getSoyad() { return soyad; }
    public void setSoyad(String soyad) { this.soyad = soyad; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
}
