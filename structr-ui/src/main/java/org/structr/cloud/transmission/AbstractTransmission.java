/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
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
