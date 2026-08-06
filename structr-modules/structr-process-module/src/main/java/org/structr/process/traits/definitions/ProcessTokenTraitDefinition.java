/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
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
import org.structr.core.traits.NodeTraitFactory;
import org.structr.process.entity.ProcessToken;
import org.structr.process.traits.wrappers.ProcessTokenTraitWrapper;
import java.util.Set;

/**
 * A token represents a point of execution within a process instance.
 * Tokens sit on BpmnElement nodes and move forward as the process advances.
 *
 * At parallel gateways, tokens are forked (one becomes many) and joined
 * (many become one). The set of active tokens defines where the process
 * currently is.
 *
 * Status lifecycle: active -> completed | waiting
 */
public class ProcessTokenTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String STATUS_PROPERTY                = "status";
	public static final String PROCESS_INSTANCE_PROPERTY      = "processInstance";
	public static final String AT_ELEMENT_PROPERTY            = "atElement";
	public static final String ACCESS_TOKEN_PROPERTY          = "accessToken";
	public static final String ACCESS_TOKEN_PRINCIPAL_PROPERTY = "accessTokenPrincipal";
	// bpmnId of the element this token last moved FROM; lets a parallel join
	// verify one token per distinct incoming edge (not just a total count).
	public static final String ARRIVED_FROM_BPMN_ID_PROPERTY  = "arrivedFromBpmnId";

	// Status constants
	public static final String STATUS_ACTIVE    = "active";
	public static final String STATUS_COMPLETED = "completed";
	public static final String STATUS_WAITING   = "waiting";

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(ProcessToken.class, (traits, node) -> new ProcessTokenTraitWrapper(traits, node));
	}

	public ProcessTokenTraitDefinition() {

		super(ProcessTraits.PROCESS_TOKEN);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> status                       = new StringProperty(STATUS_PROPERTY).indexed();
		final Property<NodeInterface> processInst           = new StartNode(traitsInstance, PROCESS_INSTANCE_PROPERTY, ProcessTraits.PROCESS_INSTANCE_HAS_TOKEN);
		final Property<NodeInterface> atElement             = new EndNode(traitsInstance, AT_ELEMENT_PROPERTY, ProcessTraits.PROCESS_TOKEN_AT_ELEMENT);
		final Property<String> accessToken                  = new StringProperty(ACCESS_TOKEN_PROPERTY).unique().indexed();
		final Property<NodeInterface> accessTokenPrincipal  = new EndNode(traitsInstance, ACCESS_TOKEN_PRINCIPAL_PROPERTY, ProcessTraits.PROCESS_TOKEN_ACCESS_TOKEN_PRINCIPAL);
		final Property<String> arrivedFromBpmnId            = new StringProperty(ARRIVED_FROM_BPMN_ID_PROPERTY);

		return newSet(status, processInst, atElement, accessToken, accessTokenPrincipal, arrivedFromBpmnId);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(STATUS_PROPERTY, AT_ELEMENT_PROPERTY, ACCESS_TOKEN_PROPERTY, ACCESS_TOKEN_PRINCIPAL_PROPERTY),
			PropertyView.Ui, newSet(STATUS_PROPERTY, PROCESS_INSTANCE_PROPERTY, AT_ELEMENT_PROPERTY, ACCESS_TOKEN_PROPERTY, ACCESS_TOKEN_PRINCIPAL_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {

		return null;
	}
}
