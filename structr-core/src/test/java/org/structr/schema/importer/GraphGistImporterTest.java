package org.structr.schema.importer;

import java.util.LinkedHashMap;
import java.util.Map;
import org.structr.common.StructrTest;

/**
 *
 * @author chrisi
 */
public class GraphGistImporterTest extends StructrTest {

	public void test() {
		
		final GraphGistImporter importer = app.command(GraphGistImporter.class);
		final Map<String, Object> params = new LinkedHashMap<>();
		
		params.put("file", "/home/chrisi/structr-ui/test.gist");

		try {
			importer.execute(params);
			
		} catch (Throwable t) {
			
			t.printStackTrace();
		}
	}
}