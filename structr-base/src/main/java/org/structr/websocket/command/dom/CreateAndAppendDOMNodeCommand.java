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
package org.structr.websocket.command.dom;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.dom.DOMElementTraitDefinition;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.command.CreateComponentCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;

import java.util.Map;
import java.util.Map.Entry;

/**
 *
 *
 *
 */
public class CreateAndAppendDOMNodeCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(CreateAndAppendDOMNodeCommand.class.getName());

	static {

		StructrWebSocket.addCommand(CreateAndAppendDOMNodeCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(true);

		final Map<String, Object> nodeData   = webSocketData.getNodeData();
		final String tagName                 = (String) nodeData.get("tagName");
		final String parentId                = (String) nodeData.remove("parentId");
		final String childContent            = (String) nodeData.get("childContent");
		final String pageId                  = webSocketData.getPageId();
		final Boolean inheritVisibilityFlags = (Boolean) nodeData.getOrDefault("inheritVisibilityFlags", false);
		final Boolean inheritGrantees        = (Boolean) nodeData.getOrDefault("inheritGrantees", false);

		// remove configuration elements from the nodeData so we don't set it on the node
		nodeData.remove("tagName");
		nodeData.remove("parentId");
		nodeData.remove("childContent");
		nodeData.remove("inheritVisibilityFlags");
		nodeData.remove("inheritGrantees");

		if (pageId != null) {

			// check for parent ID before creating any nodes
			if (parentId == null) {

				getWebSocket().send(MessageBuilder.status().code(422).message("Cannot add node without parentId").build(), true);
				return;
			}

			// check if parent node with given ID exists
			final DOMNode parentNode = getDOMNode(parentId);
			if (parentNode == null) {

				getWebSocket().send(MessageBuilder.status().code(404).message("Parent node not found").build(), true);
				return;
			}

			final Page document = getPage(pageId);
			if (document != null) {

				try (final Tx tx = StructrApp.getInstance(getWebSocket().getSecurityContext()).tx(true, true, true)) {

					tx.prefetchHint("Websocket CreateAndAppendOMNodeCommand");

					final boolean isShadowPage = document.equals(CreateComponentCommand.getOrCreateHiddenDocument());
					final boolean isTemplate   = (parentNode.is(StructrTraits.TEMPLATE));

					if (isShadowPage && isTemplate && parentNode.getParent() == null) {
						getWebSocket().send(MessageBuilder.status().code(422).message("Appending children to root-level shared component Templates is not allowed").build(), true);
						return;
					}

					if (!isShadowPage && !isTemplate && parentNode.isSynced()) {
						getWebSocket().send(MessageBuilder.status().code(422).message("Appending children to shared components (that are not Templates) in the pages tree is not allowed").build(), true);
						return;
					}

					DOMNode newNode = CreateAndAppendDOMNodeCommand.createNewNode(getWebSocket(), tagName, document);
					if (newNode == null) {
						return;
					}

					// Instantiate node again to get correct class
					newNode = getDOMNode(newNode.getUuid());

					// append new node to parent
					if (newNode != null) {

						parentNode.appendChild(newNode);

						copyNodeData(nodeData, newNode);

						if (inheritVisibilityFlags) {

							copyVisibilityFlags(parentNode, newNode);
						}

						if (inheritGrantees) {

							copyGrantees(parentNode, newNode);
						}

						// create a child text node if content is given
						if (StringUtils.isNotBlank(childContent)) {

							final DOMNode childNode = document.createTextNode(childContent);

							newNode.appendChild(childNode);

							if (inheritVisibilityFlags) {

								copyVisibilityFlags(parentNode, childNode);
							}

							if (inheritGrantees) {

								copyGrantees(parentNode, childNode);
							}
						}
					}

					tx.success();

					TransactionCommand.registerNodeCallback(newNode, callback);

					// send success
					getWebSocket().send(webSocketData, true);

				} catch (FrameworkException fex) {

					getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.toString()).jsonErrorObject(fex.toJSON()).callback(webSocketData.getCallback()).build(), true);

				} catch (DOMException dex) {

					// send DOM exception
					getWebSocket().send(MessageBuilder.status().code(422).message(dex.getMessage()).build(), true);
				}

			} else {

				getWebSocket().send(MessageBuilder.status().code(404).message("Page not found").build(), true);
			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot create node without pageId").build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "CREATE_AND_APPEND_DOM_NODE";
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	public void copyNodeData(final Map<String, Object> nodeData, final DOMNode targetNode) {

		for (Entry entry : nodeData.entrySet()) {

			final String key = (String) entry.getKey();
			final Object val = entry.getValue();

			PropertyKey propertyKey = targetNode.getTraits().key(key);
			if (propertyKey != null) {

				try {

					Object convertedValue = val;

					PropertyConverter inputConverter = propertyKey.inputConverter(SecurityContext.getSuperUserInstance(), false);
					if (inputConverter != null) {

						convertedValue = inputConverter.convert(val);
					}

					targetNode.setProperties(targetNode.getSecurityContext(), new PropertyMap(propertyKey, convertedValue));

				} catch (FrameworkException fex) {

					logger.warn("Unable to set node property {} of node {} to {}: {}", new Object[] { propertyKey, targetNode.getUuid(), val, fex.getMessage() } );
				}
			}
		}
	}

	public void copyVisibilityFlags(final DOMNode sourceNode, final DOMNode targetNode) {

		final PropertyMap visibilityFlags = new PropertyMap();
		final Traits traits               = Traits.of(StructrTraits.DOM_NODE);

		visibilityFlags.put(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), sourceNode.isVisibleToAuthenticatedUsers());
		visibilityFlags.put(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY),        sourceNode.isVisibleToPublicUsers());

		try {

			targetNode.setProperties(targetNode.getSecurityContext(), visibilityFlags);

		} catch (FrameworkException fex) {

			logger.warn("Unable to inherit visibility flags for node {} from parent node {}", targetNode, sourceNode);
		}
	}

	public void copyGrantees(final DOMNode sourceNode, final DOMNode targetNode) {

		try {

			sourceNode.copyPermissionsTo(targetNode.getSecurityContext(), targetNode, true);

		} catch (FrameworkException fex) {

			logger.warn("Unable to inherit grantees for node {} from parent node {}", targetNode, sourceNode);
		}
	}

	// ----- public static methods -----
	public static DOMNode createNewNode(final StructrWebSocket webSocket, final String tagName, final Page document) throws FrameworkException {

		DOMNode newNode;

		if (tagName != null) {

			switch (tagName) {

				case "#comment":
					newNode = document.createComment("#comment");
					break;

				case "#content":
					// maybe this is unnecessary..
					newNode = document.createTextNode("#text");
					break;

				case "#template":
					newNode = document.createTemplate("#template");
					break;

				case "custom":
					try {

						// experimental: create DOM element with literal tag
						newNode = StructrApp.getInstance(webSocket.getSecurityContext()).create(StructrTraits.DOM_ELEMENT,
							new NodeAttribute(Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition.TAG_PROPERTY), "custom")
						).as(DOMElement.class);

						if (newNode != null && document != null) {
							newNode.doAdopt(document);
						}

					} catch (FrameworkException fex) {

						// abort
						webSocket.send(MessageBuilder.status().code(422).message(fex.getMessage()).build(), true);
						return null;
					}
					break;

				default:
					newNode = document.createElement(tagName);
					break;

			}

		} else {

			newNode = document.createTextNode("#text");
		}

		return newNode;
	}
}
