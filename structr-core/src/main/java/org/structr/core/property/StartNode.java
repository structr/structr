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
import org.structr.core.entity.Target;
import org.structr.core.entity.OneStartpoint;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.core.graph.NodeService.RelationshipIndex;
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
public class StartNode<S extends NodeInterface, T extends NodeInterface> extends Property<S> implements RelationProperty<S> {

	private static final Logger logger = Logger.getLogger(StartNode.class.getName());

	// relationship members
	private Relation<S, T, OneStartpoint<S>, ? extends Target> relation = null;
	private Class<S> destType                                           = null;
	private Notion notion                                               = null;

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
		this.destType      = relation.getSourceType();

		// configure notion
		this.notion.setType(destType);
		this.notion.setRelationProperty(this);

		StructrApp.getConfiguration().registerConvertedProperty(this);
	}

	@Override
	public String typeName() {
		return "object";
	}

	@Override
	public Class valueType() {
		return relatedType();
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
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public S getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final org.neo4j.helpers.Predicate<GraphObject> predicate) {

		final OneStartpoint<? extends S> startpoint = relation.getSource();

		return startpoint.get(securityContext, (NodeInterface)obj, predicate);
	}

	@Override
	public Object setProperty(SecurityContext securityContext, GraphObject obj, S value) throws FrameworkException {

		OneStartpoint<S> startpoint = relation.getSource();

		return startpoint.set(securityContext, (NodeInterface)obj, value);
	}

	@Override
	public Class relatedType() {
		return destType;
	}

	@Override
	public boolean isCollection() {
		return false;
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
	public void addSingleElement(final SecurityContext securityContext, final GraphObject obj, final S s) throws FrameworkException {
		setProperty(securityContext, obj, s);
	}

	@Override
	public Class<? extends S> getTargetType() {
		return destType;
	}

	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, BooleanClause.Occur occur, S searchValue, boolean exactMatch, final Query query) {

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

			final Object target = relation.getTarget().get(securityContext, obj, predicate);
			if (target != null) {

				if (target instanceof Iterable) {

					Iterable<T> nodes = (Iterable<T>)target;
					for (final T n : nodes) {

						relatedNodes.add(n);
					}

				} else {

					relatedNodes.add((T)target);
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
				case Relation.TARGET_TO_SOURCE:
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
		return "in";
	}
}
