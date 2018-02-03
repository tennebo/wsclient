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
    static String webSocketUrl = "ws://localhost:8080/ws"

    /** Websocket app destination. */
    static String appDest = "/app"

    /** Websocket topic destination. */
    static String topic = "/topic/pulse"

    /** Create a new WebSocket client, with or w/o SockJS. */
    static WebSocketStompClient createStompClient(boolean useSockJs) {
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
        return stompClient
    }

    /**
     * Avoid StringMessageConverter; it only works with "text/plain" messages.
     */
    static void setupMessageConverter(WebSocketStompClient stompClient) {
        def messageConverter = new MappingJackson2MessageConverter()
        messageConverter.prettyPrint = true
        messageConverter.strictContentTypeMatch = false
        stompClient.setMessageConverter(messageConverter)
    }

    static void main(String[] args) {
        // Parse the commandline
        def cli = new CliBuilder(usage: 'WsClient -[hutas]', stopAtNonOption: false)
        cli.with {
            h longOpt: 'help', 'Show help'
            u longOpt: 'url', args: 1, argName: 'url', 'WebSocket URL to connect to'
            t longOpt: 'topic', args:1, argName: 'topic', 'WebSocket topic to listen to'
            a longOpt: 'app', args:1, argName: 'app', 'App destination'
            s longOpt: 'sockjs', 'Use SockJS'
        }

        def options = cli.parse(args)
        if (!options || options.h) {
            // Usage info has already been printed if options is null
            options && cli.usage()
            return
        }

        if (options.u) {
            webSocketUrl = options.url
        }
        if (options.a) {
            appDest = options.app
        }
        if (options.t) {
            topic = options.topic
        }
        boolean useSockJs = options.s

        log.info("Here we go! {}using SockJS.", (useSockJs? "" : "Not "))
        WebSocketStompClient stompClient = createStompClient(useSockJs)
        setupMessageConverter(stompClient)

        // For heartbeats and receipt tracking
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler()
        taskScheduler.afterPropertiesSet()
        stompClient.setTaskScheduler(taskScheduler)
        stompClient.setReceiptTimeLimit(5000)

        def sessionHandler = new WsStompSessionHandler(topic)
        def futureSession = stompClient.connect(webSocketUrl, sessionHandler)
        StompSession session
        try {
            session = futureSession.get()
            log.info("Session {} connected: {}", session.sessionId, session.connected)
        } catch (ExecutionException ex) {
            log.error("Cannot connect to {}: {}", webSocketUrl, ex.cause.message)
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
                log.info("Sending {} to destination {}", line, appDest)
                session.send(appDest, line)
            }
        }
        System.exit(0)
    }
}
