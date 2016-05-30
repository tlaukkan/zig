package org.bubblecloud;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.xml.DOMConfigurator;
import org.bubblecloud.zigbee.ZigBeeApi;
import org.bubblecloud.zigbee.api.Device;
import org.bubblecloud.zigbee.network.model.DiscoveryMode;
import org.bubblecloud.zigbee.network.port.ZigBeeSerialPortImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Zig console main.
 */
public class ZigConsole {
    /**
     * The logger.
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ZigBeeSerialPortImpl.class);

    /**
     * Main method of ZigConsole.
     * @param args the command line arguments
     * @throws IOException if IO exception occurs
     */
    public static void main(final String[] args) throws IOException {
        DOMConfigurator.configure("log4j.xml");

        final boolean resetNetwork = false;
        final ZigBeeSerialPortImpl serialPort = new ZigBeeSerialPortImpl("COM5", 38400);
        final ZigBeeApi zigbeeApi = new ZigBeeApi(serialPort, 4951, 11, false, DiscoveryMode.ALL);

        final File networkStateFile = new File("network.json");
        final boolean networkStateExists = networkStateFile.exists();
        if (!resetNetwork && networkStateExists) {
            LOGGER.info("ZigBeeApi loading network state...");
            final String networkState = FileUtils.readFileToString(networkStateFile);
            zigbeeApi.deserializeNetworkState(networkState);
            LOGGER.info("ZigBeeApi loading network state done.");
        }

        LOGGER.info("ZigBeeApi startup...");
        if (!zigbeeApi.startup()) {
            LOGGER.error("Error initializing ZigBeeApi.");
            return;
        }
        LOGGER.info("ZigBeeApi startup done.");

        if (!networkStateExists) {
            LOGGER.info("ZigBeeApi initial browsing...");
            while (!zigbeeApi.isInitialBrowsingComplete()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                LOGGER.info("Waiting for initial browsing to complete.");
            }
            LOGGER.info("ZigBeeApi initial browsing done.");
        }

        LOGGER.info("ZigBeeApi listing devices...");
        final List<Device> devices = zigbeeApi.getDevices();
        for (final Device device : devices) {
            LOGGER.info(device.getNetworkAddress() + ")" + device.getDeviceType());
        }
        LOGGER.info("ZigBeeApi listing devices done.");

        LOGGER.info("ZigBeeApi shutdown...");
        zigbeeApi.shutdown();
        serialPort.close();
        LOGGER.info("ZigBeeApi shutdown done.");

        LOGGER.info("ZigBeeApi saving network state...");
        FileUtils.writeStringToFile(networkStateFile, zigbeeApi.serializeNetworkState(), false);
        LOGGER.info("ZigBeeApi saving network state done.");

    }

}
