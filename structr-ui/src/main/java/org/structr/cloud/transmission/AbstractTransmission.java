package org.structr.cloud.transmission;

import org.structr.cloud.CloudTransmission;

/**
 * Abstract base class for cloud transmissions.
 *
 * @author Christian Morgner
 */
public abstract class AbstractTransmission<T> implements CloudTransmission<T> {

	private String userName   = null;
	private String password   = null;
	private String remoteHost = null;
	private int tcpPort       = 0;

	public AbstractTransmission(final String userName, final String password, final String remoteHost, final int port) {

		this.userName   = userName;
		this.password   = password;
		this.remoteHost = remoteHost;
		this.tcpPort    = port;
	}

	@Override
	public String getUserName() {
		return userName;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getRemoteHost() {
		return remoteHost;
	}

	@Override
	public int getRemotePort() {
		return tcpPort;
	}

}
