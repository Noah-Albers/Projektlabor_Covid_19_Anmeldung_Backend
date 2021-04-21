package de.noahalbers.plca.backend.database.entitys;

import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;

import de.noahalbers.plca.backend.database.EntityInfo;
import de.noahalbers.plca.backend.database.exceptions.EntityLoadException;
import de.noahalbers.plca.backend.database.exceptions.EntitySaveException;
import de.noahalbers.plca.backend.util.Nullable;

public abstract class Entity {

	@FunctionalInterface
	private interface Set {
		public void apply(int id, String name, Object value) throws Exception;
	}
	
	// List of converters that can be used to convert a string to the required data-type (JSON-Sql-Converting fix)
	@SuppressWarnings("serial")
	private static Map<Class<?>,Function<String,?>> CONVERTERS = new HashMap<Class<?>, Function<String,?>>(){{
		put(Date.class,Date::valueOf);
		put(Timestamp.class,Timestamp::valueOf);
		put(Long.class,Long::valueOf);
	}};

	/**
	 * Gets all entry's for a class. As they can't change at runtime, they should be
	 * stored statically. But as they are for every class that inherits different,
	 * they can best be grabbed like this so load-time can be reduced.
	 * 
	 * Use them like this: 1. Create a static Map<String,Field> for your class and
	 * directly assign them to {@link #getAttributes(Class)} with
	 * YourClass.class as the argument. 2. Override the
	 * method {@link #attributes()} and by returning your static map.
	 * 
	 * @return the entry's for the required type
	 */
	protected abstract Map<String, Field> attributes();

	/**
	 * Saves this entity to the given json-object. All parameters of which the names
	 * were supplied will be appended to the object
	 * 
	 * @param obj
	 *            the object to appen all values to
	 * @param list
	 *            the list with all values that should be appended to the object
	 * @throws EntitySaveException
	 *             if anything went wrong while suppling (Eg. some value from the
	 *             list does not exist)
	 */
	public void save(JSONObject obj, String... list) throws EntitySaveException {
		this.save((id, name, val) -> obj.put(name, val), list);
	}

	/**
	 * Saves this entity to a prepared statement. That statement has to be
	 * pre-inited and contain all required values. It will be filled with all
	 * entry's of which the names were supplied in the list parameter. The have to
	 * match the order in which the statement expects them.
	 * 
	 * @param statement
	 *            the statement that should be filled up
	 * @param list
	 *            the list with all parameters that will be used.
	 * @throws EntitySaveException
	 *             if anything went wrong with the statement (eg. to few parameters
	 *             were given at the sql-statement for the statement)
	 */
	public void save(PreparedStatement statement, String... list) throws EntitySaveException {
		this.save((id, name, val) -> statement.setObject(id + 1, val), list);
	}

	/**
	 * Loads this entity from the given resultset (Database) with all values that
	 * are specified on the required and optional lists.
	 * 
	 * The optional and required lists should be created using
	 * EntityType.STATIC_FIELD_NAME. Those field name are usually public and final.
	 * 
	 * @param result
	 *            the resultset from the database
	 * @param required
	 *            all values that are required to be inside the resultset
	 * @param optional
	 *            all values that could optionally be loaded from the resutlset
	 * @throws EntityLoadException
	 *             if anything went wrong with the loading (Invalid datatype, etc.)
	 */
	public void load(ResultSet result, String[] required, String... optional) throws EntityLoadException {
		this.load(t -> {
			try {
				return result.getObject(t);
			} catch (SQLException e) {
				return null;
			}
		}, required, optional);
	}

	/**
	 * Loads this entity from the given json-object (Database) with all values that
	 * are specified on the required and optional lists.
	 * 
	 * The optional and required lists should be created using
	 * EntityType.STATIC_FIELD_NAME. Those field name are usually public and final.
	 * 
	 * @param json
	 *            the json-object from the database
	 * @param required
	 *            all values that are required to be inside the json-object
	 * @param optional
	 *            all values that could optionally be loaded from the json-object
	 * @throws EntityLoadException
	 *             if anything went wrong with the loading (Invalid datatype, etc.)
	 */
	public void load(JSONObject json, String[] required, String... optional) throws EntityLoadException {
		this.load(i -> {
			try {
				return json.get(i);
			} catch (JSONException e) {
				return null;
			}
		}, required, optional);
	}

	/**
	 * Loads all values from the supplier by the arguments name and tries to put
	 * them into the class variables via reflection
	 * 
	 * @param supplie
	 *            the supplier that gives an argument by its name or null if
	 *            anything went wrong
	 * @param useDatabase
	 *            if the names for the database or for json should be used to
	 *            request the supplier (Usually those are equal for an entry)
	 * @param required
	 *            all values that should be loaded and are required to load to let
	 *            the function success without an error
	 * @param optional
	 *            all values that don't need to be given to load, but can load if
	 *            supplied
	 * @throws EntityLoadException
	 *             if anything went wrong during the loading of the object
	 */
	private void load(Function<String, Object> supplie, String[] required, String... optional)
			throws EntityLoadException {

		// Loads all required value
		for (String x : required)
			try {
				// Gets the field
				Field f;
				if ((f = this.attributes().getOrDefault(x, null)) == null)
					throw new EntityLoadException(false, true, x);

				// Gets the value
				Object val = supplie.apply(x);
				
				// Checks that the value could be loaded
				if(val == null)
					throw new EntityLoadException(true, false, x);

				// Checks if the value is not of the correct type
				if(val.getClass() != f.getType())
					// Tries to find a converter and applies it
					val = CONVERTERS.get(f.getType()).apply(val.toString());
				
				// Tries to set the value
				f.setAccessible(true);
				f.set(this, val);
			} catch (Exception e) {
				throw new EntityLoadException(false, false, x);
			}

		// Loads all optional values
		for (String x : optional)
			try {
				// Gets the field
				Field f;
				if ((f = this.attributes().getOrDefault(x, null)) == null)
					throw new EntityLoadException(true, true, x);

				// Gets the value
				Object val = supplie.apply(x);
				
				// Checks if the value is not given
				if(val == null)
					continue;
				
				// Checks if the value is not of the correct type
				if(val.getClass() != f.getType())
					// Tries to find a converter and applies it
					val = CONVERTERS.get(f.getType()).apply(val.toString());

				// Checks if the field is a string
				if(val instanceof String) {
					// Gets the nullable annotation
					Nullable nullable = f.getDeclaredAnnotation(Nullable.class);
					
					// Checks if the field is nullable, the field is empty and the field should be autoset to null if empty
					if(nullable != null && nullable.ifEmptyStringAutoToNull() && ((String)val).isEmpty())
						val=null;
				}
				
				// Tries to set the value
				f.setAccessible(true);
				f.set(this, val == null ? null : val);
			} catch (Exception e) {
				throw new EntityLoadException(false, true, x);
			}

	}
	
	/**
	 * Iterates over all values from the class that were supplied in the list
	 * parameter an sends them back to a setter (supplier) to store them on whatever
	 * type of object is needed.
	 * 
	 * @param set
	 *            the setter that takes the objects back together with a list index
	 *            and and the name.
	 * @param list
	 *            the list with all value-names from the class that should be used
	 * @throws EntitySaveException
	 *             if anything went wrong during the suppling
	 */
	private void save(Set set, String... list) throws EntitySaveException {
		// Iterates over all values that should be saved
		for (int i = 0; i < list.length; i++)
			try {
				// Gets the name
				String name = list[i];

				// Gets the field
				Field f;
				if ((f = this.attributes().getOrDefault(name, null)) == null)
					throw new EntitySaveException(true, name);

				// Sets the value for the statement
				set.apply(i, name, f.get(this));
			} catch (Exception e) {
				throw new EntitySaveException(false, list[i]);
			}
	}
	
	/**
	 * Returns all entity attribute-names
	 * @param cls the class to search the attributes from
	 */
	protected static String[] getAttributeNames(Class<?> cls) {
		// Gets the attributes
		Map<String, Field> atts = getAttributes(cls);
		
		// Converts to name array
		return atts.keySet().toArray(new String[atts.size()]);
	}
	
	/**
	 * Returns all entity attribute-names that are eighter optional or required (select by the optional param)
	 * @param cls the class to select from
	 * @param optional if the optional or required tags should be selected
	 */
	protected static String[] getAttributeNames(Class<?> cls, boolean optional) {
		// Selects all optional or required attributes from a class
		return
			// Gets all entries
			getAttributes(cls).entrySet().stream()
			// Filters the nullable annotations
			.filter(i->{
				int anLen = i.getValue().getDeclaredAnnotationsByType(Nullable.class).length;
				return (anLen > 0) == optional;
			})
			// Maps to their names
			.map(i->i.getKey())
			// returns them
			.toArray(String[]::new);
	}
	
	/**
	 * Searches all attributes (EntityInfo's) from the class
	 * 
	 * @param cls
	 *            the given class (will be parsed recursively using the superclass)
	 * @return a list with all entry's
	 */
	protected static Map<String, Field> getAttributes(Class<?> cls) {
		try {
			// Selects all fields and filters those, who have a EntityInfo annotation
			Map<String, Field> map = Arrays.stream(cls.getDeclaredFields()).map(i -> {
				// Gets the info
				EntityInfo info = i.getAnnotation(EntityInfo.class);
				
				// Checks if the EntityInfo annotation exists
				if (info == null)
					return null;
				
				// Gets the used name and field
				return new SimpleEntry<String, Field>(info.value(),i);
				
			})
			.filter(i -> i != null)
			// Sorts the list for a consistancy between runs
			.sorted((a,b)->a.getKey().compareTo(b.getKey()))
			.collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
			
			// Checks if the given class has another superclass that could contain entrys
			if (!cls.getSuperclass().equals(Object.class))
				// Appends all entry's from the super class info the list
				map.putAll(getAttributes(cls.getSuperclass()));
			
			return map;
			
		}catch(RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	}
}
