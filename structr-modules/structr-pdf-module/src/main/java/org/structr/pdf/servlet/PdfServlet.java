/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import com.github.jhonnymertz.wkhtmltopdf.wrapper.Pdf;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.io.EofException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.rest.common.StatsCallback;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.web.common.RenderContext;
import org.structr.web.common.StringRenderBuffer;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.servlet.HtmlServlet;
import org.structr.websocket.command.AbstractCommand;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class PdfServlet extends HtmlServlet {

	private static final Logger logger = LoggerFactory.getLogger(HtmlServlet.class.getName());

	private static final ExecutorService threadPool                   = Executors.newCachedThreadPool();

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

	@Override
	protected void renderAsyncOutput(HttpServletRequest request, HttpServletResponse response, App app, RenderContext renderContext, DOMNode rootElement, final long requestStartTime) throws IOException {

		final AsyncContext async = request.startAsync();
		final ServletOutputStream out = async.getResponse().getOutputStream();
		final AtomicBoolean finished = new AtomicBoolean(false);
		final DOMNode rootNode = rootElement;

		setCustomResponseHeaders(response);

		response.setContentType("application/pdf");
		response.setHeader("Content-Disposition","attachment;filename=\"FileName.pdf\"");

		threadPool.submit(new Runnable() {

			@Override
			public void run() {

				try (final Tx tx = app.tx()) {

					// render
					rootNode.render(renderContext, 0);
					finished.set(true);

					tx.success();

				} catch (Throwable t) {

					logger.warn("Error while rendering page {}: {}", rootNode.getName(), t.getMessage());
					logger.warn(ExceptionUtils.getStackTrace(t));

					try {

						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						finished.set(true);

					} catch (IOException ex) {
						logger.warn("", ex);
					}
				}
			}

		});

		// start output write listener
		out.setWriteListener(new WriteListener() {

			@Override
			public void onWritePossible() throws IOException {

				try {

					final Queue<String> queue = renderContext.getBuffer().getQueue();
					String pageContent = "";
					while (out.isReady()) {

						String buffer = null;

						synchronized (queue) {
							buffer = queue.poll();
						}

						if (buffer != null) {

							pageContent += buffer;

						} else {

							if (finished.get()) {

								// TODO: implement parameters for wkhtmltopdf in settings

								Pdf pdf = new Pdf();
								pdf.addPageFromString(pageContent);

								out.write(pdf.getPDF());

								async.complete();

								// prevent this block from being called again
								break;
							}

							Thread.sleep(1);
						}
					}




				} catch (EofException ee) {
					logger.warn("Could not flush the response body content to the client, probably because the network connection was terminated.");
				} catch (IOException | InterruptedException t) {
					logger.warn("Unexpected exception", t);
				}
			}

			@Override
			public void onError(Throwable t) {
				if (t instanceof EofException) {
					logger.warn("Could not flush the response body content to the client, probably because the network connection was terminated.");
				} else {
					logger.warn("Unexpected exception", t);
				}
			}
		});
	}

	@Override
	protected void writeOutputStream(HttpServletResponse response, StringRenderBuffer buffer) throws IOException {
		response.getOutputStream().write(buffer.getBuffer().toString().getBytes("utf-8"));
		response.getOutputStream().flush();
		response.getOutputStream().close();
	}

}