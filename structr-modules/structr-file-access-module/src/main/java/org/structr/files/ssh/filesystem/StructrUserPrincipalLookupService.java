/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.files.ssh.filesystem;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;

import java.io.IOException;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.attribute.UserPrincipalNotFoundException;

/**
 * Resolves user and group names against the principals in the graph.
 *
 * The attribute views already hand out principals that carry nothing but a name, so this looks a name up
 * only to establish that it belongs to someone: returning a principal for a user who does not exist would
 * let a caller set an owner that can never be resolved back.
 */
public class StructrUserPrincipalLookupService extends UserPrincipalLookupService {

	private final SecurityContext securityContext;

	public StructrUserPrincipalLookupService(final SecurityContext securityContext) {

		this.securityContext = securityContext;
	}

	@Override
	public UserPrincipal lookupPrincipalByName(final String name) throws IOException {

		final String found = findByName(StructrTraits.PRINCIPAL, name);

		return () -> found;
	}

	@Override
	public GroupPrincipal lookupPrincipalByGroupName(final String group) throws IOException {

		final String found = findByName(StructrTraits.GROUP, group);

		return () -> found;
	}

	// ----- private methods -----
	private String findByName(final String type, final String name) throws IOException {

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final NodeInterface node = StructrApp.getInstance(securityContext).nodeQuery(type).name(name).getFirst();

			tx.success();

			if (node == null) {

				throw new UserPrincipalNotFoundException(name);
			}

			return name;

		} catch (FrameworkException fex) {

			throw new IOException("Unable to look up " + type + " '" + name + "': " + fex.getMessage(), fex);
		}
	}
}
