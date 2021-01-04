/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.files.cmis;

import java.io.File;
import java.util.Map;
import org.apache.chemistry.opencmis.commons.exceptions.CmisUnauthorizedException;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import org.apache.chemistry.opencmis.commons.server.TempStoreOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.Tx;
import org.structr.rest.auth.AuthHelper;

/**
 *
 *
 */
public class StructrCMISServicesFactory implements CmisServiceFactory {

	private static final Logger logger = LoggerFactory.getLogger(CmisServiceFactory.class.getName());

	@Override
	public void init(final Map<String, String> config) {

		logger.info("Initialization map: {}", config);
	}

	@Override
	public void destroy() {
	}

	@Override
	public CmisService getService(final CallContext cc) {
		return new StructrCMISService(checkAuthentication(cc));
	}

	@Override
	public File getTempDirectory() {
		return new File("/tmp");
	}

	@Override
	public boolean encryptTempFiles() {
		return false;
	}

	@Override
	public int getMemoryThreshold() {

		// 20 MB
		return 20*1024*1024;
	}

	@Override
	public long getMaxContentSize() {

		// 1 GB
		return 1024*1024*1024;
	}

	@Override
	public TempStoreOutputStream getTempFileOutputStream(final String string) {
		return null;
	}

	// ----- private methods -----
	private SecurityContext checkAuthentication(final CallContext callContext) {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final String username           = callContext.getUsername();
			final String password           = callContext.getPassword();
			final Principal principal       = AuthHelper.getPrincipalForPassword(Principal.name, username, password);
			SecurityContext securityContext = null;

			if (principal != null) {


				if (principal instanceof SuperUser) {

					securityContext = SecurityContext.getSuperUserInstance();

				} else {

					securityContext = SecurityContext.getInstance(principal, AccessMode.Backend);
				}
			}

			tx.success();

			if (securityContext != null) {
				return securityContext;
			}

		} catch (AuthenticationException aex) {

			throw new CmisUnauthorizedException(aex.getMessage());

		} catch (FrameworkException fex) {

			logger.warn("", fex);
		}

		throw new CmisUnauthorizedException();
	}
}
