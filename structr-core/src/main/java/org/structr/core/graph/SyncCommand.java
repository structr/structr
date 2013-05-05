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


package org.structr.core.graph;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.tooling.GlobalGraphOperations;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.StaticValue;
import org.structr.core.Value;

/**
 *
 * @author Christian Morgner
 */
public class SyncCommand extends NodeServiceCommand implements MaintenanceCommand, Serializable {

	private static final Logger logger                 = Logger.getLogger(SyncCommand.class.getName());
	private static final String STRUCTR_ZIP_DB_NAME    = "db";
	
	private static final Map<Class, String> typeMap    = new LinkedHashMap<Class, String>();
	private static final Map<Class, Method> methodMap  = new LinkedHashMap<Class, Method>();
	private static final Map<String, Class> classMap   = new LinkedHashMap<String, Class>();

	private final DecimalFormat decimalFormat  = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	private final GraphDatabaseService graphDb = Services.getService(NodeService.class).getGraphDb();

	static {
		
		typeMap.put(Byte[].class,      "00");
		typeMap.put(Byte.class,        "01");
		typeMap.put(Short[].class,     "02");
		typeMap.put(Short.class,       "03");
		typeMap.put(Integer[].class,   "04");
		typeMap.put(Integer.class,     "05");
		typeMap.put(Long[].class,      "06");
		typeMap.put(Long.class,        "07");
		typeMap.put(Float[].class,     "08");
		typeMap.put(Float.class,       "09");
		typeMap.put(Double[].class,    "10");
		typeMap.put(Double.class,      "11");
		typeMap.put(Character[].class, "12");
		typeMap.put(Character.class,   "13");
		typeMap.put(String[].class,    "14");
		typeMap.put(String.class,      "15");
		typeMap.put(Boolean[].class,   "16");
		typeMap.put(Boolean.class,     "17");
		
		// build reverse mapping
		for (Entry<Class, String> entry : typeMap.entrySet()) {
			classMap.put(entry.getValue(), entry.getKey());
		}
	}
	
	
	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {
		
		String mode          = (String)attributes.get("mode");
		String fileName      = (String)attributes.get("file");
		String validate      = (String)attributes.get("validate");
		boolean doValidation = false;

		// should we validate imported nodes?
		if (validate != null) {
			
			try {
				
				doValidation = Boolean.valueOf(validate);
				
			} catch (Throwable t) {
				
				logger.log(Level.WARNING, "Unable to parse value for validation flag: {0}", t.getMessage());
			}
		}
		
		if (fileName == null) {
			
			throw new FrameworkException(400, "Please specify sync file.");
		}
		
		if ("export".equals(mode)) {
			
			exportFile(fileName);
			
		} else if ("import".equals(mode)) {
			
			importFile(fileName, doValidation);
			
		} else {
			
			throw new FrameworkException(400, "Please specify sync mode (import|export).");
		}
	}
	
	/**
	 * Serializes the given object into the given writer. The following format will
	 * be used to serialize objects. The first two characters are the type index, see
	 * typeMap above. After that, a single digit that indicates the length of the following
	 * length field follows. After that, the length field is serialized, followed by the
	 * string value of the given object and a space character for human readability.
	 * 
	 * @param writer
	 * @param obj 
	 */
	private void serialize(PrintWriter writer, Object obj) {
		
		if (obj != null) {
			
			Class clazz = obj.getClass();
			String type = typeMap.get(clazz);
			
			if (type != null)  {
				
				if (clazz.isArray()) {

					Object[] array    = (Object[])obj;
					int len           = array.length;
					int log           = Integer.valueOf(len).toString().length();

					writer.print(type);	// type: 00-nn:  data type
					writer.print(log);	// log:  1-10: length of next int field
					writer.print(len);	// len:  length of value
				
					// serialize array
					for (Object o : (Object[])obj) {
						serialize(writer, o);
					}
					
				} else {

					String str        = obj.toString();
					int len           = str.length();
					int log           = Integer.valueOf(len).toString().length();

					writer.print(type);	// type: 00-nn:  data type
					writer.print(log);	// log:  1-10: length of next int field
					writer.print(len);	// len:  length of value
					writer.print(str);	// str:  the value
				}
				
				// make it more readable for the human eye
				writer.print(" ");
				
			} else {
				
				logger.log(Level.WARNING, "Unable to serialize object of type {0}, type not supported", obj.getClass());
			}
		}
	}
	
	private Object deserialize(Reader reader) throws IOException {

		Object serializedObject = null;
		String type             = read(reader, 2);
		String lenLenSrc        = read(reader, 1);
		int lenLen              = Integer.parseInt(lenLenSrc);	// lenght of "length" field :)
		Class clazz             = classMap.get(type);
		
		if (clazz != null) {
			
			String lenSrc = read(reader, lenLen);
			int len       = Integer.parseInt(lenSrc);	// now we've got the "real" length of the following value
			
			if (clazz.isArray()) {
				
				// len is the length of the underlying array
				Object[] array = (Object[])Array.newInstance(clazz.getComponentType(), len);
				for (int i=0; i<len; i++) {
					
					array[i] = deserialize(reader);
				}
				
				// set array
				serializedObject = array;
				
			} else {
				
				// len is the length of the string representation of the real value
				String value = read(reader, len);
				
				if (clazz.equals(String.class)) {

					// strings can be returned immediately
					serializedObject = value;
					
				} else {
				
					try {
						
						Method valueOf = methodMap.get(clazz);
						if (valueOf == null) {
							
							valueOf = clazz.getMethod("valueOf", String.class);
							methodMap.put(clazz, valueOf);
						}

						// invoke static valueOf method
						if (valueOf != null) {
							
							serializedObject = valueOf.invoke(null, value);
							
						} else {
							
							logger.log(Level.WARNING, "Unable to find static valueOf method for type {0}", clazz);
						}
						
					} catch (Throwable t) {

						logger.log(Level.WARNING, "Unable to deserialize value {0} of type {1}, Class has no static valueOf method.", new Object[] { value, clazz } );
					}
				}
			}
			
		} else {
			
			logger.log(Level.WARNING, "Unsupported type {0} in input", type);
		}
		
		// skip white space after object (see serialize method)
		reader.skip(1);
		
		return serializedObject;
	}
	
	private String read(Reader reader, int len) throws IOException {
		
		char[] buf = new char[len];

		// only continue if the desired number of chars could be read
		if (reader.read(buf, 0, len) == len) {
		
			return new String(buf, 0, len);
		}
		
		// end of stream
		throw new EOFException();
	}
	
	private void exportFile(String fileName) throws FrameworkException {
	
		try {
			
			double t0                  = System.nanoTime();
			ZipOutputStream zos        = new ZipOutputStream(new FileOutputStream(fileName));
			PrintWriter writer         = new PrintWriter(new BufferedWriter(new OutputStreamWriter(zos)));

			// set compression
			zos.setLevel(6);
			
			// export files first
			exportDirectory(zos, new File("files"), "");

			// export database
			exportDatabase(zos, writer);
			
			// finish ZIP file
			zos.finish();

			// close stream
			writer.close();

			double t1   = System.nanoTime();
			double time = ((t1 - t0) / 1000000000.0);
			
		} catch (Throwable t) {
			
			t.printStackTrace();
			
			throw new FrameworkException(500, t.getMessage());
		}
	}
	
	private void importFile(final String fileName, boolean doValidation) throws FrameworkException {

		// open file for import
		
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(fileName));
			ZipEntry entry     = zis.getNextEntry();
			
			while (entry != null) {

				if (STRUCTR_ZIP_DB_NAME.equals(entry.getName())) {

					importDatabase(zis, doValidation);

				} else {
					
					// store other files in "files" dir..
					importDirectory(zis, entry);
				}

				entry = zis.getNextEntry();
			}
			
		} catch (IOException ioex) {

			ioex.printStackTrace();
		}	
	}	
	
	private void exportDirectory(ZipOutputStream zos, File dir, String path) throws IOException {
		
		String nestedPath = path + dir.getName() + "/";
		ZipEntry dirEntry = new ZipEntry(nestedPath);
		zos.putNextEntry(dirEntry);
		
		File[] contents = dir.listFiles();
		if (contents != null) {
			
			for (File file : contents) {
				
				if (file.isDirectory()) {
					
					exportDirectory(zos, file, nestedPath);
					
				} else {
				 	
					// create ZIP entry
					ZipEntry fileEntry  = new ZipEntry(nestedPath + file.getName());
					fileEntry.setTime(file.lastModified());
					zos.putNextEntry(fileEntry);

					// copy file into stream
					FileInputStream fis = new FileInputStream(file);
					IOUtils.copy(fis, zos);
					fis.close();
					
					// flush and close entry
					zos.flush();
					zos.closeEntry();
				}
			}
		}
		
		zos.closeEntry();
		
	}
	
	private void exportDatabase(ZipOutputStream zos, PrintWriter writer) throws IOException, FrameworkException {
		
		// start database zip entry
		GlobalGraphOperations ggop = GlobalGraphOperations.at(graphDb);
		ZipEntry dbEntry           = new ZipEntry(STRUCTR_ZIP_DB_NAME);
		int nodeCount              = 0;
		int relCount               = 0;
		
		zos.putNextEntry(dbEntry);

		for (Node node : ggop.getAllNodes()) {

			// ignore non-structr nodes
			if (node.hasProperty(GraphObject.uuid.dbName())) {

				writer.print("N");

				for (String key : node.getPropertyKeys()) {

					serialize(writer, key);
					serialize(writer, node.getProperty(key));
				}

				// do not use platform-specific line ending here!
				writer.print("\n");

				nodeCount++;
			}
		}

		writer.flush();

		for (Relationship rel : ggop.getAllRelationships()) {

			// ignore non-structr nodes
			if (rel.hasProperty(GraphObject.uuid.dbName())) {

				Node startNode = rel.getStartNode();
				Node endNode   = rel.getEndNode();

				if (startNode.hasProperty("uuid") && endNode.hasProperty("uuid")) {

					String startId = (String)startNode.getProperty("uuid");
					String endId   = (String)endNode.getProperty("uuid");

					writer.print("R");
					serialize(writer, startId);
					serialize(writer, endId);
					serialize(writer, rel.getType().name());

					for (String key : rel.getPropertyKeys()) {

						serialize(writer, key);
						serialize(writer, rel.getProperty(key));
					}

					// do not use platform-specific line ending here!
					writer.print("\n");

					relCount++;
				}

			}

		}

		writer.flush();

		// finish db entry
		zos.closeEntry();

		logger.log(Level.INFO, "Exported {0} nodes and {1} rels", new Object[] { nodeCount, relCount } );
	}
	
	private void importDirectory(ZipInputStream zis, ZipEntry entry) throws IOException {
		
		if (entry.isDirectory()) {
			
			File newDir = new File(entry.getName());
			if (!newDir.exists()) {
				
				newDir.mkdirs();
			}
			
		} else {
			
			File newFile      = new File(entry.getName());
			boolean overwrite = false;
			
			if (!newFile.exists()) {
				
				overwrite = true;
				
			} else {
				
				if (newFile.lastModified() < entry.getTime()) {
					
					logger.log(Level.INFO, "Overwriting existing file {0} because import file is newer.", entry.getName());
					overwrite = true;
				}
			}

			if (overwrite) {
				
				FileOutputStream fos = new FileOutputStream(newFile);
				IOUtils.copy(zis, fos);
				
				fos.flush();
				fos.close();
			}
		}
	}
	
	private void importDatabase(final ZipInputStream zis, boolean doValidation) throws FrameworkException {
	
		final Value<Long> nodeCountValue = new StaticValue<Long>(0L);
		final Value<Long> relCountValue  = new StaticValue<Long>(0L);
		double t0                        = System.nanoTime();
		
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction(doValidation) {

			@Override
			public Object execute() throws FrameworkException {

				Map<String, Node> uuidMap       = new LinkedHashMap<String, Node>();
				List<Relationship> rels         = new LinkedList<Relationship>();
				List<Node> nodes                = new LinkedList<Node>();
				PropertyContainer currentObject = null;
				BufferedReader reader           = null;
				String currentKey               = null;
				boolean finished                = false;
				long nodeCount                  = 0;
				long relCount                   = 0;
					
				try {
					reader = new BufferedReader(new InputStreamReader(zis));

					do {

						try {

							// store current position
							reader.mark(4);

							// read one byte
							String objectType = read(reader, 1);

							// skip newlines
							if ("\n".equals(objectType)) {
								continue;
							}

							if ("N".equals(objectType)) {

								currentObject = graphDb.createNode();
								nodeCount++;

								// store for later use
								nodes.add((Node)currentObject);

							} else if ("R".equals(objectType)) {

								String startId     = (String)deserialize(reader);
								String endId       = (String)deserialize(reader);
								String relTypeName = (String)deserialize(reader);

								Node endNode   = uuidMap.get(endId);
								Node startNode = uuidMap.get(startId);

								if (startNode != null && endNode != null) {

									RelationshipType relType = DynamicRelationshipType.withName(relTypeName);
									currentObject = startNode.createRelationshipTo(endNode, relType);

									// store for later use
									rels.add((Relationship)currentObject);
								}

								relCount++;

							} else {

								// reset if not at the beginning of a line
								reader.reset();

								if (currentKey == null) {

									currentKey = (String)deserialize(reader);

								} else {

									if (currentObject != null) {

										Object obj = deserialize(reader);

										if ("uuid".equals(currentKey) && currentObject instanceof Node) {

											String uuid = (String)obj;
											uuidMap.put(uuid, (Node)currentObject);
										}

										// store object in DB
										currentObject.setProperty(currentKey, obj);

										currentKey = null;

									} else {

										logger.log(Level.WARNING, "No current object to store property in.");
									}
								}
							}

						} catch (EOFException eofex) {

							finished = true;
						}

					} while (!finished);
					
				} catch (IOException ioex) {
				}
				
				logger.log(Level.INFO, "Imported {0} nodes and {1} rels, committing transaction..", new Object[] { nodeCount, relCount } );
				
				nodeCountValue.set(securityContext, nodeCount);
				relCountValue.set(securityContext, relCount);

				// make nodes visible in transaction context
				RelationshipFactory relFactory     = new RelationshipFactory(securityContext);
				NodeFactory nodeFactory            = new NodeFactory(securityContext);
				
				for (Node node : nodes) {
					TransactionCommand.nodeCreated(nodeFactory.instantiateNode(node));
				}
				
				for (Relationship rel : rels) {
					TransactionCommand.relationshipCreated(relFactory.instantiateRelationship(securityContext, rel));
				}
				
				return null;
			}

		});

		double t1   = System.nanoTime();
		double time = ((t1 - t0) / 1000000000.0);

		logger.log(Level.INFO, "Import done in {0} s", decimalFormat.format(time));
	}
}
