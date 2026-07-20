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
package org.structr.process.entity;

import org.structr.core.graph.NodeInterface;

/**
 * Typed view of a {@code BpmnPerformer} node (humanPerformer / potentialOwner /
 * performer). Obtain via {@code node.as(BpmnPerformer.class)}.
 */
public interface BpmnPerformer extends NodeInterface {

	String getBpmnId();
	String getKind();
	String getExpression();
	String getExpressionLanguage();
	String getPerformerName();

	/** Directly-linked principals (take priority over the expression). */
	Iterable<NodeInterface> getPrincipals();
}
