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
import org.structr.common.AccessPathCache;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.LocalizeFunction;
import org.structr.core.property.FunctionProperty;
import org.structr.core.traits.wrappers.ResourceAccessTraitWrapper;
import org.structr.schema.action.Actions;

import java.util.Map;

public class FlushCachesCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(FlushCachesCommand.class.getName());

	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {

		logger.info("Clearing all caches.");

		flushAll();

		logger.info("Done");
	}

	public static void flushAll() {

		ResourceAccessTraitWrapper.clearCache();
		Actions.clearCache();
		FunctionProperty.clearCache();
		AccessPathCache.invalidate();
		LocalizeFunction.invalidateCache();
		//AbstractSchemaNode.clearCachedSchemaMethods();
		TransactionCommand.flushCaches();

		StructrApp.getInstance().invalidateCache();
	}

	public static void flushLocalizationCache() {

		logger.info("Clearing localization cache.");

		LocalizeFunction.invalidateCache();

		logger.info("Done");
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return true;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}
}
