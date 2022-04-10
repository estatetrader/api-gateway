package com.estatetrader.apigw;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.estatetrader.apigw.server.GatewayNettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication(scanBasePackages = "com.estatetrader.apigw")
@ImportResource("classpath*:applicationContext.xml")
public class Application implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    private final Logger logger = LoggerFactory.getLogger(Application.class);

    private final GatewayNettyServer server;

    public Application(GatewayNettyServer server) {
        this.server = server;
    }

    /**
     * Callback used to run the bean.
     *
     * @param args incoming main method arguments
     * @throws Exception on error
     */
    @Override
    public void run(String... args) throws Exception {
        server.run();
    }

    @Value("${com.estatetrader.root.log.level}")
    public void setLogLevel(String logLevel) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        logger.info("change log level to {}", logLevel);
        context.getLogger("root").setLevel(Level.toLevel(logLevel));
    }
}
