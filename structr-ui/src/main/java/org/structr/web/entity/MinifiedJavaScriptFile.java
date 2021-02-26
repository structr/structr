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
package org.structr.web.entity;

import com.google.javascript.jscomp.BasicErrorManager;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.parsing.parser.util.format.SimpleFormat;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationEvent;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.SchemaService;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.web.common.FileHelper;

public interface MinifiedJavaScriptFile extends AbstractMinifiedFile {

	static class Impl { static {

		final JsonSchema schema = SchemaService.getDynamicSchema();
		final JsonType type     = schema.addType("MinifiedJavaScriptFile");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/MinifiedJavaScriptFile"));
		type.setExtends(URI.create("#/definitions/AbstractMinifiedFile"));
		type.setCategory("ui");

		type.addEnumProperty("optimizationLevel", PropertyView.Public, PropertyView.Ui).setEnums("WHITESPACE_ONLY", "SIMPLE_OPTIMIZATIONS", "ADVANCED_OPTIMIZATIONS").setDefaultValue("WHITESPACE_ONLY");
		type.addStringProperty("warnings",        PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("errors",          PropertyView.Public, PropertyView.Ui);

		type.overrideMethod("getOptimizationLevel",                 false, "return getProperty(optimizationLevelProperty).name();");
		type.overrideMethod("shouldModificationTriggerMinifcation", false, "return " + MinifiedJavaScriptFile.class.getName() + ".shouldModificationTriggerMinifcation(this, arg0);");

		type.addMethod("minify")
			.addParameter("ctx", SecurityContext.class.getName())
			.setSource(MinifiedJavaScriptFile.class.getName() + ".minify(this, ctx);")
			.addException(FrameworkException.class.getName())
			.addException(IOException.class.getName())
			.setDoExport(true);
	}}

	String getOptimizationLevel();

	static boolean shouldModificationTriggerMinifcation(final MinifiedJavaScriptFile thisFile, final ModificationEvent modState) {
		return modState.getModifiedProperties().containsKey(StructrApp.key(MinifiedJavaScriptFile.class, "optimizationLevel"));
	}

	static void minify(final MinifiedJavaScriptFile thisFile, final SecurityContext securityContext) throws FrameworkException, IOException {

		final Logger logger = LoggerFactory.getLogger(MinifiedJavaScriptFile.class);

		logger.info("Running minification of MinifiedJavaScriptFile: {}", thisFile.getUuid());

		final com.google.javascript.jscomp.Compiler compiler = new com.google.javascript.jscomp.Compiler();
		final CompilerOptions options                        = new CompilerOptions();
		final CompilationLevel selectedLevel                 = CompilationLevel.valueOf(thisFile.getOptimizationLevel());

		selectedLevel.setOptionsForCompilationLevel(options);

		compiler.setErrorManager(new BasicErrorManager() {

			@Override
			public void println(final CheckLevel level, final JSError error) {
			}

			@Override
			protected void printSummary() {
				if (getTypedPercent() > 0) {
					if (getErrorCount() + getWarningCount() == 0) {
						logger.info(SimpleFormat.format("%d error(s), %d warning(s), %.1f%% typed", getErrorCount(), getWarningCount(), getTypedPercent()));
					} else {
						logger.warn(SimpleFormat.format("%d error(s), %d warning(s), %.1f%% typed", getErrorCount(), getWarningCount(), getTypedPercent()));
					}
				} else if (getErrorCount() + getWarningCount() > 0) {
					logger.warn(SimpleFormat.format("%d error(s), %d warning(s)", getErrorCount(), getWarningCount()));
				}
			}
		});

		compiler.compile(CommandLineRunner.getBuiltinExterns(options.getEnvironment()), MinifiedJavaScriptFile.getSourceFileList(thisFile), options);

		FileHelper.setFileData(thisFile, compiler.toSource().getBytes(), thisFile.getContentType());

		final PropertyMap changedProperties = new PropertyMap();

		changedProperties.put(StructrApp.key(MinifiedJavaScriptFile.class, "warnings"), StringUtils.join(compiler.getWarnings(), System.lineSeparator()));
		changedProperties.put(StructrApp.key(MinifiedJavaScriptFile.class, "errors"),   StringUtils.join(compiler.getErrors(), System.lineSeparator()));

		thisFile.setProperties(securityContext, changedProperties);
	}

	static ArrayList<SourceFile> getSourceFileList(final MinifiedJavaScriptFile thisFile) throws FrameworkException, IOException {

		final Class<Relation> relType          = StructrApp.getConfiguration().getRelationshipEntityClass("AbstractMinifiedFileMINIFICATIONFile");
		final PropertyKey<Integer> key         = StructrApp.key(relType, "position");
		final ArrayList<SourceFile> sourceList = new ArrayList();

		int cnt = 0;

		for (Relation rel : AbstractMinifiedFile.getSortedMinificationRelationships(thisFile)) {

			final File src = (File)rel.getTargetNode();

			sourceList.add(SourceFile.fromCode(src.getProperty(File.name), FileUtils.readFileToString(src.getFileOnDisk(), "utf-8")));

			// compact the relationships (if necessary)
			if (rel.getProperty(key) != cnt) {
				rel.setProperty(key, cnt);
			}
			cnt++;
		}

		return sourceList;
	}
}
