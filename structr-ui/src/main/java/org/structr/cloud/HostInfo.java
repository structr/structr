/**
 * Copyright (C) 2010-2016 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud;

/**
 *
 *
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
