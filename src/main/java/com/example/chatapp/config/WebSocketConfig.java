package com.example.chatapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;


@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/user"); // /user/queue/messages gibi birebir iletişim
        config.setApplicationDestinationPrefixes("/app"); // Client -> /app/sendMessage gibi yollar
        config.setUserDestinationPrefix("/user"); // Özel kullanıcı mesajları için
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setHandshakeHandler(new UserHandshakeHandler())
                .setAllowedOrigins("http://localhost:3000") // React uygulamasının çalıştığı adres
                .withSockJS();
    }


}
