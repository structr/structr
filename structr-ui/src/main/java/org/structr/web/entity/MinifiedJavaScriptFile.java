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

import com.google.javascript.jscomp.BasicErrorManager;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.parsing.parser.util.format.SimpleFormat;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.ModificationEvent;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.relation.MinificationSource;

public class MinifiedJavaScriptFile extends AbstractMinifiedFile {

	private static final Logger logger = LoggerFactory.getLogger(MinifiedJavaScriptFile.class.getName());

	public static final Property<CompilationLevel> optimizationLevel = new EnumProperty<>("optimizationLevel", CompilationLevel.class, CompilationLevel.WHITESPACE_ONLY);
	public static final Property<String> warnings                    = new StringProperty("warnings");
	public static final Property<String> errors                      = new StringProperty("errors");

	public static final View defaultView = new View(MinifiedJavaScriptFile.class, PropertyView.Public, minificationSources, optimizationLevel, warnings, errors);
	public static final View uiView      = new View(MinifiedJavaScriptFile.class, PropertyView.Ui, minificationSources, optimizationLevel, warnings, errors);

	@Override
	public boolean shouldModificationTriggerMinifcation(ModificationEvent modState) {

		return modState.getModifiedProperties().containsKey(MinifiedJavaScriptFile.optimizationLevel);

	}

	@Override
	public void minify() throws FrameworkException, IOException {

		logger.info("Running minify: {}", this.getUuid());

		final Compiler compiler = new Compiler();
		final CompilerOptions options = new CompilerOptions();
		final CompilationLevel selectedLevel = getProperty(optimizationLevel);
		selectedLevel.setOptionsForCompilationLevel(options);

		compiler.setErrorManager(new BasicErrorManager() {
			@Override
			public void println(CheckLevel level, JSError error) {
//				if (level != CheckLevel.OFF) {
//					logger.log((level == CheckLevel.ERROR) ? Level.SEVERE : Level.WARNING, error.toString());
//				}
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
		compiler.compile(CommandLineRunner.getBuiltinExterns(options), getSourceFileList(), options);

		FileHelper.setFileData(this, compiler.toSource().getBytes(), getProperty(contentType));

		final PropertyMap changedProperties = new PropertyMap();
		changedProperties.put(warnings, StringUtils.join(compiler.getWarnings(), System.lineSeparator()));
		changedProperties.put(errors, StringUtils.join(compiler.getErrors(), System.lineSeparator()));
		setProperties(securityContext, changedProperties);

	}

	private ArrayList<SourceFile> getSourceFileList() throws FrameworkException, IOException {

		ArrayList<SourceFile> sourceList = new ArrayList();

		int cnt = 0;
		for (MinificationSource rel : getSortedRelationships()) {

			final FileBase src = rel.getTargetNode();

			sourceList.add(SourceFile.fromCode(src.getProperty(FileBase.name), FileUtils.readFileToString(src.getFileOnDisk())));

			// compact the relationships (if necessary)
			if (rel.getProperty(MinificationSource.position) != cnt) {
				rel.setProperties(securityContext, new PropertyMap(MinificationSource.position, cnt));
			}
			cnt++;
		}

		return sourceList;
	}
}
