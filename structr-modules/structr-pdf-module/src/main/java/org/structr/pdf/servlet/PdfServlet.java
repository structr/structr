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
package org.structr.pdf.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.common.SecurityContext;
import org.structr.core.graph.Tx;
import org.structr.docs.Documentation;
import org.structr.pdf.PdfRenderer;
import org.structr.rest.common.StatsCallback;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.web.common.RenderContext;
import org.structr.web.common.StringRenderBuffer;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.servlet.HtmlServlet;
import org.structr.websocket.command.AbstractCommand;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

@Documentation(name="PdfServlet", parent="Servlets", children={ "PdfServlet Settings" })
public class PdfServlet extends HtmlServlet {

	private static final Logger logger = LoggerFactory.getLogger(PdfServlet.class.getName());

	private final StructrHttpServiceConfig config                     = new StructrHttpServiceConfig();
	private final Set<String> possiblePropertyNamesForEntityResolving = new LinkedHashSet<>();
	protected StatsCallback stats                                     = null;

	public PdfServlet() {

		// resolving properties
		final String resolvePropertiesSource = Settings.HtmlResolveProperties.getValue();

		for (final String src : resolvePropertiesSource.split("[, ]+")) {

			final String name = src.trim();
			if (StringUtils.isNotBlank(name)) {

				possiblePropertyNamesForEntityResolving.add(name);
			}
		}
	}

	@Override
	public StructrHttpServiceConfig getConfig() {

		return config;
	}

	@Override
	public String getModuleName() {

		return "pdf";
	}

	@Override
	public void init() {

		try (final Tx tx = StructrApp.getInstance().tx()) {

			AbstractCommand.getOrCreateHiddenDocument();
			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("Unable to create shadow page: {}", fex.getMessage());
		}
	}

	@Override
	public void destroy() {
	}

	@Override
	public void registerStatsCallback(final StatsCallback stats) {

		this.stats = stats;
	}

	/** A PDF cannot be streamed, so both of HtmlServlet's paths render synchronously here. */
	@Override
	protected void renderAsyncOutput(final HttpServletRequest request, final HttpServletResponse response, final App app, final RenderContext renderContext, final DOMNode rootElement, final long requestStartTime) throws IOException {

		try {

			final String html = PdfRenderer.renderToHtml(rootElement, renderContext);

			writePdf(response, html, renderContext.getSecurityContext(), rootElement.getName());

		} catch (final FrameworkException fex) {

			logger.warn("Error while rendering page {} as PDF: {}", rootElement.getName(), fex.getMessage());

			response.sendError(fex.getStatus(), fex.getMessage());
		}
	}

	/** HtmlServlet's synchronous path, taken when httpservice.async is off or the page sets pageCreatesRawData. */
	@Override
	protected void writeOutputStream(final HttpServletResponse response, final StringRenderBuffer buffer, final RenderContext renderContext) throws IOException {

		final Page page = renderContext.getPage();

		try {

			writePdf(response, buffer.getBuffer().toString(), renderContext.getSecurityContext(), page != null ? page.getName() : null);

		} catch (final FrameworkException fex) {

			logger.warn("Error while converting rendered page to PDF: {}", fex.getMessage());

			response.sendError(fex.getStatus(), fex.getMessage());
		}
	}

	private void writePdf(final HttpServletResponse response, final String html, final SecurityContext securityContext, final String pageName) throws FrameworkException, IOException {

		final byte[] pdf = PdfRenderer.toPdf(html, securityContext, pageName == null ? "page" : pageName);

		// HtmlServlet has already applied the custom response headers on both of its paths
		response.setContentType("application/pdf");
		response.setContentLength(pdf.length);

		// a page may set its own disposition while rendering, so only supply a default
		if (!response.containsHeader("Content-Disposition")) {

			response.setHeader("Content-Disposition", "attachment; filename=\"" + PdfRenderer.fileNameFor(pageName) + "\"");
		}

		response.getOutputStream().write(pdf);
		response.getOutputStream().flush();
	}

}
