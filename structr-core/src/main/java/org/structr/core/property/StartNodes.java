/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.property;

import java.util.List;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.ManyStartpoint;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Target;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeService;
import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;

/**
 * A property that defines a relationship with the given parameters between a node and a collection of other nodes.
 *
 * @author Christian Morgner
 */
public class StartNodes<S extends NodeInterface, T extends NodeInterface> extends Property<List<S>> {

	private Relation<S, T, ManyStartpoint<S>, ? extends Target> relation = null;
	private Notion notion                                                   = null;
	private Class destType                                                  = null;
	
	/**
	 * Constructs a collection property with the given name, the given destination type and the given relationship type.
	 *
	 * @param name
	 * @param destType
	 * @param relType
	 */
	public  StartNodes(final String name, final Class<? extends Relation<S, T, ManyStartpoint<S>, ? extends Target>> relationClass) {
		this(name, relationClass, new ObjectNotion());
	}

	/**
	 * Constructs a collection property with the given name, the given destination type and the given relationship type.
	 *
	 * @param name
	 * @param destType
	 * @param relType
	 */
	public StartNodes(final String name, final Class<? extends Relation<S, T, ManyStartpoint<S>, ? extends Target>> relationClass, final Notion notion) {

		super(name);
		
		try {
			
			this.relation = relationClass.newInstance();
			
		} catch (Throwable t) {
			t.printStackTrace();
		}

		this.notion   = notion;
		this.destType = relation.getTargetType();

		this.notion.setType(destType);
		
		EntityContext.registerConvertedProperty(this);
	}
	
	@Override
	public String typeName() {
		return destType.getSimpleName();
	}

	@Override
	public Integer getSortType() {
		return null;
	}

	@Override
	public PropertyConverter<List<S>, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<List<S>, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, List<S>> inputConverter(SecurityContext securityContext) {
		return getNotion().getCollectionConverter(securityContext);
	}

	@Override
	public List<S> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		
		ManyStartpoint<S> startpoint = relation.getSource();
		
		return Iterables.toList(startpoint.get(securityContext, (NodeInterface)obj));
	}

	@Override
	public void setProperty(SecurityContext securityContext, GraphObject obj, List<S> collection) throws FrameworkException {
		
		ManyStartpoint<S> startpoint = relation.getSource();

		startpoint.set(securityContext, (NodeInterface)obj, collection);
	}
	
	@Override
	public Class<? extends GraphObject> relatedType() {
		return destType;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	public Notion getNotion() {
		return notion;
	}

	@Override
	public Property<List<S>> indexed() {
		return this;
	}

	@Override
	public Property<List<S>> indexed(NodeService.NodeIndex nodeIndex) {
		return this;
	}
	
	@Override
	public Property<List<S>> indexed(NodeService.RelationshipIndex relIndex) {
		return this;
	}
	
	@Override
	public Property<List<S>> passivelyIndexed() {
		return this;
	}
	
	@Override
	public Property<List<S>> passivelyIndexed(NodeService.NodeIndex nodeIndex) {
		return this;
	}
	
	@Override
	public Property<List<S>> passivelyIndexed(NodeService.RelationshipIndex relIndex) {
		return this;
	}
	
	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}
	
	@Override
	public boolean isSearchable() {
		return false;
	}
	
	@Override
	public void index(GraphObject entity, Object value) {
		// no indexing
	}

	@Override
	public Object getValueForEmptyFields() {
		return null;
	}
}
