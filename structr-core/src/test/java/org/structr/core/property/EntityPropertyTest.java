/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.property;

import java.util.List;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.TestNine;
import org.structr.core.entity.TestThree;
import org.structr.core.entity.TestSix;
import org.structr.core.graph.DeleteNodeCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;

/**
 *
 * @author Christian Morgner
 */
public class EntityPropertyTest extends StructrTest {

	public void testOneToOne() throws Exception {
		
		final TestSix a   = createTestNode(TestSix.class);
		final TestSix c   = createTestNode(TestSix.class);
		final TestThree b = createTestNode(TestThree.class);
		final TestThree d = createTestNode(TestThree.class);
		
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					a.setProperty(AbstractNode.name, "a");
					c.setProperty(AbstractNode.name, "c");
					b.setProperty(AbstractNode.name, "b");
					d.setProperty(AbstractNode.name, "d");

					return null;
				}

			});
			
		} catch (FrameworkException fex) {
		
			fex.printStackTrace();
			
			fail("Unable to create test nodes");
		}
		
		assertNotNull(a);
		assertNotNull(c);
		assertNotNull(b);
		assertNotNull(d);

		/**
		 * We test the following here:
		 * A -> B
		 * C -> D
		 * 
		 * then try to connect A -> D and B -> C to provoke an error message.
		 */

		// create two connections
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
					
					a.setProperty(TestSix.oneToOneTestThree, b);
					c.setProperty(TestSix.oneToOneTestThree, d);

					return null;
				}

			});
			
		} catch (FrameworkException fex) {
		
			fex.printStackTrace();
			
			fail("Unable to link test nodes");
		}
		
		// verify connections
		TestThree verifyB = a.getProperty(TestSix.oneToOneTestThree);
		TestThree verifyD = c.getProperty(TestSix.oneToOneTestThree);
		
		assertTrue(verifyB != null && verifyB.equals(b));
		assertTrue(verifyD != null && verifyD.equals(d));
		
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
		
					// connect a and d
					a.setProperty(TestSix.oneToOneTestThree, d);

					return null;
				}

			});
			
			fail("Linking two nodes that are already connected to other nodes via 1:1 relationship should fail.");
			
		} catch (FrameworkException fex) {
		}
		
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
		
					// connect a and d
					c.setProperty(TestSix.oneToOneTestThree, b);

					return null;
				}

			});
			
			fail("Linking two nodes that are already connected to other nodes via 1:1 relationship should fail.");
			
		} catch (FrameworkException fex) {
		}
		
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
		
					// try to connect d and c
					d.setProperty(TestThree.oneToOneTestSix, a);

					return null;
				}

			});
			
			fail("Linking two nodes that are already connected to other nodes via 1:1 relationship should fail.");
			
		} catch (FrameworkException fex) {
		}
		
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
		
					// try to connect c and b
					b.setProperty(TestThree.oneToOneTestSix, c);

					return null;
				}

			});
			
			fail("Linking two nodes that are already connected to other nodes via 1:1 relationship should fail.");
			
		} catch (FrameworkException fex) {
		}
	}

	public void testOneToMany() throws Exception {
		
		final TestSix s1   = createTestNode(TestSix.class);
		final TestSix s2   = createTestNode(TestSix.class);
		final TestSix s3   = createTestNode(TestSix.class);
		
		final TestThree t1 = createTestNode(TestThree.class);
		final TestThree t2 = createTestNode(TestThree.class);
		final TestThree t3 = createTestNode(TestThree.class);
		final TestThree t4 = createTestNode(TestThree.class);
				
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					s1.setProperty(AbstractNode.name, "s1");
					s2.setProperty(AbstractNode.name, "s2");
					s3.setProperty(AbstractNode.name, "s3");

					t1.setProperty(AbstractNode.name, "t1");
					t2.setProperty(AbstractNode.name, "t2");
					t3.setProperty(AbstractNode.name, "t3");
					t4.setProperty(AbstractNode.name, "t4");

					return null;
				}

			});
			
		} catch (FrameworkException fex) {
		
			fex.printStackTrace();
			
			fail("Unable to create test nodes");
		}

		assertNotNull(s1);
		assertNotNull(s2);
		assertNotNull(s3);

		assertNotNull(t1);
		assertNotNull(t2);
		assertNotNull(t3);
		assertNotNull(t4);

		// create test case
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
					
					// create two connections
					s1.setProperty(TestSix.oneToManyTestThrees, toList(t1, t2));
					s2.setProperty(TestSix.oneToManyTestThrees, toList(t3, t4));

					return null;
				}

			});
			
		} catch (FrameworkException fex) {
			
			fex.printStackTrace();
			
			fail("Unexpected exception");
		}

		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
					
					s3.setProperty(TestSix.oneToManyTestThrees, toList(t1));

					return null;
				}

			});
			
			// we expect that creating s3 -> t1 should fail
			// because s1 -> t1 is already linked
			fail("Linking a node to an already connected 1:n node should fail.");
			
		} catch (FrameworkException fex) {
		}

		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
					
					s1.setProperty(TestSix.oneToManyTestThrees, toList(t1, t2, t4));

					return null;
				}

			});
			
			// we expect that creating s1 -> t4 should fail
			// because s2 -> t4 is already linked
			fail("Linking a node to an already connected 1:n node should fail.");
			
		} catch (FrameworkException fex) {
		}

		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
					
					t1.setProperty(TestThree.oneToManyTestSix, s3);

					return null;
				}

			});
			
			// we expect that creating s3 -> t1 should fail
			// because s1 -> t1 is already linked
			fail("Linking a node to an already connected 1:n node should fail.");
			
		} catch (FrameworkException fex) {
		}

		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
					
					t4.setProperty(TestThree.oneToManyTestSix, s1);

					return null;
				}

			});
			
			// we expect that creating s1 -> t4 should fail
			// because s2 -> t4 is already linked
			fail("Linking a node to an already connected 1:n node should fail.");
			
		} catch (FrameworkException fex) {
		}

		// test duplicate prevention
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
					
					t4.setProperty(TestThree.oneToManyTestSix, s1);
					t4.setProperty(TestThree.oneToManyTestSix, s1);

					return null;
				}

			});
			
			// we expect that creating s1 -> t4 should fail
			// because s2 -> t4 is already linked
			fail("Creating a duplicate node should fail.");
			
		} catch (FrameworkException fex) {
		}

		// modify collection, this test should not throw an exception
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
					
					// modify collection on s1
					s1.setProperty(TestSix.oneToManyTestThrees, toList(t1));
					
					// modify collection on s2
					s2.setProperty(TestSix.oneToManyTestThrees, toList(t2, t3));
					
					// modify collection on s3
					s3.setProperty(TestSix.oneToManyTestThrees, toList(t4));

					// empty collections
					s1.setProperty(TestSix.oneToManyTestThrees, null);
					s2.setProperty(TestSix.oneToManyTestThrees, null);
					s3.setProperty(TestSix.oneToManyTestThrees, null);

					// modify collection on s1
					s1.setProperty(TestSix.oneToManyTestThrees, toList(t1));
					
					// modify collection on s2
					s2.setProperty(TestSix.oneToManyTestThrees, toList(t2));
					
					// modify collection on s3
					s3.setProperty(TestSix.oneToManyTestThrees, toList(t3, t4));

					return null;
				}

			});
			
		} catch (FrameworkException fex) {
			
			fex.printStackTrace();
			
			fail("Unexpected exception");
		}
		
		// check result of the above transaction
		List<TestThree> resultS1 = s1.getProperty(TestSix.oneToManyTestThrees);
		List<TestThree> resultS2 = s2.getProperty(TestSix.oneToManyTestThrees);
		List<TestThree> resultS3 = s3.getProperty(TestSix.oneToManyTestThrees);
		
		assertEquals("Result has wrong size.", 1, resultS1.size());
		assertEquals("Result has wrong size.", 1, resultS2.size());
		assertEquals("Result has wrong size.", 2, resultS3.size());

		assertEquals(t1, resultS1.get(0));
		assertEquals(t2, resultS2.get(0));
		assertEquals(t3, resultS3.get(1));
		assertEquals(t4, resultS3.get(0));
		
	}
	
	public void testOutgoingCascadingDelete() {

		try {
			
			final TestSix s1   = createTestNode(TestSix.class);

			final TestThree t1 = createTestNode(TestThree.class);
			final TestThree t2 = createTestNode(TestThree.class);
			final TestThree t3 = createTestNode(TestThree.class);
			
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
			
					s1.setProperty(TestSix.oneToManyTestThreesCascadeOut, toList(t1, t2, t3));
					
					s1.setProperty(AbstractNode.name, "s1");
					t1.setProperty(AbstractNode.name, "t1");
					t2.setProperty(AbstractNode.name, "t2");
					t3.setProperty(AbstractNode.name, "t3");
					
					return null;
				}
			});

			
			final List<TestThree> result1 = s1.getProperty(TestSix.oneToManyTestThreesCascadeOut);
			assertEquals("Result has wrong size.", 3, result1.size());
			assertEquals(t1, result1.get(0));
			assertEquals(t2, result1.get(1));
			assertEquals(t3, result1.get(2));
			
			// delete t2 => expect s1 to exist and have t1 and t3 as children, expect t2 to be deleted
			{
				Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						Services.command(securityContext, DeleteNodeCommand.class).execute(t2);

						return null;
					}
				});

				// test expected result
				final List<TestThree> result3 = s1.getProperty(TestSix.oneToManyTestThreesCascadeOut);
				assertEquals("Result has wrong size.", 2, result3.size());
				assertEquals(t3, result3.get(0));
				assertEquals(t1, result3.get(1));

				try {
					t2.getProperty(TestThree.oneToManyTestSix);

					fail("Node t2 has been deleted, accessing it should throw an exception.");

				} catch (Throwable fex) { }
			}
		
			// delete s1 => expect t1 and t3 to be deleted
			{
				Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						Services.command(securityContext, DeleteNodeCommand.class).execute(s1);

						return null;
					}
				});

				// test expected result
				try {
					t1.setPropertyTransactional(TestThree.name, "t1_deleted");
					fail("Node t1 has been deleted, accessing it should throw an exception.");

				} catch (FrameworkException fex) { }

				try {
					t2.setPropertyTransactional(TestThree.name, "t2_deleted");
					fail("Node t2 has been deleted, accessing it should throw an exception.");

				} catch (FrameworkException fex) { }

				try {
					t3.setPropertyTransactional(TestThree.name, "t3_deleted");
					fail("Node t3 has been deleted, accessing it should throw an exception.");

				} catch (FrameworkException fex) { }

				try {
					s1.setPropertyTransactional(TestSix.name, "s1_deleted");
					fail("Node s1 has been deleted, accessing it should throw an exception.");

				} catch (FrameworkException fex) { }
			}
			
			
			
			
			
			
		} catch (FrameworkException fex) {
			
			fex.printStackTrace();
			
			fail("Unexpected exception");
		}
		
		
	}
	
	public void testIncomingCascadingDelete() {

		try {
			
			final TestSix s1   = createTestNode(TestSix.class);

			final TestThree t1 = createTestNode(TestThree.class);
			final TestThree t2 = createTestNode(TestThree.class);
			final TestThree t3 = createTestNode(TestThree.class);
			
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
			
					s1.setProperty(TestSix.oneToManyTestThreesCascadeIn, toList(t1, t2, t3));
										
					s1.setProperty(AbstractNode.name, "s1");
					t1.setProperty(AbstractNode.name, "t1");
					t2.setProperty(AbstractNode.name, "t2");
					t3.setProperty(AbstractNode.name, "t3");
					
					return null;
				}
			});

			
			final List<TestThree> result1 = s1.getProperty(TestSix.oneToManyTestThreesCascadeIn);
			assertEquals("Result has wrong size.", 3, result1.size());
			assertEquals(t1, result1.get(0));
			assertEquals(t2, result1.get(1));
			assertEquals(t3, result1.get(2));
			
			// delete t2 => expect s1 to exist and have t1 and t3 as children, expect t2 to be deleted
			{
				Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						Services.command(securityContext, DeleteNodeCommand.class).execute(t2);

						return null;
					}
				});

				// test expected result
				// nodes that have been deleted
				try {
					t2.setPropertyTransactional(TestThree.name, "t2_deleted");
					fail("Node t2 has been deleted, accessing it should throw an exception.");

				} catch (FrameworkException fex) { }

				try {
					s1.setPropertyTransactional(TestSix.name, "s1");
					fail("Node s1 has been deleted, accessing it should throw an exception.");

				} catch (FrameworkException fex) { }

				// nodes that have NOT been deleted
				try {
					t1.setPropertyTransactional(TestThree.name, "t1_new");

				} catch (FrameworkException fex) {
					fail("Node t1 has not been deleted, ccessing it should not throw an exception.");
				}

				try {
					t3.setPropertyTransactional(TestThree.name, "t3_new");

				} catch (FrameworkException fex) {
					fail("Node t3 has been deleted, accessing it should throw an exception.");
				}
			}
			
		} catch (FrameworkException fex) {
			
			fex.printStackTrace();
			
			fail("Unexpected exception");
		}
		
	}
	
	public void testTwoWayCascadingDelete() {

		try {
			
			final TestSix s1   = createTestNode(TestSix.class);

			final TestThree t1 = createTestNode(TestThree.class);
			final TestThree t2 = createTestNode(TestThree.class);
			final TestThree t3 = createTestNode(TestThree.class);
			
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
			
					s1.setProperty(TestSix.oneToManyTestThreesCascadeBoth, toList(t1, t2, t3));
										
					s1.setProperty(AbstractNode.name, "s1");
					t1.setProperty(AbstractNode.name, "t1");
					t2.setProperty(AbstractNode.name, "t2");
					t3.setProperty(AbstractNode.name, "t3");
					
					return null;
				}
			});

			
			final List<TestThree> result1 = s1.getProperty(TestSix.oneToManyTestThreesCascadeBoth);
			assertEquals("Result has wrong size.", 3, result1.size());
			assertEquals(t1, result1.get(0));
			assertEquals(t2, result1.get(1));
			assertEquals(t3, result1.get(2));
			
			// delete t2 => expect the whole construct to be deleted
			{
				Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						Services.command(securityContext, DeleteNodeCommand.class).execute(t2);

						return null;
					}
				});

				// test expected result
				try {
					s1.setPropertyTransactional(TestSix.name, "s1");
					fail("Node s1 has been deleted, accessing it should throw an exception.");

				} catch (FrameworkException fex) { }

				try {
					t1.setPropertyTransactional(TestThree.name, "t1");
					fail("Node t1 has been deleted, accessing it should throw an exception.");

				} catch (FrameworkException fex) { }

				try {
					t2.setPropertyTransactional(TestThree.name, "t2");
					fail("Node t2 has been deleted, accessing it should throw an exception.");

				} catch (FrameworkException fex) { }

				try {
					t3.setPropertyTransactional(TestThree.name, "t3");
					fail("Node t3 has been deleted, accessing it should throw an exception.");

				} catch (FrameworkException fex) { }
			}
			
		} catch (FrameworkException fex) {
			
			fex.printStackTrace();
			
			fail("Unexpected exception");
		}
		
	}
	
	public void testConstraintBasedCascadingDelete() {

		try {
			
			final TestSix s1   = createTestNode(TestSix.class);

			final PropertyMap props = new PropertyMap();
			
			// set s1 on creation
			props.put(TestNine.oneToManyTestSixConstraint, s1);
			
			final TestNine n1 = createTestNode(TestNine.class, props);
			final TestNine n2 = createTestNode(TestNine.class, props);
			final TestNine n3 = createTestNode(TestNine.class, props);
			
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
			
					s1.setProperty(AbstractNode.name, "s1");
					n1.setProperty(AbstractNode.name, "n1");
					n2.setProperty(AbstractNode.name, "n2");
					n3.setProperty(AbstractNode.name, "n3");
					
					return null;
				}
			});

			
			final List<TestNine> result1 = s1.getProperty(TestSix.oneToManyTestNinesCascadeConstraint);
			assertEquals("Result has wrong size.", 3, result1.size());
			assertEquals(n1, result1.get(0));
			assertEquals(n2, result1.get(1));
			assertEquals(n3, result1.get(2));
			
			// delete n2 => expect no cascading
			{
				Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						Services.command(securityContext, DeleteNodeCommand.class).execute(n2);

						return null;
					}
				});

				// test expected result
				try {
					s1.setPropertyTransactional(TestSix.name, "s1");

				} catch (FrameworkException fex) {
					fail("Node s1 has not been deleted, accessing it should not throw an exception.");
				}

				try {
					n1.setPropertyTransactional(TestThree.name, "n1");

				} catch (FrameworkException fex) {
					fail("Node n1 has not been deleted, accessing it should not throw an exception.");
				}

				try {
					n2.setPropertyTransactional(TestThree.name, "n2");
					fail("Node n2 has been deleted, accessing it should throw an exception.");

				} catch (FrameworkException fex) { }

				try {
					n3.setPropertyTransactional(TestThree.name, "n3");

				} catch (FrameworkException fex) {
					fail("Node n3 has not been deleted, accessing it should not throw an exception.");
				}
			}
			
			// delete s1 => expect s1-s3 to be deleted because of constraints
			{
				Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						Services.command(securityContext, DeleteNodeCommand.class).execute(s1);

						return null;
					}
				});

				// test expected result
				try {
					s1.setPropertyTransactional(TestSix.name, "s1");
					fail("Node s1 has been deleted, accessing it should throw an exception.");

				} catch (FrameworkException fex) { }

				try {
					n1.setPropertyTransactional(TestThree.name, "n1");
					fail("Node n1 has been deleted, accessing it should throw an exception.");

				} catch (FrameworkException fex) { }

				try {
					n2.setPropertyTransactional(TestThree.name, "n2");
					fail("Node n2 has been deleted, accessing it should throw an exception.");

				} catch (FrameworkException fex) { }

				try {
					n3.setPropertyTransactional(TestThree.name, "n3");
					fail("Node n3 has been deleted, accessing it should throw an exception.");

				} catch (FrameworkException fex) { }
			}
			
		} catch (FrameworkException fex) {
			
			fex.printStackTrace();
			
			fail("Unexpected exception");
		}
		
	}


	/**
	 * Test of typeName method, of class EntityProperty.
	 */
	public void testTypeName() {

		Property instance = TestSix.oneToOneTestThree;
		String expResult = "Object";
		String result = instance.typeName();
		assertEquals(expResult, result);
	}

	/**
	 * Test of databaseConverter method, of class EntityProperty.
	 */
	public void testDatabaseConverter() {

		Property instance = TestSix.oneToOneTestThree;
		PropertyConverter expResult = null;
		PropertyConverter result = instance.databaseConverter(securityContext, null);
		assertEquals(expResult, result);
	}

	/**
	 * Test of inputConverter method, of class EntityProperty.
	 */
	public void testInputConverter() {

		Property instance = TestSix.oneToOneTestThree;
		PropertyConverter result = instance.inputConverter(securityContext);
		
		assertTrue(result != null);
	}

	/**
	 * Test of relatedType method, of class EntityProperty.
	 */
	public void testRelatedType() {

		Property instance = TestSix.oneToOneTestThree;
		Class expResult = TestThree.class;
		Class result = instance.relatedType();
		assertEquals(expResult, result);
	}

	/**
	 * Test of isCollection method, of class EntityProperty.
	 */
	public void testIsCollection() {

		Property instance = TestSix.oneToOneTestThree;
		boolean expResult = false;
		boolean result = instance.isCollection();
		assertEquals(expResult, result);
	}
}
