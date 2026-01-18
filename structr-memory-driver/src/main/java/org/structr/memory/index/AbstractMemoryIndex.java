/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.memory.index;

import org.structr.api.DatabaseService;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.index.AbstractIndex;
import org.structr.api.index.QueryFactory;
import org.structr.api.search.*;
import org.structr.memory.MemoryDatabaseService;
import org.structr.memory.index.converter.*;
import org.structr.memory.index.factory.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public abstract class AbstractMemoryIndex<T extends PropertyContainer> extends AbstractIndex<MemoryQuery, T> {

	private final Map<Class, TypeConverter> converters = new HashMap<>();
	private final Map<Class, QueryFactory> factories   = new HashMap<>();
	protected MemoryDatabaseService db                 = null;

	public AbstractMemoryIndex(final MemoryDatabaseService db) {

		super();

		this.db = db;

		init();
	}

	@Override
	public Map<T, Double> fulltextQuery(final String indexName, final String searchString) {
		return null;
	}

	@Override
	public MemoryQuery createQuery(final QueryContext context, final int pageSize, final int page) {
		return new MemoryQuery(context);
	}

	@Override
	public QueryFactory getFactoryForType(final Class type) {
		return factories.get(type);
	}

	@Override
	public TypeConverter getConverterForType(final Class type) {
		return converters.get(type);
	}

	@Override
	public DatabaseService getDatabaseService() {
		return db;
	}

	@Override
	public boolean supports(final Class type) {
		return converters.containsKey(type);
	}

	// ----- private methods -----
	private void init() {

		factories.put(NotEmptyQuery.class,     new NotEmptyQueryFactory(this));
		factories.put(SpatialQuery.class,      new SpatialQueryFactory(this));
		factories.put(GraphQuery.class,        new GraphQueryFactory(this));
		factories.put(GroupQuery.class,        new GroupQueryFactory(this));
		factories.put(RangeQuery.class,        new RangeQueryFactory(this));
		factories.put(ExactQuery.class,        new KeywordQueryFactory(this));
		factories.put(ArrayQuery.class,        new ArrayQueryFactory(this));
		factories.put(EmptyQuery.class,        new EmptyQueryFactory(this));
		factories.put(TypeQuery.class,         new TypeQueryFactory(this));
		factories.put(UuidQuery.class,         new UuidQueryFactory(this));
		factories.put(RelationshipQuery.class, new RelationshipQueryFactory(this));
		factories.put(ComparisonQuery.class,   new ComparisonQueryFactory(this));

		converters.put(Boolean.class, new BooleanTypeConverter());
		converters.put(String.class,  new StringTypeConverter());
		converters.put(Date.class,    new DateTypeConverter());
		converters.put(Long.class,    new LongTypeConverter());
		converters.put(Short.class,   new ShortTypeConverter());
		converters.put(Integer.class, new IntTypeConverter());
		converters.put(Float.class,   new FloatTypeConverter());
		converters.put(Double.class,  new DoubleTypeConverter());
		converters.put(byte.class,    new ByteTypeConverter());
	}
}
