/*
 *  Copyright (C) 2011 Axel Morgner
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



package org.structr.core.node;

import org.structr.common.CurrentRequest;
import org.structr.common.CurrentSession;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.node.operation.AsOperation;
import org.structr.core.node.operation.Callback;
import org.structr.core.node.operation.CdOperation;
import org.structr.core.node.operation.ClearOperation;
import org.structr.core.node.operation.CopyOperation;
import org.structr.core.node.operation.CreateOperation;
import org.structr.core.node.operation.DeleteOperation;
import org.structr.core.node.operation.InOperation;
import org.structr.core.node.operation.InvalidOperationException;
import org.structr.core.node.operation.InvalidParameterException;
import org.structr.core.node.operation.InvalidSwitchException;
import org.structr.core.node.operation.LinkOperation;
import org.structr.core.node.operation.ListOperation;
import org.structr.core.node.operation.MooOperation;
import org.structr.core.node.operation.MoveOperation;
import org.structr.core.node.operation.NodeCommandException;
import org.structr.core.node.operation.OnOperation;
import org.structr.core.node.operation.Operation;
import org.structr.core.node.operation.PrimaryOperation;
import org.structr.core.node.operation.SetOperation;
import org.structr.core.node.operation.Transformation;
import org.structr.core.node.operation.UnsetOperation;
import org.structr.core.node.operation.UsingOperation;
import org.structr.core.node.operation.WithOperation;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class NodeConsoleCommand extends NodeServiceCommand {

	public static final String CONSOLE_BUFFER_KEY = "consoleOutputLines";
	private static final Logger logger            = Logger.getLogger(NodeConsoleCommand.class.getName());
	public static final Map<String, Class<? extends Operation>> operationsMap;

	//~--- static initializers --------------------------------------------

	// serves as a static constructor
	static {

		operationsMap = Collections.synchronizedMap(new TreeMap<String, Class<? extends Operation>>());
		operationsMap.put("mk", CreateOperation.class);
		operationsMap.put("in", InOperation.class);
		operationsMap.put("on", OnOperation.class);
		operationsMap.put("as", AsOperation.class);
		operationsMap.put("with", WithOperation.class);
		operationsMap.put("using", UsingOperation.class);
		operationsMap.put("rm", DeleteOperation.class);
		operationsMap.put("cd", CdOperation.class);
		operationsMap.put("set", SetOperation.class);
		operationsMap.put("ln", LinkOperation.class);
		operationsMap.put("clear", ClearOperation.class);
		operationsMap.put("mv", MoveOperation.class);
		operationsMap.put("cp", CopyOperation.class);
		operationsMap.put("ls", ListOperation.class);
		operationsMap.put("moo", MooOperation.class);
		operationsMap.put("unset", UnsetOperation.class);
	}

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) {

		StringBuilder ret        = new StringBuilder(200);
		AbstractNode currentNode = null;
		Callback callback        = null;
		String commandLine       = null;

		if (parameters.length == 3) {

			if (parameters[0] instanceof AbstractNode) {
				currentNode = (AbstractNode) parameters[0];
			}

			if (parameters[1] instanceof String) {
				commandLine = (String) parameters[1];
			}

			if (parameters[2] instanceof Callback) {
				callback = (Callback) parameters[2];
			}

			if (commandLine != null) {

				User user         = CurrentSession.getUser();
				boolean superUser = (user != null) && (user instanceof SuperUser);

				ret.append("<p>");
				ret.append((user != null)
					   ? user.getName()
					   : "anonymous");
				ret.append("@structr");
				ret.append(superUser
					   ? "# "
					   : "$ ");
				ret.append(commandLine);
				ret.append("</p>");

				Object[] commands = splitCommandLine(commandLine);

				if (commands.length > 0) {

					/*
					 * ret.append("<p class=\"debug\">");
					 * ret.append("DEBUG: command line:");
					 * for(Object o : commands) ret.append(" '").append(o).append("'");
					 * ret.append("</p>");
					 */

					// now we have an array of strings that represents the command line
					// we take the first token and extract the primary operation from it
					// lets use the following line as a first example:
					// create "foo" in 0 as Domain
					// "create" is the primary operation, which takes 1 parameter ("foo")
					// "in" is the second operation, taking 1 parameter ("0")
					// "as" is the third, takin 1 parameter ("Domain")
					// the primary operation is instantiated from our command map, and all
					// subsequent operations are added to it
					try {

						PrimaryOperation primaryOperation = null;

						for (int currentPosition = 0; currentPosition < commands.length;
							currentPosition++) {

							Operation operation = getOperation(commands[currentPosition]);

							if (operation != null) {

								if (currentNode != null) {
									operation.setCurrentNode(currentNode);
								}

								// advance current position according to parameters
								currentPosition += handleParameters(ret,
									currentPosition, commands, operation);

								// set operation
								if (primaryOperation == null) {

									if (operation instanceof PrimaryOperation) {

										primaryOperation =
											(PrimaryOperation) operation;

										if (callback != null) {

											primaryOperation.addCallback(
											    callback);
										}

									} else {

										throw new InvalidOperationException(
										    operation.getKeyword()
										    + " is not a primary operation");
									}

								} else {

									if (operation instanceof Transformation) {

										Transformation transformation =
											(Transformation) operation;

										transformation.transform(
										    primaryOperation);

									} else {

										throw new InvalidOperationException(
										    operation.getKeyword()
										    + " is not a transformation");
									}
								}

							} else {

								throw new InvalidOperationException(
								    commands[currentPosition].toString()
								    + " not found");
							}
						}

						if ((primaryOperation != null) && primaryOperation.canExecute()) {

							StringBuilder stdOut = new StringBuilder(200);

							if (!primaryOperation.executeOperation(stdOut)) {
								ret.append("<p class=\"error\">Execution failed</p>\n");
							}

							if (stdOut.length() > 0) {

								ret.append("<p>");
								ret.append(stdOut.toString());
								ret.append("</p>\n");
							}

						} else {
							ret.append("<p class=\"error\">Cannot execute</p>\n");
						}

					} catch (NodeCommandException ncex) {

						ret.append("<p class=\"error\">ERROR: ").append(
						    ncex.getMessage()).append("</p>\n");
					}
				}
			}
		}

		return (ret.toString());
	}

	private int handleParameters(StringBuilder out, int currentPosition, Object[] commands, Operation operation)
		throws InvalidParameterException, InvalidSwitchException {

		int parameterCount = operation.getParameterCount();
		int ret            = 0;

		/*
		 * if(commands.length == 1 && operation instanceof PrimaryOperation && operation.getParameterCount() != 0) {
		 *
		 *       out.append(((PrimaryOperation)operation).help());
		 *
		 * } else if(commands.length < currentPosition + operation.getParameterCount() + 1) {
		 * }
		 */

		// operation found, add parameters
		for (int j = 0; j < parameterCount; j++) {

			ret += 1;

			int parameterPosition = currentPosition + ret;

			if (parameterPosition < commands.length) {

				Object parameter = commands[parameterPosition];

				// try to identifiy switches
				if (parameter.toString().startsWith("-")) {

					operation.addSwitch(parameter.toString());
					parameterCount++;

				} else {
					operation.addParameter(parameter);
				}
			}
		}

		if (!operation.canExecute()) {

			throw new InvalidParameterException(operation.getKeyword() + " needs "
				+ operation.getParameterCount() + " parameters");
		}

		return (ret);
	}

	private Object[] splitCommandLine(String commandLine) {

		ArrayList<String> parts   = new ArrayList<String>();
		StringBuilder currentPart = new StringBuilder(50);
		int len                   = commandLine.length();
		boolean inQuotes          = false;
		boolean equals            = false;

		for (int i = 0; i < len; i++) {

			char ch = commandLine.charAt(i);

			switch (ch) {

				case '\'' :
				case '"' :
					inQuotes = !inQuotes;

					if (!equals) {

						if (currentPart.toString().trim().length() > 0) {
							parts.add(currentPart.toString().trim());
						}

						currentPart.setLength(0);
					}

					break;

				case '=' :
					if (!inQuotes) {
						equals = true;
					}

					currentPart.append("=");

					break;

				case ',' :
					if (!inQuotes) {

						if (currentPart.toString().trim().length() > 0) {
							parts.add(currentPart.toString().trim());
						}

						currentPart.setLength(0);
						parts.add(",");
					}
				case ' ' :
					if (inQuotes) {
						currentPart.append(ch);
					} else {

						if (currentPart.toString().trim().length() > 0) {
							parts.add(currentPart.toString().trim());
						}

						currentPart.setLength(0);
					}

					break;

				default :
					equals = false;
					currentPart.append(ch);

					break;
			}
		}

		// add last part
		if (currentPart.toString().trim().length() > 0) {
			parts.add(currentPart.toString().trim());
		}

		List<String> secondPassResult = new LinkedList<String>();

		{

			// second pass, combine elements with '=' between them to a single String
			StringBuilder buf = new StringBuilder(40);
			int size          = parts.size();

			for (int i = 0; i < size; i++) {

				String nextPart = (parts.size() > i + 1)
						  ? parts.get(i + 1)
						  : null;
				String part     = parts.get(i);

				// next part is a list separator, add current part to list
				if ("=".equals(nextPart)) {

					buf.append(part);
					buf.append("=");
					i++;

				} else {

					// finish list and start a new one
					if (buf.length() > 0) {

						buf.append(part);
						secondPassResult.add(buf.toString());
						buf.setLength(0);

					} else {
						secondPassResult.add(part);
					}
				}
			}

			if (buf.length() > 0) {
				secondPassResult.add(buf.toString());
			}
		}

		ArrayList thirdPassResult = new ArrayList();

		{

			// third pass, combine elements with ',' between them to a List<String>
			List<String> list = new LinkedList<String>();
			int size          = secondPassResult.size();

			for (int i = 0; i < size; i++) {

				String nextPart = (secondPassResult.size() > i + 1)
						  ? secondPassResult.get(i + 1)
						  : null;
				String part     = secondPassResult.get(i);

				// next part is a list separator, add current part to list
				if (",".equals(nextPart)) {

					list.add(part);
					i++;

				} else {

					// finish list and start a new one
					if (!list.isEmpty()) {

						list.add(part);
						thirdPassResult.add(list);
						list = new LinkedList<String>();

					} else {
						thirdPassResult.add(part);
					}
				}
			}

			if (!list.isEmpty()) {
				thirdPassResult.add(list);
			}
		}

		return (thirdPassResult.toArray());
	}

	//~--- get methods ----------------------------------------------------

	private Operation getOperation(Object name) throws InvalidOperationException {

		if (name instanceof Collection) {
			throw new InvalidOperationException("Invalid operation");
		} else {

			Class<? extends Operation> clazz = operationsMap.get(name.toString());
			Operation ret                    = null;

			if (clazz != null) {

				try {
					ret = clazz.newInstance();
				} catch (Throwable t) {}
			}

			return (ret);
		}
	}
}
