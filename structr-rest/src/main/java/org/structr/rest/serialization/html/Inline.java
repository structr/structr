package org.structr.rest.serialization.html;

/**
 *
 * @author Christian Morgner
 */
public class Inline extends Tag {

	public Inline(final Tag parent, final String tagName) {
		super(parent, tagName, false, false);
	}
}
