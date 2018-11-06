/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.bolt.wrapper;

import org.neo4j.driver.v1.exceptions.TransientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.RetryException;

import java.util.function.Supplier;

public abstract class WrapperUtility {
	private static final Logger logger = LoggerFactory.getLogger(WrapperUtility.class);

	// ---- public static methods ----
	public static  <T> T executeWithRetry(Supplier<T> supplier) {
		boolean retry = false;
		int retries = 0;

		do {

			try {

				return supplier.get();

			} catch (TransientException ex) {

				logger.info("Caught TransientException in executeWithRetry. Waiting 100 ms and then performing retry #" + retries);

				try {

					Thread.sleep(100);
				} catch (InterruptedException iex) {

					break;
				}

				retry = true;
				retries ++;

			}

		} while (retry && retries < 5);

		throw new RetryException("executeWithRetry exceeded maximum amount of retries.");
	}

}
