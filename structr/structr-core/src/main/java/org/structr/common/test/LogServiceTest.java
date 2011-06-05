/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
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
			Command logCommand = Services.command(LogCommand.class);

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
		final GraphDatabaseService graphDb = (GraphDatabaseService)Services.command(GraphDatabaseCommand.class).execute();
		final Command factory = Services.command(NodeFactoryCommand.class);
		NodeList<StructrNode> nodeList = null;
		*/

		StandaloneTestHelper.finishStandaloneTest();
	}
}
