/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.bolt.index;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.QueryResult;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.index.Index;
import org.structr.api.search.ArrayQuery;
import org.structr.api.search.EmptyQuery;
import org.structr.api.search.ExactQuery;
import org.structr.api.search.FulltextQuery;
import org.structr.api.search.GroupQuery;
import org.structr.api.search.NotEmptyQuery;
import org.structr.api.search.QueryPredicate;
import org.structr.api.search.RangeQuery;
import org.structr.api.search.RelationshipQuery;
import org.structr.api.search.SpatialQuery;
import org.structr.api.search.TypeConverter;
import org.structr.api.search.TypeQuery;
import org.structr.api.search.UuidQuery;
import org.structr.api.util.Iterables;
import org.structr.bolt.BoltDatabaseService;
import org.structr.bolt.index.converter.BooleanTypeConverter;
import org.structr.bolt.index.converter.ByteTypeConverter;
import org.structr.bolt.index.converter.DateTypeConverter;
import org.structr.bolt.index.converter.DoubleTypeConverter;
import org.structr.bolt.index.converter.FloatTypeConverter;
import org.structr.bolt.index.converter.IntTypeConverter;
import org.structr.bolt.index.converter.LongTypeConverter;
import org.structr.bolt.index.converter.ShortTypeConverter;
import org.structr.bolt.index.converter.StringTypeConverter;
import org.structr.bolt.index.factory.ArrayQueryFactory;
import org.structr.bolt.index.factory.EmptyQueryFactory;
import org.structr.bolt.index.factory.GroupQueryFactory;
import org.structr.bolt.index.factory.KeywordQueryFactory;
import org.structr.bolt.index.factory.NotEmptyQueryFactory;
import org.structr.bolt.index.factory.QueryFactory;
import org.structr.bolt.index.factory.RangeQueryFactory;
import org.structr.bolt.index.factory.RelationshipQueryFactory;
import org.structr.bolt.index.factory.SpatialQueryFactory;
import org.structr.bolt.index.factory.TypeQueryFactory;
import org.structr.bolt.index.factory.UuidQueryFactory;

/**
 *
 */
public abstract class AbstractCypherIndex<T extends PropertyContainer> implements Index<T>, QueryFactory {

	private static final Logger logger                       = LoggerFactory.getLogger(AbstractCypherIndex.class.getName());
	public static final TypeConverter DEFAULT_CONVERTER      = new StringTypeConverter();
	public static final Map<Class, TypeConverter> CONVERTERS = new HashMap<>();
	public static final Map<Class, QueryFactory> FACTORIES   = new HashMap<>();

	public static final Set<Class> INDEXABLE = new HashSet<>(Arrays.asList(new Class[] {
		String.class,   Boolean.class,   Short.class,   Integer.class,   Long.class,   Character.class,   Float.class,   Double.class,   byte.class,
		String[].class, Boolean[].class, Short[].class, Integer[].class, Long[].class, Character[].class, Float[].class, Double[].class, byte[].class
	}));

	static {

		FACTORIES.put(NotEmptyQuery.class,     new NotEmptyQueryFactory());
		FACTORIES.put(FulltextQuery.class,     new KeywordQueryFactory());
		FACTORIES.put(SpatialQuery.class,      new SpatialQueryFactory());
		FACTORIES.put(GroupQuery.class,        new GroupQueryFactory());
		FACTORIES.put(RangeQuery.class,        new RangeQueryFactory());
		FACTORIES.put(ExactQuery.class,        new KeywordQueryFactory());
		FACTORIES.put(ArrayQuery.class,        new ArrayQueryFactory());
		FACTORIES.put(EmptyQuery.class,        new EmptyQueryFactory());
		FACTORIES.put(TypeQuery.class,         new TypeQueryFactory());
		FACTORIES.put(UuidQuery.class,         new UuidQueryFactory());
		FACTORIES.put(RelationshipQuery.class, new RelationshipQueryFactory());

		CONVERTERS.put(Boolean.class, new BooleanTypeConverter());
		CONVERTERS.put(String.class,  new StringTypeConverter());
		CONVERTERS.put(Date.class,    new DateTypeConverter());
		CONVERTERS.put(Long.class,    new LongTypeConverter());
		CONVERTERS.put(Short.class,   new ShortTypeConverter());
		CONVERTERS.put(Integer.class, new IntTypeConverter());
		CONVERTERS.put(Float.class,   new FloatTypeConverter());
		CONVERTERS.put(Double.class,  new DoubleTypeConverter());
		CONVERTERS.put(byte.class,    new ByteTypeConverter());
	}

	protected final BoltDatabaseService db;

	public AbstractCypherIndex(final BoltDatabaseService db) {
		this.db = db;
	}

	public abstract QueryResult<T> getResult(final PageableQuery query);
	public abstract String getQueryPrefix(final String mainType, final String sourceTypeLabel, final String targetTypeLabel);
	public abstract String getQuerySuffix();

	@Override
	public void add(final PropertyContainer t, final String key, final Object value, final Class typeHint) {

		Object indexValue = value;
		if (value != null) {

			final Class type = value.getClass();

			if (type.isEnum()) {
				indexValue = indexValue.toString();
			}

			if (!INDEXABLE.contains(type)) {
				return;
			}
		}

		t.setProperty(key, indexValue);
	}

	@Override
	public void remove(final PropertyContainer t) {
	}

	@Override
	public void remove(final PropertyContainer t, final String key) {
	}

	@Override
	public QueryResult<T> query(final QueryPredicate predicate) {

		final AdvancedCypherQuery query = new AdvancedCypherQuery(this);

		createQuery(this, predicate, query, true);

		final String sortKey = predicate.getSortKey();
		if (sortKey != null) {

			query.sort(predicate.getSortType(), sortKey, predicate.sortDescending());
		}

		return getResult(query);
	}

	// ----- interface QueryFactory -----
	@Override
	public boolean createQuery(final QueryFactory parent, final QueryPredicate predicate, final AdvancedCypherQuery query, final boolean isFirst) {

		final Class type = predicate.getQueryType();
		if (type != null) {

			final QueryFactory factory = FACTORIES.get(type);
			if (factory != null) {

				return factory.createQuery(this, predicate, query, isFirst);

			} else {

				logger.warn("No query factory registered for type {}", type);
			}
		}

		return false;
	}

	// ----- nested classes -----
	protected class CachedQueryResult implements QueryResult<T> {

		private Collection<T> result = null;

		public CachedQueryResult(final Iterable<T> source) {

			if (source instanceof Collection) {

				this.result = (Collection)source;

			} else {

				this.result = Iterables.toList(source);

			}
		}

		@Override
		public void close() {
		}

		@Override
		public Iterator<T> iterator() {
			return result.iterator();
		}

		public boolean isEmpty() {
			return result.isEmpty();
		}
	}
}
