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
import org.structr.common.PermissionPropagation;
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
 * @param <S>
 * @param <T>
 *
 *
 */
public interface Relation<S extends Source, T extends Target> extends RelationshipType, PermissionPropagation {

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

	default String getType() {
		return getSourceType() + name() + getTargetType();
	}

	enum Multiplicity { One, Many }

	String getSourceType();
	String getTargetType();

	String getOtherType(final String type);
	Direction getDirectionForType(final String type);

	Multiplicity getSourceMultiplicity();
	Multiplicity getTargetMultiplicity();

	S getSource();
	T getTarget();

	Notion getEndNodeNotion();
	Notion getStartNodeNotion();

	int getCascadingDeleteFlag();
	int getAutocreationFlag();

	void ensureCardinality(final SecurityContext securityContext, final NodeInterface sourceNode, final NodeInterface targetNode) throws FrameworkException;

	Property<String> getSourceIdProperty();
	Property<String> getTargetIdProperty();

	void setSourceProperty(final PropertyKey source);
	void setTargetProperty(final PropertyKey target);

	PropertyKey getSourceProperty();
	PropertyKey getTargetProperty();

	boolean isInternal();
}
