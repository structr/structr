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

/**
 *
 */
public class DerivedTemporalEnvironment implements PseudoTemporalEnvironment {

	private long lastTime    = 0L;
	private String uuid      = null;
	private long timeDerived = 0L;
	private long tick        = 0L;

	public DerivedTemporalEnvironment(final long lastTime, final String uuid, final long timeDerived) {

		this.timeDerived = timeDerived;
		this.lastTime    = lastTime;
		this.uuid        = uuid;
	}
	
	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	public PseudoTime current() {
		return new PseudoTime(lastTime, uuid, timeDerived, tick);
	}

	@Override
	public PseudoTime next() {
		return new PseudoTime(lastTime, uuid, timeDerived, ++tick);
	}

	@Override
	public PseudoTemporalEnvironment transaction() {
		throw new UnsupportedOperationException("Not supported.");
	}

}
