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
import java.util.List;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectList;

/**
 *
 *
 */
public class CMISObjectListWrapper extends CMISPagingListWrapper<ObjectData> implements ObjectList {

	public CMISObjectListWrapper() {
		super();
	}

	public CMISObjectListWrapper(final BigInteger maxItems, final BigInteger skipCount) {
		super(maxItems, skipCount);
	}

	@Override
	public List<ObjectData> getObjects() {
		return getPagedList();
	}

//	@Override
//	protected void sortByName() {
//
//		if(list.size() > 0) {
//
//			Collections.sort(list, new Comparator<ObjectData>() {
//
//				@Override
//				public int compare(ObjectData o1, ObjectData o2) {
//
//					Object obj1 = o1.getProperties().getProperties().get(PropertyIds.NAME).getFirstValue();
//					Object obj2 = o2.getProperties().getProperties().get(PropertyIds.NAME).getFirstValue();
//
//					String name1 = (String)obj1;
//					String name2 = (String)obj2;
//
//					return name1.compareTo(name2);
//				}
//			});
//		}
//	}
}
