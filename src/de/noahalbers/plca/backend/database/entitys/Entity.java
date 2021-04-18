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
	}};

	/**
	 * Gets all entry's for a class. As they can't change at runtime, they should be
	 * stored statically. But as they are for every class that inherits different,
	 * they can best be grabbed like this so load-time can be reduced.
	 * 
	 * Use them like this: 1. Create a static Map<String,Field> for your class and
	 * directly assign them to {@link #getEntrys(Class, boolean)} with
	 * YourClass.class as the first argument. 2. Do that for both json
	 * second-argument: false and database second-argument: true 3. Override the
	 * method {@link #entrys(boolean)} and do it like this: return
	 * databaseEntrys?YOUR_STATIC_DB_MAP:YOUR_STATIC_JSON_MAP;
	 * 
	 * @param databaseEntrys
	 *            if the database entry's are required or the entry's for json
	 * @return the entr's for the required type
	 */
	protected abstract Map<String, Field> entrys(boolean databaseEntrys);

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
		}, true, required, optional);
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
		}, false, required, optional);
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
	private void load(Function<String, Object> supplie, boolean useDatabase, String[] required, String... optional)
			throws EntityLoadException {
		// Gets all entry's from for the class
		Map<String, Field> entrys = this.entrys(useDatabase);

		// Loads all required value
		for (String x : required)
			try {
				// Gets the field
				Field f;
				if ((f = entrys.getOrDefault(x, null)) == null)
					throw new EntityLoadException(false, true, x);

				// Gets the value
				Object val = supplie.apply(x);

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
				if ((f = entrys.getOrDefault(x, null)) == null)
					throw new EntityLoadException(true, true, x);

				// Gets the value
				Object val = supplie.apply(x);

				// Checks if the value is not of the correct type
				if(val.getClass() != f.getType())
					// Tries to find a converter and applies it
					val = CONVERTERS.get(f.getType()).apply(val.toString());
				
				// Tries to set the value
				f.setAccessible(true);
				f.set(this, val);
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
		// Gets all entry's from for the class
		Map<String, Field> entrys = this.entrys(true);

		// Iterates over all values that should be saved
		for (int i = 0; i < list.length; i++)
			try {
				// Gets the name
				String name = list[i];

				// Gets the field
				Field f;
				if ((f = entrys.getOrDefault(name, null)) == null)
					throw new EntitySaveException(true, name);

				// Sets the value for the statement
				set.apply(i, name, f.get(this));
			} catch (Exception e) {
				throw new EntitySaveException(false, list[i]);
			}
	}
	
	/**
	 * Searches all entry's (EntityInfo's) from the class
	 * 
	 * @param cls
	 *            the given class (will be parsed recursively using the superclass)
	 * @return a list with all entry's
	 */
	protected static Map<String, Field> getEntrys(Class<?> cls, boolean useDatabase) {
		try {
			// Selects all fields and filters those, who have a EntityInfo annotation
			Map<String, Field> map = Arrays.stream(cls.getDeclaredFields()).map(i -> {
				// Gets the info
				EntityInfo info = i.getAnnotation(EntityInfo.class);
				
				// Checks if the EntityInfo annotation exists
				if (info == null)
					return null;
				
				// Gets the used name and field
				return new SimpleEntry<String, Field>(useDatabase || info.jsonName().isEmpty() ? info.value() : info.jsonName(),i);
				
			})
			.filter(i -> i != null)
			// Sorts the list for a consistancy between runs
			.sorted((a,b)->a.getKey().compareTo(b.getKey()))
			.collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
			
			// Checks if the given class has another superclass that could contain entrys
			if (!cls.getSuperclass().equals(Object.class))
				// Appends all entry's from the super class info the list
				map.putAll(getEntrys(cls.getSuperclass(), useDatabase));
			
			return map;
			
		}catch(RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	}
}
