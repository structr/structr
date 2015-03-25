package org.structr.schema.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.schema.json.InvalidSchemaException;
import org.structr.schema.json.JsonReferenceProperty;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonType;


/**
 *
 * @author Christian Morgner
 */
public class StructrSchemaDefinition extends StructrDefinition implements JsonSchema {

	private final Map<String, SymmetricReference> referenceMapping = new LinkedHashMap<>();

	StructrSchemaDefinition() throws URISyntaxException {
		super(null, JsonSchema.SCHEMA_ID);
	}

	StructrSchemaDefinition(final JsonSchema source) throws URISyntaxException {

		super(null, JsonSchema.SCHEMA_ID);

		put(JsonSchema.KEY_SCHEMA, JsonSchema.SCHEMA_ID);
		createFromSource(source);
	}

	@Override
	public StructrDefinition getParent() {
		return null;
	}

	// ----- interface JsonSchema -----
	@Override
	public Set<JsonType> getTypes() {

		final Map<String, StructrTypeDefinition> types = getTypeDefinitions();
		final Set<JsonType> typeSet              = new TreeSet<>();

		for (final StructrTypeDefinition type : types.values()) {
			typeSet.add(type);
		}

		return typeSet;
	}

	@Override
	public JsonType addType(final String name) throws URISyntaxException {

		final StructrTypeDefinition newType = new StructrTypeDefinition(this, "definitions/" + name);
		newType.setName(name);

		if (getTypeDefinitions().containsKey(name)) {
			throw new IllegalStateException("Type " + name + " already exists.");
		}

		// store new type
		getTypeDefinitions().put(name, newType);

		return newType;
	}

	@Override
	public String toString() {

		final Gson gson           = new GsonBuilder().setPrettyPrinting().create();
		final StringWriter writer = new StringWriter();

		gson.toJson(this, writer);

		return writer.getBuffer().toString();
	}

	@Override
	public String getTitle() {
		return (String)get(JsonSchema.KEY_TITLE);
	}

	@Override
	public void setTitle(final String title) {
		put(JsonSchema.KEY_TITLE, title);
	}

	@Override
	public String getDescription() {
		return (String)get(JsonSchema.KEY_DESCRIPTION);
	}

	@Override
	public void setDescription(final String description) {
		put(JsonSchema.KEY_DESCRIPTION, description);
	}

	// ----- package methods -----
	Map<String, StructrTypeDefinition> getTypeDefinitions() {
		return (Map)getMap(this, JsonSchema.KEY_DEFINITIONS, true);
	}

	void createFromDatabase(final App app) throws FrameworkException, URISyntaxException {

		final List<SchemaNode> types = app.nodeQuery(SchemaNode.class).sort(AbstractNode.name).getAsList();
		final Map<String, StructrTypeDefinition> typeDefinitions = getTypeDefinitions();

		for (final SchemaNode schemaNode : types) {

			final String name = schemaNode.getName();
			final StructrTypeDefinition type = new StructrTypeDefinition(this, "definitions/" + name);
			type.createFromDatabase(schemaNode);

			typeDefinitions.put(name, type);
		}
	}

	void createDatabaseSchema(final App app) throws FrameworkException {

		final Map<String, StructrTypeDefinition> typeDefinitions = getTypeDefinitions();
		for (final StructrTypeDefinition type : typeDefinitions.values()) {

			final SchemaNode schemaNode = app.create(SchemaNode.class, type.getName());
			type.setSchemaNode(schemaNode);
		}

		// second pass, resolve inheritance
		for (final StructrTypeDefinition type : typeDefinitions.values()) {

			final String extendsReference = type.getExtends();
			if (extendsReference != null) {

				final StructrDefinition def = resolveJsonPointer(extendsReference);
				final SchemaNode schemaNode = type.getSchemaNode();

				if (def != null && def instanceof JsonType) {

					final JsonType jsonType = (JsonType)def;
					final String superclassName = "org.structr.dynamic." + jsonType.getName();

					schemaNode.setProperty(SchemaNode.extendsClass, superclassName);

				} else {

					try {
						final Class superclass = StructrApp.resolveSchemaId(new URI(extendsReference));
						if (superclass != null) {

							schemaNode.setProperty(SchemaNode.extendsClass, superclass.getName());
						}

					} catch (URISyntaxException uex) {
						uex.printStackTrace();
					}
				}
			}
		}

		// Create properties in a separate run because all SchemaNodes must exist
		// before any property is created in order to resolve $ref references.
		for (final StructrTypeDefinition type : typeDefinitions.values()) {
			type.createDatabaseSchemaProperties(app, type.getSchemaNode());
		}
	}

	void createFromSource(final Map<String, Object> source) throws InvalidSchemaException, URISyntaxException {

		setDescription(getString(source, JsonSchema.KEY_DESCRIPTION));
		setTitle(getString(source, JsonSchema.KEY_TITLE));

		final Map<String, StructrTypeDefinition> typeDefinitions = getTypeDefinitions();

		final Map<String, Object> types = getMap(source, KEY_DEFINITIONS, false);
		if (types != null) {

			for (final Entry<String, Object> entry : types.entrySet()) {

				final String key   = entry.getKey();
				final Object value = entry.getValue();

				if (value instanceof Map) {

					final Map<String, Object> map   = (Map<String, Object>)value;
					final StructrTypeDefinition def = new StructrTypeDefinition(this, "definitions/" + key);

					def.setName(key);
					def.createFromSource(map);

					typeDefinitions.put(key, def);

				} else {

					throw new InvalidSchemaException("Type definition " + key + " has wrong type, expecting object.");
				}
			}

		} else {

			throw new InvalidSchemaException("No type definitions found.");
		}

		if (typeDefinitions.isEmpty()) {
			remove(JsonSchema.KEY_DEFINITIONS);
		}
	}

	void notifyReferenceChange(final StructrTypeDefinition sourceType, final JsonReferenceProperty property) {

		final String reference = property.getReference();
		if (reference != null) {

			final StructrTypeDefinition targetType = (StructrTypeDefinition)sourceType.resolveJsonPointer(reference);
			if (targetType != null) {

				// create a key that is independent of the direction of a relationship
				final String directionIndependentKey = createDirectionIndependentKey(sourceType.getName(), targetType.getName(), property.getRelationship());

				// use this key to obtain (or create) a symmetric reference instance
				SymmetricReference symmetricReference = referenceMapping.get(directionIndependentKey);
				if (symmetricReference == null) {

					symmetricReference = new SymmetricReference();
					referenceMapping.put(directionIndependentKey, symmetricReference);
				}

				symmetricReference.registerProperty(sourceType, property);
			}
		}
	}

	// ----- private methods -----
	private void createFromSource(final JsonSchema source) throws URISyntaxException {

		setDescription(source.getDescription());
		setTitle(source.getTitle());

		final Map<String, StructrTypeDefinition> typeDefinitions = getTypeDefinitions();

		for (final JsonType type : source.getTypes()) {

			final String name = type.getName();
			typeDefinitions.put(name, new StructrTypeDefinition(this, "definitions/" + name, type));
		}
	}

	private String createDirectionIndependentKey(final String type1, final String type2, final String relationship) {

		final List<Character> characters = new LinkedList<>();

		// add characters from both strings to the list
		for (final char c : type1.toLowerCase().toCharArray()) { characters.add(c); }
		for (final char c : type2.toLowerCase().toCharArray()) { characters.add(c); }

		if (relationship != null) {
			for (final char c : relationship.toLowerCase().toCharArray()) { characters.add(c); }
		}

		// sort character list
		Collections.sort(characters);

		// assemble sorted character list into string
		final StringBuilder buf = new StringBuilder();
		for (final Character c : characters) {
			buf.append(c);
		}

		return buf.toString();
	}

	// ----- nested classes -----
	private class SymmetricReference {

		private StructrTypeDefinition sourceType  = null;
		private StructrTypeDefinition targetType  = null;
		private JsonReferenceProperty outProperty = null;
		private JsonReferenceProperty inProperty = null;
		private Direction direction               = null;

		public void registerProperty(final StructrTypeDefinition _sourceType, final JsonReferenceProperty _property) {

			final StructrTypeDefinition _targetType = (StructrTypeDefinition)_sourceType.resolveJsonPointer(_property.getReference());
			Direction directionFromProperty         = _property.getDirection();
			boolean directionChanged                = false;

			// default direction: out, can be changed later
			if (directionFromProperty == null) {

				// property has no direction.. check if direction is already set on this
				// relationship and modify direction accordingly

				if (this.direction != null) {

					switch (this.direction) {

						case in:
							directionFromProperty = Direction.out;
							_property.setDirection(Direction.out);
							break;

						case out:
							directionFromProperty = Direction.in;
							_property.setDirection(Direction.in);
							break;
					}

				} else {

					// no direction was set => default to "out"
					directionFromProperty = Direction.out;
					_property.setDirection(directionFromProperty);
				}

				directionChanged = true;

			} else {

				if (this.direction == null) {

					this.direction = directionFromProperty;
					directionChanged = true;

				} else if (!this.direction.equals(direction)) {


					this.direction = directionFromProperty;
					directionChanged = true;
				}

			}

			if (directionChanged) {

				switch (directionFromProperty) {

					case out:
						this.outProperty = _property;
						this.sourceType  = _sourceType;
						this.targetType  = _targetType;
						break;

					case in:
						this.inProperty = _property;
						this.sourceType = _targetType;
						this.targetType = _sourceType;
						break;
				}
			}

			syncCascadingFlags(outProperty, inProperty);
		}

		private void syncCascadingFlags(JsonReferenceProperty source, final JsonReferenceProperty target) {

			if (source != null && target != null) {

				final Cascade sourceCreate = source.getCascadingCreate();
				final Cascade targetCreate = target.getCascadingCreate();
				final Cascade sourceDelete = source.getCascadingDelete();
				final Cascade targetDelete = target.getCascadingDelete();

				// sync
				if (sourceCreate != null && targetCreate == null) {
					target.setCascadingCreate(sourceCreate);
				}

				if (sourceCreate == null && targetCreate != null) {
					source.setCascadingCreate(targetCreate);
				}

				if (sourceDelete != null && targetDelete == null) {
					target.setCascadingDelete(sourceDelete);
				}

				if (sourceDelete == null && targetDelete != null) {
					source.setCascadingDelete(targetDelete);
				}
			}
		}
	}
}
