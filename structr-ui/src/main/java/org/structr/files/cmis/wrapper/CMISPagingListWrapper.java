/**
 * Copyright (C) 2010-2020 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.files.cmis.wrapper;

import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.structr.cmis.common.CMISExtensionsData;

/**
 *
 *
 */
public class CMISPagingListWrapper<T> extends CMISExtensionsData {

	private List<T> list  = null;
	private int maxItems  = Integer.MAX_VALUE;
	private int skipCount = 0;

	public CMISPagingListWrapper() {
		this(new LinkedList<T>(), null, null);
	}

	public CMISPagingListWrapper(final List<T> list) {
		this(list, null, null);
	}

	public CMISPagingListWrapper(final BigInteger maxItems, final BigInteger skipCount) {
		this(new LinkedList<T>(), maxItems, skipCount);
	}

	public CMISPagingListWrapper(final List<T> list, final BigInteger maxItems, final BigInteger skipCount) {

		this.list = list;

		if (maxItems != null) {
			this.maxItems = maxItems.intValue();
		}

		if (skipCount != null) {
			this.skipCount = skipCount.intValue();
		}
	}

	public void setList(final List<T> data) {
		this.list = data;
	}

	public void add(final T data) {
		list.add(data);
	}

	public void addAll(final Collection<T> data) {
		list.addAll(data);
	}

	public Boolean hasMoreItems() {
		return list.size() > skipCount + maxItems;
	}

	public BigInteger getNumItems() {
		return BigInteger.valueOf(list.size());
	}

	public List<T> getRawList() {
		return list;
	}

	public List<T> getPagedList() {

		final int size = list.size();
		int to         = Math.min(maxItems, size);
		int from       = 0;

		from = Math.min(skipCount, size);
		to   = Math.min(to+skipCount, size);

		return list.subList(from, to);
	}
}
