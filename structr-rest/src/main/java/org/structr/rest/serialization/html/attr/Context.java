package org.structr.rest.serialization.html.attr;

/**
 *
 * @author Christian Morgner
 */
public class Context {

	private int depth = 0;
	
	public Context(final int depth) {
		this.depth = depth;
	}
	
	public int getDepth() {
		return depth;
	}
}
