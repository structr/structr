/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.core.function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.LicenseManager;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedException;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 * Dummy function that replaces unlicensed functions to prevent
 * execution errors.
 */
public class UnlicensedFunction extends Function<Object, Object> {

	private static final Logger logger = LoggerFactory.getLogger(UnlicensedFunction.class);

	private String name = null;
	private int edition = LicenseManager.Community;

	public UnlicensedFunction(final String name, final int edition) {
		this.name    = name;
		this.edition = edition;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {
		throw new UnlicensedException(name, edition);
	}

	@Override
	public String usage(final boolean inJavaScriptContext) {
		return new UnlicensedException(name, edition).buildLogMessage();
	}

	@Override
	public String shortDescription() {
		return "Placeholder for unlicensed functions";
	}

	@Override
	public String getName() {
		return "unlicensed";
	}
}
