/*
 *  Copyright (C) 2010-2012 Axel Morgner
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

package org.structr.common;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.FindNodeCommand;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class PathHelper {

	public static final String PATH_SEP = "/";

	//~--- fields ---------------------------------------------------------

	private SecurityContext securityContext;

	//~--- constructors ---------------------------------------------------

	public PathHelper(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	//~--- methods --------------------------------------------------------

	public AbstractNode find(String path) {

		AbstractNode node       = null;
		Command findNodeCommand = Services.command(securityContext, FindNodeCommand.class);

		try {

			node = (AbstractNode) findNodeCommand.execute(path);

			// check security context
			if (securityContext.isVisible(node)) {

				return (node);

			}

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return (null);
	}

	public static void main(String[] args) {

		String[] input = { "/blog//blog_entries/892fa9194a36427bb87e18641c217d7d/comments", "/img/blog.gif", "blog" };

		for (int i = 0; i < input.length; i++) {

			String path = input[i];
			
			System.out.println("################### Testing " + path + " ###################");
			
			String name = getName(path);
			String pagePart = getParts(path)[0];
			
			
			System.out.println("Name: " + name);
			System.out.println("Page part: " + pagePart);
			
			String[] parts = getParts(path);

			for (String part : parts) {

				System.out.println(part);

			}
			
			System.out.println();

		}
	}

	public static String clean(String path) {

		// Remove leading and trailing /
		return StringUtils.strip(path, PATH_SEP);
	}

	//~--- get methods ----------------------------------------------------

	/**
	 * Assemble a relative path for the given absolute paths
	 *
	 * @param basePath
	 * @param targetPath
	 * @return
	 */
	public static String getRelativeNodePath(String basePath, String targetPath) {

		// Both paths are equal
		if (basePath.equals(targetPath)) {

			return ".";

		}

		if (basePath.equals(PATH_SEP) && (targetPath.length() > 1)) {

			// Base path is root path
			return targetPath.substring(1);
		}

		String[] baseAncestors   = FilenameUtils.normalizeNoEndSeparator(basePath).split(PATH_SEP);
		String[] targetAncestors = FilenameUtils.normalizeNoEndSeparator(targetPath).split(PATH_SEP);
		int length               = (baseAncestors.length < targetAncestors.length)
					   ? baseAncestors.length
					   : targetAncestors.length;
		int lastCommonRoot       = -1;
		int i;

		// Iterate over the shorter path
		for (i = 0; i < length; i++) {

			if (baseAncestors[i].equals(targetAncestors[i])) {

				lastCommonRoot = i;

			} else {

				break;

			}

		}

		// Last common root is the common base path
		if (lastCommonRoot != -1) {

			StringBuilder newRelativePath = new StringBuilder();

			// How often must we go back from base path to common root?
			for (i = lastCommonRoot + 1; i < baseAncestors.length; i++) {

				if (baseAncestors[i].length() > 0) {

					newRelativePath.append(".." + PATH_SEP);

				}

			}

			// How often must we go forth from common root to get to tagret path?
			for (i = lastCommonRoot + 1; i < targetAncestors.length; i++) {

				newRelativePath.append(targetAncestors[i]).append(PATH_SEP);

			}

			// newRelativePath.append(targetAncestors[targetAncestors.length - 1]);
			String result = newRelativePath.toString();

			if (result.endsWith(PATH_SEP)) {

				result = result.substring(0, result.length() - 1);

			}

			return result;

		}

		return targetPath;
	}

	public static String getName(final String pathPart) {

		if (pathPart.contains(PATH_SEP)) {

			return StringUtils.substringAfterLast(pathPart, PATH_SEP);

		} else {

			return pathPart;

		}
	}

	public static String[] getParts(String path) {

		path = clean(path);

		return StringUtils.splitByWholeSeparator(path, PATH_SEP);
	}
}
