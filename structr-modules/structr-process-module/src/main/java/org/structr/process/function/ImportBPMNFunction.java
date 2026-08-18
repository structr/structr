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
import org.structr.process.bpmn.BpmnImporter;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.util.List;

public class ImportBPMNFunction extends Function<Object, Object> {

	@Override
	public String getName() {

		return "importBpmn";
	}

	@Override
	public String getRequiredModule() {

		return null;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof String xml) {

				final BpmnImporter importer = new BpmnImporter(ctx.getSecurityContext());
				final NodeInterface defNode = importer.importBpmn(xml);

				return defNode;
			}

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("bpmnXml");
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(Usage.structrScript("Usage: ${import_bpmn(xml)}"), Usage.javaScript("Usage: ${{$.importBpmn(xml)}}"));
	}

	@Override
	public String getShortDescription() {

		return "Imports BPMN 2.0.2 XML and creates a BpmnDefinitions graph structure.";
	}

	@Override
	public String getLongDescription() {

		return "Parses the given BPMN 2.0.2 XML string and creates a complete graph representation including process elements, sequence flows, and DI diagram data. Returns the created BpmnDefinitions node.";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("bpmnXml", "BPMN 2.0.2 XML string to import"));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${import_bpmn(xml)}", "Import BPMN XML and return the BpmnDefinitions node"),
			Example.javaScript("${{let def = $.importBpmn(xml);}}", "Import BPMN XML in JavaScript")
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.InputOutput;
	}
}
