// Log config for logback

appender('console', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss} %highlight(%-5level) %cyan([%-9thread]) %logger{20} - %msg%n"
    }
    immediateFlush = false
}

// Set logging levels
logger("com.friggsoft", INFO)
logger("org.springframework", WARN)
logger("org.springframework.web.socket.sockjs.client.SockJsClient", DEBUG)
logger("org.springframework.web.socket.messaging", TRACE)
logger("org.springframework.web.socket", TRACE)

// Setup a console logger
root(INFO, ['console'])
