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
package org.structr.web.entity;

import java.net.URI;
import org.structr.common.PropertyView;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonType;

public interface MinifiedJavaScriptFile extends AbstractMinifiedFile {

	static class Impl { static {

		final JsonSchema schema = SchemaService.getDynamicSchema();
		final JsonType type     = schema.addType("MinifiedJavaScriptFile");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/MinifiedJavaScriptFile"));
		type.setExtends(URI.create("#/definitions/AbstractMinifiedFile"));

		type.addEnumProperty("optimizationLevel", PropertyView.Public).setEnums("WHITESPACE_ONLY", "SIMPLE_OPTIMIZATIONS", "ADVANCED_OPTIMIZATIONS");
		type.addStringProperty("warnings", PropertyView.Public);
		type.addStringProperty("errors",   PropertyView.Public);
	}}

	String getOptimizationLevel();

	/*

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

			final File src = rel.getTargetNode();

			sourceList.add(SourceFile.fromCode(src.getProperty(File.name), FileUtils.readFileToString(src.getFileOnDisk())));

			// compact the relationships (if necessary)
			if (rel.getProperty(MinificationSource.position) != cnt) {
				rel.setProperties(securityContext, new PropertyMap(MinificationSource.position, cnt));
			}
			cnt++;
		}

		return sourceList;
	}
	*/
}
