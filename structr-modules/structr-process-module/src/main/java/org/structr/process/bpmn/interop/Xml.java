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
package org.structr.process.bpmn.interop;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Minimal namespace-aware DOM traversal helpers shared by the vendor {@link BpmnVendorAdapter}s.
 * Kept tiny and dependency-free so adapters stay self-contained and unit-testable against a DOM
 * fragment (no graph, no services).
 */
final class Xml {

	private Xml() {}

	/** First direct child element with the given local name (any namespace), or null. */
	static Element firstChildByLocalName(final Element parent, final String localName) {

		if (parent == null) {

			return null;
		}

		final NodeList children = parent.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {

			final Node child = children.item(i);

			if (child.getNodeType() == Node.ELEMENT_NODE && localName.equals(child.getLocalName())) {

				return (Element) child;
			}
		}

		return null;
	}

	/** First direct child element in namespace {@code ns} with the given local name, or null. */
	static Element firstChildNS(final Element parent, final String ns, final String localName) {

		if (parent == null) {

			return null;
		}

		final NodeList children = parent.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {

			final Node child = children.item(i);

			if (isElement(child, ns, localName)) {

				return (Element) child;
			}
		}

		return null;
	}

	/** All direct child elements in namespace {@code ns} with the given local name. */
	static List<Element> childrenNS(final Element parent, final String ns, final String localName) {
		return childrenAnyNS(parent, Set.of(ns), localName);
	}

	/** All direct child elements whose namespace is in {@code namespaces} with the given local name. */
	static List<Element> childrenAnyNS(final Element parent, final Set<String> namespaces, final String localName) {

		final List<Element> result = new ArrayList<>();
		if (parent == null) {

			return result;
		}

		final NodeList children = parent.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {

			final Node child = children.item(i);

			if (child.getNodeType() == Node.ELEMENT_NODE && localName.equals(child.getLocalName()) && namespaces.contains(child.getNamespaceURI())) {

				result.add((Element) child);
			}
		}

		return result;
	}

	private static boolean isElement(final Node node, final String ns, final String localName) {
		return node.getNodeType() == Node.ELEMENT_NODE && ns.equals(node.getNamespaceURI()) && localName.equals(node.getLocalName());
	}
}
