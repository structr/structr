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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Signals that the database terminated a transaction because of a transient error, for example to
 * resolve a deadlock, so that only a repetition of the whole transaction can succeed.
 */
public class RetryException extends RuntimeException {

	public static final int MAX_ATTEMPTS = 10;

	// the pauses before repeating a transaction that a frequently changing schema terminated, see SchemaChangedException
	private static final long SCHEMA_CHANGE_INITIAL_DELAY_MILLIS = 100;
	private static final long SCHEMA_CHANGE_MAX_DELAY_MILLIS     = 5000;

	public RetryException(final String message) {

		super(message);
	}

	public RetryException(final Throwable cause) {

		super(cause);
	}

	public RetryException(final String message, final Throwable cause) {

		super(message, cause);
	}

	/**
	 * Decides whether a transaction that failed with the given exception is repeated, and pauses
	 * before the next attempt, see {@link #delay(RetryException, int)}. The exception does not have to be a RetryException itself: code that catches
	 * a RetryException can turn it into another exception, and the RetryException that closing a
	 * terminated transaction throws is attached to the exception that ended the transaction as a
	 * suppressed exception. An exception without a RetryException, or a transient error that is
	 * still there after the last attempt, is thrown on, so the caller fails instead of repeating
	 * the transaction forever.
	 *
	 * @param ex the exception that ended the transaction
	 * @param attempts the number of attempts made so far, including the one that failed
	 *
	 * @return true if the transaction is to be repeated
	 *
	 * @throws E the given exception, if the transaction is not repeated
	 */
	public static <E extends Exception> boolean retry(final E ex, final int attempts) throws E {

		if (find(ex) == null || attempts >= MAX_ATTEMPTS) {

			throw ex;
		}

		try {

			Thread.sleep(delay(find(ex), attempts));

		} catch (InterruptedException iex) {

			Thread.currentThread().interrupt();

			throw ex;
		}

		return true;
	}

	/**
	 * Returns the pause in milliseconds before the next attempt. The transactions involved in a
	 * deadlock collide again unless one of them waits, but only briefly, since the other one is
	 * through soon, so the pause grows a little with each attempt, 450 ms in total. A schema that
	 * changes too often keeps changing until the indexes are populated, which takes seconds, so the
	 * pause doubles with each attempt, from 100 ms up to 5 s, about 21 s in total.
	 *
	 * @param rex the RetryException that ended the transaction
	 * @param attempts the number of attempts made so far, including the one that failed
	 *
	 * @return the pause in milliseconds
	 */
	static long delay(final RetryException rex, final int attempts) {

		if (rex instanceof SchemaChangedException) {

			return Math.min(SCHEMA_CHANGE_INITIAL_DELAY_MILLIS << Math.min(attempts - 1, 16), SCHEMA_CHANGE_MAX_DELAY_MILLIS);
		}

		return attempts * 10L;
	}

	/**
	 * Runs a transaction and repeats it as long as the database terminates it with a transient
	 * error, at most {@link #MAX_ATTEMPTS} times. The attempt must contain the whole transaction,
	 * because a transaction that the database terminated cannot be continued, and it must not run
	 * inside another transaction, since only the outermost one can be repeated.
	 *
	 * @param attempt the transaction to run
	 *
	 * @throws E the exception the attempt throws
	 */
	public static <E extends Exception> void repeat(final Attempt<E> attempt) throws E {

		repeatAndGet(() -> {

			attempt.run();

			return null;
		});
	}

	/**
	 * Runs a transaction like {@link #repeat(Attempt)} and returns the result of the attempt that
	 * succeeded.
	 *
	 * @param attempt the transaction to run
	 *
	 * @return the result of the attempt
	 *
	 * @throws E the exception the attempt throws
	 */
	@SuppressWarnings("unchecked")
	public static <T, E extends Exception> T repeatAndGet(final Computation<T, E> attempt) throws E {

		int attempts = 0;

		while (true) {

			try {

				return attempt.run();

			} catch (RuntimeException rex) {

				retry(rex, ++attempts);

			} catch (Exception ex) {

				// the attempt throws no other checked exception than E
				retry((E)ex, ++attempts);
			}
		}
	}

	/**
	 * Returns the RetryException that caused the given exception or was suppressed by it, or null
	 * if the database did not terminate the transaction.
	 *
	 * @param t the exception to examine
	 *
	 * @return the RetryException, or null
	 */
	public static RetryException find(final Throwable t) {

		return find(t, Collections.newSetFromMap(new IdentityHashMap<>()));
	}

	// ----- private static methods -----
	private static RetryException find(final Throwable t, final Set<Throwable> visited) {

		if (t == null || !visited.add(t)) {

			return null;
		}

		if (t instanceof RetryException rex) {

			return rex;
		}

		for (final Throwable suppressed : t.getSuppressed()) {

			final RetryException rex = find(suppressed, visited);
			if (rex != null) {

				return rex;
			}
		}

		return find(t.getCause(), visited);
	}

	// ----- nested interfaces -----
	@FunctionalInterface
	public interface Attempt<E extends Exception> {

		void run() throws E;
	}

	/**
	 * An attempt that returns a result, see {@link #repeatAndGet(Computation)}.
	 */
	@FunctionalInterface
	public interface Computation<T, E extends Exception> {

		T run() throws E;
	}
}
