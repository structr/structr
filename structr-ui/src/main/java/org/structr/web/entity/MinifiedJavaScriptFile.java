/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.entity;

import com.google.common.collect.ImmutableList;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.SourceFile;
import java.io.IOException;
import java.util.logging.Logger;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.common.FileHelper;

public class MinifiedJavaScriptFile extends AbstractMinifiedFile {

	private static final Logger logger = Logger.getLogger(MinifiedJavaScriptFile.class.getName());

	public static final Property<CompilationLevel> optimizationLevel = new EnumProperty<>("optimizationLevel", CompilationLevel.class, CompilationLevel.WHITESPACE_ONLY);
	public static final Property<String> warnings = new StringProperty("warnings");
	public static final Property<String> errors = new StringProperty("errors");

	public static final View defaultView = new View(MinifiedJavaScriptFile.class, PropertyView.Public, minificationSources, optimizationLevel, warnings, errors);
	public static final View uiView      = new View(MinifiedJavaScriptFile.class, PropertyView.Ui, minificationSources, optimizationLevel, warnings, errors);

	@Override
	public void minify() throws FrameworkException, IOException {

		final String filename = getProperty(name);
		final Compiler compiler = new Compiler();
		final CompilerOptions options = new CompilerOptions();
		final SourceFile input = SourceFile.fromCode(filename, getConcatenatedSource());
		final CompilationLevel selectedLevel = getProperty(optimizationLevel);

		selectedLevel.setOptionsForCompilationLevel(options);

		compiler.compile(CommandLineRunner.getBuiltinExterns(options), ImmutableList.of(input), options);

		FileHelper.setFileData(this, compiler.toSource().getBytes(), null);

		// document warnings
		final StringBuilder warningsSB = new StringBuilder();
		for (JSError warning : compiler.getWarnings()) {
			warningsSB.append(warning.toString());
		}
		setProperty(warnings, warningsSB.toString());

		// document errors
		final StringBuilder errorsSB = new StringBuilder();
		for (JSError error : compiler.getErrors()) {
			errorsSB.append(error.toString());
		}
		setProperty(errors, errorsSB.toString());

	}

}
