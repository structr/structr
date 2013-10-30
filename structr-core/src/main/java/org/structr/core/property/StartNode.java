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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.Target;
import org.structr.core.entity.OneStartpoint;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.core.graph.NodeService.RelationshipIndex;
import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;

/**
 * A property that defines a relationship with the given parameters between two nodes.
 *
 * @author Christian Morgner
 */
public class StartNode<S extends NodeInterface, T extends NodeInterface> extends Property<S> {

	// relationship members
	private Relation<S, T, OneStartpoint<S>, ? extends Target> relation = null;
	private Notion notion                                               = null;
	private Class<T> destType                                           = null;
	
	/**
	 * Constructs an entity property with the given name, the given destination type,
	 * the given relationship type, the given direction and the given cascade delete
	 * flag.
	 * 
	 * @param name
	 * @param relationClass
	 */
	public StartNode(String name, Class<? extends Relation<S, T, OneStartpoint<S>, ? extends Target>> relationClass) {
		this(name, relationClass, new ObjectNotion());
	}

	/**
	 * Constructs an entity property with the name of the declaring field, the
	 * given destination type, the given relationship type, the given direction,
	 * the given cascade delete flag, the given notion and the given cascade
	 * delete flag.
	 * 
	 * @param name
	 * @param relationClass
	 * @param notion
	 */
	public StartNode(String name, Class<? extends Relation<S, T, OneStartpoint<S>, ? extends Target>> relationClass, Notion notion) {

		super(name);
		
		try {
			
			this.relation = relationClass.newInstance();
			
		} catch (Throwable t) {
			t.printStackTrace();
		}

		this.notion        = notion;
		this.destType      = relation.getTargetType();

		// configure notion
		this.notion.setType(destType);
		
		EntityContext.registerConvertedProperty(this);
	}
	
	@Override
	public String typeName() {
		return "Object";
	}

	@Override
	public Integer getSortType() {
		return null;
	}

	@Override
	public PropertyConverter<S, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<S, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, S> inputConverter(SecurityContext securityContext) {
		return notion.getEntityConverter(securityContext);
	}

	@Override
	public S getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		
		final OneStartpoint<S> startpoint = relation.getSource();
		
		return startpoint.get(securityContext, (NodeInterface)obj);
	}

	@Override
	public void setProperty(SecurityContext securityContext, GraphObject obj, S value) throws FrameworkException {
		
		OneStartpoint<S> startpoint = relation.getSource();

		startpoint.set(securityContext, (NodeInterface)obj, value);
	}
	
	@Override
	public Class<? extends GraphObject> relatedType() {
		return destType;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	public Notion getNotion() {
		return notion;
	}

	@Override
	public Property<S> indexed() {
		return this;
	}

	@Override
	public Property<S> indexed(NodeIndex nodeIndex) {
		return this;
	}
	
	@Override
	public Property<S> indexed(RelationshipIndex relIndex) {
		return this;
	}
	
	@Override
	public Property<S> passivelyIndexed() {
		return this;
	}
	
	@Override
	public Property<S> passivelyIndexed(NodeIndex nodeIndex) {
		return this;
	}
	
	@Override
	public Property<S> passivelyIndexed(RelationshipIndex relIndex) {
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
