/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.net.peer;

import java.security.PublicKey;

/**
 * The representation of a remote peer.
 */
public class PeerInfo {

	private PublicKey publicKey = null;
	private String address      = null;
	private String uuid         = null;
	private long lastSeen       = 0L;
	private int port            = -1;
	private long latency        = 0;

	public PeerInfo(final PublicKey publicKey, final String uuid, final String address, final int port) {

		this.publicKey = publicKey;
		this.address   = address;
		this.uuid      = uuid;
		this.port      = port;
	}

	@Override
	public String toString() {
		return address + ":" + port + " (" + uuid + "): " + latency + " ms";
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof PeerInfo) {
			return other.hashCode() == hashCode();
		}

		return false;
	}

	public String getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}

	public String getUuid() {
		return uuid;
	}

	public long getLatency() {
		return latency;
	}

	public void setLatency(final long latency) {
		this.latency = latency;
	}

	public long getLastSeen() {
		return lastSeen;
	}

	public void setLastSeen(final long lastSeen) {
		this.lastSeen = lastSeen;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}
}
