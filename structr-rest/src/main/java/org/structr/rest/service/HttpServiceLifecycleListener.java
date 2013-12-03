package org.structr.rest.service;

/**
 * Callback interface to receive lifecycle events from HTTP service.
 *
 * @author Christian Morgner
 */
public interface HttpServiceLifecycleListener {
	
	public void serverStarted();
	public void serverStopped();
}
