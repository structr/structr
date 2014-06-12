/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.CaseHelper;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.Syncable;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.GraphDataSource;
import org.structr.web.common.HtmlProperty;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.datasource.CypherGraphDataSource;
import org.structr.web.datasource.IdRequestParameterGraphDataSource;
import org.structr.web.datasource.NodeGraphDataSource;
import org.structr.web.datasource.RestDataSource;
import org.structr.web.datasource.XPathGraphDataSource;
import org.structr.web.entity.dom.relationship.DOMChildren;
import org.structr.web.entity.html.Body;
import org.structr.web.entity.relation.PageLink;
import org.structr.web.entity.relation.RenderNode;
import org.structr.web.entity.relation.Sync;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;

//~--- classes ----------------------------------------------------------------
/**
 *
 * @author Axel Morgner
 * @author Christian Morgner
 */
public class DOMElement extends DOMNode implements Element, NamedNodeMap {

	private static final Logger logger = Logger.getLogger(DOMElement.class.getName());
	private static final int HtmlPrefixLength = PropertyView.Html.length();

	public static final long RENDER_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
	private static final String STRUCTR_ACTION_PROPERTY = "data-structr-action";

	public static final Property<List<DOMElement>> syncedNodes = new EndNodes("syncedNodes", Sync.class, new PropertyNotion(id));
	public static final Property<DOMElement> sharedComponent = new StartNode("sharedComponent", Sync.class, new PropertyNotion(id));

	private static final Map<String, HtmlProperty> htmlProperties = new LRUMap(1000);	// use LURMap here to avoid infinite growing
	private static final List<GraphDataSource<List<GraphObject>>> listSources = new LinkedList<>();
	private static final String lowercaseBodyName = Body.class.getSimpleName().toLowerCase();

	public static final Property<Integer> version         = new IntProperty("version").indexed();
 	public static final Property<String> tag              = new StringProperty("tag").indexed();
 	public static final Property<String> path             = new StringProperty("path").indexed();
	public static final Property<String> partialUpdateKey = new StringProperty("partialUpdateKey").indexed();
	public static final Property<String> dataKey          = new StringProperty("dataKey").indexed();
	public static final Property<String> cypherQuery      = new StringProperty("cypherQuery");
	public static final Property<String> xpathQuery       = new StringProperty("xpathQuery");
	public static final Property<String> restQuery        = new StringProperty("restQuery");
	public static final Property<Boolean> renderDetails   = new BooleanProperty("renderDetails");

	// Event-handler attributes
	public static final Property<String> _onabort = new HtmlProperty("onabort");
	public static final Property<String> _onblur = new HtmlProperty("onblur");
	public static final Property<String> _oncanplay = new HtmlProperty("oncanplay");
	public static final Property<String> _oncanplaythrough = new HtmlProperty("oncanplaythrough");
	public static final Property<String> _onchange = new HtmlProperty("onchange");
	public static final Property<String> _onclick = new HtmlProperty("onclick");
	public static final Property<String> _oncontextmenu = new HtmlProperty("oncontextmenu");
	public static final Property<String> _ondblclick = new HtmlProperty("ondblclick");
	public static final Property<String> _ondrag = new HtmlProperty("ondrag");
	public static final Property<String> _ondragend = new HtmlProperty("ondragend");
	public static final Property<String> _ondragenter = new HtmlProperty("ondragenter");
	public static final Property<String> _ondragleave = new HtmlProperty("ondragleave");
	public static final Property<String> _ondragover = new HtmlProperty("ondragover");
	public static final Property<String> _ondragstart = new HtmlProperty("ondragstart");
	public static final Property<String> _ondrop = new HtmlProperty("ondrop");
	public static final Property<String> _ondurationchange = new HtmlProperty("ondurationchange");
	public static final Property<String> _onemptied = new HtmlProperty("onemptied");
	public static final Property<String> _onended = new HtmlProperty("onended");
	public static final Property<String> _onerror = new HtmlProperty("onerror");
	public static final Property<String> _onfocus = new HtmlProperty("onfocus");
	public static final Property<String> _oninput = new HtmlProperty("oninput");
	public static final Property<String> _oninvalid = new HtmlProperty("oninvalid");
	public static final Property<String> _onkeydown = new HtmlProperty("onkeydown");
	public static final Property<String> _onkeypress = new HtmlProperty("onkeypress");
	public static final Property<String> _onkeyup = new HtmlProperty("onkeyup");
	public static final Property<String> _onload = new HtmlProperty("onload");
	public static final Property<String> _onloadeddata = new HtmlProperty("onloadeddata");
	public static final Property<String> _onloadedmetadata = new HtmlProperty("onloadedmetadata");
	public static final Property<String> _onloadstart = new HtmlProperty("onloadstart");
	public static final Property<String> _onmousedown = new HtmlProperty("onmousedown");
	public static final Property<String> _onmousemove = new HtmlProperty("onmousemove");
	public static final Property<String> _onmouseout = new HtmlProperty("onmouseout");
	public static final Property<String> _onmouseover = new HtmlProperty("onmouseover");
	public static final Property<String> _onmouseup = new HtmlProperty("onmouseup");
	public static final Property<String> _onmousewheel = new HtmlProperty("onmousewheel");
	public static final Property<String> _onpause = new HtmlProperty("onpause");
	public static final Property<String> _onplay = new HtmlProperty("onplay");
	public static final Property<String> _onplaying = new HtmlProperty("onplaying");
	public static final Property<String> _onprogress = new HtmlProperty("onprogress");
	public static final Property<String> _onratechange = new HtmlProperty("onratechange");
	public static final Property<String> _onreadystatechange = new HtmlProperty("onreadystatechange");
	public static final Property<String> _onreset = new HtmlProperty("onreset");
	public static final Property<String> _onscroll = new HtmlProperty("onscroll");
	public static final Property<String> _onseeked = new HtmlProperty("onseeked");
	public static final Property<String> _onseeking = new HtmlProperty("onseeking");
	public static final Property<String> _onselect = new HtmlProperty("onselect");
	public static final Property<String> _onshow = new HtmlProperty("onshow");
	public static final Property<String> _onstalled = new HtmlProperty("onstalled");
	public static final Property<String> _onsubmit = new HtmlProperty("onsubmit");
	public static final Property<String> _onsuspend = new HtmlProperty("onsuspend");
	public static final Property<String> _ontimeupdate = new HtmlProperty("ontimeupdate");
	public static final Property<String> _onvolumechange = new HtmlProperty("onvolumechange");
	public static final Property<String> _onwaiting = new HtmlProperty("onwaiting");

	// needed for Importer
	public static final Property<String> _data = new HtmlProperty("data").indexed();

	// Edit-mode attributes
	public static final Property<Boolean> _reload = new BooleanProperty("data-structr-reload");
	public static final Property<Boolean> _confirm = new BooleanProperty("data-structr-confirm");
	public static final Property<String> _action = new StringProperty("data-structr-action");
	public static final Property<String> _attributes = new StringProperty("data-structr-attributes");
	public static final Property<String> _attr = new StringProperty("data-structr-attr");
	public static final Property<String> _fieldName = new StringProperty("data-structr-name");
	public static final Property<String> _hide = new StringProperty("data-structr-hide");
	public static final Property<String> _rawValue = new StringProperty("data-structr-raw-value");

	// Core attributes
	public static final Property<String> _accesskey = new HtmlProperty("accesskey").indexed();
	public static final Property<String> _class = new HtmlProperty("class").indexed();
	public static final Property<String> _contenteditable = new HtmlProperty("contenteditable");
	public static final Property<String> _contextmenu = new HtmlProperty("contextmenu");
	public static final Property<String> _dir = new HtmlProperty("dir");
	public static final Property<String> _draggable = new HtmlProperty("draggable");
	public static final Property<String> _dropzone = new HtmlProperty("dropzone");
	public static final Property<String> _hidden = new HtmlProperty("hidden");
	public static final Property<String> _id = new HtmlProperty("id");
	public static final Property<String> _lang = new HtmlProperty("lang");
	public static final Property<String> _spellcheck = new HtmlProperty("spellcheck");
	public static final Property<String> _style = new HtmlProperty("style");
	public static final Property<String> _tabindex = new HtmlProperty("tabindex");
	public static final Property<String> _title = new HtmlProperty("title").indexed();
	public static final Property<String> _translate = new HtmlProperty("translate");

	// The role attribute, see http://www.w3.org/TR/role-attribute/
	public static final Property<String> _role = new HtmlProperty("role");

	public static final org.structr.common.View publicView = new org.structr.common.View(DOMElement.class, PropertyView.Public,
		name, tag, pageId, path, parent, children, restQuery, cypherQuery, xpathQuery, partialUpdateKey, dataKey, syncedNodes, sharedComponent
	);

	public static final org.structr.common.View uiView = new org.structr.common.View(DOMElement.class, PropertyView.Ui, name, tag, pageId, path, parent, children, childrenIds, owner,
		restQuery, cypherQuery, xpathQuery, partialUpdateKey, dataKey, syncedNodes, sharedComponent,
		renderDetails, hideOnIndex, hideOnDetail, showForLocales, hideForLocales, showConditions, hideConditions,
		_accesskey, _class, _contenteditable, _contextmenu, _dir, _draggable, _dropzone, _hidden, _id, _lang, _spellcheck, _style,
		_tabindex, _title, _translate, _onabort, _onblur, _oncanplay, _oncanplaythrough, _onchange, _onclick, _oncontextmenu, _ondblclick,
		_ondrag, _ondragend, _ondragenter, _ondragleave, _ondragover, _ondragstart, _ondrop, _ondurationchange, _onemptied,
		_onended, _onerror, _onfocus, _oninput, _oninvalid, _onkeydown, _onkeypress, _onkeyup, _onload, _onloadeddata,
		_onloadedmetadata, _onloadstart, _onmousedown, _onmousemove, _onmouseout, _onmouseover, _onmouseup, _onmousewheel,
		_onpause, _onplay, _onplaying, _onprogress, _onratechange, _onreadystatechange, _onreset, _onscroll, _onseeked,
		_onseeking, _onselect, _onshow, _onstalled, _onsubmit, _onsuspend, _ontimeupdate, _onvolumechange, _onwaiting,
		_reload, _confirm, _action, _attributes, _attr, _fieldName, _hide, _rawValue, _role
	);

	public static final org.structr.common.View htmlView = new org.structr.common.View(DOMElement.class, PropertyView.Html, _accesskey, _class, _contenteditable, _contextmenu, _dir,
		_draggable, _dropzone, _hidden, _id, _lang, _spellcheck, _style, _tabindex, _title, _translate, _onabort, _onblur, _oncanplay,
		_oncanplaythrough, _onchange, _onclick, _oncontextmenu, _ondblclick, _ondrag, _ondragend, _ondragenter, _ondragleave,
		_ondragover, _ondragstart, _ondrop, _ondurationchange, _onemptied, _onended, _onerror, _onfocus, _oninput, _oninvalid,
		_onkeydown, _onkeypress, _onkeyup, _onload, _onloadeddata, _onloadedmetadata, _onloadstart, _onmousedown, _onmousemove,
		_onmouseout, _onmouseover, _onmouseup, _onmousewheel, _onpause, _onplay, _onplaying, _onprogress, _onratechange,
		_onreadystatechange, _onreset, _onscroll, _onseeked, _onseeking, _onselect, _onshow, _onstalled, _onsubmit, _onsuspend,
		_ontimeupdate, _onvolumechange, _onwaiting, _role
	);

	static {

		// register data sources
		listSources.add(new IdRequestParameterGraphDataSource("nodeId"));
		listSources.add(new CypherGraphDataSource());
		listSources.add(new XPathGraphDataSource());
		listSources.add(new NodeGraphDataSource());
		listSources.add(new RestDataSource());
	}

	@Override
	public boolean contentEquals(DOMNode otherNode) {

		// two elements can not have the same content
		return false;
	}

	@Override
	public Node cloneNode(final boolean deep) {

		if (deep) {

			throw new UnsupportedOperationException("cloneNode with deep=true is not supported yet.");

		} else {

			Node node = super.cloneNode(deep);

			for (Iterator<PropertyKey> it = getPropertyKeys(htmlView.name()).iterator(); it.hasNext();) {

				PropertyKey key = it.next();

				// omit system properties (except type), parent/children and page relationships
				if (key.equals(GraphObject.type) || (!key.isUnvalidated()
					&& !key.equals(GraphObject.id)
					&& !key.equals(DOMNode.ownerDocument) && !key.equals(DOMNode.pageId)
					&& !key.equals(DOMNode.parent) && !key.equals(DOMNode.parentId)
					&& !key.equals(DOMElement.syncedNodes)
					&& !key.equals(DOMNode.children) && !key.equals(DOMNode.childrenIds))) {

					try {
						((DOMNode) node).setProperty(key, getProperty(key));
					} catch (FrameworkException ex) {
						logger.log(Level.WARNING, "Could not set property " + key + " while cloning DOMElement " + this, ex);
					}
				}
			}
			return node;

		}

	}

	@Override
	public void updateFromNode(final DOMNode newNode) throws FrameworkException {

		if (newNode instanceof DOMElement) {

			final PropertyMap properties = new PropertyMap();
			for (final Property key : htmlView.properties()) {

				properties.put(key, newNode.getProperty(key));
			}

			// copy tag
			properties.put(DOMElement.tag, newNode.getProperty(DOMElement.tag));

			updateFromPropertyMap(properties);
		}
	}

	@Override
	public void updateFromPropertyMap(final PropertyMap properties) throws FrameworkException {

		for (final Entry<PropertyKey, Object> entry : properties.entrySet()) {

			final PropertyKey key = entry.getKey();
			final Object value1 = this.getProperty(key);
			final Object value2 = entry.getValue();

			if (value1 == null && value2 == null) {
				continue;
			}

			// copy attributes
			this.setProperty(key, properties.get(key));
		}

		final String tag = properties.get(DOMElement.tag);
		if (tag != null) {

			// overwrite tag with value from source node
			this.setProperty(DOMElement.tag, tag);
		}
	}

	public boolean avoidWhitespace() {

		return false;

	}

	public Property[] getHtmlAttributes() {

		return htmlView.properties();

	}

	public void openingTag(final SecurityContext securityContext, final AsyncBuffer out, final String tag, final EditMode editMode, final RenderContext renderContext, final int depth) throws FrameworkException {

		out.append("<").append(tag);

		for (PropertyKey attribute : StructrApp.getConfiguration().getPropertySet(entityType, PropertyView.Html)) {

			String value = getPropertyWithVariableReplacement(securityContext, renderContext, attribute);

			if (!(EditMode.RAW.equals(editMode))) {

				value = escapeForHtmlAttributes(value);

			}

			if (value != null) {

				String key = attribute.jsonName().substring(PropertyView.Html.length());

				out.append(" ").append(key).append("=\"").append(value).append("\"");

			}

		}

		// include arbitrary data-* attributes
		renderCustomAttributes(out, securityContext, renderContext);

		// include special mode attributes
		switch (editMode) {

			case CONTENT:

				if (depth == 0) {

					String pageId = renderContext.getPageId();

					if (pageId != null) {

						out.append(" data-structr-page=\"").append(pageId).append("\"");
					}
				}

				out.append(" data-structr-id=\"").append(getUuid()).append("\"");
				break;

			case RAW:
				out.append(" data-hash=\"").append(getIdHash()).append("\"");
				break;
		}

		out.append(">");
	}

	/**
	 * Main render method.
	 *
	 * TODO: This method is still way to long!
	 *
	 * @param securityContext
	 * @param renderContext
	 * @param depth
	 * @throws FrameworkException
	 */
	@Override
	public void render(final SecurityContext securityContext, final RenderContext renderContext, final int depth) throws FrameworkException {

		if (renderContext.hasTimeout(RENDER_TIMEOUT)) {
			return;
		}

		if (isDeleted() || isHidden() || !displayForLocale(renderContext) || !displayForConditions(securityContext, renderContext)) {
			return;
		}

		// final variables
		final AsyncBuffer out    = renderContext.getBuffer();
		final EditMode editMode  = renderContext.getEditMode(securityContext.getUser(false));
		final boolean isVoid     = isVoidElement();
		final String _tag        = getProperty(DOMElement.tag);

		// non-final variables
		Result localResult                 = renderContext.getResult();
		boolean anyChildNodeCreatesNewLine = false;

		renderStructrAppLib(out, securityContext, renderContext, depth);

		if (depth > 0 && !avoidWhitespace()) {

			out.append(indent(depth));

		}

		if (StringUtils.isNotBlank(_tag)) {

			openingTag(securityContext, out, _tag, editMode, renderContext, depth);

			try {

				// in body?
				if (lowercaseBodyName.equals(this.getTagName())) {
					renderContext.setInBody(true);
				}

				// fetch children
				List<DOMChildren> rels = getChildRelationships();
				if (rels.isEmpty()) {

					migrateSyncRels();

					// No child relationships, maybe this node is in sync with another node
					//Sync syncRel = getIncomingRelationship(Sync.class);
					DOMElement _syncedNode = (DOMElement) getProperty(sharedComponent);
					if (_syncedNode != null) {
						rels.addAll(_syncedNode.getChildRelationships());
					}
				}

				for (AbstractRelationship rel : rels) {

					DOMNode subNode = (DOMNode) rel.getTargetNode();

					if (!securityContext.isVisible(subNode)) {
						continue;
					}

					GraphObject details = renderContext.getDetailsDataObject();
					boolean detailMode = details != null;

					if (detailMode && subNode.getProperty(hideOnDetail)) {
						continue;
					}

					if (!detailMode && subNode.getProperty(hideOnIndex)) {
						continue;
					}

					if (subNode instanceof DOMElement) {
						// Determine the "newline-situation" for the closing tag of this element
						anyChildNodeCreatesNewLine = (anyChildNodeCreatesNewLine || !((DOMElement) subNode).avoidWhitespace());
					}

					if (EditMode.RAW.equals(editMode)) {

						subNode.render(securityContext, renderContext, depth + 1);

					} else {

						String subKey = subNode.getProperty(dataKey);
						if (StringUtils.isNotBlank(subKey)) {

							setDataRoot(renderContext, subNode, subKey);

							GraphObject currentDataNode = renderContext.getDataObject();

							// Store query result of this level
							final Result newResult = renderContext.getResult();
							if (newResult != null) {
								localResult = newResult;
							}

							// fetch (optional) list of external data elements
							List<GraphObject> listData = ((DOMElement) subNode).checkListSources(securityContext, renderContext);

							PropertyKey propertyKey = null;

							if (subNode.getProperty(renderDetails) && detailMode) {

								renderContext.setDataObject(details);
								renderContext.putDataObject(subKey, details);
								subNode.render(securityContext, renderContext, depth + 1);

							} else {

								if (listData.isEmpty() && currentDataNode != null) {

									// There are two alternative ways of retrieving sub elements:
									// First try to get generic properties,
									// if that fails, try to create a propertyKey for the subKey
									Object elements = currentDataNode.getProperty(new GenericProperty(subKey));
									renderContext.setRelatedProperty(new GenericProperty(subKey));
									renderContext.setSourceDataObject(currentDataNode);

									if (elements != null) {

										if (elements instanceof Iterable) {

											for (Object o : (Iterable) elements) {

												if (o instanceof GraphObject) {

													GraphObject graphObject = (GraphObject) o;
													renderContext.putDataObject(subKey, graphObject);
													subNode.render(securityContext, renderContext, depth + 1);

												}
											}

										}

									} else {

										propertyKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(currentDataNode.getClass(), subKey, false);
										renderContext.setRelatedProperty(propertyKey);

										if (propertyKey != null) {

											final Object value = currentDataNode.getProperty(propertyKey);
											if (value != null) {

												if (value instanceof Iterable) {

													for (Object o : ((Iterable) value)) {

														if (o instanceof GraphObject) {

															//renderContext.setStartNode(node);
															renderContext.putDataObject(subKey, (GraphObject) o);
															subNode.render(securityContext, renderContext, depth + 1);

														}
													}
												}
											}
										}

									}

									// reset data node in render context
									renderContext.setDataObject(currentDataNode);
									renderContext.setRelatedProperty(null);

								} else {

									renderContext.setListSource(listData);
									((DOMElement) subNode).renderNodeList(securityContext, renderContext, depth, subKey);

								}

							}

						} else {
							subNode.render(securityContext, renderContext, depth + 1);
						}

					}

				}

			} catch (Throwable t) {

				logger.log(Level.SEVERE, "Error while rendering node {0}: {1}", new java.lang.Object[]{getUuid(), t});

				out.append("Error while rendering node ").append(getUuid()).append(": ").append(t.getMessage());

				t.printStackTrace();

			}

			// render end tag, if needed (= if not singleton tags)
			if (StringUtils.isNotBlank(_tag) && (!isVoid)) {

				// only insert a newline + indentation before the closing tag if any child-element used a newline
				if (anyChildNodeCreatesNewLine) {

					out.append(indent(depth));

				}

				out.append("</").append(_tag).append(">");
			}

		}

		double end = System.nanoTime();

		// Set result for this level again, if there was any
		if (localResult != null) {
			renderContext.setResult(localResult);
		}

	}

	private void setDataRoot(final RenderContext renderContext, final AbstractNode node, final String dataKey) {
		// an outgoing RENDER_NODE relationship points to the data node where rendering starts
		for (RenderNode rel : node.getOutgoingRelationships(RenderNode.class)) {

			NodeInterface dataRoot = rel.getTargetNode();

			// set start node of this rendering to the data root node
			renderContext.putDataObject(dataKey, dataRoot);

			// allow only one data tree to be rendered for now
			break;
		}
	}

	private void renderNodeList(SecurityContext securityContext, RenderContext renderContext, int depth, String dataKey) throws FrameworkException {

		Iterable<GraphObject> listSource = renderContext.getListSource();
		if (listSource != null) {
			for (GraphObject dataObject : listSource) {

				// make current data object available in renderContext
				renderContext.putDataObject(dataKey, dataObject);

				render(securityContext, renderContext, depth + 1);

			}
			renderContext.clearDataObject(dataKey);
		}
	}

	public boolean isVoidElement() {
		return false;
	}

	public String getOffsetAttributeName(String name, int offset) {

		int namePosition = -1;
		int index = 0;

		List<String> keys = Iterables.toList(this.getNode().getPropertyKeys());
		Collections.sort(keys);

		List<String> names = new ArrayList<>(10);

		for (String key : keys) {

			// use html properties only
			if (key.startsWith(PropertyView.Html)) {

				String htmlName = key.substring(HtmlPrefixLength);

				if (name.equals(htmlName)) {

					namePosition = index;
				}

				names.add(htmlName);

				index++;
			}
		}

		int offsetIndex = namePosition + offset;
		if (offsetIndex >= 0 && offsetIndex < names.size()) {

			return names.get(offsetIndex);
		}

		return null;
	}

	public List<String> getHtmlAttributeNames() {

		List<String> names = new ArrayList<>(10);

		for (String key : this.getNode().getPropertyKeys()) {

			// use html properties only
			if (key.startsWith(PropertyView.Html)) {

				names.add(key.substring(HtmlPrefixLength));
			}
		}

		return names;
	}

	// ----- protected methods -----
	protected HtmlProperty findOrCreateAttributeKey(String name) {

		HtmlProperty htmlProperty = null;

		synchronized (htmlProperties) {

			htmlProperty = htmlProperties.get(name);
		}

		if (htmlProperty == null) {

			// try to find native html property defined in
			// DOMElement or one of its subclasses
			PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(entityType, name, false);

			if (key != null && key instanceof HtmlProperty) {

				htmlProperty = (HtmlProperty) key;

			} else {

				// create synthetic HtmlProperty
				htmlProperty = new HtmlProperty(name);
				htmlProperty.setDeclaringClass(DOMElement.class);
			}

			// cache property
			synchronized (htmlProperties) {

				htmlProperties.put(name, htmlProperty);
			}

		}

		return htmlProperty;
	}

	// ----- private methods -----
	private List<GraphObject> checkListSources(SecurityContext securityContext, RenderContext renderContext) {

		// try registered data sources first
		for (GraphDataSource<List<GraphObject>> source : listSources) {

			try {

				List<GraphObject> graphData = source.getData(securityContext, renderContext, this);
				if (graphData != null) {
					return graphData;
				}

			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Could not retrieve data from graph data source {0}: {1}", new Object[]{source, fex});
			}
		}

		return Collections.EMPTY_LIST;
	}

	// ----- interface org.w3c.dom.Element -----
	@Override
	public String getTagName() {

		return getProperty(tag);
	}

	@Override
	public String getAttribute(String name) {

		HtmlProperty htmlProperty = findOrCreateAttributeKey(name);

		return htmlProperty.getProperty(securityContext, this, true);
	}

	@Override
	public void setAttribute(final String name, final String value) throws DOMException {

		try {
			HtmlProperty htmlProperty = findOrCreateAttributeKey(name);
			if (htmlProperty != null) {

				htmlProperty.setProperty(securityContext, DOMElement.this, value);
			}

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());

		}
	}

	@Override
	public void removeAttribute(final String name) throws DOMException {

		try {
			HtmlProperty htmlProperty = findOrCreateAttributeKey(name);
			if (htmlProperty != null) {

				htmlProperty.setProperty(securityContext, DOMElement.this, null);
			}

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());

		}
	}

	@Override
	public Attr getAttributeNode(String name) {

		HtmlProperty htmlProperty = findOrCreateAttributeKey(name);
		String value = htmlProperty.getProperty(securityContext, this, true);

		if (value != null) {

			boolean explicitlySpecified = true;
			boolean isId = false;

			if (value.equals(htmlProperty.defaultValue())) {
				explicitlySpecified = false;
			}

			return new DOMAttribute((Page) getOwnerDocument(), this, name, value, explicitlySpecified, isId);
		}

		return null;
	}

	@Override
	public Attr setAttributeNode(final Attr attr) throws DOMException {

		// save existing attribute node
		Attr attribute = getAttributeNode(attr.getName());

		// set value
		setAttribute(attr.getName(), attr.getValue());

		// set parent of attribute node
		if (attr instanceof DOMAttribute) {
			((DOMAttribute) attr).setParent(this);
		}

		return attribute;
	}

	@Override
	public Attr removeAttributeNode(final Attr attr) throws DOMException {

		// save existing attribute node
		Attr attribute = getAttributeNode(attr.getName());

		// set value
		setAttribute(attr.getName(), null);

		return attribute;
	}

	@Override
	public NodeList getElementsByTagName(final String tagName) {

		DOMNodeList results = new DOMNodeList();

		collectNodesByPredicate(this, results, new TagPredicate(tagName), 0, false);

		return results;
	}

	@Override
	public String getAttributeNS(String string, String string1) throws DOMException {
		return null;
	}

	@Override
	public void setAttributeNS(String string, String string1, String string2) throws DOMException {
	}

	@Override
	public void removeAttributeNS(String string, String string1) throws DOMException {
	}

	@Override
	public Attr getAttributeNodeNS(String string, String string1) throws DOMException {
		return null;
	}

	@Override
	public Attr setAttributeNodeNS(Attr attr) throws DOMException {
		return null;
	}

	@Override
	public NodeList getElementsByTagNameNS(String string, String string1) throws DOMException {
		return null;
	}

	@Override
	public boolean hasAttribute(String name) {
		return getAttribute(name) != null;
	}

	@Override
	public boolean hasAttributeNS(String string, String string1) throws DOMException {
		return false;
	}

	@Override
	public TypeInfo getSchemaTypeInfo() {
		return null;
	}

	@Override
	public void setIdAttribute(final String idString, boolean isId) throws DOMException {

		checkWriteAccess();

		final App app = StructrApp.getInstance(securityContext);

		try {
			setProperty(DOMElement._id, idString);

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	@Override
	public void setIdAttributeNS(String string, String string1, boolean bln) throws DOMException {
		throw new UnsupportedOperationException("Namespaces not supported.");
	}

	@Override
	public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException {
		throw new UnsupportedOperationException("Attribute nodes not supported in HTML5.");
	}

	// ----- interface org.w3c.dom.Node -----
	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public String getNodeName() {
		return getTagName();
	}

	@Override
	public String getNodeValue() throws DOMException {
		return null;
	}

	@Override
	public void setNodeValue(String string) throws DOMException {
		// the nodeValue of an Element cannot be set
	}

	@Override
	public short getNodeType() {

		return ELEMENT_NODE;
	}

	@Override
	public NamedNodeMap getAttributes() {
		return this;
	}

	@Override
	public boolean hasAttributes() {
		return getLength() > 0;
	}

	// ----- interface org.w3c.dom.NamedNodeMap -----
	@Override
	public Node getNamedItem(String name) {
		return getAttributeNode(name);
	}

	@Override
	public Node setNamedItem(Node node) throws DOMException {

		if (node instanceof Attr) {
			return setAttributeNode((Attr) node);
		}

		return null;
	}

	@Override
	public Node removeNamedItem(String name) throws DOMException {

		// save existing attribute node
		Attr attribute = getAttributeNode(name);

		// set value to null
		setAttribute(name, null);

		return attribute;
	}

	@Override
	public Node item(int i) {

		List<String> htmlAttributeNames = getHtmlAttributeNames();
		if (i >= 0 && i < htmlAttributeNames.size()) {

			return getAttributeNode(htmlAttributeNames.get(i));
		}

		return null;
	}

	@Override
	public int getLength() {
		return getHtmlAttributeNames().size();
	}

	@Override
	public Node getNamedItemNS(String string, String string1) throws DOMException {
		return null;
	}

	@Override
	public Node setNamedItemNS(Node node) throws DOMException {
		return null;
	}

	@Override
	public Node removeNamedItemNS(String string, String string1) throws DOMException {
		return null;
	}

	// ----- interface DOMImportable -----
	@Override
	public Node doImport(final Page newPage) throws DOMException {

		DOMElement newElement = (DOMElement) newPage.createElement(getTagName());

		// copy attributes
		for (String _name : getHtmlAttributeNames()) {

			Attr attr = getAttributeNode(_name);
			if (attr.getSpecified()) {

				newElement.setAttribute(attr.getName(), attr.getValue());
			}
		}

		return newElement;
	}

	// ----- nested classes -----
	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		for (Sync rel : getOutgoingRelationships(Sync.class)) {

			DOMElement syncedNode = rel.getTargetNode();

			// sync HTML properties only
			for (Property htmlProp : syncedNode.getHtmlAttributes()) {

				syncedNode.setProperty(htmlProp, getProperty(htmlProp));
			}
		}

		return true;

	}

	/**
	 * Render all the data-* attributes
	 *
	 * @param securityContext
	 * @param renderContext
	 */
	private void renderCustomAttributes(final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext) throws FrameworkException {

		dbNode = this.getNode();
		EditMode editMode = renderContext.getEditMode(securityContext.getUser(false));

		Iterable<String> props = dbNode.getPropertyKeys();
		for (String key : props) {

			if (key.startsWith("data-")) {

				String value = getPropertyWithVariableReplacement(securityContext, renderContext, new GenericProperty(key));

				if (!(EditMode.RAW.equals(editMode))) {

					value = escapeForHtmlAttributes(value);

				}

				if (StringUtils.isNotBlank(value)) {

					out.append(" ").append(key).append("=\"").append(value).append("\"");

				}

			}

		}

		if (EditMode.RAW.equals(editMode)) {

			Property[] rawProps = new Property[]{
				dataKey, restQuery, cypherQuery, xpathQuery, hideOnIndex, hideOnDetail, showForLocales, hideForLocales, showConditions, hideConditions
			};

//			// In raw mode, add query-related data
//			String _dataKey		= getProperty(dataKey);
//			String _restQuery	= getProperty(restQuery);
//			String _cypherQuery	= getProperty(cypherQuery);
//			String _xpathQuery	= getProperty(xpathQuery);
//
//			// Add filter to raw output
//			boolean _hideOnIndex = getProperty(hideOnIndex);
//			boolean _hideOnDetail = getProperty(hideOnDetail);
//			String _showForLocales = getProperty(showForLocales);
//			String _hideForLocales = getProperty(hideForLocales);
//			String _showConditions = getProperty(showConditions);
//			String _hideConditions = getProperty(hideConditions);
			for (Property p : rawProps) {

				if (p instanceof BooleanProperty) {

				}

				String htmlName = "data-structr-meta-" + CaseHelper.toUnderscore(p.jsonName(), false).replaceAll("_", "-");
				Object value = getProperty(p);
				if ((p instanceof BooleanProperty && (boolean) value) || (!(p instanceof BooleanProperty) && value != null && StringUtils.isNotBlank(value.toString()))) {
					out.append(" ").append(htmlName).append("=\"").append(value.toString()).append("\"");
				}
			}

//			if (StringUtils.isNotBlank(_restQuery)) {
//				buffer.append(" ").append("data-structr-meta-rest-query").append("=\"").append(_restQuery).append("\"");
//			}
//
//			if (StringUtils.isNotBlank(_cypherQuery)) {
//				buffer.append(" ").append("data-structr-meta-cypher-query").append("=\"").append(_cypherQuery).append("\"");
//			}
//
//			if (StringUtils.isNotBlank(_xpathQuery)) {
//				buffer.append(" ").append("data-structr-meta-xpath-query").append("=\"").append(_xpathQuery).append("\"");
//			}
		}
	}

	/**
	 * Render script tags with jQuery and structr-app.js to current tag.
	 *
	 * Make sure it happens only once per page.
	 *
	 * @param out
	 */
	private void renderStructrAppLib(final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext, final int depth) throws FrameworkException {

		if (!(EditMode.RAW.equals(renderContext.getEditMode(securityContext.getUser(false)))) && !renderContext.appLibRendered() && getProperty(new StringProperty(STRUCTR_ACTION_PROPERTY)) != null) {

			out
				.append(indent(depth))
				.append("<script>if (!window.jQuery) { document.write('<script src=\"/structr/js/lib/jquery-1.11.0.min.js\"><\\/script>'); }</script>")
				.append(indent(depth))
				.append("<script>if (!window.jQuery.ui) { document.write('<script src=\"/structr/js/lib/jquery-ui-1.10.3.custom.min.js\"><\\/script>'); }</script>")
				.append(indent(depth))
				.append("<script>if (!window.jQuery.ui.timepicker) { document.write('<script src=\"/structr/js/lib/jquery-ui-timepicker-addon.min.js\"><\\/script>'); }</script>")
				.append(indent(depth))
				.append("<script>if (!window.StructrApp) { document.write('<script src=\"/structr/js/structr-app.min.js\"><\\/script>'); }</script>")
				.append(indent(depth))
				.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"/structr/css/jquery-ui-1.10.3.custom.min.css\">");

			renderContext.setAppLibRendered(true);

		}

	}

	/**
	 * This method concatenates the pre-defined HTML attributes and the
	 * optional custom data-* attributes.
	 *
	 * @param propertyView
	 * @return
	 */
	@Override
	public Iterable<PropertyKey> getPropertyKeys(String propertyView) {

		final List<PropertyKey> allProperties = new LinkedList();
		final Iterable<PropertyKey> htmlAttrs = super.getPropertyKeys(propertyView);

		for (PropertyKey attr : htmlAttrs) {

			allProperties.add(attr);

		}

		dbNode = this.getNode();

		Iterable<String> props = dbNode.getPropertyKeys();
		for (String key : props) {

			if (key.startsWith("data-")) {

				allProperties.add(new GenericProperty(key));

			}

		}

		return allProperties;
	}

	@Override
	public boolean isSynced() {
		return hasIncomingRelationships(Sync.class) || hasOutgoingRelationships(Sync.class);
	}

	private void migrateSyncRels() {
		try {

			org.neo4j.graphdb.Node n = getNode();

			Iterable<Relationship> incomingSyncRels = n.getRelationships(DynamicRelationshipType.withName("SYNC"), Direction.INCOMING);
			Iterable<Relationship> outgoingSyncRels = n.getRelationships(DynamicRelationshipType.withName("SYNC"), Direction.OUTGOING);

			if (getOwnerDocument() instanceof ShadowDocument) {

				// We are a shared component and must not have any incoming SYNC rels
				for (Relationship r : incomingSyncRels) {
					r.delete();
				}

			} else {

				for (Relationship r : outgoingSyncRels) {
					r.delete();
				}

				for (Relationship r : incomingSyncRels) {

					DOMElement possibleSharedComp = StructrApp.getInstance().get(DOMElement.class, (String) r.getStartNode().getProperty("id"));

					if (!(possibleSharedComp.getOwnerDocument() instanceof ShadowDocument)) {

						r.delete();

					}

				}
			}

		} catch (FrameworkException ex) {
			Logger.getLogger(DOMElement.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	// ----- interface Syncable -----
	@Override
	public List<Syncable> getSyncData() {

		final List<Syncable> data = super.getSyncData();

		data.add(getProperty(DOMElement.sharedComponent));
		data.add(getIncomingRelationship(Sync.class));

		if (isSynced()) {

			// add parent
			data.add(getProperty(ownerDocument));
			data.add(getOutgoingRelationship(PageLink.class));
		}

		return data;
	}
}
