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

import java.util.List;
import java.util.Map;

/**
 * Creates a signed JWT for process-scoped access (low security level).
 *
 * Usage:
 *   processToken(processInstanceId, taskId, action)
 *   processToken(processInstanceId, taskId, action, expiryMinutes)
 *
 * Returns a signed JWT string that can be appended to notification URLs
 * as ?token=<jwt>. The token is self-contained, tamper-proof, and has
 * a configurable expiry (default: 48 hours).
 */
public class ProcessTokenFunction extends Function<Object, Object> {

	private static final Logger logger = LoggerFactory.getLogger(ProcessTokenFunction.class);

	@Override
	public String getName() {

		return "process_token";
	}

	@Override
	public String getRequiredModule() {

		return null;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 3);

			final String processInstanceId = sources[0].toString();
			final String taskId            = sources[1].toString();
			final String action            = sources[2].toString();
			int expiryMinutes = ProcessJWTHelper.DEFAULT_EXPIRY_MINUTES;

			if (sources.length >= 4) {

				try {

					expiryMinutes = Integer.parseInt(sources[3].toString());

				} catch (NumberFormatException nfe) {

					logger.warn("Invalid expiry value '{}', using default ({} minutes)", sources[3], ProcessJWTHelper.DEFAULT_EXPIRY_MINUTES);
				}
			}

			return ProcessJWTHelper.createProcessToken(processInstanceId, taskId, action, expiryMinutes);

		} catch (ArgumentNullException | ArgumentCountException ex) {

			logParameterError(caller, sources, ex.getMessage(), ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}
	}

	// --- Documentation ---

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("processInstanceId, taskId, action [, expiryMinutes]");
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("Usage: ${process_token(processInstanceId, taskId, action [, expiryMinutes])}"),
			Usage.javaScript("Usage: ${{$.process_token(processInstanceId, taskId, action [, expiryMinutes])}}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Creates a signed JWT for process-scoped, sessionless access.";
	}

	@Override
	public String getLongDescription() {

		return "Creates a signed JWT (JSON Web Token) for use in process notification links. "
			+ "The token carries the process instance ID, task ID, and allowed action as signed claims. "
			+ "It is tamper-proof (cryptographically signed), has a configurable expiry (default: 48 hours), "
			+ "and is self-contained (no server-side token storage needed). "
			+ "Used by the 'low' security level for sessionless access to process steps via email links.";
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"Requires JWT to be configured in structr.conf (security.jwt.secret must be at least 32 characters).",
			"The token is signed with HMAC256 using the configured JWT secret.",
			"Default expiry is 48 hours. Override with the optional expiryMinutes parameter.",
			"The token is validated on the page side using doPrivileged to verify signature and claims.",
			"This function is designed for the 'low' security level. For 'high' security, no token is needed."
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("processInstanceId", "UUID of the ProcessInstance"),
			Parameter.mandatory("taskId", "UUID of the TaskInstance (or 'view' for read-only access)"),
			Parameter.mandatory("action", "allowed action: 'review', 'view', etc."),
			Parameter.optional("expiryMinutes", "token lifetime in minutes (default: 2880 = 48 hours)")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript(
				"${process_token(inst.id, task.id, 'review')}",
				"Create a process access token for a review action (48h expiry)"
			),
			Example.structrScript(
				"${process_token(inst.id, task.id, 'review', 1440)}",
				"Create a process access token with 24-hour expiry"
			),
			Example.javaScript(
				"${{let jwt = $.process_token(inst.id, task.id, 'review'); let url = pageUrl + '?token=' + jwt;}}",
				"Create token and build notification URL in JavaScript"
			)
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Security;
	}
}
