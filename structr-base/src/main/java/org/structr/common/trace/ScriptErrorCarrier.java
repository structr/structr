/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.common.trace;

/**
 * An exception that carries the {@link ScriptError} of the failure it reports. Exceptions that do not
 * implement it get their script error remembered on the side, see {@link ScriptError#attachTo(Throwable)}.
 */
public interface ScriptErrorCarrier {

	/**
	 * The failure of user code this exception reports, attributed to where it happened, or null if it
	 * has not left a script.
	 */
	ScriptError getScriptError();

	void setScriptError(final ScriptError scriptError);
}
