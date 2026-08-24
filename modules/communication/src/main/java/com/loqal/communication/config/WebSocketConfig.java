package com.loqal.communication.config;

import com.loqal.communication.chat.ChatService;
import com.loqal.communication.chat.ChatWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.util.Map;

@Configuration
public class WebSocketConfig {

    @Bean
    public HandlerMapping chatWebSocketMapping(ChatService chatService) {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        Map<String, WebSocketHandler> routes = Map.of("/communication/chat/ws", new ChatWebSocketHandler(chatService));
        mapping.setUrlMap(routes);
        mapping.setOrder(-1);
        return mapping;
    }
}
