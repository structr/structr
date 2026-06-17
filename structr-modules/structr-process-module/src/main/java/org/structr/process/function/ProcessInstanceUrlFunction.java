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
import org.structr.api.config.Settings;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.process.traits.definitions.BpmnProcessTraitDefinition;
import org.structr.process.traits.definitions.ProcessInstanceTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.util.List;

/**
 * Builds an absolute URL to a process instance's page.
 *
 * Usage:
 *   processInstanceUrl(instance)
 *   processInstanceUrl(instance, token)
 *
 * Resolves {@code instance.process.instancePage} and prefixes it with an
 * absolute base URL. The base is resolved request-free where possible, in
 * this order:
 *
 *   1. the Site that serves the instance page (its hostname + port) -- the
 *      authoritative host, available without an HTTP request (so it works in
 *      post-commit listeners, timers, and background jobs),
 *   2. the {@code application.baseurl.override} setting,
 *   3. {@link ActionContext#getBaseUrl} (request-derived, then Settings).
 *
 * The instance id is appended as the trailing path segment (bound as
 * {@code current} by the page). An optional second argument is appended as
 * {@code ?token=<token>} for sessionless access (see {@code processToken}).
 */
public class ProcessInstanceUrlFunction extends Function<Object, Object> {

	private static final Logger logger = LoggerFactory.getLogger(ProcessInstanceUrlFunction.class);

	@Override
	public String getName() {
		return "processInstanceUrl";
	}

	@Override
	public String getRequiredModule() {
		return null;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			if (!(sources[0] instanceof NodeInterface instance)) {
				logger.warn("processInstanceUrl(): first argument must be a ProcessInstance node");
				return null;
			}

			final Traits instTraits      = instance.getTraits();
			final NodeInterface process  = instance.getProperty(instTraits.key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY));
			if (process == null) {
				logger.warn("processInstanceUrl(): instance {} has no process", instance.getUuid());
				return null;
			}

			final NodeInterface page = process.getProperty(process.getTraits().key(BpmnProcessTraitDefinition.INSTANCE_PAGE_PROPERTY));
			if (page == null) {
				logger.warn("processInstanceUrl(): process for instance {} has no instancePage", instance.getUuid());
				return null;
			}

			final Traits pageTraits = page.getTraits();
			final String pageName   = page.getProperty(pageTraits.key("name"));
			if (pageName == null || pageName.isEmpty()) {
				logger.warn("processInstanceUrl(): instancePage for instance {} has no name", instance.getUuid());
				return null;
			}

			// Resolve the base URL: Site first (request-free), then the
			// configured override, then the request/Settings-derived base.
			String base = baseUrlFromSite(page, pageTraits);
			if (base == null) {
				final String override = Settings.BaseUrlOverride.getValue();
				base = (override != null && !override.isEmpty())
					? override
					: ActionContext.getBaseUrl(ctx.getSecurityContext().getRequest());
			}

			final StringBuilder url = new StringBuilder(stripTrailingSlash(base))
				.append("/").append(pageName).append("/").append(instance.getUuid());

			if (sources.length > 1 && sources[1] != null) {
				url.append("?token=").append(sources[1].toString());
			}

			return url.toString();

		} catch (ArgumentNullException | ArgumentCountException ex) {

			logParameterError(caller, sources, ex.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	/**
	 * Build {@code scheme://hostname[:port]} from the first Site that serves
	 * the given page. Returns {@code null} when the page is not assigned to a
	 * Site (or no site has a hostname), so the caller can fall back.
	 */
	private String baseUrlFromSite(final NodeInterface page, final Traits pageTraits) throws FrameworkException {

		final Iterable<NodeInterface> sites = page.getProperty(pageTraits.key("sites"));
		if (sites == null) {
			return null;
		}

		for (final NodeInterface site : sites) {

			final Traits siteTraits = site.getTraits();
			final String hostname   = site.getProperty(siteTraits.key("hostname"));
			if (hostname == null || hostname.isEmpty()) {
				continue;
			}

			final Integer port   = site.getProperty(siteTraits.key("port"));
			// Site stores only hostname + port, not scheme; use the server's
			// https setting, treating port 443 as https as well.
			final boolean https  = Boolean.TRUE.equals(Settings.HttpsEnabled.getValue()) || (port != null && port == 443);

			final StringBuilder sb = new StringBuilder(https ? "https" : "http").append("://").append(hostname);
			if (port != null && ((https && port != 443) || (!https && port != 80))) {
				sb.append(":").append(port);
			}

			return sb.toString();
		}

		return null;
	}

	private static String stripTrailingSlash(final String s) {
		return (s != null && s.endsWith("/")) ? s.substring(0, s.length() - 1) : s;
	}

	// --- Documentation ---

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("instance [, token]");
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${processInstanceUrl(instance [, token])}"),
			Usage.javaScript("Usage: ${{$.processInstanceUrl(instance [, token])}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Builds an absolute URL to a process instance's page.";
	}

	@Override
	public String getLongDescription() {
		return "Resolves the process instance's process.instancePage and prefixes it with an absolute base URL, "
			+ "appending the instance id as the trailing path segment. The base URL is resolved request-free where "
			+ "possible: first from the Site that serves the instance page (its hostname and port), then from the "
			+ "application.baseurl.override setting, then from the current request (falling back to server settings). "
			+ "This makes it safe to call from post-commit task listeners, timers, and other contexts without an HTTP request "
			+ "-- for example when building notification email links.";
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"The Site lookup uses the first Site assigned to the instance page; assign the page to a Site for deterministic, host-qualified links.",
			"When no Site is found, set application.baseurl.override for correct email links; otherwise the URL is assembled from the request or server settings.",
			"The scheme is derived from the application.https.enabled setting (port 443 is also treated as https).",
			"The optional second argument is appended as ?token=<token>; pair it with processToken for sessionless access."
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("instance", "the ProcessInstance node"),
			Parameter.optional("token", "access token to append as ?token=<token> (e.g. from processToken)")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.javaScript(
				"${{ $.processInstanceUrl($.this.processInstance) }}",
				"Absolute URL to the current task's process instance page"
			),
			Example.javaScript(
				"${{ let url = $.processInstanceUrl(inst, $.processToken(inst.id, task.id, 'review')); }}",
				"Instance URL with a sessionless access token appended"
			)
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Http;
	}
}
