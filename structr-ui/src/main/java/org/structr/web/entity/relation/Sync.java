/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.relation;

import java.util.Collections;
import java.util.List;
import org.structr.common.Syncable;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.OneToMany;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.dom.DOMElement;

/**
 *
 * @author Christian Morgner
 */
public class Sync extends OneToMany<DOMElement, DOMElement> implements Syncable {

	@Override
	public Class<DOMElement> getSourceType() {
		return DOMElement.class;
	}

	@Override
	public Class<DOMElement> getTargetType() {
		return DOMElement.class;
	}

	@Override
	public String name() {
		return "SYNC";
	}

	@Override
	public List<Syncable> getSyncData() {
		return Collections.emptyList();
	}

	@Override
	public boolean isNode() {
		return false;
	}

	@Override
	public boolean isRelationship() {
		return true;
	}

	@Override
	public NodeInterface getSyncNode() {
		return null;
	}

	@Override
	public RelationshipInterface getSyncRelationship() {
		return this;
	}

	@Override
	public void updateFromPropertyMap(PropertyMap properties) throws FrameworkException {
	}
}
