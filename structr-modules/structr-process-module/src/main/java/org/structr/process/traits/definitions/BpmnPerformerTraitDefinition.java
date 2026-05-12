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
package org.structr.process.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.process.ProcessTraits;

import java.util.Map;
import java.util.Set;

/**
 * BPMN 2.0 Performer (and its specializations HumanPerformer and PotentialOwner).
 *
 * Represents a typed actor association on a BPMN activity, parsed from one of:
 *
 *   <bpmn:performer>         -- generic actor association
 *   <bpmn:humanPerformer>    -- human actor currently responsible (~ assignee)
 *   <bpmn:potentialOwner>    -- principals eligible to claim a userTask
 *
 * Each performer wraps a {@code <bpmn:resourceAssignmentExpression>} containing a
 * {@code <bpmn:formalExpression>}. We persist the expression body and (when it
 * differs from the document default) its language URI, plus the BPMN name/id
 * attributes for round-trip fidelity.
 *
 * Expression evaluation lives in the engine and currently supports a small
 * Structr-specific syntax: {@code ${initiator}}, {@code user(<name>)},
 * {@code group(<name>)}, and comma-separated lists thereof.
 */
public class BpmnPerformerTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String KIND_PROPERTY                 = "kind";
	public static final String EXPRESSION_PROPERTY           = "expression";
	public static final String EXPRESSION_LANGUAGE_PROPERTY  = "expressionLanguage";
	public static final String PERFORMER_NAME_PROPERTY       = "performerName";
	public static final String ELEMENT_PROPERTY              = "element";
	// Typed alternative to `expression`. When non-empty, the engine
	// resolves task assignment from the linked Principal (User / Group)
	// nodes directly and skips expression evaluation. The string stays
	// authoring-visible for back-compat and as the round-trip carrier
	// (Camunda extension attributes on import; expression body in the
	// resourceAssignmentExpression on export).
	public static final String PRINCIPALS_PROPERTY           = "principals";

	// Kind constants -- match the BPMN local element names exactly so
	// import/export are trivial and lookups don't need a translation table.
	public static final String KIND_PERFORMER         = "performer";
	public static final String KIND_HUMAN_PERFORMER   = "humanPerformer";
	public static final String KIND_POTENTIAL_OWNER   = "potentialOwner";

	public BpmnPerformerTraitDefinition() {
		super(ProcessTraits.BPMN_PERFORMER);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> kind                = new EnumProperty(KIND_PROPERTY,
			Set.of(KIND_PERFORMER, KIND_HUMAN_PERFORMER, KIND_POTENTIAL_OWNER)).indexed();
		final Property<String> expression          = new StringProperty(EXPRESSION_PROPERTY);
		final Property<String> expressionLanguage  = new StringProperty(EXPRESSION_LANGUAGE_PROPERTY);
		final Property<String> performerName       = new StringProperty(PERFORMER_NAME_PROPERTY);
		final Property<NodeInterface> element      = new StartNode(traitsInstance, ELEMENT_PROPERTY, ProcessTraits.BPMN_ELEMENT_HAS_PERFORMER);
		final Property<Iterable<NodeInterface>> principals = new EndNodes(traitsInstance, PRINCIPALS_PROPERTY, ProcessTraits.BPMN_PERFORMER_HAS_PRINCIPAL);

		return newSet(kind, expression, expressionLanguage, performerName, element, principals);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(KIND_PROPERTY, EXPRESSION_PROPERTY, EXPRESSION_LANGUAGE_PROPERTY, PRINCIPALS_PROPERTY),
			PropertyView.Ui,     newSet(KIND_PROPERTY, EXPRESSION_PROPERTY, EXPRESSION_LANGUAGE_PROPERTY, PERFORMER_NAME_PROPERTY, BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY, BpmnBaseNodeTraitDefinition.VERSION_PROPERTY, ELEMENT_PROPERTY, PRINCIPALS_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
