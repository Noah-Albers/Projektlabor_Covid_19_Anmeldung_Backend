package de.noahalbers.plca.backend.database.entitys;

import org.json.JSONPropertyName;

import de.noahalbers.plca.backend.database.DBInfo;

public class SimpleUserEntity {

	@DBInfo("id")
	private int id;
	
	@DBInfo("firstname")
	private String firstname;
	
	@DBInfo("lastname")
	private String lastname;

	/*
	 * Used to grab the entity from the database. Autofills all other values using reflection
	 * */
	public SimpleUserEntity() {}
	
	public SimpleUserEntity(int id, String firstname, String lastname) {
		super();
		this.id = id;
		this.firstname = firstname;
		this.lastname = lastname;
	}

	@JSONPropertyName("firstname")
	public String getFirstname() {
		return firstname;
	}

	@JSONPropertyName("firstname")
	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	@JSONPropertyName("lastname")
	public String getLastname() {
		return lastname;
	}

	@JSONPropertyName("lastname")
	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	@JSONPropertyName("id")
	public int getId() {
		return id;
	}
	
	
}
