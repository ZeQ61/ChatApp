package com.example.chatapp.service;

import com.example.chatapp.dto.FriendListResponse;
import com.example.chatapp.dto.FriendshipResponse;
import com.example.chatapp.model.Friendship;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.FriendshipRepository;
import com.example.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FriendshipService {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    // Arkadaşlık isteği gönderme
    public FriendshipResponse sendFriendRequest(User requester, Long receiverId) {
        // Alıcı kullanıcıyı bul
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kullanıcı bulunamadı"));

        // Kendinize arkadaşlık isteği gönderemezsiniz
        if(requester.getId().equals(receiverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kendinize arkadaşlık isteği gönderemezsiniz");
        }

        // Mevcut bir arkadaşlık ilişkisi var mı kontrol et
        boolean existingRequest = friendshipRepository.findByRequesterAndReceiver(requester, receiver).isPresent() ||
                friendshipRepository.findByRequesterAndReceiver(receiver, requester).isPresent();

        if(existingRequest) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bu kullanıcı ile zaten bir arkadaşlık ilişkiniz var");
        }

        // Yeni arkadaşlık isteği oluştur
        Friendship friendship = new Friendship(requester, receiver);
        friendshipRepository.save(friendship);

        return new FriendshipResponse(friendship);
    }

    // Arkadaşlık isteğini kabul etme
    public FriendshipResponse acceptFriendRequest(User user, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arkadaşlık isteği bulunamadı"));

        // Sadece isteğin alıcısı kabul edebilir
        if(!friendship.getReceiver().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu isteği kabul etmeye yetkiniz yok");
        }

        // İstek zaten kabul edilmiş mi kontrol et
        if(friendship.getStatus() == Friendship.FriendshipStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bu istek zaten kabul edilmiş");
        }

        // İsteği kabul et
        friendship.setStatus(Friendship.FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);

        return new FriendshipResponse(friendship);
    }

    // Arkadaşlık isteğini reddetme
    public FriendshipResponse rejectFriendRequest(User user, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arkadaşlık isteği bulunamadı"));

        // Sadece isteğin alıcısı reddedebilir
        if(!friendship.getReceiver().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu isteği reddetmeye yetkiniz yok");
        }

        // İstek zaten reddedilmiş mi kontrol et
        if(friendship.getStatus() == Friendship.FriendshipStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bu istek zaten reddedilmiş");
        }

        // İsteği reddet
        friendship.setStatus(Friendship.FriendshipStatus.REJECTED);
        friendshipRepository.save(friendship);

        return new FriendshipResponse(friendship);
    }

    // Arkadaşlık isteğini iptal etme (gönderen tarafından)
    public void cancelFriendRequest(User user, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arkadaşlık isteği bulunamadı"));

        // Sadece isteğin göndereni iptal edebilir
        if(!friendship.getRequester().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu isteği iptal etmeye yetkiniz yok");
        }

        // İstek beklemede mi kontrol et
        if(friendship.getStatus() != Friendship.FriendshipStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sadece bekleyen istekler iptal edilebilir");
        }

        // İsteği sil
        friendshipRepository.delete(friendship);
    }

    // Arkadaşı silme
    public void removeFriend(User user, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Arkadaşlık bulunamadı"));

        // Kullanıcı bu arkadaşlık ilişkisinin bir parçası mı?
        if(!friendship.getRequester().getId().equals(user.getId()) && !friendship.getReceiver().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu arkadaşlık ilişkisini yönetmeye yetkiniz yok");
        }

        // İlişki kabul edilmiş mi kontrol et
        if(friendship.getStatus() != Friendship.FriendshipStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bu bir arkadaşlık ilişkisi değil");
        }

        // Arkadaşlık ilişkisini sil
        friendshipRepository.delete(friendship);
    }

    // Kullanıcının arkadaş listesini ve bekleyen istekleri getir
    public FriendListResponse getFriendsList(User user) {
        // Kabul edilmiş arkadaşlıkları bul (kullanıcı hem gönderen hem alıcı olabilir)
        List<Friendship> acceptedAsSender = friendshipRepository.findByRequesterAndStatus(user, Friendship.FriendshipStatus.ACCEPTED);
        List<Friendship> acceptedAsReceiver = friendshipRepository.findByReceiverAndStatus(user, Friendship.FriendshipStatus.ACCEPTED);

        // Bekleyen giden istekleri bul
        List<Friendship> pendingRequests = friendshipRepository.findByRequesterAndStatus(user, Friendship.FriendshipStatus.PENDING);

        // Bekleyen gelen istekleri bul
        List<Friendship> receivedRequests = friendshipRepository.findByReceiverAndStatus(user, Friendship.FriendshipStatus.PENDING);

        // Arkadaşları özel DTO formatına dönüştür
        List<FriendshipResponse> friendResponses = new ArrayList<>();
        
        // Tüm kabul edilmiş arkadaşlıkları birleştir
        List<Friendship> allAcceptedFriendships = new ArrayList<>();
        allAcceptedFriendships.addAll(acceptedAsSender);
        allAcceptedFriendships.addAll(acceptedAsReceiver);
        
        // Arkadaşlıkları dönüştür ve her biri için doğru arkadaş bilgisini belirle
        for (Friendship friendship : allAcceptedFriendships) {
            // Kullanıcının ID'sini kullanarak arkadaşı belirleyen constructor'ı kullan
            FriendshipResponse response = new FriendshipResponse(friendship, user.getId());
            friendResponses.add(response);
        }

        // Giden istekleri DTO'lara dönüştür
        List<FriendshipResponse> pendingResponses = pendingRequests.stream()
                .map(FriendshipResponse::new)
                .collect(Collectors.toList());

        // Gelen istekleri DTO'lara dönüştür
        List<FriendshipResponse> receivedResponses = receivedRequests.stream()
                .map(FriendshipResponse::new)
                .collect(Collectors.toList());

        return new FriendListResponse(friendResponses, pendingResponses, receivedResponses);
    }

    // Kullanıcı arama
    public List<FriendshipResponse.UserSummary> searchUsers(User currentUser, String query) {
        // Kullanıcı adı veya email ile arama yap
        List<User> users = userRepository.findByUsernameContainingOrEmailContainingOrIsimContainingOrSoyadContaining(query, query, query, query);

        // Kendini listeden çıkar
        users = users.stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .collect(Collectors.toList());

        // Kullanıcı bilgilerini DTO'ya dönüştür
        return users.stream()
                .map(user -> new FriendshipResponse.UserSummary(
                        user.getId(),
                        user.getUsername(),
                        user.getIsim(),
                        user.getSoyad(),
                        user.getProfileImageUrl(),
                        user.isOnline())
                )
                .collect(Collectors.toList());
    }
} 