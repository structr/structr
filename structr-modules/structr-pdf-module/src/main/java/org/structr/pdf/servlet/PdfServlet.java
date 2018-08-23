/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.pdf;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.ThreadLocalMatcher;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.web.common.StringRenderBuffer;
import org.structr.web.servlet.HtmlServlet;
import org.structr.websocket.command.AbstractCommand;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class PdfServlet extends HtmlServlet {

	private static final Logger logger = LoggerFactory.getLogger(HtmlServlet.class.getName());

	private static final List<String> customResponseHeaders        = new LinkedList<>();
	private static final ThreadLocalMatcher threadLocalUUIDMatcher = new ThreadLocalMatcher("[a-fA-F0-9]{32}");
	private static final ExecutorService threadPool                = Executors.newCachedThreadPool();

	private final Pattern FilenameCleanerPattern                      = Pattern.compile("[\n\r]", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
	private final StructrHttpServiceConfig config                     = new StructrHttpServiceConfig();
	private final Set<String> possiblePropertyNamesForEntityResolving = new LinkedHashSet<>();

	private boolean isAsync = false;

	public PdfServlet() {

		final String customResponseHeadersString = Settings.HtmlCustomResponseHeaders.getValue();
		if (StringUtils.isNotBlank(customResponseHeadersString)) {

			customResponseHeaders.addAll(Arrays.asList(customResponseHeadersString.split("[ ,]+")));
		}

		// resolving properties
		final String resolvePropertiesSource = Settings.HtmlResolveProperties.getValue();
		for (final String src : resolvePropertiesSource.split("[, ]+")) {

			final String name = src.trim();
			if (StringUtils.isNotBlank(name)) {

				possiblePropertyNamesForEntityResolving.add(name);
			}
		}

		this.isAsync = Settings.Async.getValue();
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
	protected void writeOutputSteam(HttpServletResponse response, StringRenderBuffer buffer) throws IOException {
		logger.warn("This is awesome!!!");
		response.getOutputStream().write(buffer.getBuffer().toString().getBytes("utf-8"));
		response.getOutputStream().flush();
		response.getOutputStream().close();
	}

}