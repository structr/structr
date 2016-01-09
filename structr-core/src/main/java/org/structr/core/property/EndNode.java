/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.property;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.BooleanClause;
import org.neo4j.helpers.Predicate;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.OneEndpoint;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Source;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.search.EmptySearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SourceSearchAttribute;
import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;

/**
 * A property that defines a relationship with the given parameters between two nodes.
 *
 *
 */
public class EndNode<S extends NodeInterface, T extends NodeInterface> extends Property<T> implements RelationProperty<T> {

	private static final Logger logger = Logger.getLogger(EndNode.class.getName());

	private Relation<S, T, ? extends Source, OneEndpoint<T>> relation = null;
	private Notion notion                                             = null;
	private Class<T> destType                                         = null;

	/**
	 * Constructs an entity property with the given name, the given destination type,
	 * the given relationship type, the given direction and the given cascade delete
	 * flag.
	 *
	 * @param name
	 * @param relationClass
	 */
	public EndNode(String name, Class<? extends Relation<S, T, ? extends Source, OneEndpoint<T>>> relationClass) {
		this(name, relationClass, new ObjectNotion());
	}

	/**
	 * Constructs an entity property with the given name, the given destination type,
	 * the given relationship type, the given direction and the given notion.
	 *
	 * @param name
	 * @param relationClass
	 * @param notion
	 */
	public EndNode(String name, Class<? extends Relation<S, T, ? extends Source, OneEndpoint<T>>> relationClass, Notion notion) {

		super(name);

		try {

			this.relation  = relationClass.newInstance();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		this.notion    = notion;
		this.destType  = relation.getTargetType();

		this.notion.setType(destType);
		this.notion.setRelationProperty(this);

		StructrApp.getConfiguration().registerConvertedProperty(this);
	}

	@Override
	public String typeName() {
		return "object";
	}

	@Override
	public Integer getSortType() {
		return null;
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext) {
		return notion.getEntityConverter(securityContext);
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final org.neo4j.helpers.Predicate<GraphObject> predicate) {

		OneEndpoint<T> endpoint  = relation.getTarget();

		return endpoint.get(securityContext, (NodeInterface)obj, predicate);
	}

	@Override
	public Object setProperty(SecurityContext securityContext, GraphObject obj, T value) throws FrameworkException {

		OneEndpoint<T> endpoint  = relation.getTarget();

		return endpoint.set(securityContext, (NodeInterface)obj, value);
	}

	@Override
	public Class relatedType() {
		return destType;
	}

	@Override
	public Class valueType() {
		return relatedType();
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public Property<T> indexed() {
		return this;
	}

	@Override
	public Property<T> indexed(NodeService.NodeIndex nodeIndex) {
		return this;
	}

	@Override
	public Property<T> indexed(NodeService.RelationshipIndex relIndex) {
		return this;
	}

	@Override
	public Property<T> passivelyIndexed() {
		return this;
	}

	@Override
	public Property<T> passivelyIndexed(NodeService.NodeIndex nodeIndex) {
		return this;
	}

	@Override
	public Property<T> passivelyIndexed(NodeService.RelationshipIndex relIndex) {
		return this;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public boolean isSearchable() {
		return true;
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
	public void addSingleElement(final SecurityContext securityContext, final GraphObject obj, final T t) throws FrameworkException {
		setProperty(securityContext, obj, t);
	}

	@Override
	public Class<T> getTargetType() {
		return destType;
	}

	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, BooleanClause.Occur occur, T searchValue, boolean exactMatch, final Query query) {

		final Predicate<GraphObject> predicate    = query != null ? query.toPredicate() : null;
		final SourceSearchAttribute attr          = new SourceSearchAttribute(occur);
		final Set<GraphObject> intersectionResult = new LinkedHashSet<>();
		boolean alreadyAdded                      = false;

		if (searchValue != null && !StringUtils.isBlank(searchValue.toString())) {

			if (exactMatch) {

				switch (occur) {

					case MUST:

						if (!alreadyAdded) {

							// the first result is the basis of all subsequent intersections
							intersectionResult.addAll(getRelatedNodesReverse(securityContext, searchValue, declaringClass, predicate));

							// the next additions are intersected with this one
							alreadyAdded = true;

						} else {

							intersectionResult.retainAll(getRelatedNodesReverse(securityContext, searchValue, declaringClass, predicate));
						}

						break;

					case SHOULD:
						intersectionResult.addAll(getRelatedNodesReverse(securityContext, searchValue, declaringClass, predicate));
						break;

					case MUST_NOT:
						break;
				}

			} else {

				intersectionResult.addAll(getRelatedNodesReverse(securityContext, searchValue, declaringClass, predicate));
			}

			attr.setResult(intersectionResult);

		} else {

			// experimental filter attribute that
			// removes entities with a non-empty
			// value in the given field
			return new EmptySearchAttribute(this, null);
		}

		return attr;
	}

	// ----- overridden methods from super class -----
	@Override
	protected <T extends NodeInterface> Set<T> getRelatedNodesReverse(final SecurityContext securityContext, final NodeInterface obj, final Class destinationType, final Predicate<GraphObject> predicate) {

		Set<T> relatedNodes = new LinkedHashSet<>();

		try {

			final Object source = relation.getSource().get(securityContext, obj, predicate);
			if (source != null) {

				if (source instanceof Iterable) {

					Iterable<T> nodes = (Iterable<T>)source;
					for (final T n : nodes) {

						relatedNodes.add(n);
					}

				} else {

					relatedNodes.add((T)source);
				}
			}

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Unable to fetch related node: {0}", t.getMessage());
		}

		return relatedNodes;
	}

	@Override
	public Relation getRelation() {
		return relation;
	}

	@Override
	public boolean doAutocreate() {

		if (relation != null) {

			switch (relation.getAutocreationFlag()) {

				case Relation.ALWAYS:
				case Relation.SOURCE_TO_TARGET:
					return true;
			}
		}

		return false;
	}

	@Override
	public String getAutocreateFlagName() {

		if (relation != null) {
			return Relation.CASCADING_DESCRIPTIONS[relation.getAutocreationFlag()];
		}

		return Relation.CASCADING_DESCRIPTIONS[0];
	}

	@Override
	public String getDirectionKey() {
		return "out";
	}
}
