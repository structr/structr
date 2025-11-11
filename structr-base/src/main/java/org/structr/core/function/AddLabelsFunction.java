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
package org.structr.core.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AddLabelsFunction extends ManageLabelsFunction {

	@Override
	public String getName() {
		return "add_labels";
	}

	@Override
	public String getShortDescription() {
		return "Adds the given set of labels to the given node";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		validateArguments(ctx, caller, sources);

		final NodeInterface node = (NodeInterface)sources[0];
		final Set<String> set    = new LinkedHashSet<>((List)sources[1]);

		node.getNode().addLabels(set);

		return null;
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.javaScript("addLabels(node, [ 'LABEL1', 'LABEL2' ])"),
			Usage.structrScript("add_labels(node, merge('LABEL1', 'LABEL2'))")
		);
	}
}
