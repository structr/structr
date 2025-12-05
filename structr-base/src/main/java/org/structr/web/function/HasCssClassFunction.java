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
package org.structr.web.function;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.dom.DOMNode;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class HasCssClassFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "hasCssClass";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("element, css");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndTypes(sources, 2, NodeInterface.class, String.class);

			// we can safely cast here because of the assert.. above

			final NodeInterface element = (NodeInterface)sources[0];
			final String css            = (String)sources[1];
			final String elementClasses = element.as(DOMNode.class).getCssClass();

			if (StringUtils.isNotBlank(css) && StringUtils.isNotBlank(elementClasses)) {

				final Set<String> elementParts = new LinkedHashSet<>(Arrays.asList(elementClasses.split(" ")));
				final Set<String> inputParts   = new LinkedHashSet<>(Arrays.asList(css.split(" ")));

				return elementParts.containsAll(inputParts);
			}

			return false;

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${hasCssClass(element, css)}. Example: ${hasCssClass(this, 'active')}"),
			Usage.javaScript("Usage: ${{Structr.hasCssClass(element, css)}}. Example: ${{Structr.hasCssClass(this, 'active')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns whether the given element has the given CSS class(es).";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
