/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.extraction.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.EndNode;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.extraction.ContentExtractionTraits;
import org.structr.extraction.processing.FigureContentWriter;
import org.structr.extraction.traits.properties.FigureImageProperty;

import java.util.Map;
import java.util.Set;

/**
 * A figure of a document (chart, diagram, table, picture), stored as an image of its own. It is an Image
 * (and so a File) with what figure extraction found out about the figure, and the document it is from.
 * Content extraction creates them when a configuration enables extractFigures; an application can also
 * store a result of documents.extractFigures() with $.create('DocumentFigure', figure), and can extend
 * the type in its own data model.
 *
 * The figure has ONE text: what extraction read in it (caption, labels, legends, table cells, OCR), until
 * an application writes another, e.g. what a vision model says about the image. The text is kept as a
 * chunk of the document, written whenever the figure changes, so the searches over chunks find it.
 */
public class DocumentFigureTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DOCUMENT_PROPERTY   = "document";
	public static final String PAGE_PROPERTY       = "page";
	public static final String POSITION_PROPERTY   = "position";
	public static final String LABEL_PROPERTY      = "label";
	public static final String CAPTION_PROPERTY    = "caption";
	public static final String SOURCE_PROPERTY     = "source";
	public static final String BBOX_PROPERTY       = "bbox";
	public static final String IMAGE_HASH_PROPERTY = "imageHash";
	public static final String MENTIONS_PROPERTY   = "mentions";
	public static final String IMAGE_PROPERTY      = "image";
	public static final String TEXT_PROPERTY       = "text";
	public static final String CONTENT_PROPERTY    = "content";

	public DocumentFigureTraitDefinition() {

		super(ContentExtractionTraits.DOCUMENT_FIGURE);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(final TraitsInstance traitsInstance) {

		return Map.of(

			OnCreation.class, new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final PropertyKey<String> nameKey = graphObject.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

					// a figure stored without a name is named after its label, e.g. "Figure 3.png"
					final String name = graphObject.getProperty(nameKey);
					if (name == null || name.isBlank()) {

						final String label = graphObject.getProperty(graphObject.getTraits().key(LABEL_PROPERTY));

						graphObject.setProperty(nameKey, (label != null ? label : "Figure") + ".png");
					}

					FigureContentWriter.write((NodeInterface) graphObject);
				}
			},

			OnModification.class, new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					// the chunk follows the text, page, label and document; unchanged, it is left alone
					FigureContentWriter.write((NodeInterface) graphObject);
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<NodeInterface> document = new StartNode(traitsInstance, DOCUMENT_PROPERTY, ContentExtractionTraits.FILE_HAS_FIGURE).description("The document the figure is from.");
		final Property<Integer> page           = new IntProperty(PAGE_PROPERTY).description("The page of the document the figure is on, from 1; null for a document without pages.");
		final Property<Integer> position       = new IntProperty(POSITION_PROPERTY).description("The position of the figure among the figures of its document, in reading order, from 0.");
		final Property<String> label           = new StringProperty(LABEL_PROPERTY).indexed().description("The label of the figure, e.g. \"Figure 3\" or \"Table 2\".");
		final Property<String> caption         = new StringProperty(CAPTION_PROPERTY).description("The caption of the figure.");
		final Property<String> source          = new StringProperty(SOURCE_PROPERTY).description("How the figure was found: region (a region of a PDF page), page (a whole PDF page), embedded (a picture in a document) or file (an image file).");
		final Property<Double[]> bbox          = new ArrayProperty<>(BBOX_PROPERTY, Double.class).description("Where the figure is on its PDF page: x, y, width and height in points from the top left corner.");
		final Property<String> imageHash       = new StringProperty(IMAGE_HASH_PROPERTY).indexed().description("SHA-256 of the rendered image, hex encoded: the same figure rendered again has the same hash.");
		final Property<String[]> mentions      = new ArrayProperty<>(MENTIONS_PROPERTY, String.class).description("The sentences of the document that refer to the figure.");
		final Property<String> text            = new StringProperty(TEXT_PROPERTY).description("The text of the figure: what extraction read in it (caption, labels, legends, table cells, OCR), or what an application wrote instead, e.g. an interpretation by a vision model. It is searchable as content of the document.");
		final Property<NodeInterface> content  = new EndNode(traitsInstance, CONTENT_PROPERTY, ContentExtractionTraits.FIGURE_CONTENT).description("The chunk of the document that holds the text of the figure; written by content extraction.");
		final Property<String> image           = new FigureImageProperty(IMAGE_PROPERTY).disableSerialization(true).description("Write only: the image as { data, mediaType } with base64 data, as documents.extractFigures() returns it, or as a base64 string; it becomes the content of the file.");

		return newSet(document, page, position, label, caption, source, bbox, imageHash, mentions, text, content, image);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(DOCUMENT_PROPERTY, PAGE_PROPERTY, POSITION_PROPERTY, LABEL_PROPERTY, CAPTION_PROPERTY, SOURCE_PROPERTY, TEXT_PROPERTY),
			PropertyView.Ui, newSet(DOCUMENT_PROPERTY, PAGE_PROPERTY, POSITION_PROPERTY, LABEL_PROPERTY, CAPTION_PROPERTY, SOURCE_PROPERTY, BBOX_PROPERTY, IMAGE_HASH_PROPERTY, MENTIONS_PROPERTY, TEXT_PROPERTY, CONTENT_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {

		return null;
	}
}
