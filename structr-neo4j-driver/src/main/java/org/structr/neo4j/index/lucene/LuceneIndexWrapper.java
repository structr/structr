/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.neo4j.index.lucene;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.neo4j.index.lucene.QueryContext;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.QueryResult;
import org.structr.api.search.ExactQuery;
import org.structr.api.search.FulltextQuery;
import org.structr.api.search.GroupQuery;
import org.structr.api.search.QueryPredicate;
import org.structr.api.search.RangeQuery;
import org.structr.api.search.SortType;
import org.structr.api.search.TypeConverter;
import org.structr.neo4j.Neo4jDatabaseService;
import org.structr.neo4j.index.AbstractIndexWrapper;
import org.structr.neo4j.index.IndexHitsWrapper;
import org.structr.neo4j.index.lucene.converter.DateTypeConverter;
import org.structr.neo4j.index.lucene.converter.DoubleTypeConverter;
import org.structr.neo4j.index.lucene.converter.IntTypeConverter;
import org.structr.neo4j.index.lucene.converter.LongTypeConverter;
import org.structr.neo4j.index.lucene.converter.StringTypeConverter;
import org.structr.neo4j.index.lucene.factory.FulltextQueryFactory;
import org.structr.neo4j.index.lucene.factory.GroupQueryFactory;
import org.structr.neo4j.index.lucene.factory.KeywordQueryFactory;
import org.structr.neo4j.index.lucene.factory.QueryFactory;
import org.structr.neo4j.index.lucene.factory.RangeQueryFactory;

/**
 *
 */
public class LuceneIndexWrapper<S extends org.neo4j.graphdb.PropertyContainer, T extends PropertyContainer> extends AbstractIndexWrapper<S, T> implements QueryFactory<Query> {

	public static final TypeConverter DEFAULT_CONVERTER        = new StringTypeConverter();
	public static final Map<Class, TypeConverter> CONVERTERS   = new HashMap<>();
	public static final Map<Class, QueryFactory> FACTORIES = new HashMap<>();

	static {

		FACTORIES.put(ExactQuery.class,    new KeywordQueryFactory());
		FACTORIES.put(FulltextQuery.class, new FulltextQueryFactory());
		FACTORIES.put(GroupQuery.class,    new GroupQueryFactory());
		FACTORIES.put(RangeQuery.class,    new RangeQueryFactory());

		CONVERTERS.put(Boolean.class, new StringTypeConverter());
		CONVERTERS.put(String.class,  new StringTypeConverter());
		CONVERTERS.put(Date.class,    new DateTypeConverter());
		CONVERTERS.put(Long.class,    new LongTypeConverter());
		CONVERTERS.put(Integer.class, new IntTypeConverter());
		CONVERTERS.put(Double.class,  new DoubleTypeConverter());
	}

	public LuceneIndexWrapper(final Neo4jDatabaseService graphDb, final org.neo4j.graphdb.index.Index<S> index) {
		super(graphDb,index);
	}

	@Override
	public QueryResult<T> query(final QueryPredicate predicate) {

		final Query query = getQuery(this, predicate);
		if (query != null) {

			final QueryContext queryContext = new QueryContext(query);
			final String key                = predicate.getSortKey();
			final SortType sortType         = predicate.getSortType();
			final boolean sortDescending    = predicate.sortDescending();

			// Note: Structr sorts collections "nulls first", because we index
			// the numerical MIN_VALUE of a number as the "empty value".
			// This behaviour can be changed by setting the EMPTY_VALUE
			// field of the  type converters: StringTypeConverter,
			// LongTypeConverter, etc.

			if (sortType != null) {

				queryContext.sort(new Sort(new SortField(key, getSortType(sortType), sortDescending)));

			} else {

				queryContext.sort(new Sort(new SortField(key, Locale.getDefault(), sortDescending)));
			}

			return new IndexHitsWrapper<>(graphDb, index.query(queryContext));
		}

		return null;
	}

	@Override
	public Query getQuery(final QueryFactory parent, final QueryPredicate predicate) {

		final Class type = predicate.getQueryType();
		if (type != null) {

			final QueryFactory<Query> factory = FACTORIES.get(type);
			if (factory != null) {

				return factory.getQuery(this, predicate);
			}
		}

		return null;
	}

	// ----- protected methods -----
	@Override
	protected Object convertForIndexing(final Object value, final Class typeHint) {

		Object indexValue = value;
		if (indexValue != null) {

			final TypeConverter conv = CONVERTERS.get(indexValue.getClass());
			if (conv != null) {

				indexValue = conv.getWriteValue(indexValue);
			}

		} else if (typeHint != null) {

			final TypeConverter conv = CONVERTERS.get(typeHint);
			if (conv != null) {

				indexValue = conv.getWriteValue(null);
			}
		}

		return indexValue != null && indexValue.toString() == null ? null : indexValue;
	}

	@Override
	protected Object convertForQuerying(final Object value, final Class typeHint) {

		Object indexValue = value;
		if (indexValue != null) {

			final TypeConverter conv = CONVERTERS.get(indexValue.getClass());
			if (conv != null) {

				indexValue = conv.getReadValue(indexValue);
			}

		} else if (typeHint != null) {

			final TypeConverter conv = CONVERTERS.get(typeHint);
			if (conv != null) {

				indexValue = conv.getReadValue(null);
			}
		}

		return indexValue;
	}

	// ----- private methods -----
	private int getSortType(final SortType sortType) {

		switch (sortType) {

			case Double:
				return SortField.DOUBLE;

			case Long:
				return SortField.LONG;

			case Integer:
				return SortField.INT;

			default:
			case Default:
				return SortField.STRING;
		}
	}
}
