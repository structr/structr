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
package org.structr.web.common;

import java.util.LinkedList;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.dom.DOMNode;

import java.util.List;
import java.util.Queue;

/**
 */
public class SlotData {

	private final Queue<PropertyMap> dataAttributes = new LinkedList<>();
	private final Queue<PropertyMap> attributes     = new LinkedList<>();
	private final Queue<List<DOMNode>> elements     = new LinkedList<>();
	private String id                               = null;

	public SlotData(final String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public List<DOMNode> getNextSlotElements() {
		return elements.poll();
	}

	public PropertyMap getNextSlotAttributes() {
		return attributes.poll();
	}

	public PropertyMap getNextDataAttributes() {
		return dataAttributes.poll();
	}

	public void putSlotAttributes(final PropertyMap attributes) {
		this.attributes.add(attributes);
	}

	public void putDataAttributes(final PropertyMap attributes) {
		this.dataAttributes.add(attributes);
	}

	public void putSlotElements(final List<DOMNode> children) {
		this.elements.add(children);
	}
}
