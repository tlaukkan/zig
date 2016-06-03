package org.bubblecloud;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackMessageHandle;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import com.ullink.slack.simpleslackapi.replies.SlackChannelReply;
import com.ullink.slack.simpleslackapi.replies.SlackReply;
import org.apache.log4j.xml.DOMConfigurator;
import org.bubblecloud.zigbee.ZigBeeConsoleApiClient;
import org.bubblecloud.zigbee.ZigBeeConsoleRpcApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Zig console main.
 */
public class Zig {
    /**
     * The logger.
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(Zig.class);
    /**
     * Flag reflecting whether shutdown is requested.
     */
    private static boolean shutdown = false;

    /**
     * Main method of ZigConsole.
     * @param args the command line arguments
     * @throws IOException if IO exception occurs
     */
    public static void main(final String[] args) throws IOException {
        DOMConfigurator.configure("log4j.xml");

        final ZigBeeConsoleApiClient zigBeeConsoleApiClient = new ZigBeeConsoleApiClient(args[1], args[2]);
        final ZigBeeConsoleRpcApi zigBeeConsoleApi = zigBeeConsoleApiClient.getZigBeeConsoleApi();

        final SlackSession session = SlackSessionFactory.createWebSocketSlackSession(args[0]);
        session.connect();
        final String zigId = session.sessionPersona().getId();

        try {
            final SlackChannel channel = session.findChannelByName("general");

            session.addMessagePostedListener(new SlackMessagePostedListener() {
                @Override
                public void onEvent(final SlackMessagePosted event, final SlackSession session) {
                    if (!zigId.equals(event.getSender().getId())
                            && event.getChannel().isDirect()) {
                        session.sendMessage(event.getChannel(), zigBeeConsoleApi.execute(event.getMessageContent()));
                    }
                }
            });
            LOGGER.info(channel.toString());

            //System.out.println(zigBeeConsoleApi.execute("help"));
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    shutdown = true;
                }
            }));

            while (!shutdown) {
                Thread.sleep(100);
            }

        } catch (final Exception e) {
            LOGGER.error("Error in Zig, exiting. ", e);
        } finally {
            session.disconnect();
        }
    }

}
