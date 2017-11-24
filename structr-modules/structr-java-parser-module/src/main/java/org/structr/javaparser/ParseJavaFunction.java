/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class ParseJavaFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_PARSE_JAVA = "Usage: ${parse_java(javaSource)}";

	@Override
	public String getName() {
		return "parse_java()";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			if (!(arrayHasLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof String)) {

				return null;
			}

			try {
			
				// parse it
				CompilationUnit cu = JavaParser.parse((String) sources[0]);

				return cu;
				
			} catch (final ParseProblemException ex) {
				
				logException(caller, ex, sources);
				
			}

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());

		}
		
		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_PARSE_JAVA;
	}

	@Override
	public String shortDescription() {
		return "Parses the given string as Java file into an object representation of the Java model declared by the given source code.";
	}
}
