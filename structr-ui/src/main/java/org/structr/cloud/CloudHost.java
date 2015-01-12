package org.structr.cloud;

/**
 *
 * @author Christian Morgner
 */


public interface CloudHost {

	public String getHostName();
	public String getUserName();
	public String getPassword();
	public int getPort();
}
