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
package org.structr.process.function;

import com.auth0.jwt.interfaces.Claim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.process.auth.ProcessJWTHelper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates a process access JWT and returns its claims as a map.
 *
 * Usage:
 *   validate_process_token(token)
 *
 * Returns a map with keys: processInstanceId, taskId, action, scope
 * or null if the token is invalid, expired, or tampered with.
 */
public class ValidateProcessTokenFunction extends Function<Object, Object> {

	private static final Logger logger = LoggerFactory.getLogger(ValidateProcessTokenFunction.class);

	@Override
	public String getName() {

		return "validate_process_token";
	}

	@Override
	public String getRequiredModule() {

		return null;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final String token = sources[0].toString();
			final Map<String, Claim> claims = ProcessJWTHelper.validateProcessToken(token);

			if (claims == null) {

				return null;
			}

			// Convert to a simple String map for scripting access
			final Map<String, Object> result = new LinkedHashMap<>();
			result.put("processInstanceId", ProcessJWTHelper.getProcessInstanceId(claims));
			result.put("taskId",            ProcessJWTHelper.getTaskId(claims));
			result.put("action",            ProcessJWTHelper.getAction(claims));
			result.put("scope",             "process");

			return result;

		} catch (ArgumentNullException | ArgumentCountException ex) {

			logParameterError(caller, sources, ex.getMessage(), ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}
	}

	// --- Documentation ---

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("token");
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(Usage.structrScript("Usage: ${validate_process_token(token)}"), Usage.javaScript("Usage: ${{$.validate_process_token(token)}}"));
	}

	@Override
	public String getShortDescription() {

		return "Validates a process access JWT and returns its claims.";
	}

	@Override
	public String getLongDescription() {

		return "Validates a process access JWT by checking its cryptographic signature, expiry, "
			+ "and scope claim. If valid, returns a map with the token's claims (processInstanceId, "
			+ "taskId, action, scope). If invalid, expired, or tampered with, returns null. "
			+ "This function should be called inside doPrivileged on the process page to validate "
			+ "access tokens from notification links.";
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"Returns null for any invalid, expired, or tampered token -- never throws an exception for bad tokens.",
			"Should be called inside doPrivileged on the process page since it needs access to the JWT secret.",
			"The returned map contains: processInstanceId, taskId, action, scope.",
			"After validating, the page should also check that the processInstanceId matches the URL path and that the task is still in 'created' status."
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("token", "the JWT string from the URL parameter"));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.javaScript(
				"${{let claims = $.doPrivileged(() => $.validate_process_token($.request.token)); if (claims && claims.processInstanceId === $.current.id) { /* valid */ }}}",
				"Validate token and check it matches the current process instance"
			),
			Example.structrScript(
				"${validate_process_token(request.token)}",
				"Validate a process access token (call inside doPrivileged)"
			)
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Security;
	}

	/**
	 * The token is a credential, and a parameter error would otherwise write it into the server log.
	 */
	@Override
	protected boolean redactParameters() {

		return true;
	}
}
