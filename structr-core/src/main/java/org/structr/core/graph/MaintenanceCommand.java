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
package org.structr.core.graph;

import java.util.LinkedHashMap;
import java.util.Map;
import org.structr.common.error.FrameworkException;

/**
 * Common base interface for commands that can be registered as a maintenance
 * command. Maintenance commands can be called via REST if registered
 * appropriately.
 *
 *
 */
public interface MaintenanceCommand {

	final Map<String, String> customHeaders = new LinkedHashMap();

	public void execute(Map<String, Object> attributes) throws FrameworkException;
	public boolean requiresEnclosingTransaction();
	public boolean requiresFlushingOfCaches();

	default public Map<String, String> getCustomHeaders () {
		return customHeaders;
	}

}
