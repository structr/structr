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
 * Signals that the database terminated a transaction because of a transient error, for example to
 * resolve a deadlock, so that only a repetition of the whole transaction can succeed.
 */
public class RetryException extends RuntimeException {

	public static final int MAX_ATTEMPTS = 10;

	public RetryException(final String message) {

		super(message);
	}

	public RetryException(final Throwable cause) {

		super(cause);
	}

	/**
	 * Decides whether a transaction that failed with the given exception is repeated, and pauses
	 * a little longer with each attempt so that the transactions involved in a deadlock do not
	 * collide again. A transient error that is still there after the last attempt is thrown on,
	 * so the caller fails instead of repeating the transaction forever.
	 *
	 * @param rex the exception that terminated the transaction
	 * @param attempts the number of attempts made so far, including the one that failed
	 *
	 * @return true if the transaction is to be repeated
	 */
	public static boolean retry(final RetryException rex, final int attempts) {

		if (attempts >= MAX_ATTEMPTS) {

			throw rex;
		}

		try {

			Thread.sleep(attempts * 10L);

		} catch (InterruptedException iex) {

			Thread.currentThread().interrupt();

			throw rex;
		}

		return true;
	}
}
