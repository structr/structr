/**
 * Copyright (C) 2010-2016 Structr GmbH
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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderData;
import org.structr.cmis.common.CMISExtensionsData;

/**
 *
 *
 */
public class CMISPagingListWrapper<T> extends CMISExtensionsData  {

	protected List<T> list  = null;
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

	public int getSize() {

		return list.size();
	}


	public void sort(String orderBy) {

		if(orderBy != null) {

			if(orderBy.equals(PropertyIds.NAME)) {

				sortByName();
			}

			//add here other potencial orderby in the future
		}
	}

	public void sortByName() {

		Collections.sort(list, new Comparator<T>() {

			@Override
			public int compare(T o1, T o2) {

				Object obj1 = null;
				Object obj2 = null;

				//Type of CMISObjectInFolderWrapper
				if(o1 instanceof ObjectInFolderData) {

					ObjectInFolderData folderData1 = (ObjectInFolderData) o1;
					ObjectInFolderData folderData2 = (ObjectInFolderData) o2;

					obj1 = folderData1.getObject().getProperties().getProperties().get(PropertyIds.NAME).getFirstValue();
					obj2 = folderData2.getObject().getProperties().getProperties().get(PropertyIds.NAME).getFirstValue();

				//Type of CMISObjectListWrapper
				} else if(o1 instanceof ObjectData) {

					ObjectData objectData1 = (ObjectData) o1;
					ObjectData objectData2 = (ObjectData) o2;

					obj1 = objectData1.getProperties().getProperties().get(PropertyIds.NAME).getFirstValue();
					obj2 = objectData2.getProperties().getProperties().get(PropertyIds.NAME).getFirstValue();
				}

				String name1 = (String)obj1;
				String name2 = (String)obj2;

				return name1.compareTo(name2);
			}
		});
	}
}
