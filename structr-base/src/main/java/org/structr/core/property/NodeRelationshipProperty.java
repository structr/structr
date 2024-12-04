/*
 * Copyright (C) 2010-2024 Structr GmbH
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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.property;

import org.structr.api.Predicate;
import org.structr.api.search.SortType;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.Notion;

import java.util.Map;

/**
 *
 *
 */
public class NodeRelationshipProperty<T extends AbstractRelationship> extends AbstractReadOnlyProperty<Iterable<T>> {

	private Notion notion = null;
	private String type   = null;

	public NodeRelationshipProperty(final String name, final String type) {
		this(name, type, null);
	}

	public NodeRelationshipProperty(String name, final String type, final Notion notion) {

		super(name);

		this.type   = type;
		this.notion = notion;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public String typeName() {
		return "Relationship";
	}

	@Override
	public String relatedType() {
		return "RelationshipInterface";
	}

	@Override
	public String valueType() {
		return relatedType();
	}

	@Override
	public PropertyConverter<?, Iterable<T>> inputConverter(SecurityContext securityContext) {

		if (notion != null) {
			return notion.getCollectionConverter(securityContext);
		}

		return null;
	}

	@Override
	public Iterable<T> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public Iterable<T> getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<GraphObject> predicate) {
		NodeInterface node = (NodeInterface)obj;
		return (Iterable)node.getRelationships(type);
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}
}
