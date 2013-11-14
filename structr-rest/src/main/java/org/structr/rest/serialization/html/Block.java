package org.structr.rest.serialization.html;

/**
 *
 * @author Christian Morgner
 */
public class Block extends Tag {

	public Block(final Tag parent, final String tagName) {
		super(parent, tagName, false, true);
	}
}
