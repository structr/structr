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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;

import java.util.List;

/**
 * Convenience method to find named nodes in the same page. If more than one node is found,
 * an error message is returned that informs the user that this is not allowed and can
 * result in unexpected behavior (instead of returning the node).
 */
public class ComponentFunction extends UiCommunityFunction {

	@Override
	public String getName() {
		return "component";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("name");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndTypes(sources, 1, String.class);

			return getComponent(caller, sources[0].toString());

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${component(name)}. Example: ${component('Main Template')}"),
			Usage.javaScript("Usage: ${{ $.component(name); }}. Example: ${{ $.component('Main Template'); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Finds and returns the component with the given name in the current page.";
	}

	@Override
	public String getLongDescription() {
		return """
		Components can be referenced via their `name` property.
		
		Possible nodes **MUST**:
		1. have a unique name
		2. NOT be in the trash

		Possible nodes **CAN**:
		1. be somewhere in the pages tree
		2. be in the Shared Components

		See also `include()`, `includeChild()` and `render()`.
		""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("name", "name of the node (and subtree) to include")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.html("<input type=\"text\" ${component('Table 1').filterControls()} />", "Render an input field with the filter controls for the component \"Table 1\" into the output buffer")
		);
	}

	protected DOMNode getComponent(final Object caller, final String name) throws FrameworkException {

		if (caller instanceof NodeInterface node && node.is(StructrTraits.DOM_NODE)) {

			final DOMNode callerNode = node.as(DOMNode.class);
			final Page page          = callerNode.getOwnerDocument();

			for (final NodeInterface child : page.getAllChildNodes()) {

				if (name.equals(child.getName())) {

					return child.as(DOMNode.class);
				}
			}
		}

		return null;
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Rendering;
	}
}
