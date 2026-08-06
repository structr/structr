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
package org.structr.common;

import java.util.Set;

/**
 * Read-only projection of a principal's effective access on a node.
 *
 * <p>An {@code AccessEntry} is computed from existing graph state (owner edge,
 * SECURITY relationships, group membership, schema grants) - it is NOT a persisted
 * entity. One entry represents the union of permissions a single principal has on
 * a given node, together with a composite provenance string describing through
 * which paths those permissions were obtained.</p>
 *
 * <p>Produced by {@link org.structr.core.traits.operations.accesscontrollable.GetAccessEntries}
 * and surfaced on {@link AccessControllable} as {@code getDirectAccessEntries()} and
 * {@code getEffectiveAccessEntries()}.</p>
 *
 * <p>The {@code via} field encodes provenance as a {@code '+'}-separated list of path
 * tokens in a stable order: {@code owner} < {@code direct} < {@code schema} <
 * {@code group:<uuid>:<name>} (groups sorted by name). Examples:</p>
 * <ul>
 *     <li>{@code "owner"} - principal is the owner</li>
 *     <li>{@code "direct"} - single direct SECURITY edge</li>
 *     <li>{@code "direct+group:abc123:Editors"} - direct edge plus membership in Editors</li>
 *     <li>{@code "schema+group:abc123:Admins+group:def456:Reviewers"} - schema grant plus two group paths</li>
 * </ul>
 *
 * @param grantee      principal UUID
 * @param granteeName  principal name (convenience, may go stale)
 * @param granteeType  principal type, typically "User" or "Group"
 * @param permissions  union of permissions from all contributing paths
 * @param via          composite provenance string, see class javadoc for format
 */
public record AccessEntry(String grantee, String granteeName, String granteeType, Set<Permission> permissions, String via) {
}
