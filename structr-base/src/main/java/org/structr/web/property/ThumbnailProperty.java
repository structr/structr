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
package org.structr.web.property;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.Predicate;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.AbstractReadOnlyProperty;
import org.structr.core.property.Property;
import org.structr.web.entity.File;
import org.structr.web.entity.Image;

import java.util.Map;

/**
 * A property that automatically creates a thumbnail for an image.
 */
public class ThumbnailProperty extends AbstractReadOnlyProperty<NodeInterface> {

	private int width    = 0;
	private int height   = 0;
	private boolean crop = false;


	public ThumbnailProperty(final String name) {

		super(name);

		this.unvalidated = true;

	}

	@Override
	public NodeInterface getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public NodeInterface getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<GraphObject> predicate) {

		if (obj == null) {
			return null;
		}

		if (obj.is("File") && obj.as(File.class).isTemplate()) {

			return null;

		} else if (obj.is("Image") && obj.as(Image.class).isThumbnail()) {

			return null;
		}

		final Image tn = obj.as(Image.class).getScaledImage(width, height, crop);
		if (tn != null) {

			return tn.getWrappedNode();
		}

		return null;
	}

	@Override
	public String relatedType() {
		return "Image";
	}

	@Override
	public Class valueType() {
		return NodeInterface.class;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public SortType getSortType() {
		return null;
	}

	@Override
	public Property<NodeInterface> format(final String format) {

		if (StringUtils.isNotBlank(format) && format.contains(",")) {

			final String[] parts = format.split("[, ]+");

			if (parts.length >= 1) {

				width    = Integer.parseInt(parts[0].trim());
				height   = Integer.parseInt(parts[1].trim());
			}

			if (parts.length == 3) {

				crop = Boolean.parseBoolean(parts[2].trim());
			}
		}

		return this;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public boolean getCrop() {
		return crop;
	}

	@Override
	public boolean isIndexed() {
		return false;
	}

	@Override
	public boolean isPassivelyIndexed() {
		return false;
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}
}
