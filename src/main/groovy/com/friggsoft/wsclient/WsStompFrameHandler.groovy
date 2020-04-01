package com.friggsoft.wsclient

import groovy.util.logging.Slf4j

import java.lang.reflect.Type

import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders

@Slf4j
final class WsStompFrameHandler implements StompFrameHandler {

    /**
     * The type we want the message deserialized into.
     *
     * The payload comes as "application/json"; convert it to a map. This needs
     * {@link org.springframework.messaging.converter.MappingJackson2MessageConverter}
     * to be registered as the {@link org.springframework.messaging.converter.MessageConverter}.
     */
    @Override
    Type getPayloadType(StompHeaders headers) {
        return Map.class
    }

    @Override
    void handleFrame(StompHeaders headers, Object payload) {
        WsStompSessionHandler.logHeaders(log, headers)
        log.info("Payload of type {}: {}", payload.getClass().getSimpleName(), payload.toString())
    }
}
