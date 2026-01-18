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
package org.structr.odf.traits.wrappers;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.odf.entity.ODFExporter;
import org.structr.odf.traits.definitions.ODFExporterTraitDefinition;
import org.structr.transform.VirtualType;
import org.structr.web.entity.File;

/**
 * Base class for ODF exporter
 */
public class ODFExporterTraitWrapper extends AbstractNodeTraitWrapper implements ODFExporter {

	public ODFExporterTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public File getDocumentTemplate() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(ODFExporterTraitDefinition.DOCUMENT_TEMPLATE_PROPERTY));
		if (node != null) {

			return node.as(File.class);
		}

		return null;
	}

	public File getResultDocument() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(ODFExporterTraitDefinition.RESULT_DOCUMENT_PROPERTY));
		if (node != null) {

			return node.as(File.class);
		}

		return null;
	}

	public void setResultDocument(final File resultDocument) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ODFExporterTraitDefinition.RESULT_DOCUMENT_PROPERTY), resultDocument);
	}

	public VirtualType getTransformationProvider() {

		final NodeInterface node = getProperty(traits.key(ODFExporterTraitDefinition.TRANSFORMATION_PROVIDER_PROPERTY));
		if (node != null) {

			return node.as(VirtualType.class);
		}

		return null;
	}
}
