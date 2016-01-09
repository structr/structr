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
import org.apache.lucene.search.BooleanClause.Occur;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.MUST_NOT;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;
import org.neo4j.helpers.Predicate;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.search.EmptySearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SourceSearchAttribute;
import org.structr.core.notion.Notion;

/**
* A property that uses the value of a related node property to create
* a relationship between two nodes. This property should only be used
* with related properties that uniquely identify a given node, as the
* value will be used to search for a matching node to which the
* relationship will be created.
 *
 *
 */
public class EntityNotionProperty<S extends NodeInterface, T> extends Property<T> {

	private static final Logger logger = Logger.getLogger(EntityNotionProperty.class.getName());

	private Property<S> entityProperty = null;
	private Notion<S, T> notion        = null;

	public EntityNotionProperty(final String name, final Property<S> base, final Notion<S, T> notion) {

		super(name);

		this.notion = notion;
		this.entityProperty   = base;

		notion.setType(base.relatedType());
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
	public boolean isSearchable() {
		return true;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public String typeName() {
		return "";
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
		return null;
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final org.neo4j.helpers.Predicate<GraphObject> predicate) {

		try {
			return notion.getAdapterForGetter(securityContext).adapt(entityProperty.getProperty(securityContext, obj, applyConverter, predicate));

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to apply notion of type {0} to property {1}", new Object[] { notion.getClass(), this } );
		}

		return null;
	}

	@Override
	public Object setProperty(SecurityContext securityContext, GraphObject obj, T value) throws FrameworkException {

		if (value != null) {

			return entityProperty.setProperty(securityContext, obj, notion.getAdapterForSetter(securityContext).adapt(value));

		} else {

			return entityProperty.setProperty(securityContext, obj, null);
		}
	}

	@Override
	public Class relatedType() {
		return entityProperty.relatedType();
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
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, Occur occur, T searchValue, boolean exactMatch, final Query query) {

		final Predicate<GraphObject> predicate    = query != null ? query.toPredicate() : null;
		final SourceSearchAttribute attr          = new SourceSearchAttribute(occur);
		final Set<GraphObject> intersectionResult = new LinkedHashSet<>();
		boolean alreadyAdded                      = false;

		try {

			if (searchValue != null && !StringUtils.isBlank(searchValue.toString())) {

				final App app                          = StructrApp.getInstance(securityContext);
				final PropertyKey key                  = notion.getPrimaryPropertyKey();
				final PropertyConverter inputConverter = key != null ? key.inputConverter(securityContext) : null;

				// transform search values using input convert of notion property
				final Object transformedValue          = inputConverter != null ? inputConverter.convert(searchValue) : searchValue;

				if (exactMatch) {

					Result<AbstractNode> result = app.nodeQuery(entityProperty.relatedType()).and(key, transformedValue).getResult();

					for (AbstractNode node : result.getResults()) {

						switch (occur) {

							case MUST:

								if (!alreadyAdded) {

									// the first result is the basis of all subsequent intersections
									intersectionResult.addAll(entityProperty.getRelatedNodesReverse(securityContext, node, declaringClass, predicate));

									// the next additions are intersected with this one
									alreadyAdded = true;

								} else {

									intersectionResult.retainAll(entityProperty.getRelatedNodesReverse(securityContext, node, declaringClass, predicate));
								}

								break;

							case SHOULD:
								intersectionResult.addAll(entityProperty.getRelatedNodesReverse(securityContext, node, declaringClass, predicate));
								break;

							case MUST_NOT:
								break;
						}
					}

				} else {

					Result<AbstractNode> result = app.nodeQuery(entityProperty.relatedType(), false).and(key, transformedValue, false).getResult();

					// loose search behaves differently, all results must be combined
					for (AbstractNode node : result.getResults()) {

						intersectionResult.addAll(entityProperty.getRelatedNodesReverse(securityContext, node, declaringClass, predicate));
					}
				}

				attr.setResult(intersectionResult);

			} else {

				// experimental filter attribute that
				// removes entities with a non-empty
				// value in the given field
				return new EmptySearchAttribute(this, null);
			}

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		return attr;
	}

	@Override
	public void index(GraphObject entity, Object value) {
		// no direct indexing
	}

	@Override
	public Object getValueForEmptyFields() {
		return null;
	}

	@Override
	public int getProcessingOrderPosition() {
		return 1000;
	}
}
