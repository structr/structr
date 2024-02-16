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
package org.structr.console.tabcompletion;

import org.structr.common.SecurityContext;
import org.structr.console.rest.RestCommand;
import org.structr.core.app.StructrApp;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class RestTabCompletionProvider extends AbstractTabCompletionProvider {

	@Override
	public List<TabCompletionResult> getTabCompletion(final SecurityContext securityContext, final String line) {

		final List<TabCompletionResult> results = new ArrayList<>();
		final String token                      = getToken(line, " /.");

		results.addAll(getExactResultsForCollection(RestCommand.commandNames(), token, " "));

		Collections.sort(results);

		return results;
	}

	private Set<String> getNodeTypes() {
		return StructrApp.getConfiguration().getNodeEntities().keySet();
	}
}
