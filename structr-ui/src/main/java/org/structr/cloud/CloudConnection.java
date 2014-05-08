package org.structr.cloud;

import java.security.InvalidKeyException;

/**
 *
 * @author Christian Morgner
 */
public interface CloudConnection {

	public boolean isConnected();
	public void shutdown();
	public void setEncryptionKey(final String key, final int keyLength) throws InvalidKeyException;
}
