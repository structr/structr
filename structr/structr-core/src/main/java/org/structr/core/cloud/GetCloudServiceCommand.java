package org.structr.core.cloud;

/**
 *
 * @author Christian Morgner
 */
public class GetCloudServiceCommand extends CloudServiceCommand
{
	@Override
	public Object execute(Object... parameters)
	{
		return(arguments.get("service"));
	}
}
