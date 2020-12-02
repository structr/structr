/*
 * Copyright (C) 2010-2020 Structr GmbH
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

import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
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

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

/**
 *
 *
 */
public class NodeExtender {

	private static final Logger logger   = LoggerFactory.getLogger(NodeExtender.class.getName());

	private static final JavaCompiler compiler       = ToolProvider.getSystemJavaCompiler();
	private static final ClassFileManager fileManager = new ClassFileManager(compiler.getStandardFileManager(null, null, null));
	private static final ClassLoader classLoader     = fileManager.getClassLoader(null);
	private static final Map<String, Class> classes   = new TreeMap<>();
	private static final Map<String, String> contentsMD5 = new HashMap<>();

	private List<SourceFile> sources     = null;
	private Set<String> fqcns            = null;
	private String initiatedBySessionId  = null;

	// used to detect deleted classes
	private Set<String> retainedFqcns = new HashSet<>();

	public NodeExtender(final String initiatedBySessionId) {

		this.initiatedBySessionId = initiatedBySessionId;
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

	public void addClass(final String className, final SourceFile sourceFile) throws ClassNotFoundException {

		if (className != null && sourceFile != null) {

			final String fqcn = JarConfigurationProvider.DYNAMIC_TYPES_PACKAGE + "." + className;

			retainedFqcns.add(fqcn);

			// continue only if changed
			String oldMD5 = contentsMD5.get(fqcn);
			String newMD5 = md5Hex(sourceFile.getContent());
			if(newMD5.equals(oldMD5)){
				return;
			}
			contentsMD5.put(fqcn, newMD5);

			sources.add(sourceFile);
			fqcns.add(fqcn);

			if (Settings.LogSchemaOutput.getValue()) {

				logger.info("######################################## {}", sourceFile.getName());

				int count = 0;

				for (final SourceLine line : sourceFile.getLines()) {
					logger.info(StringUtils.rightPad(++count + ": ", 6) + line);
				}
			}
		}
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

				// add new classes to map
				for (final Class newType : newClasses) {
					classes.put(newType.getName(), newType);
				}

				logger.info("Successfully compiled {} dynamic entities: {}", new Object[] { sources.size(), sources.stream().map(f -> f.getName().replaceFirst("/", "")).collect(Collectors.joining(", ")) });

				final Map<String, Object> data = new LinkedHashMap();
				data.put("success", true);
				TransactionCommand.simpleBroadcast("SCHEMA_COMPILED", data, Predicate.allExcept(getInitiatedBySessionId()));

				Services.getInstance().setOverridingSchemaTypesAllowed(false);
			}
		}

		return classes;
	}

	public void prune() {
		fileManager.objects.entrySet().removeIf(entry -> !retainedFqcns.contains(entry.getKey()));
		classes.entrySet().removeIf(entry -> !retainedFqcns.contains(entry.getKey()));
		contentsMD5.entrySet().removeIf(entry -> !retainedFqcns.contains(entry.getKey()));
	}

	public String getInitiatedBySessionId () {
		return initiatedBySessionId;
	}

	public void setInitiatedBySessionId (final String initiatedBySessionId) {
		this.initiatedBySessionId = initiatedBySessionId;
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

				errorBuffer.add(new DiagnosticErrorToken(name, diagnostic));

				if (Settings.LogSchemaErrors.getValue()) {

					final SourceFile sourceFile = (SourceFile)diagnostic.getSource();
					final List<SourceLine> code = sourceFile.getLines();
					final SourceLine line       = code.get(errorLineNumber - 1);
					final AbstractNode source   = (AbstractNode)line.getCodeSource();
					final int size              = code.size();

					logger.error(diagnostic.getMessage(Locale.ENGLISH));

					if (source != null) {
						logger.error("code source: {} of type {} name {}", source.getUuid(), source.getClass().getSimpleName(), source.getName());
					}

					for (int i = errorLineNumber - 3; i < errorLineNumber + 3; i++) {

						if (inRange(i, size)) {

							String prefix = "  ";

							if (i == errorLineNumber-1) {
								prefix = "> ";
							}

							logger.error(prefix + StringUtils.leftPad("" + i, 5) + ": " + code.get(i));
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
