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
package org.structr.core.traits.operations.accesscontrollable;

import org.structr.common.AccessEntry;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.operations.FrameworkMethod;

import java.util.List;

/**
 * Framework method that returns the access entries of a node - the enriched, permission-bit-carrying
 * counterpart to the lightweight {@code grantees} property on {@code NodeInterface}.
 *
 * <p>Two flavors:</p>
 * <ul>
 *     <li>{@link #getDirectAccessEntries(NodeInterface)} covers tiers 1+2: owner and direct
 *         {@code SECURITY} relationships.</li>
 *     <li>{@link #getEffectiveAccessEntries(NodeInterface)} additionally covers tiers 3+4:
 *         transitive group members and schema grants.</li>
 * </ul>
 *
 * <p>Both flavors merge contributions per principal: a principal that has permissions via
 * multiple paths (e.g. direct edge plus group membership) appears as a single {@link AccessEntry}
 * whose {@code permissions} set is the union of all paths, and whose {@code via} string is a
 * composite describing every contributing path.</p>
 *
 * <p>Permission propagation along domain relationships (tier 5) is intentionally excluded from both,
 * because it is unbounded in the general case. The existing {@code isGranted()} path handles it
 * per-principal and should be used when propagation-aware checks are needed.</p>
 */
public abstract class GetAccessEntries extends FrameworkMethod<GetAccessEntries> {

	public abstract List<AccessEntry> getDirectAccessEntries(final NodeInterface node);
	public abstract List<AccessEntry> getEffectiveAccessEntries(final NodeInterface node);
}
