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
package org.structr.api.search;

/**
 * Context object for index queries.
 */
public class QueryContext {

	private boolean overridesFetchSize = false;
	private boolean deferred           = false;
	private boolean isSuperuser        = false;
	private boolean sliced             = false;
	private boolean prefetch           = false;
	private int overriddenFetchSize    = -1;
	private int skip                   = -1;
	private int limit                  = -1;
	private int skipped                = 0;
	private boolean isPing             = false;

	public QueryContext() {
	}

	/**
	 * Creates a QueryContext for a query that will not start to execute until
	 * hasNext() has been called on the Iterable. This setting can be used to
	 * setup a query outside of a transaction and run the actual query when a
	 * transaction context exists.
	 *
	 * @param deferred
	 */
	public QueryContext(final boolean deferred) {
		this.deferred = deferred;
	}

	public QueryContext page(final int pageSize, final int page) {

		sliced = true;
		skip   = (page - 1) * pageSize;
		limit  = pageSize;

		return this;
	}

	public QueryContext slice(final int from, final int to) {

		sliced = true;
		skip   = from;
		limit  = to - from;

		return this;
	}

	public boolean isSliced () {
		return sliced;
	}

	public int getSkip() {
		return skip;
	}

	public int getLimit() {
		return limit;
	}

	public int getPage() {
		return (skip / Math.max(1, limit)) + 1;
	}

	public int getPageSize() {
		return limit;
	}

	public QueryContext isPing(final boolean isPing) {
		this.isPing = isPing;
		return this;
	}

	public boolean isPing() {
		return this.isPing;
	}

	public void setSkipped(final int skipped) {
		this.skipped = skipped;
	}

	public int getSkipped() {
		return skipped;
	}

	public void setIsSuperuser(final boolean isSuperuser) {
		this.isSuperuser = isSuperuser;
	}

	public boolean isSuperuser() {
		return isSuperuser;
	}

	public boolean isDeferred() {
		return deferred;
	}

	public boolean overridesFetchSize() {
		return overridesFetchSize;
	}

	public int getOverriddenFetchSize() {
		return overriddenFetchSize;
	}

	public void overrideFetchSize(final int newFetchSize) {

		this.overriddenFetchSize = newFetchSize;
		this.overridesFetchSize  = true;
	}

	public void prefetch() {
		this.prefetch = true;
	}
}
