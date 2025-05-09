package com.example.chatapp.service;

import com.example.chatapp.dto.LoginRequest;
import com.example.chatapp.dto.ProfileResponse;
import com.example.chatapp.dto.ProfileUpdateRequest;
import com.example.chatapp.dto.RegisterRequest;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Value("${file.upload-dir:uploads/images}")
    private String uploadDir;
    
    private Path getUploadPath() {
        Path uploadPath = Paths.get(uploadDir);
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            return uploadPath;
        } catch (IOException e) {
            throw new RuntimeException("Yükleme dizini oluşturulamadı", e);
        }
    }

    // Kullanıcı kayıt işlemi
    public String registerUser(RegisterRequest registerRequest) throws Exception {
        if (registerRequest.getIsim() == null || registerRequest.getSoyad() == null || registerRequest.getUsername() == null || registerRequest.getPassword() == null || registerRequest.getEmail() == null) {
            throw new Exception("Kullanıcı adı, şifre ve email boş olamaz.");
        }
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new Exception("Bu isim zaten var");
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new Exception("Bu E-mail zaten var");
        }
        User user = new User();
        user.setBio("ben " + registerRequest.getUsername());
        user.setCreatedAt(LocalDateTime.now());
        user.setIsim(registerRequest.getIsim());
        user.setSoyad(registerRequest.getSoyad());
        user.setProfileImageUrl("https://www.google.com/url?sa=i&url=https%3A%2F%2Fwww.vecteezy.com%2Ffree-vector%2Fdefault-user&psig=AOvVaw0bf3KnP0ZkMsPPFwmYLR_q&ust=1746032747378000&source=images&cd=vfe&opi=89978449&ved=0CBEQjRxqFwoTCOjfkIDd_YwDFQAAAAAdAAAAABAE");
        user.setOnline(false);
        user.setUpdatedAt(null);
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        
        // Şifreyi BCrypt ile hashle ve kaydet
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        
        userRepository.save(user);
        // Kayıt işlemini yap...
        return "Kullanıcı kaydı başarılı.";
    }

    // Kullanıcı girişi işlemi
    public String loginUser(LoginRequest loginRequest) throws Exception {
        if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
            throw new Exception("Kullanıcı adı ve şifre boş olamaz.");
        }

        Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());

        if (userOpt.isEmpty()) {
            throw new Exception("Kullanıcı bulunamadı.");
        }

        User user = userOpt.get();
        
        // Şifreyi BCrypt ile doğrula
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new Exception("Şifre yanlış.");
        }
        
        // Kullanıcı girişinde online durumunu değiştirmiyoruz
        // Kullanıcı son durumunu koruyor
        
        // Şifre doğruysa token oluştur ve dön
        return jwtService.generateToken(loginRequest.getUsername());
    }

    // Profil bilgisini getir
    public ProfileResponse getProfile(String token) throws Exception {

        if (token.isEmpty()) {
            throw new Exception("token bulunamadı.");
        }
        String userName = jwtService.extractUsername(token);
        Optional<User> user = userRepository.findByUsername(userName);

        if (user.isEmpty()) {
            throw new Exception("Kullanıcı bulunamadı.");
        }
        return ProfileResponse.builder()
                .bio(user.get().getBio())
                .email(user.get().getEmail())
                .isim(user.get().getIsim())
                .userName(user.get().getUsername())
                .profileImageUrl(user.get().getProfileImageUrl())
                .soyad(user.get().getSoyad())
                .isOnline(user.get().isOnline())
                .build();
    }
    
    // Profil güncelleme
    public ProfileResponse updateProfile(String token, ProfileUpdateRequest updateRequest) throws Exception {
        if (token.isEmpty()) {
            throw new Exception("Token bulunamadı.");
        }
        
        String username = jwtService.extractUsername(token);
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            throw new Exception("Kullanıcı bulunamadı.");
        }
        
        User user = userOpt.get();
        
        // Güncellenecek alanları kontrol et ve güncelle
        if (updateRequest.getBio() != null && !updateRequest.getBio().isEmpty()) {
            user.setBio(updateRequest.getBio());
        }
        
        if (updateRequest.getProfileImageUrl() != null && !updateRequest.getProfileImageUrl().isEmpty()) {
            user.setProfileImageUrl(updateRequest.getProfileImageUrl());
        }
        
        // Şifre güncelleme (opsiyonel)
        if (updateRequest.getPassword() != null && !updateRequest.getPassword().isEmpty()) {
            // Yeni şifreyi BCrypt ile hashle
            user.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
        }
        
        // Güncelleme zamanını ayarla
        user.setUpdatedAt(LocalDateTime.now());
        
        // Kullanıcıyı kaydet
        userRepository.save(user);
        
        // Güncellenmiş profil bilgilerini döndür
        return ProfileResponse.builder()
                .bio(user.getBio())
                .email(user.getEmail())
                .isim(user.getIsim())
                .userName(user.getUsername())
                .profileImageUrl(user.getProfileImageUrl())
                .soyad(user.getSoyad())
                .isOnline(user.isOnline())
                .build();
    }
    
    // Profil fotoğrafı yükleme
    public ProfileResponse updateProfileImage(String token, MultipartFile file) throws Exception {
        if (token.isEmpty()) {
            throw new Exception("Token bulunamadı.");
        }
        
        String username = jwtService.extractUsername(token);
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isEmpty()) {
            throw new Exception("Kullanıcı bulunamadı.");
        }
        
        User user = userOpt.get();
        
        // Dosya adını benzersiz yap (UUID ile)
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFileName = UUID.randomUUID().toString() + fileExtension;
        
        // Dosyayı kaydet
        Path targetLocation = getUploadPath().resolve(newFileName);
        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new Exception("Profil resmi yüklenemedi: " + e.getMessage());
        }
        
        // Kullanıcının profil resmini güncelle
        String fileUrl = "/user/images/" + newFileName;
        user.setProfileImageUrl(fileUrl);
        user.setUpdatedAt(LocalDateTime.now());
        
        // Kullanıcıyı kaydet
        userRepository.save(user);
        
        // Güncellenmiş profil bilgilerini döndür
        return ProfileResponse.builder()
                .bio(user.getBio())
                .email(user.getEmail())
                .isim(user.getIsim())
                .userName(user.getUsername())
                .profileImageUrl(user.getProfileImageUrl())
                .soyad(user.getSoyad())
                .isOnline(user.isOnline())
                .build();
    }
    
    // Profil fotoğrafını getir
    public Resource loadImageAsResource(String fileName) {
        try {
            Path filePath = getUploadPath().resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("Dosya bulunamadı: " + fileName);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Dosya bulunamadı: " + fileName, e);
        }
    }
    
    // Online durum güncelleme
    public ProfileResponse updateOnlineStatus(String token, boolean isOnline) throws Exception {
        System.out.println("UserService: Updating online status to: " + isOnline + " for token: " + token.substring(0, 10) + "...");
        
        if (token == null || token.isEmpty()) {
            System.err.println("Token is empty or null");
            throw new Exception("Token bulunamadı.");
        }
        
        try {
            // Token'dan kullanıcı adını çıkar
            String username = jwtService.extractUsername(token);
            System.out.println("UserService: Extracted username from token: " + username);
            
            if (username == null || username.isEmpty()) {
                throw new Exception("Geçersiz token - kullanıcı adı alınamadı");
            }
            
            // Kullanıcıyı bul
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                System.err.println("User not found for username: " + username);
                throw new Exception("Kullanıcı bulunamadı.");
            }
            
            User user = userOpt.get();
            System.out.println("UserService: Found user: " + user.getUsername() + ", current online status: " + user.isOnline());
            
            // Online durumunu güncelle - Boolean değerini double-check et
            boolean newStatus = Boolean.valueOf(isOnline);
            System.out.println("UserService: Setting status to " + newStatus + " (boolean type)");
            user.setOnline(newStatus);
            
            // Güncelleme zamanını ayarla
            user.setUpdatedAt(LocalDateTime.now());
            
            // Kullanıcıyı kaydet
            User savedUser = userRepository.save(user);
            
            // Veritabanına doğru kaydedildiğinden emin ol
            if (savedUser.isOnline() != newStatus) {
                System.err.println("WARNING: Database update did not reflect the requested status change!");
                System.err.println("Requested: " + newStatus + ", Saved: " + savedUser.isOnline());
                // Tekrar kaydetmeyi dene
                savedUser.setOnline(newStatus);
                savedUser = userRepository.save(savedUser);
                
                if (savedUser.isOnline() != newStatus) {
                    throw new Exception("Durum güncellemesi veritabanına kaydedilemedi.");
                }
            }
            
            System.out.println("UserService: User saved successfully with new online status: " + savedUser.isOnline());
            
            // Güncellenmiş profil bilgilerini döndür
            ProfileResponse response = ProfileResponse.builder()
                    .bio(user.getBio())
                    .email(user.getEmail())
                    .isim(user.getIsim())
                    .userName(user.getUsername())
                    .profileImageUrl(user.getProfileImageUrl())
                    .soyad(user.getSoyad())
                    .isOnline(user.isOnline())
                    .build();
            
            System.out.println("UserService: Returning profile response with online status: " + response.isOnline());
            return response;
        } catch (Exception e) {
            System.err.println("ERROR in updateOnlineStatus: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    // Online durum güncelleme - token olmadan (test için)
    public ProfileResponse updateOnlineStatusWithoutToken(boolean isOnline) throws Exception {
        System.out.println("Updating online status without token check to: " + isOnline);
        
        try {
            // Test için ilk kullanıcının durumunu güncelle
            User user = userRepository.findAll().stream().findFirst().orElseThrow(() -> new Exception("Kullanıcı bulunamadı"));
            System.out.println("Found first user: " + user.getUsername() + ", updating online status");
            
            // Online durumunu parametreden gelen değere göre ayarla
            user.setOnline(isOnline);
            
            // Güncelleme zamanını ayarla
            user.setUpdatedAt(LocalDateTime.now());
            
            // Kullanıcıyı kaydet
            User savedUser = userRepository.save(user);
            System.out.println("User saved successfully with new online status: " + savedUser.isOnline());
            
            // Güncellenmiş profil bilgilerini döndür
            return ProfileResponse.builder()
                    .bio(user.getBio())
                    .email(user.getEmail())
                    .isim(user.getIsim())
                    .userName(user.getUsername())
                    .profileImageUrl(user.getProfileImageUrl())
                    .soyad(user.getSoyad())
                    .isOnline(user.isOnline())
                    .build();
        } catch (Exception e) {
            System.err.println("ERROR in updateOnlineStatusWithoutToken: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public User getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kullanıcı oturum açmamış");
        }
        
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));
    }
}

