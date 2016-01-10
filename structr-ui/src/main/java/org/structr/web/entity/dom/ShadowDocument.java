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
package org.structr.web.entity.dom;

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.web.entity.relation.PageLink;

/**
 * Shadow document.
 *
 * The sole purpose of this class is to have a node to append reused elements
 * (aka) components to.
 *
 *
 */
public class ShadowDocument extends Page {

	public static final org.structr.common.View publicView = new org.structr.common.View(ShadowDocument.class, PropertyView.Public, type, name, id);

	public ShadowDocument() { }

	// ----- interface Syncable -----
	@Override
	public List<GraphObject> getSyncData() throws FrameworkException {

		final List<GraphObject> data = super.getSyncData();

		for (final PageLink pageLink : getIncomingRelationships(PageLink.class)) {

			data.add(pageLink.getSourceNode());
			data.add(pageLink);
		}

		return data;
	}
}
