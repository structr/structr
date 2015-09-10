package org.structr.files.cmis.wrapper;

import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.structr.cmis.common.CMISExtensionsData;

/**
 *
 * @author Christian Morgner
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
