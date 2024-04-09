/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.schema.compiler;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.common.error.DiagnosticErrorToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.TransactionCommand;
import org.structr.module.JarConfigurationProvider;
import org.structr.schema.SourceFile;
import org.structr.schema.SourceLine;

import javax.tools.*;
import javax.tools.Diagnostic.Kind;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

/**
 *
 *
 */
public class NodeExtender {

	private static final Logger logger   = LoggerFactory.getLogger(NodeExtender.class.getName());

	private static final JavaCompiler compiler           = ToolProvider.getSystemJavaCompiler();
	private static final ClassFileManager fileManager    = new ClassFileManager(compiler.getStandardFileManager(null, null, null));
	private static final ClassLoader classLoader         = fileManager.getClassLoader(null);
	private static final Map<String, Class> classes      = new TreeMap<>();
	private static final Map<String, String> contentsMD5 = new HashMap<>();

	private List<SourceFile> sources     = null;
	private Set<String> fqcns            = null;
	private String initiatedBySessionId  = null;
	private boolean fullReload           = false;

	public NodeExtender(final String initiatedBySessionId, final boolean fullReload) {

		this.initiatedBySessionId = initiatedBySessionId;
		this.fullReload           = fullReload;
		this.sources              = new ArrayList<>();
		this.fqcns                = new LinkedHashSet<>();
	}

	public static ClassLoader getClassLoader() {
		return classLoader;
	}

	public static Class getClass(final String fqcn) {
		return classes.get(fqcn);
	}

	public static Map<String, Class> getClasses() {
		return classes;
	}

	public boolean addClass(final String className, final SourceFile sourceFile) throws ClassNotFoundException {

		if (className != null && sourceFile != null) {

			final String fqcn = getFQCNForClassname(className);
			fqcns.add(fqcn);

			// skip if not changed
			String oldMD5 = contentsMD5.get(fqcn);
			String newMD5 = md5Hex(sourceFile.getContent());

			if (!fullReload && newMD5.equals(oldMD5)) {
				return false;
			}

			sources.add(sourceFile);

			if (Settings.LogSchemaOutput.getValue()) {

				logger.info("######################################## {}", sourceFile.getName());

				int count = 0;

				for (final SourceLine line : sourceFile.getLines()) {
					logger.info(StringUtils.rightPad(++count + ": ", 6) + line);
				}
			}

			return true;
		}

		return false;
	}

	public synchronized Map<String, Class> compile(final ErrorBuffer errorBuffer) throws ClassNotFoundException {

		final Writer errorWriter     = new StringWriter();
		final List<Class> newClasses = new LinkedList<>();

		if (!sources.isEmpty()) {

			logger.info("Compiling {} dynamic entities...", sources.size());

			final long t0 = System.currentTimeMillis();

			Boolean success = compiler.getTask(errorWriter, fileManager, new Listener(errorBuffer), Arrays.asList("-g"), null, sources).call();

			logger.info("Compiling done in {} ms", System.currentTimeMillis() - t0);

			if (success) {

				// update MD5 hashes after compilation success
				for (final SourceFile sf : sources) {

					final String fqcn = getFQCNForClassname(sf.getClassName());

					contentsMD5.put(fqcn, md5Hex(sf.getContent()));
				}

				final ClassLoader loader = fileManager.getClassLoader(null);

				for (final String fqcn : fqcns) {

					try {

						newClasses.add(loader.loadClass(fqcn));

					} catch (Throwable t) {

						logger.warn("Unable to load dynamic entity {}: {}", new Object[] { fqcn, t.toString() });
						logger.warn("", t);

						success = false;
					}
				}

				for (final Class oldType : classes.values()) {
					StructrApp.getConfiguration().unregisterEntityType(oldType);
				}

				// clear classes map
				classes.clear();

				// add new classes to map
				for (final Class newType : newClasses) {
					classes.put(newType.getName(), newType);
				}

				// remove deleted classes (note: handle inner classes)
				fileManager.objects.entrySet().removeIf(entry -> !fqcns.contains(entry.getKey().split("\\$")[0]));
				contentsMD5.entrySet().removeIf(entry -> !fqcns.contains(entry.getKey()));

				logger.info("Successfully compiled {} dynamic entities: {}", new Object[] { sources.size(), sources.stream().map(f -> f.getName().replaceFirst("/", "")).collect(Collectors.joining(", ")) });

				final Map<String, Object> data = new LinkedHashMap();
				data.put("success", true);
				TransactionCommand.simpleBroadcast("SCHEMA_COMPILED", data, Predicate.allExcept(getInitiatedBySessionId()));

				Services.getInstance().setOverridingSchemaTypesAllowed(false);
			}
		}

		return classes;
	}

	public String getInitiatedBySessionId () {
		return initiatedBySessionId;
	}

	public void setInitiatedBySessionId (final String initiatedBySessionId) {
		this.initiatedBySessionId = initiatedBySessionId;
	}

	private String getFQCNForClassname (final String className) {
		return JarConfigurationProvider.DYNAMIC_TYPES_PACKAGE + "." + className;
	}

	private static class Listener implements DiagnosticListener<JavaFileObject> {

		private ErrorBuffer errorBuffer = null;

		public Listener(final ErrorBuffer errorBuffer) {
			this.errorBuffer = errorBuffer;
		}

		@Override
		public void report(Diagnostic<? extends JavaFileObject> diagnostic) {

			if (diagnostic.getKind().equals(Kind.ERROR)) {

				final int errorLineNumber = Long.valueOf(diagnostic.getLineNumber()).intValue();
				final SourceFile obj      = (SourceFile)diagnostic.getSource();
				String name               = obj.getName();

				final SourceFile sourceFile = (SourceFile)diagnostic.getSource();
				final List<SourceLine> code = sourceFile.getLines();

				SourceLine line = null;

				// count newlines before the errorLineNumber to target the correct SourceLine (which can be multiple lines in reality)
				int lineCount = 0;
				for (SourceLine sl : code) {

					if (lineCount < errorLineNumber) {
						lineCount += sl.getNumberOfLines();

						if (lineCount >= errorLineNumber) {
							line = sl;
						}
					}
				}

				final AbstractNode source   = (AbstractNode)line.getCodeSource();
				final int size              = code.size();

				if (source != null) {
					errorBuffer.add(new DiagnosticErrorToken(name, diagnostic, source.getClass().getSimpleName(), source.getUuid(), source.getName()));
				} else {
					errorBuffer.add(new DiagnosticErrorToken(name, diagnostic));
				}

				if (Settings.LogSchemaErrors.getValue()) {

					logger.error(diagnostic.getMessage(Locale.ENGLISH));

					if (source != null) {
						logger.error("code source: {} of type {} name {}", source.getUuid(), source.getClass().getSimpleName(), source.getName());
					}

					final int contextLines   = 3;
					final String[] codeLines = sourceFile.getContent().split("\n");

					for (int i = errorLineNumber - contextLines; i < errorLineNumber + contextLines; i++) {

						if (codeLines.length > i) {

							String prefix = "  ";

							if (i == errorLineNumber - 1) {
								prefix = "> ";
							}

							logger.error(prefix + StringUtils.leftPad("" + i, 5) + ": " + codeLines[i]);
						}
					}
				}
			}
		}

		private boolean inRange(final int index, final int size) {

			if (index < 0) {
				return false;
			}

			if (index >= size) {
				return false;
			}

			return true;
		}
	}
}
