package org.structr.rest.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.structr.common.error.FrameworkException;
import org.structr.core.IJsonInput;
import org.structr.core.Result;
import org.structr.core.Value;
import org.structr.core.app.StructrApp;
import org.structr.rest.JsonInputGSONAdapter;
import org.structr.rest.adapter.FrameworkExceptionGSONAdapter;
import org.structr.rest.adapter.ResultGSONAdapter;

/**
 *
 * @author Christian Morgner
 */
public class ThreadLocalGson extends ThreadLocal<Gson> {

	private int outputNestingDepth     = 3;
	private Value<String> propertyView = null;

	public ThreadLocalGson(final Value<String> propertyView, final int outputNestingDepth) {
		
		this.propertyView       = propertyView;
		this.outputNestingDepth = outputNestingDepth;
	}

	@Override
	protected Gson initialValue() {

		ResultGSONAdapter resultGsonAdapter   = new ResultGSONAdapter(propertyView, outputNestingDepth);
		JsonInputGSONAdapter jsonInputAdapter = new JsonInputGSONAdapter();

		// create GSON serializer
		final GsonBuilder gsonBuilder = new GsonBuilder()
			.setPrettyPrinting()
			.serializeNulls()
			.registerTypeHierarchyAdapter(FrameworkException.class, new FrameworkExceptionGSONAdapter())
			.registerTypeAdapter(IJsonInput.class, jsonInputAdapter)
			.registerTypeAdapter(Result.class, resultGsonAdapter);


		final boolean lenient = Boolean.parseBoolean(StructrApp.getConfigurationValue("json.lenient", "false"));
		if (lenient) {

			// Serializes NaN, -Infinity, Infinity, see http://code.google.com/p/google-gson/issues/detail?id=378
			gsonBuilder.serializeSpecialFloatingPointValues();

		}

		return gsonBuilder.create();
	}
}
