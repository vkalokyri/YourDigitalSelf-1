package com.rutgers.neemi.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.ArrayList;
import java.util.HashMap;

@DatabaseTable(tableName = "ScriptDefinition")
public class ScriptDefinition{

	@DatabaseField(generatedId = true)
	int id;
	@DatabaseField
	String name;
	@DatabaseField
	String argument;

	ArrayList<LocalProperties> localProperties;

	ArrayList<ScriptDefinition> subscripts;
	HashMap<String, TaskDefinition> tasksMap;

	public ScriptDefinition(){
		this.tasksMap=new HashMap<String, TaskDefinition>();
		this.subscripts=new ArrayList<ScriptDefinition>();
		this.localProperties = new ArrayList<LocalProperties>();
	}

	public ArrayList<LocalProperties> getLocalProperties() {
		return localProperties;
	}
	public void setLocalProperties(ArrayList<LocalProperties> locals) {
		this.localProperties = locals;
	}
	public void addLocalProperties(LocalProperties sublocal) {
		this.localProperties.add(sublocal);
	}

	public HashMap<String, TaskDefinition> getTaskMap() {
		return tasksMap;
	}
	public void setTaskMap(HashMap<String, TaskDefinition> tasks) {
		this.tasksMap = tasks;
	}
	public void addTaskMap(String name, TaskDefinition task) {
		this.tasksMap.put(name, task);
	}

	public ArrayList<ScriptDefinition> getSubscripts() {
		return subscripts;
	}

	public void addSubscript(ScriptDefinition script) {
		this.subscripts.add(script);
	}

	public void setSubscripts(ArrayList<ScriptDefinition> subscripts) {
		this.subscripts = subscripts;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getArgument() {
		return argument;
	}

	public void setArgument(String argument) {
		this.argument = argument;
	}
}
