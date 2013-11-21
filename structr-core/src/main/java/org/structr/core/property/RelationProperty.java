package org.structr.core.property;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.notion.Notion;

/**
 *
 * @author Christian Morgner
 */


public interface RelationProperty<T> {

	public Notion getNotion();

	public Class<? extends T> getTargetType();
	
	public void addSingleElement(final SecurityContext securityContext, final GraphObject obj, final T t) throws FrameworkException;
}
