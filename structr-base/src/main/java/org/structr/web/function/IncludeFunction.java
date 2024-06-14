/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.storage.StorageProviderFactory;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;
import org.structr.web.datasource.FunctionDataSource;
import org.structr.web.entity.File;
import org.structr.web.entity.dom.DOMNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Convenience method to render named nodes. If more than one node is found, an error message is returned that informs the user that this is not allowed and can result in unexpected
 * behavior (instead of including the node).
 */
public class IncludeFunction extends UiCommunityFunction {

	public static final String ERROR_MESSAGE_INCLUDE    = "Usage: ${include(name)}. Example: ${include(\"Main Template\")}";
	public static final String ERROR_MESSAGE_INCLUDE_JS = "Usage: ${{Structr.include(name)}}. Example: ${{Structr.include(\"Main Template\")}}";

	@Override
	public String getName() {
		return "include";
	}

	@Override
	public String getSignature() {
		return "name [, collection, dataKey]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			if (!(sources[0] instanceof String)) {

				return null;
			}

			final PropertyKey<DOMNode> sharedCompKey = StructrApp.key(DOMNode.class, "sharedComponent");
			final SecurityContext securityContext    = ctx.getSecurityContext();
			final App app                            = StructrApp.getInstance(securityContext);
			final List<DOMNode> nodeList             = app.nodeQuery(DOMNode.class).andName((String)sources[0]).getAsList();


			RenderContext innerCtx = null;
			boolean useBuffer      = false;
			DOMNode node           = null;

			if (ctx.isRenderContext()) {

				innerCtx  = new RenderContext((RenderContext)ctx);
				useBuffer = true;

			} else {

				innerCtx  = new RenderContext(securityContext);
				useBuffer = false;
			}

			/**
			 * Nodes can be included via their name property These nodes MUST: 1. be unique in name 2. NOT be in the trash => have an ownerDocument AND a parent (public
			 * users are not allowed to see the __ShadowDocument__ ==> this check must either be made in a superuser-context OR the __ShadowDocument could be made public?)
			 *
			 * These nodes can be: 1. somewhere in the pages tree 2. in the shared components 3. both ==> causes a problem because we now have multiple nodes with the same
			 * name (one shared component and multiple linking instances of that component)
			 *
			 * INFOS:
			 *
			 * - If a DOMNode has "syncedNodes" it MUST BE a shared component - If a DOMNodes "sharedComponent" is set it MUST BE AN INSTANCE of a shared component => Can
			 * we safely ignore these? I THINK SO!
			 */
			for (final DOMNode n : nodeList) {

				// IGNORE everything that REFERENCES a shared component!
				if (n.getProperty(sharedCompKey) == null && !n.inTrash()) {

					// the DOMNode is either a shared component OR a named node in the pages tree
					if (node == null) {

						node = n;

					} else {

						// ERROR: we have found multiple DOMNodes with the same name
						logger.warn("Ambiguous node name \"" + ((String)sources[0]) + "\" (nodes found: " + StringUtils.join(nodeList, ", ") + ")");
						return "";
					}
				}
			}

			return renderNode(securityContext, ctx, innerCtx, sources, app, node, useBuffer);

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_INCLUDE_JS : ERROR_MESSAGE_INCLUDE);
	}

	@Override
	public String shortDescription() {
		return "Includes the content of the node with the given name (optionally as a repeater element)";
	}

	protected String renderNode(final SecurityContext securityContext, final ActionContext ctx, final RenderContext innerCtx, final Object[] sources, final App app, final DOMNode node, final boolean useBuffer) throws FrameworkException {

		if (node != null) {

			if (sources.length == 3 && sources[1] instanceof Iterable && sources[2] instanceof String ) {

				final Iterable<GraphObject> iterable = FunctionDataSource.map((Iterable)sources[1]);
				final String dataKey                 = (String)sources[2];

				innerCtx.setListSource(iterable);
				node.renderNodeList(securityContext, innerCtx, 0, dataKey);

			} else {

				node.render(innerCtx, 0);
			}

			if (innerCtx.appLibRendered()) {
				((RenderContext)ctx).setAppLibRendered(true);
			}

		} else {

			final File file = app.nodeQuery(File.class).andName((String)sources[0]).getFirst();

			if (file != null) {

				final String name        = file.getProperty(NodeInterface.name);
				final String contentType = file.getContentType();
				final String charset     = StringUtils.substringAfterLast(contentType, "charset=");
				final String extension   = StringUtils.substringAfterLast(name, ".");

				if (contentType == null || StringUtils.isBlank(extension)) {

					logger.warn("No valid file type detected. Please make sure {} has a valid content type set or file extension. Parameters: {}", new Object[] { name, getParametersAsString(sources) });
					return "No valid file type detected. Please make sure " + name + " has a valid content type set or file extension.";

				}

				if (contentType.startsWith("text/css")) {

					return "<link href=\"" + file.getPath() + "\" rel=\"stylesheet\">";

				} else if (contentType.contains("/javascript")) {

					return "<script src=\"" + file.getPath() + "\"></script>";

				} else if (contentType.startsWith("image/svg")) {

					try (final InputStream is = file.getInputStream()) {

						final byte[] buffer = new byte[Long.valueOf(StorageProviderFactory.getStorageProvider(file).size()).intValue()];
						IOUtils.read(is, buffer);
						return StringUtils.toEncodedString(buffer, Charset.forName(charset));

					} catch (IOException ex) {

						logger.warn("Exception for parameters: {}", getParametersAsString(sources));
						logger.error("", ex);

					}

					return "<img alt=\"" + name + "\" src=\"" + file.getPath() + "\">";

				} else if (contentType.startsWith("image/")) {

					return "<img alt=\"" + name + "\" src=\"" + file.getPath() + "\">";

				} else {

					logger.warn("Don't know how to render content type or extension of {}. Parameters: {}", new Object[] { name, getParametersAsString(sources) });
					return "Don't know how to render content type or extension of  " + name + ".";

				}

			}

		}

		if (useBuffer) {

			// output was written to RenderContext async buffer
			return null;

		} else {

			// output needs to be returned as a function result
			return StringUtils.join(innerCtx.getBuffer().getQueue(), "");
		}
	}

	public static boolean isRenderContext (final ActionContext ctx) {
		return (ctx instanceof RenderContext);
	}
}
