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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.util.Iterables;
import org.structr.common.FactoryDefinition;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.schema.SchemaHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public abstract class Factory<S extends PropertyContainer, T extends GraphObject> implements Adapter<S, T>, Function<S, T> {

	private static final Logger logger          = LoggerFactory.getLogger(Factory.class.getName());
	public static final ExecutorService service = Executors.newCachedThreadPool();
	public static final int DEFAULT_PAGE_SIZE   = Integer.MAX_VALUE;
	public static final int DEFAULT_PAGE        = 1;

	protected SecurityContext securityContext = null;
	protected boolean disablePaging           = false;
	protected boolean includeHidden           = true;
	protected boolean publicOnly              = false;
	protected int pageSize                    = DEFAULT_PAGE_SIZE;
	protected int page                        = DEFAULT_PAGE;

	public Factory(final SecurityContext securityContext) {
		this(securityContext, true, false, DEFAULT_PAGE_SIZE, DEFAULT_PAGE);
	}

	public Factory(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly) {
		this(securityContext, includeHidden, publicOnly, DEFAULT_PAGE_SIZE, DEFAULT_PAGE);
	}

	public Factory(final SecurityContext securityContext, final int pageSize, final int page) {

		this(securityContext, true, false, pageSize, page);
	}

	public Factory(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly, final int pageSize, final int page) {

		this.securityContext = securityContext;
		this.includeHidden   = includeHidden;
		this.publicOnly      = publicOnly;
		this.pageSize        = pageSize;
		this.page            = page;
	}

	public abstract T instantiateWithType(final S obj, final String type, final Identity pathSegmentId, boolean isCreation);

	protected abstract String determineActualType(final S obj);

	public T instantiate(final S node) {
		return instantiate(node, null);
	}

	public T instantiate(final S node, final Identity pathSegmentId) {

		if (node == null || TransactionCommand.isDeleted(node) || node.isDeleted()) {
			return null;
		}

		return instantiateWithType(node, determineActualType(node), pathSegmentId, false);
	}

	public T instantiate(final S node, final boolean includeHidden, final boolean publicOnly) throws FrameworkException {

		this.includeHidden = includeHidden;
		this.publicOnly    = publicOnly;

		return instantiate(node);
	}

	/**
	 * Create structr nodes from all given underlying database nodes
	 * No paging, but security check
	 *
	 * @param input
	 * @return nodes
	 * @throws org.structr.common.error.FrameworkException
	 */
	public Iterable<T> bulkInstantiate(final Iterable<S> input) throws FrameworkException {
		return Iterables.map(this, input);
	}

	@Override
	public T adapt(S s) {
		return instantiate(s);
	}

	@Override
	public T apply(final S from) {
		return adapt(from);
	}

	public void disablePaging() {
		this.disablePaging = true;
	}
}
