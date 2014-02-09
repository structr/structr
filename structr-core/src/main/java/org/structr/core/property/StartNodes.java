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
package org.structr.core.property;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.NotNullPredicate;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.ManyStartpoint;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Target;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeService;
import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;

/**
 * A property that defines a relationship with the given parameters between a node and a collection of other nodes.
 *
 * @author Christian Morgner
 */
public class StartNodes<S extends NodeInterface, T extends NodeInterface> extends Property<List<S>> implements RelationProperty<S> {

	private static final Logger logger = Logger.getLogger(StartNodes.class.getName());

	private Relation<S, T, ManyStartpoint<S>, ? extends Target> relation = null;
	private Notion notion                                                = null;
	private Class<S> destType                                            = null;
	
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
		this.destType = relation.getSourceType();

		this.notion.setType(destType);
		
		StructrApp.getConfiguration().registerConvertedProperty(this);
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
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public List<S> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final Predicate<GraphObject> predicate) {
		
		ManyStartpoint<S> startpoint = relation.getSource();

		return Iterables.toList(Iterables.filter(new NotNullPredicate(), startpoint.get(securityContext, (NodeInterface)obj, predicate)));
	}

	@Override
	public void setProperty(SecurityContext securityContext, GraphObject obj, List<S> collection) throws FrameworkException {
		
		ManyStartpoint<S> startpoint = relation.getSource();

		startpoint.set(securityContext, (NodeInterface)obj, collection);
	}
	
	@Override
	public Class relatedType() {
		return destType;
	}

	@Override
	public boolean isCollection() {
		return true;
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

	// ----- interface RelationProperty -----
	@Override
	public Notion getNotion() {
		return notion;
	}

	@Override
	public void addSingleElement(final SecurityContext securityContext, final GraphObject obj, final S s) throws FrameworkException {
		
		List<S> list = getProperty(securityContext, obj, false);
		list.add(s);
		
		setProperty(securityContext, obj, list);
	}

	@Override
	public Class<S> getTargetType() {
		return destType;
	}
	
	// ----- overridden methods from super class -----
	@Override
	protected <T extends NodeInterface> Set<T> getRelatedNodesReverse(final SecurityContext securityContext, final NodeInterface obj, final Class destinationType) {

		Set<T> relatedNodes = new LinkedHashSet<>();
		
		if (obj instanceof AbstractNode) {

			AbstractNode node = (AbstractNode)obj;

			NodeFactory nodeFactory = new NodeFactory(securityContext);
			Node dbNode             = node.getNode();
			NodeInterface value     = null;

			try {

				for (Relationship rel : dbNode.getRelationships(relation, Direction.OUTGOING)) {

					value = nodeFactory.instantiate(rel.getOtherNode(dbNode));

					// break on first hit of desired type
					if (value != null && destinationType.isInstance(value)) {
						relatedNodes.add((T)value);
					}
				}

			} catch (Throwable t) {

				logger.log(Level.WARNING, "Unable to fetch related node: {0}", t.getMessage());
			}

		} else {

			logger.log(Level.WARNING, "Property {0} is registered on illegal type {1}", new Object[] { this, obj.getClass() } );
		}

		return relatedNodes;
	}

	@Override
	public Relation getRelation() {
		return relation;
	}
}
