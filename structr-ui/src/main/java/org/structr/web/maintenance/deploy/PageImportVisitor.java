/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.maintenance.deploy;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.web.importer.Importer;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public class PageImportVisitor implements FileVisitor<Path> {

	private static final Logger logger = Logger.getLogger(PageImportVisitor.class.getName());

	@Override
	public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {

		if (attrs.isRegularFile()) {

			final String fileName = file.getFileName().toString();
			if (fileName.endsWith(".html")) {

				final String name       = StringUtils.substringBeforeLast(fileName, ".html");
				final App app           = StructrApp.getInstance();
				final Page existingPage = getExistingPage(name);

				try (final Tx tx = app.tx()) {

					if (existingPage != null) {

						deletePage(app, name);
					}

					final String src        = new String (Files.readAllBytes(file),Charset.forName("UTF-8"));
					final Importer importer = new Importer(SecurityContext.getSuperUserInstance(), src, null, name, false, false);
					final boolean parseOk   = importer.parse();

					if (parseOk) {

						// set comment handler that can parse and apply special Structr comments in HTML source files
						importer.setCommentHandler(new DeploymentCommentHandler());

						// parse page
						final String pageId = importer.readPage().getUuid();

						logger.log(Level.INFO, "Successfully parsed page {0}: {1}", new Object[] { name, pageId });
					}

					tx.success();

				} catch (FrameworkException fex) {
					logger.log(Level.WARNING, "Exception while importing page {0}: {1}", new Object[] { name, fex.getMessage() });
				}

			} else {

				logger.log(Level.INFO, "Ignoring non-HTML file {0}", fileName);
			}
		}

		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {

		logger.log(Level.WARNING, "Exception while importing page {0}: {1}", new Object[] { file.toString(), exc.getMessage() });
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	// ----- private methods -----
	private Page getExistingPage(final String name) {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			return app.nodeQuery(Page.class).andName(name).getFirst();

		} catch (FrameworkException fex) {
			logger.log(Level.WARNING, "Unable to determine if page {0} already exists, ignoring.", name);
		}

		return null;
	}

	private void deletePage(final App app, final String name) throws FrameworkException {

		final Page page = app.nodeQuery(Page.class).andName(name).getFirst();
		if (page != null) {

			for (final DOMNode child : page.getProperty(Page.elements)) {
				app.delete(child);
			}

			app.delete(page);
		}
	}
}
