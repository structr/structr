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
package org.structr.core.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class RemoveLabelsFunction extends ManageLabelsFunction {

	@Override
	public String getName() {
		return "removeLabels";
	}

	@Override
	public String getShortDescription() {
		return "Removes the given set of labels from the given node.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		validateArguments(ctx, caller, sources);

		final NodeInterface node = (NodeInterface)sources[0];
		final List<String> list  = (List)sources[1];

		for (final String label : list) {
			node.getNode().removeLabel(label);
		}

		return null;
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.javaScript("$.removeLabels(node, [ 'LABEL1', 'LABEL2' ])"),
			Usage.structrScript("removeLabels(node, merge('LABEL1', 'LABEL2'))")
		);
	}
}
