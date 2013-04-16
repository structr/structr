/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.node;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.AbstractNode;
import org.structr.web.entity.Image;
import org.structr.web.entity.Image;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeServiceCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Crawl a website and save images as sub nodes of parent node.
 *
 * @author amorgner
 */
public class SaveImagesFromWebsiteUrl extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(SaveImagesFromWebsiteUrl.class.getName());

	//~--- methods --------------------------------------------------------

	/**
	 * Takes three parameters.
	 *
	 * <ul>
	 * <li>1: Principal
	 * <li>2: URL string
	 * <li>3: Parent node
	 * </ul>
	 *
	 * @param parameters
	 * @return
	 */
	public Object execute(Object... parameters) throws FrameworkException {

		Principal user          = null;
		String urlString        = null;
		AbstractNode parentNode = null;
		List<Image> result      = new LinkedList<Image>();

		switch (parameters.length) {

			case 3 :
				if (parameters[0] instanceof Principal) {

					user = (Principal) parameters[0];
				}

				if (parameters[1] instanceof String) {

					urlString = (String) parameters[1];
				}

				if (parameters[2] instanceof AbstractNode) {

					parentNode = (AbstractNode) parameters[2];
				}

				break;

			default :
				throw new UnsupportedArgumentError("Wrong number of arguments");

		}

		if ((user != null) && (urlString != null) && (parentNode != null)) {

			result = saveImagesFromWebsiteUrl(user, urlString, parentNode);
		}

		return result;

	}

	/**
	 * Save all images from the given website as image nodes
	 *
	 * @param imageNode
	 */
	private List<Image> saveImagesFromWebsiteUrl(final Principal user, final String urlString, final AbstractNode parentNode) throws FrameworkException {

		SaveImageFromUrl saveImage = Services.command(securityContext, SaveImageFromUrl.class);
		List<Image> result         = new LinkedList<Image>();
		Document doc               = Jsoup.parse(urlString);
		Elements elements          = doc.getElementsByTag("img");

		for (Element el : elements) {

			String imageUrl    = el.attr("src");
			Image newImageNode = (Image) saveImage.execute(user, imageUrl, parentNode);

			result.add(newImageNode);

		}

		return result;

	}

}
