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
package org.structr.core.entity;

import org.structr.api.graph.Direction;
import org.structr.api.graph.RelationshipType;
import org.structr.common.PermissionPropagation;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.Notion;
import org.structr.core.property.PropertyKey;
import org.structr.docs.Documentation;
import org.structr.docs.ontology.ConceptType;

/**
 * Defines constants for structr's relationship entities.
 *
 * @param <S>
 * @param <T>
 *
 */
@Documentation(name="Cascading Delete", shortDescription = "When a node is deleted, related nodes can be deleted automatically based on the cascading delete setting.", parent="Relationships")
@Documentation(name="Automatic Creation of Related Nodes", shortDescription = "When a node is created, related nodes can be created or linked automatically based on unique properties.", parent="Relationships")
public interface Relation<S extends Source, T extends Target> extends RelationshipType, PermissionPropagation {

	/**
	 * No cascading delete / autocreate.
	 */
	@Documentation(type=ConceptType.Constant, shortDescription = "No cascading delete", parent="Cascading Delete")
	@Documentation(type=ConceptType.Constant, shortDescription = "No automatic creation of related nodes", parent="Automatic Creation of Related Nodes")
	int NONE              = 0;

	/**
	 * Target node will be deleted if source node
	 * gets deleted.
	 */
	@Documentation(type=ConceptType.Constant, shortDescription="If source is deleted, target will be deleted automatically.", parent="Cascading Delete")
	@Documentation(type=ConceptType.Constant, shortDescription="Target node will be created automatically if the input value is unique.", parent="Automatic Creation of Related Nodes")
	int SOURCE_TO_TARGET  = 1;

	/**
	 * Source node will be deleted if target node
	 * gets deleted.
	 */
	@Documentation(type=ConceptType.Constant, shortDescription="If target is deleted, source will be deleted automatically.", parent="Cascading Delete")
	@Documentation(type=ConceptType.Constant, shortDescription="Source node will be created automatically if the input value is unique.", parent="Automatic Creation of Related Nodes")
	int TARGET_TO_SOURCE  = 2;

	/**
	 * Both nodes will be deleted whenever one of
	 * the two nodes gets deleted.
	 *
	 */
	@Documentation(type=ConceptType.Constant, shortDescription="If any of the two nodes is deleted, the other will be deleted automatically.", parent="Cascading Delete")
	@Documentation(type=ConceptType.Constant, shortDescription="Any of the two nodes will be created automatically if the input value is unique.", parent="Automatic Creation of Related Nodes")
	int ALWAYS            = 3;

	/**
	 * Source and/or target nodes will be deleted
	 * if they become invalid.
	 */
	@Documentation(type=ConceptType.Constant, shortDescription="If any of the two nodes is deleted, the other will be deleted automatically if the missing node would cause a validation error.", parent="Cascading Delete")
	int CONSTRAINT_BASED  = 4;

	String[] CASCADING_DESCRIPTIONS = {
		"NONE",
		"SOURCE_TO_TARGET",
		"TARGET_TO_SOURCE",
		"ALWAYS",
		"CONSTRAINT_BASED"
	};

	enum Multiplicity { One, Many }

	String getSourceType();
	String getTargetType();
	String getType();

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

	PropertyKey<String> getSourceIdProperty();
	PropertyKey<String> getTargetIdProperty();

	void setSourceProperty(final PropertyKey source);
	void setTargetProperty(final PropertyKey target);

	PropertyKey getSourceProperty();
	PropertyKey getTargetProperty();

	boolean isInternal();
}
