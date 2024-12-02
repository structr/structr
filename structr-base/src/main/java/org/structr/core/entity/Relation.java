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
package org.structr.core.entity;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Cardinality;
import org.structr.api.graph.Direction;
import org.structr.api.graph.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.notion.Notion;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Defines constants for structr's relationship entities.
 *
 * @param <A>
 * @param <B>
 * @param <S>
 * @param <T>
 *
 *
 */
public interface Relation<A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target> extends RelationshipInterface, RelationshipType {

	/**
	 * No cascading delete / autocreate.
	 */
	public static final int NONE              = 0;

	/**
	 * Target node will be deleted if source node
	 * gets deleted.
	 */
	public static final int SOURCE_TO_TARGET  = 1;

	/**
	 * Source node will be deleted if target node
	 * gets deleted.
	 */
	public static final int TARGET_TO_SOURCE  = 2;

	/**
	 * Both nodes will be deleted whenever one of
	 * the two nodes gets deleted.
	 *
	 */
	public static final int ALWAYS            = 3;

	/**
	 * Source and/or target nodes will be deleted
	 * if they become invalid.
	 */
	public static final int CONSTRAINT_BASED  = 4;

	public static final String[] CASCADING_DESCRIPTIONS = {
		"NONE",
		"SOURCE_TO_TARGET",
		"TARGET_TO_SOURCE",
		"ALWAYS",
		"CONSTRAINT_BASED"
	};

	public enum Multiplicity { One, Many }

	public Class<A> getSourceType();
	public Class<B> getTargetType();

	public Class getOtherType(final Class type);

	public Direction getDirectionForType(final Class<? extends NodeInterface> type);

	public Multiplicity getSourceMultiplicity();
	public Multiplicity getTargetMultiplicity();

	public S getSource();
	public T getTarget();

	public Property<String> getSourceIdProperty();
	public Property<String> getTargetIdProperty();
	public Notion getEndNodeNotion();
	public Notion getStartNodeNotion();

	public int getCascadingDeleteFlag();
	public int getAutocreationFlag();

	public void ensureCardinality(final SecurityContext securityContext, final NodeInterface sourceNode, final NodeInterface targetNode) throws FrameworkException;

	public boolean isHidden();

	public void setSourceProperty(final PropertyKey source);
	public void setTargetProperty(final PropertyKey target);

	public PropertyKey getSourceProperty();
	public PropertyKey getTargetProperty();

	public static final Map<Class, Relation> relationCache = new LinkedHashMap<>();

	public static Relation getInstance(final Class<? extends Relation> type) {

		Relation instance = relationCache.get(type);
		if (instance == null) {

			try {

				instance = type.getDeclaredConstructor().newInstance();
				relationCache.put(type, instance);


			} catch (Throwable t) {
				LoggerFactory.getLogger(Relation.class).error("{}", ExceptionUtils.getStackTrace(t));
			}
		}

		return instance;
	}

	public static Cardinality getCardinality(final Relation relation) {

		final Multiplicity sm = relation.getSourceMultiplicity();
		final Multiplicity tm = relation.getTargetMultiplicity();

		switch (sm) {

			case One:
				switch (tm) {
					case One: return Cardinality.OneToOne;
					case Many: return Cardinality.OneToMany;
				}
			case Many:
				switch (tm) {
					case One: return Cardinality.ManyToOne;
					case Many: return Cardinality.ManyToMany;
				}
		}

		return null;
	}
}
