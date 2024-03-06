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
package org.structr.core.script.polyglot.wrappers;

import org.graalvm.polyglot.proxy.ProxyDate;
import org.graalvm.polyglot.proxy.ProxyTime;
import org.graalvm.polyglot.proxy.ProxyTimeZone;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

public class DateWrapper implements ProxyDate, ProxyTime, ProxyTimeZone {
	private final Date date;
	private final ZoneId zoneId;
	public DateWrapper(final Date date) {
		this.date = date;
		this.zoneId = ZoneId.systemDefault();
	}

	public Date getWrappedDate() {

		return this.date;
	}
	@Override
	public LocalDate asDate() {
		return LocalDate.ofInstant(date.toInstant(), zoneId);
	}

	@Override
	public ZoneId asTimeZone() {
		return ZoneId.systemDefault();
	}

	@Override
	public LocalTime asTime() {
		return LocalTime.ofInstant(date.toInstant(), zoneId);
	}
}
