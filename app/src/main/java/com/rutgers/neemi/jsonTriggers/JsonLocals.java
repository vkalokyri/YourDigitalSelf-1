package com.rutgers.neemi.jsonTriggers;

import android.content.Context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.rutgers.neemi.DatabaseHelper;
import com.rutgers.neemi.interfaces.Clues;
import com.rutgers.neemi.interfaces.Triggers;
import com.rutgers.neemi.interfaces.W5hLocals;
import com.rutgers.neemi.model.Category;
import com.rutgers.neemi.model.Email;
import com.rutgers.neemi.model.Event;
import com.rutgers.neemi.model.Feed;
import com.rutgers.neemi.model.FeedWithTags;
import com.rutgers.neemi.model.Message;
import com.rutgers.neemi.model.Person;
import com.rutgers.neemi.model.Photo;
import com.rutgers.neemi.model.Place;
import com.rutgers.neemi.model.Task;
import com.rutgers.neemi.model.Transaction;
import com.rutgers.neemi.parser.TriggersFactory;
import com.rutgers.neemi.util.ConfigReader;
import com.rutgers.neemi.util.PROPERTIES;

import org.apache.poi.util.SystemOutLogger;
import org.json.JSONException;
import org.json.JSONObject;


public class JsonLocals implements W5hLocals{
	
	
	ConfigReader config;
	JsonReader jsonReader;
	InputStream fis;
	DatabaseHelper helper;

	public JsonLocals(Context context){
		helper = DatabaseHelper.getHelper(context);
		config = ConfigReader.getInstance();
		 try {				
	 
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	}

    public ArrayList<String> getConstraints(String local, Context context, Task task) throws IOException {
		//Object pid = task.getPid();
		this.fis = context.getAssets().open(config.getStr(PROPERTIES.LOCALS_FILE));
		this.jsonReader = Json.createReader(fis);
		JsonObject jsonObject = jsonReader.readObject();
		ArrayList<String> localValues = new ArrayList<String>();
		JsonValue localObject = jsonObject.get(local);
		if (localObject != null) {
			if (localObject instanceof JsonObject){
				localValues = getLocals(local, task, context);
			}
			if (localObject instanceof JsonArray) {
				for (int j = 0; j < ((JsonArray) localObject).size(); j++) {
					JsonValue stringValue = ((JsonArray) localObject).get(j);
					localValues.add(stringValue.toString().replace("\"",""));
				}
			} else if(localObject instanceof JsonString) {
				localValues.add(localObject.toString().replace("\"",""));
			}
		}
        return  localValues;
	}


	@Override
	public ArrayList<String> getLocals(String local, Task task, Context context) throws IOException {
		this.fis = context.getAssets().open(config.getStr(PROPERTIES.LOCALS_FILE));
		this.jsonReader = Json.createReader(fis);
		JsonObject jsonObject = jsonReader.readObject();
		ArrayList<String> localValues = new ArrayList<String>();

		try {
			JsonObject localObject = jsonObject.getJsonObject(local);

		if (localObject!=null){
			for (String objectClass: localObject.keySet()){
				Object pid = task.getPid();
				if (pid !=null) {
					if (pid.getClass().getSimpleName().equalsIgnoreCase(objectClass)) {
						JsonValue attributeName = localObject.get(objectClass);
						if (objectClass.equalsIgnoreCase("Email")) {
							if (attributeName.toString().equalsIgnoreCase("\"from\"")) {
								localValues.add(((Email) pid).getFrom().getName());
							} else if (attributeName.toString().equalsIgnoreCase("\"to\"")) {
								for (Person to : ((Email) pid).getTo()) {
									if (to != null) {
										if (to.getName() != null && !to.getName().isEmpty()) {
											localValues.add(to.getName());
										} else if (to.getEmail() != null && !to.getEmail().isEmpty()) {
											localValues.add(to.getEmail());
										}
									}
								}
							} else if (attributeName.toString().equalsIgnoreCase("\"cc\"")) {
								for (Person to : ((Email) pid).getCc()) {
									if (to != null) {
										if (to.getName() != null && !to.getName().isEmpty()) {
											localValues.add(to.getName());
										} else if (to.getEmail() != null && !to.getEmail().isEmpty()) {
											localValues.add(to.getEmail());
										}
									}
								}
							} else if (attributeName.toString().equalsIgnoreCase("\"bcc\"")) {
								for (Person to : ((Email) pid).getBcc()) {
									if (to != null) {
										if (to.getName() != null && !to.getName().isEmpty()) {
											localValues.add(to.getName());
										} else if (to.getEmail() != null && !to.getEmail().isEmpty()) {
											localValues.add(to.getEmail());
										}
									}
								}
							} else if (attributeName.toString().equalsIgnoreCase("\"date\"")) {
								localValues.add(new Date(((Email) pid).getDate()).toString());
							} else if (attributeName.toString().equalsIgnoreCase("\"subject\"")) {
								localValues.add(((Email) pid).getSubject().toString());
							} else if (attributeName.toString().equalsIgnoreCase("\"subjectAt\"")) {
								String reservationConfirmation= "Your Reservation Confirmation for ";
								String reservationSubject = ((Email) pid).getSubject().toString();

								if(reservationSubject.startsWith(reservationConfirmation)){
									String reservationName = (reservationSubject.substring(reservationConfirmation.length(),reservationSubject.length()));
									localValues.add(reservationName);
								}
								String reservationReview= "How was ";
								if(reservationSubject.startsWith(reservationReview)){
									String reservationName = (reservationSubject.substring(reservationReview.length(),reservationSubject.length()-1));
									localValues.add(reservationName);
								}
								reservationReview= "Re: Your review of ";
								if(reservationSubject.startsWith(reservationReview)){
									String reservationName = (reservationSubject.substring(reservationReview.length(),reservationSubject.length()));
									localValues.add(reservationName);
								}
								String reservationPoints= "Your Points Confirmation for Dining at ";
								if(reservationSubject.startsWith(reservationPoints)){
									String reservationName = (reservationSubject.substring(reservationPoints.length(),reservationSubject.length()));
									localValues.add(reservationName);
								}
							} else if (attributeName.toString().equalsIgnoreCase("\"bodyDate\"")) {
								if (((Email) pid).getBodyDate() != null) {
									localValues.add(((Email) pid).getBodyDate().toString());
								}
							}
						} if (objectClass.equalsIgnoreCase("Message")) {
							if (attributeName.toString().equalsIgnoreCase("\"from\"")) {
								if(((Message) pid).getFrom().getName()!=null && !((Message) pid).getFrom().getName().isEmpty()) {
									localValues.add(((Message) pid).getFrom().getName());
								}else if (((Message) pid).getFrom().getUsername()!=null) {
										localValues.add(((Message) pid).getFrom().getUsername());
								}else if (((Message) pid).getFrom().getEmail()!=null) {
									localValues.add(((Message) pid).getFrom().getEmail());
								}else if (((Message) pid).getFrom().getPhone()!=null) {
									localValues.add(((Message) pid).getFrom().getPhone());
								}
							} else if (attributeName.toString().equalsIgnoreCase("\"to\"")) {
								for (Person to : ((Message) pid).getTo()) {
									if (to != null) {
										if (to.getName() != null && !to.getName().isEmpty()) {
											localValues.add(to.getName());
										}
									}
								}
							} else if (attributeName.toString().equalsIgnoreCase("\"timestamp\"")) {
								localValues.add(new Date(((Message) pid).getTimestamp()).toString());
							} else if (attributeName.toString().equalsIgnoreCase("\"contentDate\"")) {
								if (((Message) pid).getContentDate() != null) {
									localValues.add(((Message) pid).getContentDate().toString());
								}
							}
						} else if (objectClass.equalsIgnoreCase("Transaction")) {
							if (attributeName.toString().equalsIgnoreCase("\"merchant_name\"")) {
								localValues.add(((Transaction) pid).getMerchant_name());
							} else if (attributeName.toString().equalsIgnoreCase("\"date\"")) {
								localValues.add(String.valueOf(((Transaction) pid).getDate()));
							} else if (attributeName.toString().equalsIgnoreCase("\"amount\"")) {
								localValues.add(String.valueOf(((Transaction) pid).getAmount()));
							} else if (attributeName.toString().equalsIgnoreCase("\"date\"")) {
								localValues.add(String.valueOf(((Transaction) pid).getDate()));
							}
						} else if (objectClass.equalsIgnoreCase("Photo")) {
							if (attributeName.toString().equalsIgnoreCase("\"creator_id\"")) {
								localValues.add(((Photo) pid).getCreator().getName());
							} else if (attributeName.toString().equalsIgnoreCase("\"name\"")) {
								localValues.add(String.valueOf(((Photo) pid).getName()));
							} else if (attributeName.toString().equalsIgnoreCase("\"created_time\"")) {
								localValues.add(String.valueOf(((Photo) pid).getCreated_time()));
							} else if (attributeName.toString().equalsIgnoreCase("\"place_id\"")) {
								if (((Photo) pid).getPlace().getName() != null) {
									localValues.add(String.valueOf(((Photo) pid).getPlace().getName()));
								} else if (((Photo) pid).getPlace().getCity() != null) {
									localValues.add(String.valueOf(((Photo) pid).getPlace().getCity()));
								}

							} else if (attributeName.toString().equalsIgnoreCase("\"PhotoTags\"")) {
								try {
									ArrayList<Person> tags = helper.getPhotoWithTags(((Photo) pid).get_id());
									for (Person p : tags) {
										if (p.getName() != null) {
											localValues.add(p.getName());
										} else {
											localValues.add(p.getUsername());
										}
									}
								} catch (SQLException e) {
									e.printStackTrace();
								}
							}else if (attributeName.toString().equalsIgnoreCase("\"Category\"")) {
								try {
									ArrayList<Category> categories = helper.getPhotoCategory(((Photo) pid).get_id());
									for (Category c : categories) {
										if (c.getCategoryName() != null) {
											localValues.add(c.getCategoryName());
										}
									}
								} catch (SQLException e) {
									e.printStackTrace();
								}
							}
						} else if (objectClass.equalsIgnoreCase("Event")) {
							if (attributeName.toString().equalsIgnoreCase("\"creator_id\"")) {
								if(((Event) pid).getCreator()!=null) {
									if (((Event) pid).getCreator().getName() != null) {
										localValues.add(((Event) pid).getCreator().getName());
									} else {
										localValues.add(((Event) pid).getCreator().getEmail());
									}
								}
							} else if (attributeName.toString().equalsIgnoreCase("\"title\"")) {
								localValues.add(String.valueOf(((Event) pid).getTitle()));
							} else if (attributeName.toString().equalsIgnoreCase("\"dateCreated\"")) {
								localValues.add(new Date(((Event) pid).getDateCreated()).toString());
							} else if (attributeName.toString().equalsIgnoreCase("\"startTime\"")) {
								localValues.add(new Date(((Event) pid).getStartTime()).toString());
							} else if (attributeName.toString().equalsIgnoreCase("\"endTime\"")) {
								localValues.add(new Date(((Event) pid).getEndTime()).toString());
							} else if (attributeName.toString().equalsIgnoreCase("\"location\"")) {
								if (((Event) pid).getLocation() != null) {
									localValues.add(String.valueOf(((Event) pid).getLocation()));
								}
							} else if (attributeName.toString().equalsIgnoreCase("\"organizer_id\"")) {
								if(((Event) pid).getOrganizer()!=null) {
									if (((Event) pid).getOrganizer().getName() != null) {
										localValues.add(((Event) pid).getOrganizer().getName());
									} else {
										localValues.add(((Event) pid).getOrganizer().getEmail());
									}
								}
							} else if (attributeName.toString().equalsIgnoreCase("\"place_id\"")) {
								if (pid instanceof Photo) {
									if (((Photo) pid).getPlace().getName() != null) {
										localValues.add(String.valueOf(((Photo) pid).getPlace().getName()));
									} else if (((Photo) pid).getPlace().getCity() != null) {
										localValues.add(String.valueOf(((Photo) pid).getPlace().getCity()));
									}
								}else if(pid instanceof Event) {
									if(((Event) pid).getPlace()!=null) {
										if (((Event) pid).getPlace().getName() != null) {
											localValues.add(String.valueOf(((Event) pid).getPlace().getName()));
										} else if (((Event) pid).getPlace().getCity() != null) {
											localValues.add(String.valueOf(((Event) pid).getPlace().getCity()));
										}
									}
								}
							}
						} else if (objectClass.equalsIgnoreCase("Feed")) {
							if (attributeName.toString().equalsIgnoreCase("\"creator_id\"")) {
								localValues.add(((Feed) pid).getCreator().getName());
							} else if (attributeName.toString().equalsIgnoreCase("\"message\"")) {
								localValues.add(String.valueOf(((Feed) pid).getMessage()));
							} else if (attributeName.toString().equalsIgnoreCase("\"created_time\"")) {
								localValues.add(String.valueOf(((Feed) pid).getCreated_time()));
							} else if (attributeName.toString().equalsIgnoreCase("\"place_id\"")) {
								if (((Feed) pid).getPlace().getName() != null) {
									localValues.add(String.valueOf(((Feed) pid).getPlace().getName()));
								} else if (((Feed) pid).getPlace().getCity() != null) {
									localValues.add(String.valueOf(((Feed) pid).getPlace().getCity()));
								}

							} else if (attributeName.toString().equalsIgnoreCase("\"FeedWithTags\"")) {
								try {
									ArrayList<Person> tags = helper.getFeedWithTags(((Feed) pid).get_id());
									for (Person p : tags) {
										if (p.getName() != null) {
											localValues.add(p.getName());
										} else {
											localValues.add(p.getUsername());
										}
									}
								} catch (SQLException e) {
									e.printStackTrace();
								}


							}
							// else if(attributeName.toString().equalsIgnoreCase("\"person_id\"")) {
							//							localValues.add(((Feed) pid).getId());
							//						}
						}
					}
				}else{

					for (Object id : task.getList_of_pids()) {
						if (id.getClass().getSimpleName().equalsIgnoreCase(objectClass)) {
							JsonValue attributeName = localObject.get(objectClass);
							if (objectClass.equalsIgnoreCase("Place")) {
								if (attributeName.toString().equalsIgnoreCase("\"name\"")) {
									localValues.add(((Place) id).getName());
								}else if (attributeName.toString().equalsIgnoreCase("\"arrive\"")) {
                                    localValues.add(new Date(((Place) id).getSp().getArrive()).toString());
                                }
							}
						}

					}
				}
			}
		}
		}catch(Exception e){

		}
		return localValues;

	}
        

            
            
            
            
            
		


}
