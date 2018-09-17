package com.friggsoft.wsclient

import java.lang.reflect.Type

import org.slf4j.Logger
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandler

import groovy.util.logging.Slf4j

@Slf4j
final class WsStompSessionHandler implements StompSessionHandler {

    /** URL of topic to subscribe to. */
    private final String topicDst

    /** Capture the subscription, so we can unsubscribe later. */
    StompSession.Subscription subscription = null

    WsStompSessionHandler(String topicDst) {
        this.topicDst = topicDst
    }

    static void logHeaders(StompHeaders headers) {
        logHeaders(log, headers)
    }

    static void logHeaders(Logger logger, StompHeaders headers) {
        for (def entry : headers) {
            List<String> values = entry.value
            logger.info("STOMP Header '{}': {}", entry.key, values)
        }
    }

    @Override
    void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        log.info("Connected to session {}", session.sessionId)
        logHeaders(connectedHeaders)

        log.info("Subscribing to {}...", topicDst)
        session.autoReceipt = true
        subscription = session.subscribe(topicDst, new WsStompFrameHandler())
        log.info("Subscribed to {}", subscription.toString())
    }

    /** When is this called!!? */
    @Override
    Type getPayloadType(StompHeaders headers) {
        return String.class
    }

    @Override
    void handleFrame(StompHeaders headers, Object payload) {
        logHeaders(headers)
        log.info("Received payload: {}", payload == null? "<null>" : payload.toString())
    }

    @Override
    void handleException(
            StompSession session, StompCommand command, StompHeaders headers,
            byte[] payload, Throwable ex) {
        log.error("Command {} failed on session {}: {}", command, session.sessionId, ex.message)
        logHeaders(headers)
    }

    @Override
    void handleTransportError(StompSession session, Throwable ex) {
        log.error("Transport error on session {}: {}", session.sessionId, ex.message)
    }
}
