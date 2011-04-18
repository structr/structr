/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author chrisi
 */
public class DataNode extends AbstractNode
{
	@Override
	public void renderView(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
	}

	@Override
	public Object getProperty(String key)
	{
		return(super.getProperty(key));
	}

	@Override
	public void setProperty(String key, Object value)
	{
		super.setProperty(key, value);
	}

	@Override
	public String getIconSrc()
	{
		return("/images/database.png");
	}
}
