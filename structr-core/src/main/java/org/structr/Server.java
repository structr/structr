package org.structr;

import org.structr.core.Services;

/**
 * Main startup class for Structr.
 * 
 * @author Christian Morgner
 */
public class Server {

	public static void main(String[] args) {
		
		// TODO: handle parameters here?
		
		// start service layer using default configuration
		// augmented by local structr.conf
		Services.getInstance();
	}
}
