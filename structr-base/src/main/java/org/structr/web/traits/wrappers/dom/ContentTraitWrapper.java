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
package org.structr.web.traits.wrappers.dom;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.web.entity.dom.Content;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;

/**
 * Represents a content node. This class implements the org.w3c.dom.Text interface.
 * All methods in the W3C Text interface are based on the raw database content.
 */
public class ContentTraitWrapper extends DOMNodeTraitWrapper implements Content {

	public ContentTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {

		super(traits, wrappedObject);
	}

	@Override public String getContent() {
		return wrappedObject.getProperty(traits.key(ContentTraitDefinition.CONTENT_PROPERTY));
	}

	@Override public void setContent(final String content) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ContentTraitDefinition.CONTENT_PROPERTY), content);
	}

	@Override public void setContentType(final String contentType) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), contentType);
	}

	@Override
	public final String getContentType() {

		return wrappedObject.getProperty(traits.key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY));
	}

	@Override
	public String getNodeValue() {

		try {

			return getData();

		} catch (FrameworkException e) {

			e.printStackTrace();
		}

		return null;
	}

	public void setData(final String data) throws FrameworkException {

		this.checkWriteAccess();

		try {

			this.setContent(data);

		} catch (FrameworkException fex) {

			throw new FrameworkException(422, fex.toString());

		}
	}

	public int getLength() {

		final String text = this.getContent();
		if (text != null) {

			return text.length();
		}

		return 0;
	}

	public String substringData(int offset, int count) throws FrameworkException {

		this.checkReadAccess();

		String text = this.getContent();
		if (text != null) {

			try {

				return text.substring(offset, offset + count);

			} catch (IndexOutOfBoundsException iobex) {

				throw new FrameworkException(422, INDEX_SIZE_ERR_MESSAGE);
			}
		}

		return "";
	}

	public void appendData(final String data) throws FrameworkException {

		this.checkWriteAccess();

		try {

			final String text = this.getContent();

			this.setContent(text.concat(data));

		} catch (FrameworkException fex) {

			throw new FrameworkException(422, fex.toString());

		}
	}

	public void insertData(final int offset, final String data) throws FrameworkException {

		this.checkWriteAccess();

		try {

			final String text       = this.getContent();
			final String leftPart   = text.substring(0, offset);
			final String rightPart  = text.substring(offset);
			String buf = leftPart +
				data +
				rightPart;

			// finally, set content to concatenated left, data and right parts
			this.setContent(buf);

		} catch (FrameworkException fex) {

			throw new FrameworkException(422, fex.toString());

		}
	}

	public void deleteData(final int offset, final int count) throws FrameworkException {

		this.checkWriteAccess();

		// finally, set content to concatenated left and right parts
		try {

			final String text      = this.getContent();
			final String leftPart  = text.substring(0, offset);
			final String rightPart = text.substring(offset + count);

			this.setContent(leftPart.concat(rightPart));

		} catch (FrameworkException fex) {

			throw new FrameworkException(422, fex.toString());

		}
	}

	public void replaceData(final int offset, final int count, final String data) throws FrameworkException {

		this.checkWriteAccess();

		// finally, set content to concatenated left and right parts
		try {

			final String text       = this.getContent();
			final String leftPart   = text.substring(0, offset);
			final String rightPart  = text.substring(offset + count);
			String buf = leftPart +
				data +
				rightPart;

			this.setContent(buf);

		} catch (FrameworkException fex) {

			throw new FrameworkException(422, fex.toString());

		}
	}

	public void setTextContent(final String value) throws FrameworkException {

		setData(value);
	}

	public String getData() throws FrameworkException {

		checkReadAccess();

		return getContent();
	}
}
