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
package org.structr.api;

import java.util.Set;

public interface Prefetcher {

	void prefetch(final String type1, final String type2, final Set<String> keys);
	void prefetch(final String query, final Set<String> keys);
	void prefetch(final String query, final Set<String> outgoingKeys, final Set<String> incomingKeys);
	void prefetch2(final String query, final Set<String> outgoingKeys, final Set<String> incomingKeys, final String id);

	void prefetchHint(final String hint);
}
