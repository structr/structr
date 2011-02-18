/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import au.com.bytecode.opencsv.CSVReader;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.CsvFile;
import org.structr.core.entity.File;
import org.structr.core.entity.NodeList;
import org.structr.core.entity.StructrNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;

/**
 * Converts a CSV file to a node list. Each row will be represented
 * as a node.
 *
 * @author axel
 */
public class ConvertCsvToNodeListCommand extends NodeServiceCommand
{
	private static final Logger logger = Logger.getLogger(ConvertCsvToNodeListCommand.class.getName());

	@Override
	public Object execute(Object... parameters)
	{

		if(parameters == null || parameters.length < 2)
		{
			throw new UnsupportedArgumentError("Wrong number of arguments");
		}

		Long csvNodeId = null;
		StructrNode sourceNode = null;
		Class targetClass = null;
		File csvFileNode = null;
		String filePath = null;
		User user = null;

		for(Object o : parameters)
		{

			if(o instanceof StructrNode)
			{
				sourceNode = (StructrNode)o;

				if(sourceNode.getType().equals(CsvFile.class.getSimpleName()))
				{
					csvFileNode = new CsvFile();
					csvFileNode.init(sourceNode);

					filePath = Services.getFilesPath() + "/" + csvFileNode.getRelativeFilePath();
				}

			}

			if(o instanceof Long)
			{
				csvNodeId = (Long)o;
				sourceNode = (StructrNode)Services.createCommand(FindNodeCommand.class).execute(new SuperUser(), csvNodeId);

				if(sourceNode.getType().equals(CsvFile.class.getSimpleName()))
				{
					csvFileNode = new CsvFile();
					csvFileNode.init(sourceNode);

					filePath = Services.getFilesPath() + "/" + csvFileNode.getRelativeFilePath();
				}
			}

			if(o instanceof Class)
			{
				targetClass = (Class)o;
			}

			if(o instanceof User)
			{
				user = (User)o;
			}

		}

		try
		{

			// TODO: Implement auto-detection for field separator and quote characters
			CSVReader reader = new CSVReader(new FileReader(filePath), '|', '\"');

			// Read first line, these should be the column keys
			String[] keys = reader.readNext();

			// Get the fields of the target node
			Field[] fields = targetClass.getFields();

			// The field index stores the field name of a column
			Map<Integer, String> fieldIndex = new HashMap<Integer, String>();

			// Instanciate object
			StructrNode o = (StructrNode)targetClass.newInstance();

			int col = 0;
			// Match with fields
			for(String key : keys)
			{

				for(Field f : fields)
				{

					String fieldName = (String)f.get(o);

					if(fieldName.toUpperCase().equals(key.toUpperCase()))
					{
						fieldIndex.put(col, fieldName);
					}

				}
				col++;
			}

			for(Entry<Integer, String> entry : fieldIndex.entrySet())
			{
				Integer i = entry.getKey();
				String v = entry.getValue();

				System.out.println("v: " + v + ", i: " + i);
			}

//			List<String[]> lines = reader.readAll();

			final User userCopy = user;
			final StructrNode sourceNodeCopy = sourceNode;

			final Command transactionCommand = Services.createCommand(TransactionCommand.class);
			final Command createNode = Services.createCommand(CreateNodeCommand.class);
			final Command createRel = Services.createCommand(CreateRelationshipCommand.class);

			final NodeList<StructrNode> nodeListNode = (NodeList<StructrNode>)transactionCommand.execute(new StructrTransaction()
			{
				@Override
				public Object execute() throws Throwable
				{

					NodeList<StructrNode> result = new NodeList<StructrNode>();
					// If the node list node doesn't exist, create one
					StructrNode s = (StructrNode)createNode.execute(userCopy,
						new NodeAttribute(StructrNode.TYPE_KEY, NodeList.class.getSimpleName()),
						new NodeAttribute(StructrNode.NAME_KEY, sourceNodeCopy.getName() + " List"));
					result.init(s);

					createRel.execute(sourceNodeCopy, result, RelType.HAS_CHILD);
					return result;
				}
			});


			final List<List<NodeAttribute>> creationList = new LinkedList<List<NodeAttribute>>();
			String targetClassName = targetClass.getSimpleName();
			String[] line = null;
			do
			{
				// read line, one at a time
				try { line = reader.readNext(); } catch(Throwable t) { }

				if(line != null)
				{
					// create a new list for each item
					List<NodeAttribute> nodeAttributes = new LinkedList<NodeAttribute>();
					nodeAttributes.add(new NodeAttribute(StructrNode.TYPE_KEY, targetClassName));

					for(int i = 0; i < col; i++)
					{
						String csvValue = line[i];
						String key = fieldIndex.get(i);

						nodeAttributes.add(new NodeAttribute(key, csvValue));
					}

					// add node attributes to creation list
					creationList.add(nodeAttributes);
				}


			} while(line != null);

			reader.close();

			// everything in one transaction
			transactionCommand.execute(new StructrTransaction()
			{
				@Override
				public Object execute() throws Throwable
				{
					List<StructrNode> nodesToAdd = new LinkedList<StructrNode>();
					for(List<NodeAttribute> attrList : creationList)
					{
						nodesToAdd.add((StructrNode)createNode.execute(attrList));
					}

					// use bulk add
					nodeListNode.addAll(nodesToAdd);

					return(null);
				}
			});


			/*
			final List<NodeAttribute> attrList = new ArrayList<NodeAttribute>();

			 NodeAttribute typeAttr = new NodeAttribute(StructrNode.TYPE_KEY, targetClass.getSimpleName());
			attrList.add(typeAttr);

			String[] line = null;

			do
			{
				try
				{
					line = reader.readNext();

				} catch(Throwable t)
				{
					line = null;
				}

				if(line != null)
				{
					for(int i = 0; i < col; i++)
					{

						String csvValue = line[i];
						String key = fieldIndex.get(i);
						
						System.out.println("Creating attribute " + key + " = '" + csvValue + "'..");
						
						NodeAttribute attr = new NodeAttribute(key, csvValue);
						attrList.add(attr);

						logger.log(Level.FINEST, "Created node attribute {0}={1}", new Object[]
							{
								attr.getKey(), attr.getValue()
							});
					}

					StructrNode newNode = (StructrNode)transactionCommand.execute(new StructrTransaction()
					{
						@Override
						public Object execute() throws Throwable
						{
							Command createNode = Services.createCommand(CreateNodeCommand.class);

							// Create new node
							StructrNode newNode = (StructrNode)createNode.execute(userCopy, attrList);
							transactionCommand.setExitCode(createNode.getExitCode());
							transactionCommand.setErrorMessage(createNode.getErrorMessage());
							return newNode;
						}
					});

					nodeListNode.add(newNode);
					logger.log(Level.INFO, "Node {0} added to node list", newNode.getId());
				}

			} while(line != null);
			*/

			return nodeListNode;

		} catch(Throwable t)
		{
			// TODO: use logger
			t.printStackTrace(System.out);
		}

		return null;

	}
}
