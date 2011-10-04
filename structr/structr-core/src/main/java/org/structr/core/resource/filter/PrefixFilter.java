/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.resource.filter;

import org.neo4j.graphdb.Node;

/**
 *
 * @author Christian Morgner
 */
public class PrefixFilter implements Filter {

	private boolean caseSensitive = false;
	private String propertyKey = null;
	private String prefix = null;

	public PrefixFilter(String key, String prefix) {

		this(key, prefix, false);
	}

	public PrefixFilter(String propertyKey, String prefix, boolean caseSensitive) {

		this.propertyKey  = propertyKey;
		this.prefix = prefix;
		this.caseSensitive = caseSensitive;
	}

	@Override
	public boolean includeInResultSet(Node currentNode) {

		if(currentNode.hasProperty(propertyKey)) {

			if(caseSensitive) {

				return currentNode.getProperty(propertyKey).toString().startsWith(prefix);
				
			} else {

				// case insensitive
				return currentNode.getProperty(propertyKey).toString().toLowerCase().startsWith(prefix.toLowerCase());
			}
		}

		return false;
	}
}
