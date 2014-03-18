package org.structr.web.diff;

import org.structr.common.error.FrameworkException;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public interface InvertibleModificationOperation {

	public void apply(Page page, final DOMNode node) throws FrameworkException;
	public InvertibleModificationOperation revert();
}
