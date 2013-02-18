package org.structr.core.experimental;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.module.ModuleService;

/**
 *
 * @author Christian Morgner (christian@morgner.de)
 */
public class NodeExtender<T extends AbstractNode> {

	private static final JavaCompiler compiler             = ToolProvider.getSystemJavaCompiler();
	private static final JavaFileManager fileManager       = new ClassFileManager(compiler.getStandardFileManager(null, null, null));
	private static final Map<String, Class> dynamicClasses = new LinkedHashMap<String, Class>();

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
				
				// very experimental: store newly created dynamic class in EntityContext
				EntityContext.init(dynamicClass);
				Services.getService(ModuleService.class).getCachedNodeEntities().put(simpleName, dynamicClass);
				
				Object newEntity = dynamicClass.newInstance();
				EntityContext.scanEntity(newEntity);
				
			} catch (Throwable ignore) {
				
				
				ignore.printStackTrace();
			}
		}
		
		return dynamicClass;
	}
	
	private synchronized Class compile(String className) throws ClassNotFoundException {

		// Here we specify the source code of the class to be compiled
		StringBuilder src = new StringBuilder();

		src.append("package ").append(packageName).append(";\n\n");
		src.append("import ").append(baseType.getName()).append(";\n\n");
		src.append("public class ").append(className).append(" extends ").append(baseType.getSimpleName()).append(" {\n");
		src.append("}\n");

		List<JavaFileObject> jfiles = new ArrayList<JavaFileObject>();

		jfiles.add(new CharSequenceJavaFileObject(className, src));
		compiler.getTask(null, fileManager, null, null, null, jfiles).call();
		
		return fileManager.getClassLoader(null).loadClass(packageName.concat(".".concat(className)));
	}
}
