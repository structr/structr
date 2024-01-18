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
package org.structr.core.graph;


import java.lang.reflect.InvocationTargetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.common.AccessControllable;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

/**
 * A factory for Structr nodes.
 *
 * @param <T>
 */
public class NodeFactory<T extends NodeInterface & AccessControllable> extends Factory<Node, T> {

	private static final Logger logger = LoggerFactory.getLogger(NodeFactory.class.getName());

	public NodeFactory(final SecurityContext securityContext) {
		super(securityContext);
	}

	public NodeFactory(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly) {
		super(securityContext, includeHidden, publicOnly);
	}

	public NodeFactory(final SecurityContext securityContext, final int pageSize, final int page) {
		super(securityContext, pageSize, page);
	}

	public NodeFactory(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly, final int pageSize, final int page) {
		super(securityContext, includeHidden, publicOnly, pageSize, page);
	}

	@Override
	public T instantiate(final Node node) {
		return instantiate(node, null);
	}

	@Override
	public T instantiate(final Node node, final Identity pathSegmentId) {

		if (node == null) {
			return null;
		}

		if (TransactionCommand.isDeleted(node)) {
			return (T)instantiateWithType(node, null, pathSegmentId, false);
		}

		return (T) instantiateWithType(node, factoryDefinition.determineNodeType(node), pathSegmentId, false);
	}

	@Override
	public T instantiateWithType(final Node node, final Class<T> nodeClass, final Identity pathSegmentId, boolean isCreation) {

		// cannot instantiate node without type
		if (nodeClass == null) {
			return null;
		}

		SecurityContext securityContext = factoryProfile.getSecurityContext();
		T newNode                       = null;

		try {
			newNode = nodeClass.getDeclaredConstructor().newInstance();

		} catch (NoSuchMethodException|NoClassDefFoundError|InvocationTargetException|InstantiationException|IllegalAccessException itex) {
			newNode = null;
		}

		if (newNode == null) {
			newNode = (T)factoryDefinition.createGenericNode();
		}

		newNode.init(factoryProfile.getSecurityContext(), node, nodeClass, TransactionCommand.getCurrentTransactionId());
		newNode.setRawPathSegmentId(pathSegmentId);
		newNode.onNodeInstantiation(isCreation);

		// check access
		if (isCreation || securityContext.isReadable(newNode, factoryProfile.includeHidden(), factoryProfile.publicOnly())) {

			return newNode;
		}

		return null;
	}

	@Override
	public T instantiate(final Node node, final boolean includeHidden, final boolean publicOnly) throws FrameworkException {

		factoryProfile.setIncludeHidden(includeHidden);
		factoryProfile.setPublicOnly(publicOnly);

		return instantiate(node);
	}
}
