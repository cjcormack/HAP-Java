package com.beowulfe.hap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import com.beowulfe.hap.impl.HomekitWebHandler;

/**
 * A server for exposing standalone Homekit accessory (as opposed to a Bridge accessory which contains multiple accessories).
 * Each standalone accessory will have its own pairing information, port, and pin. Instantiate this class via 
 * {@link HomekitServer#createStandaloneAccessory(HomekitAuthInfo, HomekitAccessory)}.
 *
 * @author Andy Lintner
 */
public class HomekitStandaloneAccessoryServer {
	
	private final HomekitRoot root;

	HomekitStandaloneAccessoryServer(HomekitAccessory accessory,
			HomekitWebHandler webHandler, InetAddress localhost,
			HomekitAuthInfo authInfo, String jmdnsName) throws UnknownHostException, IOException {

		List<InetAddress> addressList = null;
		if (localhost != null) {
			addressList = Collections.singletonList(localhost);
		}

		root = new HomekitRoot(accessory.getLabel(), webHandler, addressList, authInfo, jmdnsName);
		root.addAccessory(accessory);
	}
	
	/**
	 * Begins advertising and handling requests for this accessory.
	 */
	public void start() {
		root.start();
	}

	

}
