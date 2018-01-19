package com.beowulfe.hap.impl.jmdns;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import javax.jmdns.JmDNS;
import javax.jmdns.NetworkTopologyDiscovery;
import javax.jmdns.ServiceInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmdnsHomekitAdvertiser {
	
	private static final String SERVICE_TYPE = "_hap._tcp.local.";

	private List<JmDNS> jmdnsList = new ArrayList<>();
	private boolean discoverable = true;
	private final static Logger logger = LoggerFactory.getLogger(JmdnsHomekitAdvertiser.class);
	private boolean isAdvertising = false;
	
	private String label;
	private String mac;
	private int port;
	private int configurationIndex;

	public JmdnsHomekitAdvertiser() throws IOException {
		this(null, null);
	}

	public JmdnsHomekitAdvertiser(String name) throws IOException {
		this(null, name);
	}

	public JmdnsHomekitAdvertiser(InetAddress localAddress) throws IOException {
		this(Collections.singletonList(localAddress), null);
	}

	public JmdnsHomekitAdvertiser(List<InetAddress> addressList, String name) throws UnknownHostException, IOException {
		if (addressList == null) {
			addressList = new ArrayList<>();
			for (InetAddress inet : NetworkTopologyDiscovery.Factory.getInstance().getInetAddresses()) {
				try {
					if (inet.isReachable(1000)) {
						addressList.add(inet);
					}
				} catch (IOException ignored) {}
			}
		}

		IOException firstE;

		addressList.parallelStream().forEach((inet) -> {
			JmDNS jmdns = null;
			try {
				jmdns = JmDNS.create(inet, name);
			} catch (IOException e) {
				logger.error("Error creating jmDNS", e);
			}
			jmdnsList.add(jmdns);
		});
	}

	public synchronized void advertise(String label, String mac, int port, int configurationIndex) throws Exception {
		if (isAdvertising) {
			throw new IllegalStateException("Homekit advertiser is already running");
		}
		this.label = label;
		this.mac = mac;
		this.port = port;
		this.configurationIndex = configurationIndex;
		
		logger.info("Advertising accessory "+label);

		jmdnsList.parallelStream().forEach((jmdns) -> {
			try {
				registerService(jmdns);
			} catch (IOException e) {
				logger.error("Error registering", e);
			}
		});

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    		logger.info("Stopping advertising in response to shutdown.");
    		stop();
    	}));
		isAdvertising = true;
	}
	
	public synchronized void stop() {

		jmdnsList.parallelStream().forEach((jmdns) -> {
			jmdns.unregisterAllServices();

			try {
				jmdns.close();
			} catch (IOException e) {
				logger.error("Error closing jmdns", e);
			}
		});
	}
	
	public synchronized void setDiscoverable(boolean discoverable) throws IOException {
		if (this.discoverable != discoverable) {
			this.discoverable = discoverable;
			if (isAdvertising) {
				logger.info("Re-creating service due to change in discoverability to "+discoverable);
				for (JmDNS jmdns : jmdnsList) {
					jmdns.unregisterAllServices();
					try {
						registerService(jmdns);
					} catch (IOException e) {
						logger.error("Error registering", e);
					}
				}
			}
		}
	}
	
	public synchronized void setConfigurationIndex(int revision) throws IOException {
		if (this.configurationIndex != revision) {
			this.configurationIndex = revision;
			if (isAdvertising) {
				logger.info("Re-creating service due to change in configuration index to "+revision);
				jmdnsList.parallelStream().forEach((jmdns) -> {
					jmdns.unregisterAllServices();
					try {
						registerService(jmdns);
					} catch (IOException e) {
						logger.error("Error registering", e);
					}
				});

			}
		}
	}
	
	private void registerService(JmDNS jmdns) throws IOException {
		logger.info("Registering "+SERVICE_TYPE+" on port "+port);
		Map<String, String> props = new HashMap<>();
		props.put("sf", discoverable ? "1" : "0");
		props.put("id", mac);
		props.put("md", label);
		props.put("c#", Integer.toString(configurationIndex));
		props.put("s#", "1");
		props.put("ff", "0");
		props.put("ci", "1");
		jmdns.registerService(ServiceInfo.create(SERVICE_TYPE, label, port, 1, 1, props));
	}
	
}
