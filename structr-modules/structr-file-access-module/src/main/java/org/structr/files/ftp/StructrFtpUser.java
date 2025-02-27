/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.files.ftp;

import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 */
public class StructrFtpUser implements User {

	private static final Logger logger = LoggerFactory.getLogger(StructrFtpUser.class.getName());

	private final Principal structrUser;
	private SecurityContext securityContext;

	public StructrFtpUser(final SecurityContext securityContext, final Principal structrUser) {
		this.securityContext = securityContext;
		this.structrUser     = structrUser;
	}

	@Override
	public String getName() {
		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {
			final String name = structrUser.getName();
			tx.success();
			return name;
		} catch (Exception fex) { }

		return null;
	}

	@Override
	public String getPassword() {
		throw new UnsupportedOperationException("We don't disclose user passwords ever.");
	}

	@Override
	public List<Authority> getAuthorities() {
		List<Authority> auths = new ArrayList<>();

		auths.add(new ConcurrentLoginPermission(10, 10));
		auths.add(new WritePermission());

		return auths;
	}

	@Override
	public List<Authority> getAuthorities(Class<? extends Authority> type) {
		return getAuthorities();
	}

	@Override
	public AuthorizationRequest authorize(AuthorizationRequest request) {

		List<Authority> authorities = getAuthorities();

		// check for no authorities at all
		if (authorities == null) {
			return null;
		}

		boolean someoneCouldAuthorize = false;
		for (Authority authority : authorities) {
			if (authority.canAuthorize(request)) {

				logger.info("Authority {} can authorize {}", new Object[]{authority, request});

				someoneCouldAuthorize = true;

				request = authority.authorize(request);

				// authorization failed, return null
				if (request == null) {

					logger.info("Authorization of request {} failed", new Object[]{request});
					return null;
				}
			}

		}


		if (someoneCouldAuthorize) {
			logger.info("Request {} successfully authorized", new Object[]{request});
			return request;
		} else {
			return null;
		}
	}

	@Override
	public int getMaxIdleTime() {
		return 3000;
	}

	@Override
	public boolean getEnabled() {
		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {
			final boolean blocked = structrUser.isBlocked();
			tx.success();
			return !blocked;
		} catch (Exception fex) { }

		return false;
	}

	@Override
	public String getHomeDirectory() {

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final String homeDir = structrUser.as(org.structr.web.entity.User.class).getHomeDirectory().getName();

			tx.success();

			return homeDir;

		} catch (Exception fex) { }

		return null;
	}

	public Principal getStructrUser() {
		return structrUser;
	}

}
