/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import java.text.SimpleDateFormat;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class AppTimestamp extends AbstractNode implements InteractiveNode
{
	private static final String FORMAT_KEY =		"format";

	@Override
	public void renderView(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
	}

	@Override
	public String getIconSrc()
	{
		return("/images/time.png");
	}

	@Override
	public void onNodeCreation()
	{
	}

	@Override
	public void onNodeInstantiation()
	{
	}

	// ----- interface InteractiveNode -----
	@Override
	public Class getParameterType()
	{
		return(String.class);
	}

	@Override
	public Object getValue()
	{
		String format = getStringProperty(FORMAT_KEY);
		if(format == null)
		{
			format = "dd.MM.yyyy HH:mm";
		}

		SimpleDateFormat df = new SimpleDateFormat(format);
		return(df.format(System.currentTimeMillis()));
	}

	@Override
	public String getStringValue()
	{
		return(getValue().toString());
	}

	@Override
	public void setMappedName(String mappedName)
	{
	}

	@Override
	public String getMappedName()
	{
		return(getStringProperty(NAME_KEY));
	}

	@Override
	public void setErrorValue(Object errorValue)
	{
	}
}
