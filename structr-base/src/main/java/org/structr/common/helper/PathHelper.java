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
package org.structr.common.helper;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * A helper class that provides methods for URL path splitting etc.
 */
public class PathHelper {

	public static final String PATH_SEP = "/";

	public static String clean(final String path) {

		// Remove leading and trailing /
		return StringUtils.strip(path, PATH_SEP);
	}

	public static String replaceWhitespaceByPlus(final String path) {
		return StringUtils.replace(path, " ", "+");
	}

	public static String replaceWhitespaceByPercentTwenty(final String path) {
		return StringUtils.replace(path, " ", "%20");
	}

	/**
	 * Assemble a relative path for the given absolute paths
	 *
	 * @param basePath
	 * @param targetPath
	 * @return relative node path
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

	/**
	 * Return last part of the given path after separator or the path if no path separator was found.
	 *
	 * @param path
	 * @return name
	 */
	public static String getName(final String path) {

		String cleanedPath = clean(path);

		if (cleanedPath != null && cleanedPath.contains(PATH_SEP)) {

			return StringUtils.substringAfterLast(cleanedPath, PATH_SEP);

		} else {

			return cleanedPath;

		}
	}

	/**
	 * Return part of the given path before the last separator or the root path ("/") if no separator was found
	 *
	 * @param path
	 * @return name
	 */
	public static String getFolderPath(final String path) {

		String cleanedPath = clean(path);

		if (cleanedPath != null && cleanedPath.contains(PATH_SEP)) {

			return PATH_SEP + StringUtils.substringBeforeLast(cleanedPath, PATH_SEP);

		} else {

			return PATH_SEP;

		}
	}

	/**
	 * Return array of path parts.
	 *
	 * @param path
	 * @return path parts
	 */
	public static String[] getParts(final String path) {

		String cleanedPath = clean(path);

		return StringUtils.splitByWholeSeparator(cleanedPath, PATH_SEP);
	}

	/**
	 * Simply removes all relative parts of a path (meaning ".." and ".")
	 *
	 * @param path
	 * @return path without relative parts
	 */
	public static String removeRelativeParts(final String path) {

		String partialPath = "";

		for (String part : PathHelper.getParts(path)) {

			// ignore ".." and "." in paths
			if ("..".equals(part) || ".".equals(part)) {
				continue;
			}

			partialPath += PathHelper.PATH_SEP + part;
		}

		return partialPath;
	}
}
