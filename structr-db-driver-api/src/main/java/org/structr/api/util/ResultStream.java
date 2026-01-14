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
package org.structr.api.util;

/**
 * The result stream of a query operation.
 */

public interface ResultStream<T> extends Iterable<T>, AutoCloseable {

	/**
	 * Calculates and returns the total number of results
	 * in this result stream. Note that calling this method
	 * causes the full result to be pulled into memory.
	 *
	 * @param progressConsumer optional consumer to allow progress notifications
	 * @param softLimit limit after which calculation is stopped
	 *
	 * @return the total number of results in this result stream
	 */
	int calculateTotalResultCount(final ProgressWatcher progressConsumer, final int softLimit);

	/**
	 * Calculates and returns the total number of result pages
	 * in this result stream. Note that calling this method
	 * causes the full result to be pulled into memory.
	 *
	 * @param progressConsumer optional consumer to allow progress notifications
	 * @param softLimit limit after which calculation is stopped
	 *
	 * @return the total number of result pages in this result stream
	 */
	int calculatePageCount(final ProgressWatcher progressConsumer, final int softLimit);

	int getPageSize();
	int getPage();

	void setQueryTime(final String formattedTime);
	String getQueryTime();

	@Override
	public void close(); // hide the exception to make closing easier
}
