/**
 * Copyright (C) 2010-2015 Structr GmbH
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
import java.math.BigInteger;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.chemistry.opencmis.commons.exceptions.CmisUnauthorizedException;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.commons.server.CmisServiceFactory;
import org.apache.chemistry.opencmis.commons.server.TempStoreOutputStream;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.AuthHelper;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.Tx;
import org.apache.chemistry.opencmis.server.support.wrapper.ConformanceCmisServiceWrapper;
import org.structr.web.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class StructrCMISServicesFactory implements CmisServiceFactory {

	private static final Logger logger = Logger.getLogger(CmisServiceFactory.class.getName());

	@Override
	public void init(final Map<String, String> config) {

	    logger.log(Level.INFO, "Initialization map: {0}", config);
	}

	@Override
	public void destroy() {
	}

	@Override
	public CmisService getService(final CallContext cc) {

		//Probably more performant, if Services dont get newly created
		//all the time? -> see getService() in example CMIS Server-> Threadlocal
		StructrCMISService service = new StructrCMISService(checkAuthentication(cc));

		//The wrapper catches invalid CMIS requests and sets default values
		//for parameters that have not been provided by the client
		ConformanceCmisServiceWrapper wrapperService =
			new ConformanceCmisServiceWrapper(service, BigInteger.TEN, BigInteger.TEN, BigInteger.ZERO, BigInteger.ZERO);

		return wrapperService;
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

			SecurityContext securityContext = null;

			final String username           = callContext.getUsername();
			final String password           = callContext.getPassword();

			//temporary solution
			if((username == null) && (password == null)) {

				//temporary
				securityContext = SecurityContext.getInstance(null, AccessMode.Backend);

				return securityContext;
			}

			if(username.isEmpty() && password.isEmpty()) {

				//gets logged in as anonymous
				securityContext = SecurityContext.getInstance(null, AccessMode.Backend);

			} else {

				final Principal principal = AuthHelper.getPrincipalForPassword(Principal.name, username, password);

				if (principal != null) {

					if (principal instanceof SuperUser) {

						securityContext = SecurityContext.getSuperUserInstance();

					} else {

						Boolean isBackendUser = principal.getProperty(User.backendUser);
						Boolean isAdmin	      = principal.getProperty(Principal.isAdmin);

						if(isBackendUser || isAdmin) {

							securityContext = SecurityContext.getInstance(principal, AccessMode.Backend);
						}
					}
				}
			}


			tx.success();

			if (securityContext != null) {
				return securityContext;
			}

		} catch (AuthenticationException aex) {

			throw new CmisUnauthorizedException(aex.getMessage());

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		throw new CmisUnauthorizedException();
	}
}
