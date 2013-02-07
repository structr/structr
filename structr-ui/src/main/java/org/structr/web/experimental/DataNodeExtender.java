package org.structr.web.experimental;

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
import org.structr.core.module.ModuleService;
import org.structr.web.entity.DataNode;

/**
 *
 * @author Christian Morgner (christian@morgner.de)
 */
public class DataNodeExtender {

	private static final JavaCompiler compiler             = ToolProvider.getSystemJavaCompiler();
	private static final JavaFileManager fileManager       = new ClassFileManager(compiler.getStandardFileManager(null, null, null));
	private static final String prefix                     = "org.structr.web.entity.dynamic.";
	private static final Map<String, Class> dynamicClasses = new LinkedHashMap<String, Class>();

	public DataNode instantiateExtendedDataNode(String simpleName) {

		Class dynamicClass = getType(simpleName);
		if (dynamicClass != null) {
			
			try {

				return (DataNode)dynamicClass.newInstance();
				
			} catch (Throwable ignore) { }
		}
		
		return null;
	}
	
	public Class getType(String simpleName) {

		Class dynamicClass = dynamicClasses.get(simpleName);
		if (dynamicClass == null) {
			
			try {
				dynamicClass = compile(simpleName);
				dynamicClasses.put(simpleName, dynamicClass);
				
				// very experimental: store newly created dynamic class in EntityContext
				EntityContext.init(dynamicClass);
				Services.getService(ModuleService.class).getCachedNodeEntities().put(simpleName, dynamicClass);
				
				Object newEntity = dynamicClass.newInstance();
				EntityContext.scanEntity(newEntity);
				
			} catch (Throwable ignore) { }
		}
		
		return dynamicClass;
	}
	
	private Class compile(String className) throws ClassNotFoundException {

		// Here we specify the source code of the class to be compiled
		StringBuilder src = new StringBuilder();

		src.append("package org.structr.web.entity.dynamic;\n\n");
		src.append("import org.structr.web.entity.DataNode;\n\n");
		src.append("public class ").append(className).append(" extends DataNode {\n");
		src.append("}\n");

		List<JavaFileObject> jfiles = new ArrayList<JavaFileObject>();

		jfiles.add(new CharSequenceJavaFileObject(className, src));
		compiler.getTask(null, fileManager, null, null, null, jfiles).call();
		
		return fileManager.getClassLoader(null).loadClass(prefix.concat(className));
	}
}
