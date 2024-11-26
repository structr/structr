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
import org.structr.core.traits.NodeTrait;
import org.structr.core.traits.RelationshipTrait;
import org.structr.core.traits.Trait;
import org.structr.core.traits.Traits;

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
public interface Relation<A extends NodeTrait, B extends NodeTrait, S extends Source, T extends Target> extends RelationshipTrait<A, B>, RelationshipType {

	/**
	 * No cascading delete / autocreate.
	 */
	final int NONE              = 0;

	/**
	 * Target node will be deleted if source node
	 * gets deleted.
	 */
	static final int SOURCE_TO_TARGET  = 1;

	/**
	 * Source node will be deleted if target node
	 * gets deleted.
	 */
	static final int TARGET_TO_SOURCE  = 2;

	/**
	 * Both nodes will be deleted whenever one of
	 * the two nodes gets deleted.
	 *
	 */
	static final int ALWAYS            = 3;

	/**
	 * Source and/or target nodes will be deleted
	 * if they become invalid.
	 */
	static final int CONSTRAINT_BASED  = 4;

	static final String[] CASCADING_DESCRIPTIONS = {
		"NONE",
		"SOURCE_TO_TARGET",
		"TARGET_TO_SOURCE",
		"ALWAYS",
		"CONSTRAINT_BASED"
	};

	enum Multiplicity { One, Many }

	Trait<A> getSourceType();
	Trait<B> getTargetType();

	Trait<Relation<A, B, S, T>> getTrait();

	Trait<?> getOtherType(final Trait<?> type);

	Direction getDirectionForType(final Trait<?> type);

	Multiplicity getSourceMultiplicity();
	Multiplicity getTargetMultiplicity();

	S getSource();
	T getTarget();

	Property<String> getSourceIdProperty();
	Property<String> getTargetIdProperty();
	Notion getEndNodeNotion();
	Notion getStartNodeNotion();

	int getCascadingDeleteFlag();
	int getAutocreationFlag();

	void ensureCardinality(final SecurityContext securityContext, final NodeTrait sourceNode, final NodeTrait targetNode) throws FrameworkException;

	boolean isHidden();

	void setSourceProperty(final PropertyKey source);
	void setTargetProperty(final PropertyKey target);

	PropertyKey getSourceProperty();
	PropertyKey getTargetProperty();

	static final Map<Class, Relation> relationCache = new LinkedHashMap<>();

	static Relation getInstance(final Class<? extends Relation> type) {

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

	static Cardinality getCardinality(final Relation relation) {

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
