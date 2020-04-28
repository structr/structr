/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import com.github.javaparser.ParseProblemException;
import com.google.gson.GsonBuilder;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.schema.action.ActionContext;

public class ParseJavaFunction extends ParserModuleFunction {

	public static final String ERROR_MESSAGE_PARSE_JAVA = "Usage: ${parse_java(javaSource)}";

	@Override
	public String getName() {
		return "parse_java";
	}

	@Override
	public String getSignature() {
		return "javaCode";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof String) {

				final SecurityContext securityContext = ctx.getSecurityContext();
				final App app                         = StructrApp.getInstance(securityContext);

				// Parse string as Java code
				final String resultJson = new GsonBuilder().setPrettyPrinting().create().toJson(new JavaParserModule(app).parse((String) sources[0]).get());

				return resultJson;
			}

			return null;

		} catch (final ParseProblemException ex) {

			logException(caller, ex, sources);
			return "";

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
		return ERROR_MESSAGE_PARSE_JAVA;
	}

	@Override
	public String shortDescription() {
		return "Parses the given string as Java file into an JSON representation of the Java model declared by the given source code.";
	}
}
