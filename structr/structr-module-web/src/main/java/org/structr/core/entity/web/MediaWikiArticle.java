/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.entity.web;

import net.sourceforge.jwbf.core.contentRep.SimpleArticle;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

import org.structr.common.CurrentRequest;
import org.structr.common.PropertyKey;
import org.structr.common.RenderMode;
import org.structr.core.NodeRenderer;
import org.structr.core.TemporaryValue;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;

//~--- classes ----------------------------------------------------------------

/**
 * A MediaWikiArticle is an interface to articleName in a MediaWiki.
 *
 * @author axel
 */
public class MediaWikiArticle extends AbstractNode {

	private static final String CACHED_MEDIAWIKI_CONTENT = "cached_mediawiki_content";

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ wikiUrl, articleName }

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> rendererMap) {}

	@Override
	public void onNodeCreation() {}

	@Override
	public void onNodeInstantiation() {}

	@Override
	public void onNodeDeletion() {}

	//~--- get methods ----------------------------------------------------

	@Override
	public Iterable<AbstractNode> getDataNodes() {

		// articleName is cached in servlet context
		ServletContext context = CurrentRequest.getRequest().getSession().getServletContext();
		List<AbstractNode> dataNodes = null;

		// TODO: synchronization
		if (context != null) {

			TemporaryValue<List<AbstractNode>> value =
				(TemporaryValue<List<AbstractNode>>) context.getAttribute(
				    CACHED_MEDIAWIKI_CONTENT.concat(this.getIdString()));

			if (value == null) {

				value = new TemporaryValue<List<AbstractNode>>(null, TimeUnit.MINUTES.toMillis(10));
				context.setAttribute(CACHED_MEDIAWIKI_CONTENT.concat(this.getIdString()), value);
			}

			if ((value.getStoredValue() == null) || value.isExpired()) {

				dataNodes = getContentFromSource();
				value.refreshStoredValue(dataNodes);

			} else {
				dataNodes = value.getStoredValue();
			}
		}

		return dataNodes;
	}

	@Override
	public String getIconSrc() {
		return "/images/lightbulb.png";
	}

	// ----- private methods ----
	private LinkedList<AbstractNode> getContentFromSource() {

		LinkedList<AbstractNode> ret = new LinkedList<AbstractNode>();
		String wikiUrl               = getStringProperty(Key.wikiUrl);
		String articleName               = getStringProperty(Key.articleName);

		try {

			// do fetch articleName from MediaWiki wiki
			MediaWikiBot b   = new MediaWikiBot(wikiUrl);
			SimpleArticle sa = new SimpleArticle(b.readContent(articleName));

			
			System.out.println(sa.getText());
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return (ret);
	}
}
