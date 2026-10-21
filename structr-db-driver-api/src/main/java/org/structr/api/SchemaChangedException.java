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
package org.structr.api;

/**
 * Signals that the database gave up a query because its schema changed too often while the query was compiled, e.g.
 * while many new indexes come online. Unlike a deadlock, which is resolved as soon as the other transaction is through,
 * this lasts until the indexes are populated, which takes seconds, so a repetition waits longer, see
 * {@link RetryException#retry(Exception, int)}.
 */
public class SchemaChangedException extends RetryException {

	public SchemaChangedException(final Throwable cause) {

		super(cause);
	}

	public SchemaChangedException(final String message, final Throwable cause) {

		super(message, cause);
	}
}
