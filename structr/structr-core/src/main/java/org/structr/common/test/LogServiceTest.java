/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.common.test;

import org.structr.common.StandaloneTestHelper;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.log.LogCommand;

/**
 *
 * @author Christian Morgner
 */
public class LogServiceTest
{
	public static void main(String[] args)
	{
		StandaloneTestHelper.prepareStandaloneTest("/tmp/structr-test/");

		try
		{
			Command logCommand = Services.createCommand(LogCommand.class);

			for(int i=0; i<1000; i++)
			{
				logCommand.execute("LogObject" + i);
				StandaloneTestHelper.sleep(10);
			}

		} catch(Throwable t)
		{
			t.printStackTrace();
		}

		/*
		final GraphDatabaseService graphDb = (GraphDatabaseService)Services.createCommand(GraphDatabaseCommand.class).execute();
		final Command factory = Services.createCommand(NodeFactoryCommand.class);
		NodeList<StructrNode> nodeList = null;
		*/

		StandaloneTestHelper.finishStandaloneTest();
	}
}
