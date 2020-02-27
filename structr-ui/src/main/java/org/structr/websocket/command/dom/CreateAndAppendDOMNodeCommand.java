/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.websocket.command.dom;

import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.command.CreateComponentCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

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
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final Map<String, Object> nodeData   = webSocketData.getNodeData();
		final String parentId                = (String) nodeData.get("parentId");
		final String childContent            = (String) nodeData.get("childContent");
		final String pageId                  = webSocketData.getPageId();

		Boolean inheritVisibilityFlags = (Boolean) nodeData.get("inheritVisibilityFlags");

		if (inheritVisibilityFlags == null) {
			inheritVisibilityFlags = false;
		}

		// remove configuration elements from the nodeData so we don't set it on the node
		nodeData.remove("parentId");
		nodeData.remove("inheritVisibilityFlags");

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

			final Document document = getPage(pageId);
			if (document != null) {

				final String tagName = (String) nodeData.get("tagName");

				nodeData.remove("tagName");

				try (final Tx tx = StructrApp.getInstance(getWebSocket().getSecurityContext()).tx(true, true, true)) {

					final boolean isShadowPage = document.equals(CreateComponentCommand.getOrCreateHiddenDocument());
					final boolean isTemplate   = (parentNode instanceof Template);

					if (isShadowPage && isTemplate && parentNode.getParent() == null) {
						getWebSocket().send(MessageBuilder.status().code(422).message("Appending children to root-level shared component Templates is not allowed").build(), true);
						return;
					}

					if (!isShadowPage && !isTemplate && parentNode.isSynced()) {
						getWebSocket().send(MessageBuilder.status().code(422).message("Appending children to shared components (that are not Templates) in the pages tree is not allowed").build(), true);
						return;
					}

					DOMNode newNode;

					if (tagName != null && "comment".equals(tagName)) {

						newNode = (DOMNode) document.createComment("#comment");

					} else if (tagName != null && "template".equals(tagName)) {

						newNode = (DOMNode) document.createTextNode("#template");

						try {

							newNode.unlockSystemPropertiesOnce();

							newNode.setProperties(newNode.getSecurityContext(), new PropertyMap(NodeInterface.type, Template.class.getSimpleName()));

						} catch (FrameworkException fex) {

							logger.warn("Unable to set type of node {} to Template: {}", new Object[] { newNode.getUuid(), fex.getMessage() } );

						}

					} else if (tagName != null && !tagName.isEmpty()) {

						if ("custom".equals(tagName)) {

							try {

								final Class entityClass = StructrApp.getConfiguration().getNodeEntityClass("DOMElement");

								// experimental: create DOM element with literal tag
								newNode = (DOMElement) StructrApp.getInstance(webSocket.getSecurityContext()).create(entityClass,
									new NodeAttribute(StructrApp.key(DOMElement.class, "tag"),          "custom"),
									new NodeAttribute(StructrApp.key(DOMElement.class, "hideOnDetail"), false),
									new NodeAttribute(StructrApp.key(DOMElement.class, "hideOnIndex"),  false)
								);

								if (newNode != null && document != null) {
									newNode.doAdopt((Page)document);
								}

							} catch (FrameworkException fex) {

								// abort
								getWebSocket().send(MessageBuilder.status().code(422).message(fex.getMessage()).build(), true);
								return;
							}

						} else {

							newNode = (DOMNode) document.createElement(tagName);
						}

					} else {

						newNode = (DOMNode) document.createTextNode("#text");
					}

					// Instantiate node again to get correct class
					newNode = getDOMNode(newNode.getUuid());

					// append new node to parent
					if (newNode != null) {

						parentNode.appendChild(newNode);

						for (Entry entry : nodeData.entrySet()) {

							final String key = (String) entry.getKey();
							final Object val = entry.getValue();

							PropertyKey propertyKey = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(newNode.getClass(), key);
							if (propertyKey != null) {

								try {
									Object convertedValue = val;

									PropertyConverter inputConverter = propertyKey.inputConverter(SecurityContext.getSuperUserInstance());
									if (inputConverter != null) {

										convertedValue = inputConverter.convert(val);
									}

									newNode.setProperties(newNode.getSecurityContext(), new PropertyMap(propertyKey, convertedValue));

								} catch (FrameworkException fex) {

									logger.warn("Unable to set node property {} of node {} to {}: {}", new Object[] { propertyKey, newNode.getUuid(), val, fex.getMessage() } );

								}
							}
						}

						PropertyMap visibilityFlags = null;
						if (inheritVisibilityFlags) {

							visibilityFlags = new PropertyMap();
							visibilityFlags.put(DOMNode.visibleToAuthenticatedUsers, parentNode.getProperty(DOMNode.visibleToAuthenticatedUsers));
							visibilityFlags.put(DOMNode.visibleToPublicUsers, parentNode.getProperty(DOMNode.visibleToPublicUsers));

							try {
								newNode.setProperties(newNode.getSecurityContext(), visibilityFlags);
							} catch (FrameworkException fex) {

								logger.warn("Unable to inherit visibility flags for node {} from parent node {}", newNode, parentNode );

							}
						}

						// create a child text node if content is given
						if (StringUtils.isNotBlank(childContent)) {

							final DOMNode childNode = (DOMNode)document.createTextNode(childContent);

							newNode.appendChild(childNode);

							if (inheritVisibilityFlags) {

								try {
									childNode.setProperties(childNode.getSecurityContext(), visibilityFlags);
								} catch (FrameworkException fex) {

									logger.warn("Unable to inherit visibility flags for node {} from parent node {}", childNode, newNode );

								}
							}
						}
					}

					tx.success();

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
}
