package ocrlabeler.controllers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General utilities to wait for ports and URL responses to be available.
 * Especially useful when waiting for container services to be fully "up".
 */
public class WaitFor {
    private WaitFor() {

    }

    private static final Logger logger = LoggerFactory.getLogger(WaitFor.class);

    public static void waitForPort(String hostname, int port, long timeoutMs) {
        logger.info("Waiting for port " + port);
        long startTs = System.currentTimeMillis();
        boolean scanning = true;
        while (scanning) {
            if (System.currentTimeMillis() - startTs > timeoutMs) {
                throw new RuntimeException("Timeout waiting for port " + port);
            }
            try {
                SocketAddress addr = new InetSocketAddress(hostname, port);
                Selector.open();
                SocketChannel socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(true);
                try {
                    socketChannel.connect(addr);
                } finally {
                    socketChannel.close();
                }

                scanning = false;
            } catch (IOException e) {
                logger.debug("Still waiting for port " + port);
                try {
                    Thread.sleep(2000);// 2 seconds
                } catch (InterruptedException ie) {
                    logger.error("Interrupted", ie);
                }
            }
        }
        logger.info("Port " + port + " ready.");
    }
}