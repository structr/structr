/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.test;

import java.util.List;
import static junit.framework.TestCase.fail;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.dynamic.File;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.FileBase;
import static ucar.httpservices.CustomX509TrustManager.logger;

/**
 *
 * @author Christian Morgner
 */
public class IndexingTest extends StructrUiTest {

	public void test08PassiveIndexing() {

		try {

			// create some test nodes
			FileBase test1 = null;

			// check initial sort order
			try (final Tx tx = app.tx()) {
				
				// create some test nodes
				test1 = createTestNode(File.class, new NodeAttribute<>(AbstractNode.name, "aaaaa"));
				createTestNode(File.class, new NodeAttribute<>(AbstractNode.name, "bbbbb"));
				createTestNode(File.class, new NodeAttribute<>(AbstractNode.name, "ccccc"));
				createTestNode(File.class, new NodeAttribute<>(AbstractNode.name, "ddddd"));
				createTestNode(File.class, new NodeAttribute<>(AbstractNode.name, "eeeee"));

				tx.success();
			}

			// check initial sort order
			try (final Tx tx = app.tx()) {

				final List<FileBase> files = app.nodeQuery(FileBase.class).sort(File.path).getAsList();

				assertEquals("Invalid indexing sort result", "aaaaa", files.get(0).getName());
				assertEquals("Invalid indexing sort result", "bbbbb", files.get(1).getName());
				assertEquals("Invalid indexing sort result", "ccccc", files.get(2).getName());
				assertEquals("Invalid indexing sort result", "ddddd", files.get(3).getName());
				assertEquals("Invalid indexing sort result", "eeeee", files.get(4).getName());

				tx.success();
			}


			// modify file name to move the first file to the end of the sorted list
			try (final Tx tx = app.tx()) {

				test1.setProperty(AbstractNode.name, "zzzzz");

				tx.success();
			}


			// check final sort order
			try (final Tx tx = app.tx()) {

				final List<FileBase> files = app.nodeQuery(FileBase.class).sort(File.path).getAsList();

				assertEquals("Invalid indexing sort result", "bbbbb", files.get(0).getName());
				assertEquals("Invalid indexing sort result", "ccccc", files.get(1).getName());
				assertEquals("Invalid indexing sort result", "ddddd", files.get(2).getName());
				assertEquals("Invalid indexing sort result", "eeeee", files.get(3).getName());
				assertEquals("Invalid indexing sort result", "zzzzz", files.get(4).getName());

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}
	}
}
