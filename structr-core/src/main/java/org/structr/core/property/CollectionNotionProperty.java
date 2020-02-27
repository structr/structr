/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.search.Occurrence;
import org.structr.api.search.SortType;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
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
public class CollectionNotionProperty<S extends NodeInterface, T> extends Property<Iterable<T>> {

	private static final Logger logger = LoggerFactory.getLogger(CollectionIdProperty.class.getName());

	private Property<Iterable<S>> collectionProperty = null;
	private Notion<S, T> notion                  = null;

	public CollectionNotionProperty(String name, Property<Iterable<S>> base, Notion<S, T> notion) {

		super(name);

		this.notion             = notion;
		this.collectionProperty = base;

		notion.setType(base.relatedType());
	}

	@Override
	public Property<Iterable<T>> indexed() {
		return this;
	}

	@Override
	public Property<Iterable<T>> passivelyIndexed() {
		return this;
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
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public PropertyConverter<Iterable<T>, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<Iterable<T>, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, Iterable<T>> inputConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public Iterable<T> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public Iterable<T> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final Predicate<GraphObject> predicate) {

		try {

			return (notion.getCollectionAdapterForGetter(securityContext).adapt(collectionProperty.getProperty(securityContext, obj, applyConverter, predicate)));

		} catch (FrameworkException fex) {

			logger.warn("Unable to apply notion of type {} to property {}", new Object[] { notion.getClass(), this } );
		}

		return null;
	}

	@Override
	public Object setProperty(SecurityContext securityContext, GraphObject obj, Iterable<T> value) throws FrameworkException {

		if (value != null) {

			return collectionProperty.setProperty(securityContext, obj, notion.getCollectionAdapterForSetter(securityContext).adapt(value));

		} else {

			return collectionProperty.setProperty(securityContext, obj, null);
		}
	}

	@Override
	public Class relatedType() {
		return collectionProperty.relatedType();
	}

	@Override
	public Class valueType() {
		return relatedType();
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public List<T> convertSearchValue(SecurityContext securityContext, String requestParameter) throws FrameworkException {

		PropertyKey propertyKey = notion.getPrimaryPropertyKey();
		List<T> list            = new LinkedList<>();

		if (propertyKey != null) {

			PropertyConverter inputConverter = propertyKey.inputConverter(securityContext);
			if (inputConverter != null) {

				for (String part : requestParameter.split("[,;]+")) {

					list.add((T)inputConverter.convert(part));
				}

			} else {

				for (String part : requestParameter.split("[,;]+")) {

					list.add((T)part);
				}
			}
		}

		return list;
	}

	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, Occurrence occur, Iterable<T> searchValueIterable, boolean exactMatch, final Query query) {

		final Predicate<GraphObject> predicate    = query != null ? query.toPredicate() : null;
		final List<T> searchValues                = Iterables.toList(searchValueIterable);
		final SourceSearchAttribute attr          = new SourceSearchAttribute(occur);
		final Set<GraphObject> intersectionResult = new LinkedHashSet<>();
		boolean alreadyAdded                      = false;

		try {

			if (searchValues != null && !searchValues.isEmpty()) {

				final PropertyKey key                  = notion.getPrimaryPropertyKey();
				final PropertyConverter inputConverter = key.inputConverter(securityContext);
				final List<Object> transformedValues   = new LinkedList<>();
				boolean allBlank                       = true;

				// transform search values using input convert of notion property
				for (T searchValue : searchValues) {

					if (inputConverter != null) {

						transformedValues.add(inputConverter.convert(searchValue));
					} else {

						transformedValues.add(searchValue);
					}
				}

				// iterate over transformed values
				for (Object searchValue : transformedValues) {

					// check if the list contains non-empty search values
					if (StringUtils.isBlank(searchValue.toString())) {

						continue;

					} else {

						allBlank = false;
					}

					final App app = StructrApp.getInstance(securityContext);


					if (exactMatch) {

						final List<AbstractNode> result = app.nodeQuery(collectionProperty.relatedType()).and(notion.getPrimaryPropertyKey(), searchValue).getAsList();
						for (AbstractNode node : result) {

							switch (occur) {

								case REQUIRED:

									if (!alreadyAdded) {

										// the first result is the basis of all subsequent intersections
										intersectionResult.addAll(collectionProperty.getRelatedNodesReverse(securityContext, node, declaringClass, predicate));

										// the next additions are intersected with this one
										alreadyAdded = true;

									} else {

										intersectionResult.retainAll(collectionProperty.getRelatedNodesReverse(securityContext, node, declaringClass, predicate));
									}

									break;

								case OPTIONAL:
									intersectionResult.addAll(collectionProperty.getRelatedNodesReverse(securityContext, node, declaringClass, predicate));
									break;

								case FORBIDDEN:
									break;
							}
						}

					} else {

						// inexact search behaves differently, all results must be combined
						final List<AbstractNode> result = app.nodeQuery(collectionProperty.relatedType()).and(notion.getPrimaryPropertyKey(), searchValue, false).getAsList();
						for (AbstractNode node : result) {

							intersectionResult.addAll(collectionProperty.getRelatedNodesReverse(securityContext, node, declaringClass, predicate));
						}

					}
				}

				if (allBlank) {

					// experimental filter attribute that
					// removes entities with a non-empty
					// value in the given field
					return new EmptySearchAttribute(this, Collections.emptyList());

				} else {

					attr.setResult(intersectionResult);
				}

			} else {

				// experimental filter attribute that
				// removes entities with a non-empty
				// value in the given field
				return new EmptySearchAttribute(this, Collections.emptyList());

			}

		} catch (FrameworkException fex) {

			logger.warn("", fex);
		}

		return attr;
	}

	@Override
	public int getProcessingOrderPosition() {
		return 1000;
	}

	@Override
	public boolean isIndexed() {
		return false;
	}

	@Override
	public boolean isPassivelyIndexed() {
		return false;
	}

	// ----- protected methods overridden from superclass -----
	@Override
	protected boolean multiValueSplitAllowed() {
		return false;
	}
}
