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
package org.structr.core.experimental;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner (christian@morgner.de)
 */
public class NodeExtender<T extends NodeInterface> {

	private static final JavaCompiler compiler             = ToolProvider.getSystemJavaCompiler();
	private static final JavaFileManager fileManager       = new ClassFileManager(compiler.getStandardFileManager(null, null, null));
	private static final Map<String, Class> dynamicClasses = new LinkedHashMap<>();

	private String packageName = null;
	private Class baseType     = null;
	
	public NodeExtender(Class baseType, String packageName) {
		
		this.baseType = baseType;
		this.packageName = packageName;
	}
	
	public T instantiateExtendedDataNode(String simpleName) {

		Class dynamicClass = getType(simpleName);
		if (dynamicClass != null) {
			
			try {

				return (T)dynamicClass.newInstance();
				
			} catch (Throwable ignore) { 
				
				ignore.printStackTrace();
			}
		}
		
		return null;
	}
	
	public Class<T> getType(String simpleName) {

		Class<T> dynamicClass = dynamicClasses.get(simpleName);
		if (dynamicClass == null) {
			
			try {
				dynamicClass = compile(simpleName);
				dynamicClasses.put(simpleName, dynamicClass);
				
				// very experimental: store newly created dynamic class in SchemaHelper
				StructrApp.getConfiguration().registerEntityType(dynamicClass);
				StructrApp.getConfiguration().getNodeEntities().put(simpleName, dynamicClass);
				
			} catch (Throwable ignore) {
				
				ignore.printStackTrace();
			}
		}
		
		return dynamicClass;
	}
	
	private synchronized Class compile(String className) throws ClassNotFoundException {

		StringBuilder src = new StringBuilder();

		src.append("package ").append(packageName).append(";\n\n");
		src.append("import ").append(baseType.getName()).append(";\n\n");
		src.append("public class ").append(className).append(" extends ").append(baseType.getSimpleName()).append(" {\n");
		src.append("}\n");

		List<JavaFileObject> jfiles = new ArrayList<>();

		jfiles.add(new CharSequenceJavaFileObject(className, src));
		compiler.getTask(null, fileManager, null, null, null, jfiles).call();
		
		return fileManager.getClassLoader(null).loadClass(packageName.concat(".".concat(className)));
	}
}
