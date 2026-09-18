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
package org.structr.core.function;

import jakarta.servlet.http.HttpServletRequest;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.Principal;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.rest.auth.AuthHelper;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class LoginFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {

		return "login";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("user, password");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			final Principal user             = ((AbstractNode) sources[0]).as(Principal.class);
			final String password            =                 sources[1].toString();
			final HttpServletRequest request = ctx.getSecurityContext().getRequest();

			if (request == null) {

				logger.warn("login(): no request to open a session on, refusing the login.");

				return false;
			}

			/* This function exists to delegate authentication to whatever source the script decides on,
			   and a second factor belongs to that source. While Structr is configured to demand one for
			   this account, there is no step here that could ask for a code - so a session opened here
			   would be one that the configured level says must not exist, and which login does the asking
			   would decide whether the setting means anything. */
			if (AuthHelper.isTwoFactorRequiredForUser(user)) {

				logger.warn("login(): a second factor is required for this account, which this function cannot ask for - the application has to use the login endpoint instead.");

				return false;
			}

			try {

				if (AuthHelper.getPrincipalForPassword(Traits.of(StructrTraits.PRINCIPAL).key("id"), user.getUuid(), password) == null) {

					return false;
				}

			} catch (AuthenticationException aex) {

				// a wrong password is an answer, not a failure of the call: this is the false the description promises

				return false;
			}

			AuthHelper.doLogin(request, user);

			return true;

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

			return null;
		}
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(Usage.structrScript("Usage: ${login(user, password)}"), Usage.javaScript("Usage: ${{$.login(user, password)}}"));
	}

	@Override
	public String getShortDescription() {

		return "Logs the given user in if the given password is correct. Returns true on successful login, false otherwise.";
	}

	@Override
	public String getLongDescription() {

		return "This function checks the password itself, which is what lets an application delegate authentication to an external source and log the user in afterwards. It cannot ask for a second factor, so it refuses to log in any account for which `security.twofactorauthentication.level` requires one and returns false; an application that needs two-factor authentication has to use the login endpoint, which runs the whole exchange. A wrong password returns false as well, while a locked account or a required password change still raise an error, because those carry a reason the application should see.";
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Security;
	}
}
