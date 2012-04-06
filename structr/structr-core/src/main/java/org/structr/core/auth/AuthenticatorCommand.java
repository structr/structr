/*
 *  Copyright (C) 2011 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.core.auth;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;

/**
 * A command that creates and returns an {@see Authenticator} instance
 * for the given ServletConfig. This command takes a ServletConfig
 * as its only parameter.
 *
 * @author Christian Morgner
 */
public class AuthenticatorCommand extends Command {

	private static final Logger logger = Logger.getLogger(AuthenticatorCommand.class.getName());

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		if(parameters != null && parameters.length == 1 && parameters[0] instanceof ServletConfig) {

			ServletConfig servletConfig = (ServletConfig)parameters[0];
			Authenticator authenticator = null;
			if(servletConfig != null) {

				// In a multi-context environment (e.g. WebSocket, REST and plain HTTP),
				// it's bad idea to store the authenticator service in the servlet context
				//Authenticator authenticator = (Authenticator)servletConfig.getServletContext().getAttribute(AuthenticationService.SERVLET_PARAMETER_AUTHENTICATOR);
				//if(authenticator == null) {

					
					String authenticatorClassName = servletConfig.getInitParameter(AuthenticationService.SERVLET_PARAMETER_AUTHENTICATOR);
					if(authenticatorClassName != null) {

						try {
							Class authenticatorClass = Class.forName(authenticatorClassName);
							authenticator = (Authenticator)authenticatorClass.newInstance();

							// cache instance
							servletConfig.getServletContext().setAttribute(AuthenticationService.SERVLET_PARAMETER_AUTHENTICATOR, authenticator);	

						} catch(Throwable t) {

							logger.log(Level.SEVERE, "Error instantiating authenticator for servlet with context path {0}: {1}",
							new Object[] {
								servletConfig.getServletName(), t
							});

						}

					} else {

						logger.log(Level.SEVERE, "No authenticator for servlet with context path {0}", servletConfig.getServletName());
					}
				//}

				// return instance
				return authenticator;
			}
		}

		return null;
	}

	@Override
	public Class getServiceClass() {
		return AuthenticationService.class;
	}
}
