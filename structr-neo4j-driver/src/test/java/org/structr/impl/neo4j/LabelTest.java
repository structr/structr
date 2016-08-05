/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.impl.neo4j;

import org.structr.neo4j.Neo4jDatabaseService;
import junit.framework.TestCase;
import org.neo4j.graphdb.DynamicLabel;
import org.structr.api.DatabaseService;
import org.structr.api.graph.Label;
import org.structr.neo4j.wrapper.LabelWrapper;

/**
 *
 */
public class LabelTest extends TestCase {

	public void testLabelEquality() {

		final DatabaseService db = new Neo4jDatabaseService();
		final Label label1       = db.forName(Label.class, "TestLabel");
		final Label label2       = new LabelWrapper(DynamicLabel.label("TestLabel"));

		assertEquals(label1, label2);
	}

}
