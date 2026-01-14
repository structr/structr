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
package org.structr.core.datasources;

import java.util.*;

/**
 *
 *
 */
public class DataSources {

	private static final Map<String, GraphDataSource> dataSources = new LinkedHashMap<>();

	public static void put(final String module, final String name, final GraphDataSource dataSource) {
		dataSources.put(name, dataSource);
	}

	public static Set<String> getNames() {
		return new LinkedHashSet<>(dataSources.keySet());
	}

	public static GraphDataSource get(final String name) {
		return dataSources.get(name);
	}

	public static Collection<GraphDataSource> getDataSources() {
		return dataSources.values();
	}
}
