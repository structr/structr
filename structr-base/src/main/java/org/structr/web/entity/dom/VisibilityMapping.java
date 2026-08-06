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
package org.structr.web.entity.dom;

import org.structr.common.SecurityContext;
import org.structr.core.graph.NodeInterface;

/**
 * Render-time visibility predicate attached to a DOMNode. The predicate is opaque
 * to the structr-base rendering layer: it is invoked through the
 * {@link #evaluate(SecurityContext, NodeInterface)} method, which a concrete trait
 * (e.g. the process-engine module) supplies. structr-base only needs the basic
 * accessors and the boolean evaluation result.
 *
 * <p>The DOMNode render gate consults all VisibilityMappings on a node and
 * renders the node when any mapping evaluates to true (OR semantics). When no
 * mappings are attached, the gate stays open: the feature is opt-in.</p>
 */
public interface VisibilityMapping extends NodeInterface {

	String DOM_NODE_PROPERTY     = "domNode";
	String VISIBLE_WHEN_PROPERTY = "visibleWhen";

	/**
	 * Run the predicate in the given security context, scoped to a specific
	 * context object (typically a ProcessInstance derived from the page's
	 * render data, or a TaskInstance whose parent instance is used). Returns
	 * whether the mapping currently matches.
	 *
	 * <p>The caller passes the security context explicitly so the predicate
	 * sees the actual request user, independent of which context the
	 * VisibilityMapping node happens to have been loaded with. The context
	 * object scopes the evaluation: process-engine states are evaluated
	 * against the user's relationship with that specific instance, not "any
	 * instance ever." When null, only "no-instance"-style states should
	 * match.</p>
	 */
	boolean evaluate(SecurityContext securityContext, NodeInterface contextObject);

	/**
	 * The object the host partial acts on, or null when there is none. Like {@link #evaluate},
	 * the meaning is supplied by the concrete trait: the process engine returns the TaskInstance
	 * the partial's step is about, so a template can read its status and use its id as an action
	 * target without re-deriving it.
	 *
	 * <p>Two cases, and the distinction matters: when the context object already IS the feature's
	 * action target (a task-list row), it is returned as-is, so the row talks about itself. Only
	 * when the context is the enclosing scope (a process instance) is the target derived from it
	 * and this mapping's bound step.</p>
	 */
	NodeInterface resolveTask(SecurityContext securityContext, NodeInterface contextObject);
}
