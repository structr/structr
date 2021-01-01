/*
 * Copyright (C) 2010-2021 Structr GmbH
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
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderData;
import org.apache.chemistry.opencmis.commons.data.ObjectInFolderList;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ObjectInFolderDataImpl;
import org.structr.cmis.wrapper.CMISObjectWrapper;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;

/**
 *
 *
 */
public class CMISObjectInFolderWrapper extends CMISPagingListWrapper<ObjectInFolderData> implements ObjectInFolderList {

	private Boolean includeAllowableActions = false;
	private String propertyFilter           = null;

	public CMISObjectInFolderWrapper(final Boolean includeAllowableActions) {
		this(null, includeAllowableActions, null, null);
	}

	public CMISObjectInFolderWrapper(final String propertyFilter, final Boolean includeAllowableActions, final BigInteger maxItems, final BigInteger skipCount) {

		super(maxItems, skipCount);

		this.includeAllowableActions = includeAllowableActions;
		this.propertyFilter          = propertyFilter;
	}

	public ObjectInFolderData wrapObjectData(final ObjectData element, final String pathSegment) {

		final ObjectInFolderDataImpl data = new ObjectInFolderDataImpl(element);
		data.setPathSegment(pathSegment);

		return data;
	}

	public ObjectData wrapGraphObject(final GraphObject item) throws FrameworkException {
		return CMISObjectWrapper.wrap(item, propertyFilter, includeAllowableActions);
	}

	public void wrap(final List<? extends GraphObject> list) throws FrameworkException {

		for (final GraphObject element : list) {
			add(wrapObjectData(wrapGraphObject(element), element.getProperty(AbstractNode.name)));
		}
	}

	@Override
	public List<ObjectInFolderData> getObjects() {
		return getPagedList();
	}
}
