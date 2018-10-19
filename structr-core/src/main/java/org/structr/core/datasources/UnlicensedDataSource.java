/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.core.datasources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.LicenseManager;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.action.ActionContext;

/**
 * Dummy data source that replaces unlicensed data sources to prevent
 * execution errors.
 */
public class UnlicensedDataSource implements GraphDataSource {

	private static final Logger logger = LoggerFactory.getLogger(UnlicensedDataSource.class);

	private String name = null;
	private int edition = LicenseManager.Community;

	public UnlicensedDataSource(final String name, final int edition) {
		this.name    = name;
		this.edition = edition;
	}

	@Override
	public Object getData(ActionContext actionContext, NodeInterface referenceNode) throws FrameworkException {
		return null;
	}

}
