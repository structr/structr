/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.RetryException;

import java.util.function.Supplier;

public abstract class RetryWrapper {
	private static final int maxRetries = 20;
	private static final int retryDelay = 100;
	private static final int firstRetryDelay = 10;

	private static final Logger logger = LoggerFactory.getLogger(RetryWrapper.class);

	// ---- public static methods ----
	public static  <T> T executeWithRetry(Supplier<T> supplier) {
		boolean retry = false;
		int retries = 0;

		do {

			try {

				return supplier.get();

			} catch (RetryException ex) {

				logger.info("Caught RetryException in executeWithRetry. Waiting " + (retries > 0 ? retryDelay : firstRetryDelay) + " ms and then performing retry #" + retries);

				try {

					Thread.sleep(retries > 0 ? retryDelay : firstRetryDelay);
				} catch (InterruptedException iex) {

					break;
				}

				retry = true;
				retries ++;

			}

		} while (retry && retries < maxRetries);

		throw new RetryException("executeWithRetry exceeded maximum amount of retries.");
	}

}
