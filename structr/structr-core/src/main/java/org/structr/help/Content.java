package org.structr.help;

import org.structr.common.AbstractComponent;

/**
 *
 * @author Christian Morgner
 */
public class Content extends AbstractComponent {

	private Object[] content = null;
	
	public Content(Object... content) {
		this.content = content;
	}

	@Override
	public void initComponents() {
	}
	
	@Override
	public Object[] getContent() {
		return(content);
	}
}
