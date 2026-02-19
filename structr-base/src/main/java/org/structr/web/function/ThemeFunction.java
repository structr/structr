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
package org.structr.web.function;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.script.Scripting;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.rest.service.HttpService;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ThemeFunction extends UiCommunityFunction {

	@Override
	public String getName() {
		return "theme";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		assertArrayHasLengthAndTypes(sources, 1, String.class);

		final String name  = (String) sources[0];

		if (ctx instanceof RenderContext renderContext) {

			try {

				final HttpService service                = Services.getInstance().getServiceImplementation(HttpService.class);
				final ResourceHandler resourceHandler    = service.getExportedResourceHandler();
				final Resource baseResource              = resourceHandler.getBaseResource();
				final Resource styleResource             = baseResource.resolve("/themes/style.css");
				final Path themePath                     = getThemePathOrDefault(baseResource, name);
				final Path stylePath                     = styleResource.getPath();

				final String themeSource = Files.readString(themePath);
				final String styleSource = Files.readString(stylePath);

				// evaluate JS to apply theme
				Scripting.evaluate(ctx, null, "${{" + themeSource + "}}", "Parsing " + name + "/theme.js");

				// replace variables in CSS (this is the place where we can later include a CSS library etc.)
				renderContext.getBuffer().append(Scripting.replaceVariables(ctx, null, styleSource));

			} catch (IOException ioex) {
				throw new FrameworkException(422, ioex.getMessage());
			}
		}

		return null;
	}

	@Override
	public String getShortDescription() {
		return "Applies the theme with the given name.";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${theme('dark')}"),
			Usage.structrScript("Usage: ${{ $.theme('dark'); }}")
		);
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("name");
	}

	// ----- private methods -----
	private Path getThemePathOrDefault(final Resource baseResource, final String name) {

		final Resource themeResource = baseResource.resolve("/themes/" + name + "/theme.js");
		final Path themePath         = themeResource.getPath();

		if (Files.exists(themePath)) {
			return themePath;
		}

		logger.warn("Theme {} does not exist, using default.", name);

		return baseResource.resolve("/themes/default/theme.js").getPath();
	}
}
