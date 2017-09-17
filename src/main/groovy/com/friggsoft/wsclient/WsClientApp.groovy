package com.friggsoft.wsclient

import java.util.concurrent.ExecutionException

import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.client.WebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.Transport
import org.springframework.web.socket.sockjs.client.WebSocketTransport

import groovy.util.logging.Slf4j

@Slf4j
final class WsClientApp {

    /** Websocket server URL. */
    static final String WS_URL = "ws://localhost:8080/ws"

    /** Websocket app destination. */
    static final String APP_DST = "/app"

    /** Websocket topic destination. */
    static final String TOPIC_DST = "/topic/pulse"

    static void main(String[] args) {
        boolean useSockJs = args.length > 0

        log.info("Here we go! {}using SockJS.", (useSockJs? "" : "Not "))

        WebSocketClient webSocketClient = new StandardWebSocketClient()
        WebSocketStompClient stompClient
        if (useSockJs) {
            def transports = new ArrayList<Transport>()
            transports.add(new WebSocketTransport(webSocketClient))
            transports.add(new RestTemplateXhrTransport())
            def sockJsClient = new SockJsClient(transports)
            stompClient = new WebSocketStompClient(sockJsClient)
        } else {
            stompClient = new WebSocketStompClient(webSocketClient)
        }

        // Avoid StringMessageConverter; it only works with "text/plain" messages
        def messageConverter = new MappingJackson2MessageConverter()
        messageConverter.prettyPrint = true
        messageConverter.strictContentTypeMatch = false
        stompClient.setMessageConverter(messageConverter)

        // For heartbeats and receipt tracking
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler()
        taskScheduler.afterPropertiesSet()
        stompClient.setTaskScheduler(taskScheduler)
        stompClient.setReceiptTimeLimit(5000)

        def sessionHandler = new WsStompSessionHandler(TOPIC_DST)
        def futureSession = stompClient.connect(WS_URL, sessionHandler)
        StompSession session

        try {
            session = futureSession.get()
            log.info("Session {} connected: {}", session.sessionId, session.connected)
        } catch (ExecutionException ex) {
            log.error("Cannot connect to {}: {}", WS_URL, ex.cause.message)
            System.exit(1)
            return // To keep the compiler happy
        }

        // Handle SIGTERM and SIGINT (Control+C) we well as normal exits
        def shutdownRunner = { ->
            log.info("Unsubscribing to {}", sessionHandler.subscription)
            sessionHandler.subscription.unsubscribe()
            log.info("Disconnecting session {}", session.sessionId)
            session.disconnect()
        }
        Thread shutdownThread = new Thread(shutdownRunner, "shutdown-hook")
        Runtime.getRuntime().addShutdownHook(shutdownThread)

        def inReader = new BufferedReader(new InputStreamReader(System.in))
        for (;;) {
            String line = inReader.readLine()
            if (line == null) {
                log.info("Control+D... Bye.")
                break
            }
            if (!line.empty) {
                log.info("Sending {} to destination {}", line, APP_DST)
                session.send(APP_DST, line)
            }
        }
        System.exit(0)
    }
}
