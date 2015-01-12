package org.structr.cloud;

/**
 *
 * @author Christian Morgner
 */
public class HostInfo implements CloudHost {

	private String userName   = null;
	private String password   = null;
	private String hostName = null;
	private int port          = -1;

	public HostInfo(final String userName, final String password, final String hostName, final int port) {

		this.hostName = hostName;
		this.port     = port;
		this.userName = userName;
		this.password = password;
	}

	@Override
	public String getHostName() {
		return hostName;
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
	public int getPort() {
		return port;
	}

}
