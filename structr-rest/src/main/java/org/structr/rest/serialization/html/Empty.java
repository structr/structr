package org.structr.rest.serialization.html;

/**
 *
 * @author Christian Morgner
 */
public class Empty extends Tag {

	public Empty(final Tag parent, final String tagName) {
		super(parent, tagName, true, true);
	}
}
