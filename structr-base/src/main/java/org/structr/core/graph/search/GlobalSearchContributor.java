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
package org.structr.core.graph.search;

import org.structr.common.SecurityContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds results to the global search of the admin UI (websocket command SEARCH_NODES), next to the
 * property matches the database service finds. Modules register contributors with
 * {@link GlobalSearchContributors#register(GlobalSearchContributor)}.
 *
 * Each result is a map in the shape of the database results: id, type, name, keys (the matched keys),
 * values (per key a map with before, match and after) and labels (used to decide where a click on the
 * result leads, e.g. "File" for the Files area).
 */
public interface GlobalSearchContributor {

	/**
	 * @param securityContext  the user who searches
	 * @param searchContexts   the selected contexts (DatabaseService.GLOBAL_SEARCH_CONTEXT_*)
	 * @param searchString     what the user typed
	 * @param searchIgnoreCase whether the user asked for case-insensitive matching
	 */
	List<Map<String, Object>> search(SecurityContext securityContext, Set<String> searchContexts, String searchString, boolean searchIgnoreCase);
}
