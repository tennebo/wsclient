package com.friggsoft.wsclient

import java.lang.reflect.Type

import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders

import groovy.util.logging.Slf4j

@Slf4j
final class WsStompFrameHandler implements StompFrameHandler {

    /**
     * The type we want the message serialized into.
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
