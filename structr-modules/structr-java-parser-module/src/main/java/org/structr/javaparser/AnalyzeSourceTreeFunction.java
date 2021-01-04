/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.javaparser;

import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.Folder;

public class AnalyzeSourceTreeFunction extends ParserModuleFunction {

	public static final String ERROR_MESSAGE_ANALYZE_SOURCE_TREE = "Usage: ${analyze_source_tree(path)}";

	@Override
	public String getName() {
		return "analyze_source_tree";
	}

	@Override
	public String getSignature() {
		return "path";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof String) {

				final SecurityContext securityContext = ctx.getSecurityContext();
				final App app                         = StructrApp.getInstance(securityContext);

				new JavaParserModule().analyzeSourceTree(app.nodeQuery(Folder.class).and(StructrApp.key(Folder.class, "path"), (String) sources[0]).getFirst());

				return "";
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_ANALYZE_SOURCE_TREE;
	}

	@Override
	public String shortDescription() {
		return "Analyze the method calls of all Java classes/interfaces within a source tree.";
	}
}
