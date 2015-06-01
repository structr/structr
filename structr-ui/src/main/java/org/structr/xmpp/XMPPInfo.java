package org.structr.xmpp;


/**
 *
 * @author Christian Morgner
 */
public interface XMPPInfo {

	public String getUsername();
	public String getPassword();
	public String getService();
	public String getHostName();
	public int getPort();

	public String getUuid();
}
