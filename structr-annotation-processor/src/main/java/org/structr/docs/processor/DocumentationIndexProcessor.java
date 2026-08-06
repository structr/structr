/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.docs.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compile-time annotation processor that records the fully-qualified names of all
 * types annotated with {@code @Documentation} or {@code @Documentations} into the
 * resource file {@value #INDEX_RESOURCE}.
 *
 * At runtime Structr reads these per-module index files (see JarConfigurationProvider)
 * instead of scanning the whole class path for annotated classes. Reading a resource
 * via the class loader works identically on the class path and on the module path,
 * which is why this replaces the previous class-path scan.
 *
 * Only type-level annotations are indexed because that is all the runtime consumer
 * (the documentation ontology) reads.
 */
@SupportedAnnotationTypes({"org.structr.docs.Documentation", "org.structr.docs.Documentations"})
public class DocumentationIndexProcessor extends AbstractProcessor {

	public static final String INDEX_RESOURCE = "META-INF/structr/documented-classes";

	private final Set<String> documentedTypes = new TreeSet<>();

	@Override
	public SourceVersion getSupportedSourceVersion() {

		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {

		for (final TypeElement annotation : annotations) {

			for (final Element element : roundEnv.getElementsAnnotatedWith(annotation)) {

				if (isType(element)) {

					documentedTypes.add(((TypeElement) element).getQualifiedName().toString());
				}
			}
		}

		if (roundEnv.processingOver() && !documentedTypes.isEmpty()) {

			writeIndex();
		}

		// do not claim the annotations: other processors may want to see them too

		return false;
	}

	private boolean isType(final Element element) {

		final ElementKind kind = element.getKind();

		return kind == ElementKind.CLASS || kind == ElementKind.INTERFACE || kind == ElementKind.ENUM || kind == ElementKind.RECORD || kind == ElementKind.ANNOTATION_TYPE;
	}

	private void writeIndex() {

		try {

			final FileObject indexFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", INDEX_RESOURCE);

			try (final Writer writer = indexFile.openWriter()) {

				for (final String type : documentedTypes) {

					writer.write(type);
					writer.write('\n');
				}
			}

		} catch (IOException ioex) {

			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unable to write " + INDEX_RESOURCE + ": " + ioex.getMessage());
		}
	}
}
