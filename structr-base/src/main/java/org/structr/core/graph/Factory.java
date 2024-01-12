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

public abstract class Factory<S, T extends GraphObject> implements Adapter<S, T>, Function<S, T> {

	private static final Logger logger          = LoggerFactory.getLogger(Factory.class.getName());
	public static final ExecutorService service = Executors.newCachedThreadPool();
	public static final int DEFAULT_PAGE_SIZE   = Integer.MAX_VALUE;
	public static final int DEFAULT_PAGE        = 1;

	// encapsulates all criteria for node creation
	protected FactoryDefinition factoryDefinition = StructrApp.getConfiguration().getFactoryDefinition();
	protected FactoryProfile factoryProfile       = null;
	protected boolean disablePaging               = false;

	public Factory(final SecurityContext securityContext) {

		factoryProfile = new FactoryProfile(securityContext);
	}

	public Factory(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly) {

		factoryProfile = new FactoryProfile(securityContext, includeHidden, publicOnly);
	}

	public Factory(final SecurityContext securityContext, final int pageSize, final int page) {

		factoryProfile = new FactoryProfile(securityContext);

		factoryProfile.setPageSize(pageSize);
		factoryProfile.setPage(page);
	}

	public Factory(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly, final int pageSize, final int page) {
		factoryProfile = new FactoryProfile(securityContext, includeHidden, publicOnly, pageSize, page);
	}

	public abstract T instantiate(final S obj);
	public abstract T instantiate(final S obj, final Identity pathSegmentId);
	public abstract T instantiateWithType(final S obj, final Class<T> type, final Identity pathSegmentId, boolean isCreation) throws FrameworkException;
	public abstract T instantiate(final S obj, final boolean includeHidden, final boolean publicOnly) throws FrameworkException;

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

	protected Class<T> getClassForName(final String rawType) {
		return SchemaHelper.getEntityClassForRawType(rawType);
	}

	// ----- nested classes -----
	protected class FactoryProfile {

		private boolean includeHidden           = true;
		private boolean publicOnly              = false;
		private int pageSize                    = DEFAULT_PAGE_SIZE;
		private int page                        = DEFAULT_PAGE;
		private SecurityContext securityContext = null;

		public FactoryProfile(final SecurityContext securityContext) {

			this.securityContext = securityContext;
		}

		public FactoryProfile(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly) {

			this.securityContext         = securityContext;
			this.includeHidden           = includeHidden;
			this.publicOnly              = publicOnly;
		}

		public FactoryProfile(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly, final int pageSize, final int page) {

			this.securityContext         = securityContext;
			this.includeHidden           = includeHidden;
			this.publicOnly              = publicOnly;
			this.pageSize                = pageSize;
			this.page                    = page;
		}

		/**
		 * @return the includeHidden
		 */
		public boolean includeHidden() {

			return includeHidden;
		}

		/**
		 * @return the publicOnly
		 */
		public boolean publicOnly() {
			return publicOnly;
		}

		/**
		 * @return the pageSize
		 */
		public int getPageSize() {
			return pageSize;
		}

		/**
		 * @return the page
		 */
		public int getPage() {
			return page;
		}

		/**
		 * @return the securityContext
		 */
		public SecurityContext getSecurityContext() {
			return securityContext;
		}

		/**
		 * @param includeHidden the includeHidden to set
		 */
		public void setIncludeHidden(boolean includeHidden) {
			this.includeHidden = includeHidden;
		}

		/**
		 * @param publicOnly the publicOnly to set
		 */
		public void setPublicOnly(boolean publicOnly) {
			this.publicOnly = publicOnly;
		}

		/**
		 * @param pageSize the pageSize to set
		 */
		public void setPageSize(int pageSize) {
			this.pageSize = pageSize;
		}

		/**
		 * @param page the page to set
		 */
		public void setPage(int page) {
			this.page = page;
		}

		/**
		 * @param securityContext the securityContext to set
		 */
		public void setSecurityContext(SecurityContext securityContext) {
			this.securityContext = securityContext;
		}
	}
}
