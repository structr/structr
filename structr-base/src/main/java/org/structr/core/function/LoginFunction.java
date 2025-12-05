/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
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

			Principal user  = ((AbstractNode) sources[0]).as(Principal.class);
			String password =                 sources[1].toString();

			if (AuthHelper.getPrincipalForPassword(Traits.of(StructrTraits.PRINCIPAL).key("id"), user.getUuid(), password) != null) {
				AuthHelper.doLogin(ctx.getSecurityContext().getRequest(), user);
			}

			return true;

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			// only show the error message for wrong parameter count
			return usage(ctx.isJavaScriptContext());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

			// only show the error message for wrong parameter count
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${login(user, password)}"),
			Usage.javaScript("Usage: ${{$.login(user, password)}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Logs the given user in if the given password is correct. Returns true on successful login.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
