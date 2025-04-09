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
package org.structr.core.graphql;

import graphql.language.*;
import org.structr.api.Predicate;
import org.structr.api.search.Operation;
import org.structr.common.PathResolvingComparator;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.QueryGroup;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.search.GraphSearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;

import java.util.*;
import java.util.Map.Entry;

/**
 */
public class QueryConfig implements GraphQLQueryConfiguration {

	private final Map<String, SearchAttribute> attributes = new LinkedHashMap<>();
	private final Set<PropertyKey> propertyKeys           = new LinkedHashSet<>();
	private ActionContext actionContext                   = null;
	private String sortKeySource                          = null;
	private PropertyKey sortKey                           = null;
	private boolean sortDescending                        = false;
	private int pageSize                                  = Integer.MAX_VALUE;
	private int page                                      = 1;

	public QueryConfig(final ActionContext actionContext) {
		this.actionContext = actionContext;
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {
		return propertyKeys;
	}

	@Override
	public Predicate getPredicateForPropertyKey(final String key) {

		if (key != null) {

			final SearchAttribute attribute = attributes.get(key);
			if (attribute != null) {

				// insert comparator to allow sorting
				attribute.setComparator(getComparator());

				// SearchAttribute implements Predicate
				return attribute;

			} else if (sortKeySource != null || sortKey != null) {

				// accept-all predicate just for sorting
				return new Predicate<GraphObject>() {

					@Override
					public boolean accept(final GraphObject value) {
						return true;
					}

					@Override
					public Comparator<GraphObject> comparator() {
						return getComparator();
					}
				};
			}
		}

		return null;
	}

	public void addPropertyKey(final PropertyKey key) {
		propertyKeys.add(key);
	}

	public void setPage(final int page) {
		this.page = page;
	}

	@Override
	public int getPage() {
		return page;
	}

	public void setPageSize(final int pageSize) {
		this.pageSize = pageSize;
	}

	@Override
	public int getPageSize() {
		return pageSize;
	}

	public void setSortKey(final PropertyKey sortKey) {
		this.sortKey = sortKey;
	}

	public PropertyKey getSortKey() {
		return sortKey;
	}

	public void setSortDescending(final boolean sortDescending) {
		this.sortDescending = sortDescending;
	}

	public boolean sortDescending() {
		return this.sortDescending;
	}

	public void configureQuery(final QueryGroup query) {

		query.page(page);
		query.pageSize(pageSize);
		query.attributes(new LinkedList<>(attributes.values()));

		if (sortKey != null) {

			query.sort(sortKey, sortDescending);

		} else if (sortKeySource != null) {

			query.comparator(new PathResolvingComparator(actionContext, sortKeySource, sortDescending));
		}
	}

	public void handleTypeArguments(final SecurityContext securityContext, final Traits type, final List<Argument> arguments) throws FrameworkException {

		// parse arguments
		for (final Argument argument : arguments) {

			final String name = argument.getName();
			final Value value = argument.getValue();

			switch (name) {

				case "_page":
					this.page = getIntegerValue(value, 1);
					break;

				case "_pageSize":
				case "_first":
					this.pageSize = getIntegerValue(value, Integer.MAX_VALUE);
					break;

				case "_sort":
					this.sortKeySource = getStringValue(value, "name");
					this.sortKey       = type.hasKey(sortKeySource) ? type.key(sortKeySource) : null;
					break;

				case "_desc":
					this.sortDescending = getBooleanValue(value, false);
					break;

				default:
					handleNonPrimitiveSearchObject(securityContext, type, name, value);
					break;
			}
		}
	}

	public void handleFieldArguments(final SecurityContext securityContext, final Traits type, final Field parentField, final Field field) throws FrameworkException {

		final List<SearchTuple> searchTuples = new LinkedList<>();
		final String parentName              = parentField.getName();
		final PropertyKey key                = type.key(field.getName());
		Operation operation                  = Operation.AND;

		// FIXME: create a search group here...

		// parse arguments
		for (final Argument argument : field.getArguments()) {

			final String name = argument.getName();
			final Value value = argument.getValue();

			switch (name) {

				case "_equals":
					//searchTuples.add(new SearchTuple(castValue(securityContext, Traits.of(key.relatedType()), key, value), true));
					searchTuples.add(new SearchTuple(castValue(securityContext, type, key, value), true));
					break;

				case "_contains":
					//searchTuples.add(new SearchTuple(castValue(securityContext, Traits.of(key.relatedType()), key, value), false));
					searchTuples.add(new SearchTuple(castValue(securityContext, type, key, value), false));
					break;

				case "_conj":
					operation = getOperationForSearchValue(value);
					break;

				default:
					break;
			}
		}

		//final SearchAttributeGroup group = new SearchAttributeGroup(operation);
		// FIXME: conjunction is not used here, how can we fix this?

		// only add field if a value was set
		for (final SearchTuple tuple : searchTuples) {

			addAttribute(parentName, key.getSearchAttribute(securityContext, tuple.value, tuple.exact, null));
		}

		//addAttribute(parentName, group);
	}

	// ----- private methods -----
	private boolean getBooleanValue(final Value value, final boolean defaultValue) {

		if (value != null && value instanceof BooleanValue) {

			return ((BooleanValue)value).isValue();
		}

		return defaultValue;
	}

	private double getDoubleValue(final Value value, final double defaultValue) {

		if (value != null && value instanceof FloatValue) {

			return ((FloatValue)value).getValue().doubleValue();
		}

		return defaultValue;
	}

	private int getIntegerValue(final Value value, final int defaultValue) {

		if (value != null && value instanceof IntValue) {

			return ((IntValue)value).getValue().intValue();
		}

		return defaultValue;
	}

	private String getStringValue(final Value value, final String defaultValue) {

		if (value != null && value instanceof StringValue) {

			return ((StringValue)value).getValue();
		}

		return defaultValue;
	}

	private Map<String, Object> getMapValue(final SecurityContext securityContext, final Traits type, final Value value) throws FrameworkException {

		final Map<String, Object> map = new LinkedHashMap<>();

		if (value != null && value instanceof ObjectValue) {

			final ObjectValue obj = (ObjectValue)value;

			for (final ObjectField field : obj.getObjectFields()) {

				// recurse
				map.put(field.getName(), castValue(securityContext, type, null, field.getValue()));
			}
		}

		return map;
	}

	private Operation getOperationForSearchValue(final Value value) {

		final String val = getStringValue(value, null);
		if (val != null) {

			return getOperation(val);
		}

		return Operation.AND;
	}

	private Operation getOperation(final Object value) {

		if (value instanceof String) {

			final String conj = value.toString().toLowerCase();

			switch (conj) {

				case "or":
					return Operation.OR;

				case "and":
					return Operation.AND;

				case "not":
					return Operation.NOT;
			}
		}

		return Operation.AND;
	}

	private Object castValue(final SecurityContext securityContext, final Traits type, final PropertyKey key, final Value value) throws FrameworkException {

		if (value instanceof StringValue) {
			return getStringValue(value, null);
		}

		if (value instanceof FloatValue) {
			return getDoubleValue(value, -1);
		}

		if (value instanceof IntValue) {
			return getIntegerValue(value, -1);
		}

		if (value instanceof BooleanValue) {
			return getBooleanValue(value, false);
		}

		if (value instanceof ObjectValue && key != null) {

			final Map<String, Object> parameters = new LinkedHashMap<>();

			parameters.put(key.jsonName(), getMapValue(securityContext, type, value));

			final PropertyMap propertyMap = PropertyMap.inputTypeToJavaType(securityContext, type.getName(), parameters);

			// return converted result (should be replaced by NodeInterface)
			return propertyMap.get(key);

		}

		return null;
	}

	private void addAttribute(final String parentName, final SearchAttribute newAttribute) {

		final SearchAttribute existingAttribute = attributes.get(parentName);
		if (existingAttribute == null) {

			// single value, no group, no existing attribute
			attributes.put(parentName, newAttribute);

		} else {

			// we need to combine the attributes
			if (existingAttribute instanceof SearchAttributeGroup group) {

				// attribute already set, add
				group.add(newAttribute);

			} else {

				// create group from two single attributes
				final SearchAttributeGroup group = new SearchAttributeGroup(Operation.AND);
				group.add(existingAttribute);
				group.add(newAttribute);

				attributes.put(parentName, group);
			}
		}
	}

	private Comparator getComparator() {

		if (sortKey != null) {

			return GraphObject.sorted(sortKey, sortDescending);
			//return new GraphObjectComparator(sortKey, sortDescending);
		}

		if (sortKeySource != null) {

			return new PathResolvingComparator(actionContext, sortKeySource, sortDescending);
		}

		return null;
	}

	private void handleNonPrimitiveSearchObject(final SecurityContext securityContext, final Traits type, final String name, final Value value) throws FrameworkException {

		if (type.hasKey(name)) {

			final PropertyKey key = type.key(name);

			if (value instanceof StringValue || value instanceof IntValue || value instanceof FloatValue || value instanceof BooleanValue) {

				// handle simple selections like an _equals on the field
				addAttribute(name, key.getSearchAttribute(securityContext, castValue(securityContext, type, key, value), true, null));

			} else if (value instanceof ObjectValue) {

				// handle complex query object
				final Map<String, Object> input           = unwrapValue((ObjectValue)value);
				final Set<Entry<String, Object>> entrySet = input.entrySet();

				for (final Entry<String, Object> entry : entrySet) {

					final String searchKey   = entry.getKey();
					final Object searchValue = entry.getValue();
					final String relatedType = key.relatedType();

					if (searchValue instanceof Map) {

						final Map<String, Object> searchMap = (Map)searchValue;
						Object contains                     = searchMap.get("_contains");
						Object equals                       = searchMap.get("_equals");

						if (relatedType != null) {

							final Traits traits = Traits.of(relatedType);
							if (traits.hasKey(searchKey)) {

								final PropertyKey notionKey = traits.key(searchKey);

								if (equals != null) {

									final PropertyConverter conv = notionKey.inputConverter(securityContext);
									if (conv != null) {

										equals = conv.convert(equals);
									}

									addAttribute(name, new GraphSearchAttribute(notionKey, key, equals, true));

									// primitive property
									//addAttribute(key, key.getSearchAttribute(securityContext, Occurrence.REQUIRED, equals, true, null), Occurrence.REQUIRED);

								} else if (contains != null) {

									final PropertyConverter conv = notionKey.inputConverter(securityContext);
									if (conv != null) {

										contains = conv.convert(contains);
									}

									addAttribute(name, new GraphSearchAttribute(notionKey, key, contains, false));

									//addAttribute(key, key.getSearchAttribute(securityContext, Occurrence.REQUIRED, contains, false, null), Occurrence.REQUIRED);
								}

							} else {

								// throw error here!
							}

						} else {

							if (equals != null) {

								// primitive property
								addAttribute(name, key.getSearchAttribute(securityContext, equals, true, null));

							} else if (contains != null) {

								addAttribute(name, key.getSearchAttribute(securityContext, contains, false, null));
							}
						}

					} else if (searchValue instanceof List) {

						// handle multiple values with conjunction (AND or OR)
						final List list     = (List)searchValue;
						Operation operation = Operation.AND;

						if (input.containsKey("_conj")) {

							operation = getOperation(input.get("_conj"));
						}

						// FIXME: this must create a new group!

						for (final Object listValue : list) {

							switch (searchKey) {

								case "_contains":
									addAttribute(name, key.getSearchAttribute(securityContext, listValue, false, null));
									break;

								case "_equals":
									addAttribute(name, key.getSearchAttribute(securityContext, listValue, true, null));
									break;
							}
						}

					} else {

						switch (searchKey) {

							case "_contains":
								addAttribute(name, key.getSearchAttribute(securityContext, searchValue, false, null));
								break;

							case "_equals":
								addAttribute(name, key.getSearchAttribute(securityContext, searchValue, true, null));
								break;
						}

					}
				}
			}
		}
	}

	private Map<String, Object> unwrapValue(final ObjectValue value) {

		final Map<String, Object> map = new LinkedHashMap<>();

		for (final ObjectField field : value.getObjectFields()) {

			final String name = field.getName();
			final Value child = field.getValue();

			if (child instanceof ObjectValue) {

				putObjectOrList(map, name, unwrapValue((ObjectValue)child));

			} else if (child instanceof StringValue) {

				putObjectOrList(map, name, ((StringValue)child).getValue());

			} else if (child instanceof IntValue) {

				putObjectOrList(map, name, ((IntValue)child).getValue().intValue());

			} else if (child instanceof FloatValue) {

				putObjectOrList(map, name, ((FloatValue)child).getValue().doubleValue());

			} else if (child instanceof BooleanValue) {

				putObjectOrList(map, name, ((BooleanValue)child).isValue());
			}
		}

		return map;
	}

	private void putObjectOrList(final Map<String, Object> map, final String key, final Object value) {

		if (map.containsKey(key)) {

			final Object existingValue = map.get(key);
			if (existingValue instanceof List) {

				((List)existingValue).add(value);

			} else {

				final List list = new ArrayList();
				list.add(existingValue);
				list.add(value);

				map.put(key, list);
			}

		} else {

			map.put(key, value);
		}
	}

	// ----- nested classes -----
	private class SearchTuple {

		public boolean exact = false;
		public Object value  = null;

		public SearchTuple(final Object value, final boolean exact) {
			this.value = value;
			this.exact = exact;
		}
	}
}



/**


							// relationship property
							final PropertyKey searchPropertyKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(key.relatedType(), searchKey, false);
							if (searchPropertyKey != null) {

								final Query<GraphObject> query = StructrApp.getInstance(securityContext).nodeQuery(relatedType);
								Occurrence occurrence          = Occurrence.REQUIRED;
								boolean exactMatch             = true;

								if (equals != null) {

									query.and(searchPropertyKey, equals);
									exactMatch = true;

								} else if (contains != null) {

									query.and(searchPropertyKey, contains, false);
									occurrence = Occurrence.OPTIONAL;
									exactMatch = false;
								}

								// add sources that will be merged later on
								if (key.isCollection()) {

									final List<GraphObject> list = query.getAsList();
									if (list.isEmpty()) {

										addAttribute(key, new SourceSearchAttribute(Occurrence.REQUIRED), occurrence);

									} else {

										addAttribute(key, key.getSearchAttribute(securityContext, Occurrence.REQUIRED, list, exactMatch, null), Occurrence.REQUIRED);
									}

								} else {

									final List<GraphObject> list = query.getAsList();
									if (list.isEmpty()) {

										addAttribute(key, new SourceSearchAttribute(Occurrence.REQUIRED), occurrence);

									} else {

										// collect all results in a single SourceSearchAttribute instead of using one for each item
										SearchAttribute attribute = null;

										for (final GraphObject candidate : query.getAsList()) {

											final SearchAttribute attr = key.getSearchAttribute(securityContext, Occurrence.REQUIRED, candidate, exactMatch, null);
											if (attribute == null) {

												attribute = attr;

												addAttribute(key, attribute, Occurrence.REQUIRED);

											} else {

												attribute.addToResult(attr.getResult());
											}
										}
									}
								}
							}
 */

