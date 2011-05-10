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

package org.structr.core.node;

import org.apache.commons.lang.StringUtils;

/**
 *
 * @author cmorgner
 */
public class XPath
{
	private String xpath = null;

        public XPath() {}
        
	public XPath(final String xpath)
	{
		this.xpath = StringUtils.trimToEmpty(xpath);
	}

        /**
         * Full-featured XPath expression.
         *
         * Example: /Domain[@name='test.com']/Site[@name='www']/Page[@name='Home']
         *
         * @param xpath
         */
	public void setXPath(final String xpath)
	{
		this.xpath = xpath;
	}

        /**
         * A path, like f.e. /path/to/node
         * @param path
         */
        public void setPath(final String path) {
            String[] pathElements = path.split("/");
            StringBuilder xPathExpression = new StringBuilder();

            for (String name : pathElements) {
                if (name != null && !name.isEmpty()) {
                    xPathExpression.append("/*[@name=\"").append(name).append("\"]");
                }
            }
            this.xpath = xPathExpression.toString();
        }


	public String getXPath()
	{
		return(xpath);
	}
}
