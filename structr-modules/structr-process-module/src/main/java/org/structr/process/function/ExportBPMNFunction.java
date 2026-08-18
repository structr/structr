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
package org.structr.process.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.process.bpmn.BpmnExporter;
import org.structr.process.entity.BpmnDefinitions;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.util.List;

public class ExportBPMNFunction extends Function<Object, Object> {

	@Override
	public String getName() {

		return "exportBpmn";
	}

	@Override
	public String getRequiredModule() {

		return null;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof NodeInterface defNode) {

				final BpmnExporter exporter = new BpmnExporter();

				return exporter.exportBpmn(defNode.as(BpmnDefinitions.class));
			}

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("bpmnDefinitionsNode");
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(Usage.structrScript("Usage: ${export_bpmn(node)}"), Usage.javaScript("Usage: ${{S.exportBpmn(node)}}"));
	}

	@Override
	public String getShortDescription() {

		return "Exports a BpmnDefinitions graph structure to BPMN 2.0.2 XML.";
	}

	@Override
	public String getLongDescription() {

		return "Reads the process definition graph from the given BpmnDefinitions node and produces a valid BPMN 2.0.2 XML string including all process elements, sequence flows, and DI diagram data.";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("bpmnDefinitionsNode", "BpmnDefinitions node to export"));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${export_bpmn(first(find('BpmnDefinitions')))}", "Export the first BpmnDefinitions node to XML"),
			Example.javaScript("${{let xml = $.exportBpmn(def);}}", "Export in JavaScript")
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.InputOutput;
	}
}
