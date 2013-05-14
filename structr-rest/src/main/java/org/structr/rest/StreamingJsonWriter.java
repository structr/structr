package org.structr.rest;

import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.List;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class StreamingJsonWriter {

	private static final Logger logger                   = Logger.getLogger(StreamingJsonWriter.class.getName());
	private static final long MAX_SERIALIZATION_TIME     = TimeUnit.SECONDS.toMillis(30);

	private final Map<Class, Serializer> serializerCache = new LinkedHashMap<Class, Serializer>();
	private final Map<Class, Serializer> serializers     = new LinkedHashMap<Class, Serializer>();
	private final Serializer<GraphObject> root           = new RootSerializer();
	private final Set<Class> nonSerializerClasses        = new LinkedHashSet<Class>();
	private final Property<String> id                    = new StringProperty("id");
	private final int outputNestingDepth                 = Services.getOutputNestingDepth();
	private DecimalFormat decimalFormat                  = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	private PropertyKey idProperty                       = GraphObject.uuid;
	private SecurityContext securityContext              = null;
	private Value<String> propertyView                   = null;
	private boolean indent                               = true;
	//private JsonWriter writer                            = null;
	
	public StreamingJsonWriter(Value<String> propertyView, boolean indent) {

		this.securityContext = SecurityContext.getSuperUserInstance();
		this.propertyView    = propertyView;
		this.indent          = indent;

		serializers.put(GraphObject.class, root);
		serializers.put(PropertyMap.class, new PropertyMapSerializer());
		serializers.put(Iterable.class,    new IterableSerializer());
		serializers.put(Map.class,         new MapSerializer());
		
		nonSerializerClasses.add(Object.class);
		nonSerializerClasses.add(String.class);
		nonSerializerClasses.add(Integer.class);
		nonSerializerClasses.add(Long.class);
		nonSerializerClasses.add(Double.class);
		nonSerializerClasses.add(Float.class);
		nonSerializerClasses.add(Byte.class);
		nonSerializerClasses.add(Character.class);
		nonSerializerClasses.add(StringBuffer.class);
		nonSerializerClasses.add(Boolean.class);
		
		//this.writer = new JsonWriter(writer);
		//this.writer.setIndent("   ");
	}
	
	public void stream(Writer w, Result src) throws IOException {
		
		long t0 = System.nanoTime();
		
		JsonWriter writer = new JsonWriter(w);
		
		if (indent) {
			writer.setIndent("   ");
		}
		
		// result fields in alphabetical order
		List<? extends GraphObject> results = src.getResults();
		Integer page = src.getPage();
		Integer pageCount = src.getPageCount();
		Integer pageSize = src.getPageSize();
		String queryTime = src.getQueryTime();
		Integer resultCount = src.getRawResultCount();
		String searchString = src.getSearchString();
		String sortKey = src.getSortKey();
		String sortOrder = src.getSortOrder();
		
		// flush after 20 elements by default
		int flushSize = pageSize != null ? pageSize.intValue() : 20;

		// open result set
		writer.beginObject();
		
		if(page != null) {
			writer.name("page").value(page);
		}

		if(pageCount != null) {
			writer.name("page_count").value(pageCount);
		}

		if(pageSize != null) {
			writer.name("page_size").value(pageSize);
		}

		if(queryTime != null) {
			writer.name("query_time").value(queryTime);
		}

		if(resultCount != null) {
			writer.name("result_count").value(resultCount);
		}

		if(results != null) {

			if(results.isEmpty()) {

				writer.name("result").beginArray().endArray();

			} else if(src.isPrimitiveArray()) {

				writer.name("result").beginArray();
				
				for(GraphObject graphObject : results) {
					
					Object value = graphObject.getProperty(AbstractNode.uuid);	// FIXME: UUID key hard-coded, use variable in Result here!
					if(value != null) {
						
						writer.value(value.toString());
					}
				}

				writer.endArray();


			} else {

				if (results.size() > 1 && !src.isCollection()){
					throw new IllegalStateException(src.getClass().getSimpleName() + " is not a collection resource, but result set has size " + results.size());
				}

				// keep track of serialization time
				long startTime            = System.currentTimeMillis();
				String localPropertyView  = propertyView.get(null);
				int flushCounter          = 0;

				if(src.isCollection()) {

					writer.name("result").beginArray();

					// serialize list of results
					for(GraphObject graphObject : results) {
						
						root.serialize(writer, graphObject, localPropertyView, 0);

						// flush every once in a while
						if ((++flushCounter % flushSize) == 0) {
							writer.flush();
						}
						
						// check for timeout
						if (System.currentTimeMillis() > startTime + MAX_SERIALIZATION_TIME) {

							logger.log(Level.SEVERE, "JSON serialization took more than {0} ms, aborted. Please review output view size or adjust timeout.", MAX_SERIALIZATION_TIME);
							writer.flush();
							
							// TODO: create some output indicating that streaming was interrupted
							break;
						}
					}

					writer.endArray();

				} else {

					writer.name("result");
					root.serialize(writer, results.get(0), localPropertyView, 0);
				}
			}
		}

		if(searchString != null) {
			writer.name("search_string").value(searchString);
		}

		if(sortKey != null) {
			writer.name("sort_key").value(sortKey);
		}

		if(sortOrder != null) {
			writer.name("sort_order").value(sortOrder);
		}
		
		writer.name("serialization_time").value(decimalFormat.format((System.nanoTime() - t0) / 1000000000.0));

		// finished
		writer.endObject();
		writer.flush();
	}

	private Serializer getSerializerForType(Class type) {

		Class localType       = type;
		Serializer serializer = serializerCache.get(type);

		if (serializer == null && !nonSerializerClasses.contains(type)) {

			do {
				serializer = serializers.get(localType);
				
				if (serializer == null) {

					Set<Class> interfaces = new LinkedHashSet<Class>();
					collectAllInterfaces(localType, interfaces);

					for (Class interfaceType : interfaces) {

						serializer = serializers.get(interfaceType);

						if (serializer != null) {
							break;
						}
					}
				}
				
				localType = localType.getSuperclass();

			} while (serializer == null && !localType.equals(Object.class));
			
			
			// cache found serializer
			if (serializer != null) {
				serializerCache.put(type, serializer);
			}
		}
		
		return serializer;
	}
	
	private void collectAllInterfaces(Class type, Set<Class> interfaces) {

		if (interfaces.contains(type)) {
			return;
		}
		
		for (Class iface : type.getInterfaces()) {
			
			collectAllInterfaces(iface, interfaces);
			interfaces.add(iface);
		}
	}

	private void serializePrimitive(JsonWriter writer, final Object value) throws IOException {

		if (value != null) {

			if (value instanceof Number) {

				writer.value((Number)value);

			} else if (value instanceof Boolean) {

				writer.value((Boolean)value);

			} else {

				writer.value(value.toString());
			}
			
		} else {
		
			writer.nullValue();
		}
	}
	
	public abstract class Serializer<T> {
		
		public abstract void serialize(JsonWriter writer, T value, String localPropertyView, int depth) throws IOException;
		
		public void serializeRoot(JsonWriter writer, Object value, String localPropertyView, int depth) throws IOException {
			
			if (value != null) {
				
				Serializer serializer = getSerializerForType(value.getClass());
				if (serializer != null) {

					serializer.serialize(writer, value, localPropertyView, depth);
					
					return;
				}
			}
					
			serializePrimitive(writer, value);
		}
		
		public void serializeProperty(JsonWriter writer, PropertyKey key, Object value, String localPropertyView, int depth) {

			try {
				PropertyConverter converter = key.inputConverter(securityContext);

				if (converter != null) {

					Object convertedValue = null;
					
					// ignore conversion errors
					try { convertedValue = converter.revert(value); } catch (Throwable t) {}
					
					serializeRoot(writer, convertedValue, localPropertyView, depth);

				} else {

					serializeRoot(writer, value, localPropertyView, depth);
				}

			} catch(Throwable t) {

				// CHM: remove debug code later
				t.printStackTrace();

				logger.log(Level.WARNING, "Exception while serializing property {0} ({1}) of entity {2} (value {3}) : {4}", new Object[] {
					key.jsonName(),
					key.getClass(),
					key.getClass().getDeclaringClass(),
					value.getClass().getName(),
					value,
					t.getMessage()
				});
			}
		}
	}
	
	public class RootSerializer extends Serializer<GraphObject> {

		@Override
		public void serialize(JsonWriter writer, GraphObject source, String localPropertyView, int depth) throws IOException {
			
			writer.beginObject();

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {

				
				/*
				// id (only if idProperty is not set)
				if (idProperty == null) {

					writer.name("id").value(source.getId());

				} else {

					Object idPropertyValue = source.getProperty(idProperty);
					if (idPropertyValue != null) {

						writer.name("id").value(idPropertyValue.toString());
					}

				}
				*/
				
				// property keys
				Iterable<PropertyKey> keys = source.getPropertyKeys(localPropertyView);
				if(keys != null) {

					for (PropertyKey key : keys) {

						Object value = source.getProperty(key);
						PropertyKey localKey = key;

						if (localKey.equals(idProperty)) {
							localKey = id;
						}

						if (value != null) {

							writer.name(localKey.jsonName());
							serializeProperty(writer, key, value, localPropertyView, depth+1);

						} else {

							writer.name(localKey.jsonName()).nullValue();
						}
					}
				}
			}
			
			writer.endObject();
		}
	}
	
	public class IterableSerializer extends Serializer<Iterable> {

		@Override
		public void serialize(JsonWriter writer, Iterable value, String localPropertyView, int depth) throws IOException {

			writer.beginArray();

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {

				for (Object o : value) {

					serializeRoot(writer, o, localPropertyView, depth);
				}
			}

			writer.endArray();
		}
	}
	
	public class MapSerializer extends Serializer {

		@Override
		public void serialize(JsonWriter writer, Object source, String localPropertyView, int depth) throws IOException {

			writer.beginObject();

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {

				for (Map.Entry<String, Object> entry : ((Map<String, Object>)source).entrySet()) {

					String key = entry.getKey();
					Object value = entry.getValue();

					// id property mapping again..
					if (idProperty.jsonName().equals(key)) {
						key = "id";
					}

					writer.name(key);
					serializeRoot(writer, value, localPropertyView, depth+1);
				}
			}
			
			writer.endObject();
		}
	}
	
	public class PropertyMapSerializer extends Serializer<PropertyMap> {
		
		public PropertyMapSerializer() {}

		@Override
		public void serialize(JsonWriter writer, PropertyMap source, String localPropertyView, int depth) throws IOException {

			writer.beginObject();

			// prevent endless recursion by pruning at depth n
			if (depth <= outputNestingDepth) {

				for (Map.Entry<PropertyKey, Object> entry : source.entrySet()) {

					PropertyKey key = entry.getKey();
					if (key.equals(idProperty)) {

						key = id;
					}

					writer.name(key.jsonName());
					serializeProperty(writer, key, entry.getValue(), localPropertyView, depth+1);
				}
			}

			writer.endObject();
		}
	}
}
