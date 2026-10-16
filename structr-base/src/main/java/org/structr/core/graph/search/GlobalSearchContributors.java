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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The registered {@link GlobalSearchContributor}s.
 */
public final class GlobalSearchContributors {

	private static final CopyOnWriteArrayList<GlobalSearchContributor> contributors = new CopyOnWriteArrayList<>();

	private GlobalSearchContributors() {
	}

	public static void register(final GlobalSearchContributor contributor) {

		contributors.addIfAbsent(contributor);
	}

	public static void unregister(final GlobalSearchContributor contributor) {

		contributors.remove(contributor);
	}

	public static List<GlobalSearchContributor> getContributors() {

		return List.copyOf(contributors);
	}
}
