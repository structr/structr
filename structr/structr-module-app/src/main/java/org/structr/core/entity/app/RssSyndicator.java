/*
 *  Copyright (C) 2011 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.core.entity.app;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.PropertyKey;
import org.structr.common.RenderMode;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.renderer.NodeViewRenderer;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 *
 * @author Christian Morgner
 */
public class RssSyndicator extends AbstractNode  {

	public enum Key implements PropertyKey {

		source
	}

	@Override
	protected List<AbstractNode> getDirectChildren(final RelationshipType relType, final String nodeType)
	{
//	public List<AbstractNode> getSortedDirectChildren(final RelationshipType relType) {

		List<AbstractNode> ret = new LinkedList<AbstractNode>();
		String source = getStringProperty(Key.source);

		try {

			URL url = new URL(source);

			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(url.openStream());

			NodeList items = doc.getElementsByTagName("item");
			int len = items.getLength();

			for(int i=0; i<len; i++) {

				ret.add(new RssItem(items.item(i)));
			}

		} catch(Throwable t) {

			t.printStackTrace();
		}

		return(ret);
	}

	// ----- AbstractNode -----
	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> rendererMap) {

		rendererMap.put(RenderMode.Default, new NodeViewRenderer());
	}

	@Override
	public String getIconSrc() {

		return("/images/feed.png");
	}

	@Override
	public void onNodeCreation() {
	}

	@Override
	public void onNodeInstantiation() {
	}

	@Override
	public void onNodeDeletion() {
	}
}
