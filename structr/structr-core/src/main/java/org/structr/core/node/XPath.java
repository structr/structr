/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
