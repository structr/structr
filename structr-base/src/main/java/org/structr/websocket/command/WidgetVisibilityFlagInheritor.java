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
package org.structr.websocket.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.web.entity.dom.DOMNode;

/**
 * Copies the parent's {@code visibleToPublicUsers} and
 * {@code visibleToAuthenticatedUsers} flags onto a freshly-appended widget
 * root and every DOMNode descendant.
 *
 * <p>Rationale: a widget pasted into a page should inherit the page's
 * audience, the same way a single manually-created DOMNode does. Without
 * this step, widgets ship with whatever flags the deployment importer
 * defaulted to (often public-only), so freshly inserted widgets vanish for
 * the very users the surrounding page targets.</p>
 *
 * <p>Best-effort: any failure is logged at warn level; the widget install
 * itself does not get unwound.</p>
 */
public class WidgetVisibilityFlagInheritor {

	private static final Logger logger = LoggerFactory.getLogger(WidgetVisibilityFlagInheritor.class);

	/**
	 * Mirror the parent's two visibility flags onto rootNode and all its
	 * DOMNode descendants. No-op when parentNode or rootNode is null.
	 */
	public static void apply(final DOMNode rootNode, final DOMNode parentNode) {

		if (rootNode == null || parentNode == null) return;

		try {
			final PropertyKey<Boolean> publicKey = rootNode.getTraits().key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY);
			final PropertyKey<Boolean> authKey   = rootNode.getTraits().key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY);

			final Boolean parentPublic = parentNode.getProperty(publicKey);
			final Boolean parentAuth   = parentNode.getProperty(authKey);

			applyFlags(rootNode, publicKey, parentPublic, authKey, parentAuth);
			for (final NodeInterface descendant : rootNode.getAllChildNodes()) {
				if (descendant.is(StructrTraits.DOM_NODE)) {
					applyFlags(descendant.as(DOMNode.class), publicKey, parentPublic, authKey, parentAuth);
				}
			}
		} catch (FrameworkException fex) {
			logger.warn("WidgetVisibilityFlagInheritor: failed to inherit visibility flags onto widget root '{}': {}",
				rootNode.getUuid(), fex.getMessage());
		}
	}

	private static void applyFlags(final DOMNode node,
			final PropertyKey<Boolean> publicKey, final Boolean publicVal,
			final PropertyKey<Boolean> authKey,   final Boolean authVal) throws FrameworkException {

		node.setProperty(publicKey, Boolean.TRUE.equals(publicVal));
		node.setProperty(authKey,   Boolean.TRUE.equals(authVal));
	}
}
