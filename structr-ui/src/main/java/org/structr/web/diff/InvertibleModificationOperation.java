package org.structr.web.diff;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.web.entity.dom.Page;

/**
 *
 * @author Christian Morgner
 */
public interface InvertibleModificationOperation {

	public void apply(final App app, final Page page) throws FrameworkException;
	public InvertibleModificationOperation revert();
}
