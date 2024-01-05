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

import java.util.UUID;

/**
 *
 */
public class ToplevelTemporalEnvironment implements PseudoTemporalEnvironment {

	private Clock clock      = null;
	private String uuid      = null;
	private long lastTime    = 0L;

	public ToplevelTemporalEnvironment(final Clock clock) {

		this.lastTime = System.currentTimeMillis();
		this.uuid     = UUID.randomUUID().toString().replaceAll("\\-", "");
		this.clock    = clock;
	}

	@Override
	public String getUuid() {
		return uuid;
	}

	public long getLastTime() {
		return lastTime;
	}

	// ----- interface PseudoTemporalEnvironment -----
	@Override
	public PseudoTemporalEnvironment transaction() {
		next();
		return new DerivedTemporalEnvironment(lastTime, uuid, 0);
	}

	@Override
	public PseudoTime current() {
		return new PseudoTime(lastTime, uuid, 0);
	}

	@Override
	public PseudoTime next() {

		long clockTime = clock.getTime();

		while (clockTime < lastTime) {

			sleep(10);
			clockTime = clock.getTime();
		}

		this.lastTime = clockTime;

		return current();
	}

	// ----- private methods -----
	private void sleep(final long time) {
		try { Thread.sleep(time); } catch (InterruptedException iex) {}
	}
}
