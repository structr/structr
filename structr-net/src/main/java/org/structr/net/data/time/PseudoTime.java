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
package org.structr.net.data.time;

import org.structr.net.protocol.AbstractMessage;
import org.structr.net.protocol.ProtocolEntity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 *
 */
public class PseudoTime implements Comparable<PseudoTime>, ProtocolEntity {

	private long instant     = 0L;
	private String uuid      = null;
	private long timeDerived = 0L;
	private long tick        = 0L;

	public PseudoTime() {
	}

	public PseudoTime(final long instant, final String uuid, final long timeDerived) {
		this(instant, uuid, timeDerived, 0L);
	}

	public PseudoTime(final long instant, final String uuid, final long timeDerived, final long tick) {

		this.uuid        = uuid;
		this.instant     = instant;
		this.timeDerived = timeDerived;
		this.tick        = tick;
	}

	@Override
	public String toString() {
		return instant + "." + uuid + "." + pad(timeDerived) + "." + pad(tick);
	}

	public long getTimestamp() {
		return instant;
	}

	public boolean after(final PseudoTime other) {
		return compareTo(other) == 1;
	}

	public boolean before(final PseudoTime other) {
		return compareTo(other) == -1;
	}

	public PseudoTime next() {
		return new PseudoTime(instant, uuid, timeDerived, tick+1);
	}

	@Override
	public int compareTo(final PseudoTime other) {

		// instants are equal
		if (this.instant == other.instant) {

			// IDs are equal
			if (this.uuid.equals(other.uuid)) {

				if (this.timeDerived == other.timeDerived) {

					// times are equal!
					if (this.tick == other.tick) {
						return 0;
					}

					return compare(this.tick, other.tick);
				}

				return compare(this.timeDerived, other.timeDerived);
			}

			return this.uuid.compareTo(other.uuid);
		}

		return compare(this.instant, other.instant);
	}

	// ----- private methods -----
	private int compare(final long t1, final long t2) {

		if (t1 < t2) {
			return -1;
		}

		if (t1 > t2) {
			return 1;
		}

		return 0;
	}

	private String pad(final long value) {

		if (value < 100) {

			if (value < 10) {

				return "00" + value;
			}

			return "0" + value;
		}

		return String.valueOf(value);
	}

	@Override
	public void serialize(final DataOutputStream dos) throws IOException {

		dos.writeLong(instant);
		AbstractMessage.serializeUUID(dos, uuid); // dos.writeUTF(uuid);
		dos.writeLong(timeDerived);
		dos.writeLong(tick);
	}

	@Override
	public void deserialize(final DataInputStream dis) throws IOException {

		this.instant     = dis.readLong();
		this.uuid        = AbstractMessage.deserializeUUID(dis); // dis.readUTF();
		this.timeDerived = dis.readLong();
		this.tick        = dis.readLong();
	}

	public static PseudoTime fromStream(final DataInputStream dis) throws IOException {

		final PseudoTime time = new PseudoTime();

		time.deserialize(dis);

		return time;
	}

	public static PseudoTime fromString(final String src) {

		final PseudoTime time = new PseudoTime();

		if (src != null) {

			final String[] parts  = src.split("[\\.]+");
			switch (parts.length) {
				case 4: time.tick        = Long.valueOf(parts[3]);
				case 3: time.timeDerived = Long.valueOf(parts[2]);
				case 2: time.uuid        = parts[1];
				case 1: time.instant     = Long.valueOf(parts[0]);
			}

		} else {

			time.instant = System.currentTimeMillis();
			time.uuid    = UUID.randomUUID().toString().replaceAll("\\-", "");
		}

		return time;
	}

	public static PseudoTime now(final Clock clock) {

		final PseudoTime time = new PseudoTime();

		time.instant = clock.getTime();
		time.uuid    = UUID.randomUUID().toString().replaceAll("\\-", "");

		return time;
	}

	public static PseudoTime fromTimestamp(final long timestamp, final String pteId) {

		final PseudoTime time = new PseudoTime();

		time.instant = timestamp;
		time.uuid    = pteId;

		return time;
	}

	public static PseudoTime epoch() {

		final PseudoTime time = new PseudoTime();

		time.instant = 0L;
		time.uuid    = UUID.randomUUID().toString().replaceAll("\\-", "");

		return time;
	}
}
