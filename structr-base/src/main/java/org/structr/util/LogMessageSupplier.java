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
package org.structr.util;

import java.text.MessageFormat;
import java.util.function.Supplier;

public class LogMessageSupplier implements Supplier<String> {

	private final String msg;
	private final Object[] parameters;

	public LogMessageSupplier (final String msg, final Object[] parameters) {
		this.msg = msg;
		this.parameters = parameters;
	}

	public static LogMessageSupplier create (final String msg, final Object[] parameters) {
		return new LogMessageSupplier(msg, parameters);
	}

	public static LogMessageSupplier create (final String msg, final Object parameter1) {
		return new LogMessageSupplier(msg, new Object[] { parameter1 });
	}


	@Override
	public String get() {
		return MessageFormat.format(this.msg, this.parameters);
	}

}