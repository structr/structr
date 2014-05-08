package org.structr.cloud;

import java.security.InvalidKeyException;

/**
 *
 * @author Christian Morgner
 */
public interface CloudConnection {

	public void closeConnection();
	public void setEncryptionKey(final String key) throws InvalidKeyException;
}
