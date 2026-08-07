/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.web.advanced;

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.FolderTraitDefinition;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Searching a relationship-backed property for null must return the nodes whose property READS as
 * empty -- including nodes that have a relationship of the same underlying type to a node of a
 * different type.
 *
 * <p>Folder is the clearest example in the product: {@code files}, {@code folders}, {@code images}
 * and {@code children} are four separate properties over one and the same database relationship
 * type, {@code CONTAINS}, told apart only by the type at the other end. So "give me the folders
 * that contain no files" must still return a folder that contains a sub-folder -- reading
 * {@code files} on it yields nothing, and a query has to agree with the read.</p>
 *
 * <p>It did not on Neo4j: {@code AdvancedCypherQuery.addNullObjectParameter} emitted
 * {@code not (n)-[:CONTAINS]->()}, i.e. "has no CONTAINS relationship at all", ignoring the
 * endpoint type that defines the property -- while the same class constrains that type for every
 * non-null search, and the read path filters on it too. A folder with a sub-folder therefore
 * dropped out of a search for folders without files. The in-memory driver's NoRelationshipPredicate
 * checks the label and was right.</p>
 */
public class NullRelationshipKeyQueryTest extends StructrUiTest {

	@Test
	public void testSearchingForNullReturnsWhatTheReadSays() {

		final String emptyId;
		final String withSubfolderId;
		final String withFileId;

		try (final Tx tx = app.tx()) {

			final PropertyKey<NodeInterface> parentKey = Traits.of(StructrTraits.ABSTRACT_FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY);

			// a folder with nothing in it
			final NodeInterface empty = app.create(StructrTraits.FOLDER, "empty");

			// a folder whose only child is another folder -- no files
			final NodeInterface withSubfolder = app.create(StructrTraits.FOLDER, "with-subfolder");
			app.create(StructrTraits.FOLDER, new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key("name"), "child"), new NodeAttribute<>(parentKey, withSubfolder));

			// a folder that really does contain a file
			final NodeInterface withFile = app.create(StructrTraits.FOLDER, "with-file");
			app.create(StructrTraits.FILE, new NodeAttribute<>(Traits.of(StructrTraits.FILE).key("name"), "test.txt"), new NodeAttribute<>(parentKey, withFile));

			emptyId         = empty.getUuid();
			withSubfolderId = withSubfolder.getUuid();
			withFileId      = withFile.getUuid();

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected setup failure: " + fex.getMessage());

			return;
		}

		try (final Tx tx = app.tx()) {

			final PropertyKey<Iterable<NodeInterface>> filesKey = Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.FILES_PROPERTY);

			// what the READ says
			assertEquals("an empty folder contains no files", 0, count(app.getNodeById(emptyId), filesKey));
			assertEquals("a folder containing only a sub-folder contains no files", 0, count(app.getNodeById(withSubfolderId), filesKey));
			assertEquals("the third folder contains one file", 1, count(app.getNodeById(withFileId), filesKey));

			// what the QUERY must say
			final List<String> withoutFiles = new ArrayList<>();

			for (final NodeInterface folder : app.nodeQuery(StructrTraits.FOLDER).key(filesKey, null).getAsList()) {

				withoutFiles.add(folder.getUuid());
			}

			assertTrue("a folder with nothing in it must be found when searching for folders without files", withoutFiles.contains(emptyId));

			assertTrue("a folder whose only child is a SUB-FOLDER must be found when searching for folders without files:"
				+ " reading its 'files' yields nothing, so a search for null must return it. It has a CONTAINS relationship,"
				+ " but to a Folder, and 'files' is only about Files.", withoutFiles.contains(withSubfolderId));

			assertTrue("a folder that does contain a file must NOT be found", !withoutFiles.contains(withFileId));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected query failure: " + fex.getMessage());
		}
	}

	private int count(final NodeInterface node, final PropertyKey<Iterable<NodeInterface>> key) {

		assertNotNull("test node should exist", node);

		final Iterable<NodeInterface> value = node.getProperty(key);

		return (value == null) ? 0 : Iterables.toList(value).size();
	}
}
