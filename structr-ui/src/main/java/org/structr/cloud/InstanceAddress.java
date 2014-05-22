/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud;

/**
 * An instance address represents the connection information for a specific structr instance.
 *
 * @author Axel Morgner
 */
public class InstanceAddress {

	private String host;
	private String tcpPort;
	private String udpPort;
	private int timeout;

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return the tcpPort
	 */
	public String getTcpPort() {
		return tcpPort;
	}

	/**
	 * @param tcpPort the tcpPort to set
	 */
	public void setTcpPort(String tcpPort) {
		this.tcpPort = tcpPort;
	}

	/**
	 * @return the udpPort
	 */
	public String getUdpPort() {
		return udpPort;
	}

	/**
	 * @param udpPort the udpPort to set
	 */
	public void setUdpPort(String udpPort) {
		this.udpPort = udpPort;
	}

	/**
	 * @return the timeout
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * @param timeout the timeout to set
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

}
