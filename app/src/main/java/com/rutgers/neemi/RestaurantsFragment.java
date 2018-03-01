package com.rutgers.neemi;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.rutgers.neemi.interfaces.Clues;
import com.rutgers.neemi.interfaces.Triggers;
import com.rutgers.neemi.interfaces.W5hLocals;
import com.rutgers.neemi.model.Email;
import com.rutgers.neemi.model.Event;
import com.rutgers.neemi.model.LocalProperties;
import com.rutgers.neemi.model.LocalValues;
import com.rutgers.neemi.model.Payment;
import com.rutgers.neemi.model.Script;
import com.rutgers.neemi.model.ScriptDefinition;
import com.rutgers.neemi.model.ScriptHasTasks;
import com.rutgers.neemi.model.Subscript;
import com.rutgers.neemi.model.Task;
import com.rutgers.neemi.model.TaskDefinition;
import com.rutgers.neemi.parser.TriggersFactory;
import com.rutgers.neemi.parser.ScriptParser;
import com.rutgers.neemi.util.ApplicationManager;
import com.rutgers.neemi.util.ConfigReader;
import com.rutgers.neemi.util.PROPERTIES;
import com.rutgers.neemi.util.TinyDB;
import com.rutgers.neemi.util.Utilities;

import org.apache.poi.hssf.record.SCLRecord;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.sql.Array;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.json.JsonString;

import static com.facebook.FacebookSdk.getApplicationContext;
import static com.rutgers.neemi.parser.InitiateScript.JsonTriggerFactory;
import static com.rutgers.neemi.parser.InitiateScript.util;


public class RestaurantsFragment extends Fragment {


    private static final String TAG = "RestaurantsFragment";
    View myView;
    String scriptKeywords;
    static Triggers scriptTriggers;
    static Clues clues;
    public static HashMap<Object,Object> triggers_Clues = new HashMap<Object,Object>();
    DatabaseHelper helper;
    List<Task> tasksRunning=new ArrayList<Task>();
    ArrayList<Script> listOfScripts = new ArrayList<Script>();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    RuntimeExceptionDao<ScriptDefinition, String> scriptDefDao;
    RuntimeExceptionDao<Subscript, String> subscriptsDao;
    RuntimeExceptionDao<TaskDefinition, String> taskDefDao;
    RuntimeExceptionDao<ScriptHasTasks, String> scriptHasTasksDao;
    RuntimeExceptionDao<LocalValues, String> localValuesDao;



    Integer[] imgid={
            R.drawable.restaurant
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        myView = inflater.inflate(R.layout.fragment_restaurants, container, false);
        ListView list1 =  (ListView) myView.findViewById(R.id.restaurant_list);

//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
//        SharedPreferences.Editor editor = prefs.edit();
//        TinyDB tinydb = new TinyDB(getContext());
//        editor.putBoolean("restaurantFirstTime", false);
//
//        if (!prefs.getBoolean("restaurantFirstTime", false)) {
//            // <---- run your one time code here
//            findScriptInstances();
//
//            ArrayList<Object> scriptObjects = new ArrayList<Object>();
//
//            for(Script s : listOfScripts){
//                scriptObjects.add((Object)s);
//                break;
//            }
//
//            tinydb.putListObject("restaurantScripts", scriptObjects);
//
////            // mark first time has runned.
//            editor.putBoolean("restaurantFirstTime", true);
////
////            Gson gson = new Gson();
////            ArrayList<String> objStrings = new ArrayList<String>();
////            for(Script obj : listOfScripts){
////                objStrings.add(gson.toJson(obj));
////            }
////            String[] myStringList = objStrings.toArray(new String[objStrings.size()]);
////            editor.putString("restaurantScripts", TextUtils.join("‚‗‚", myStringList)).apply();
////            editor.commit();
//        }else{
//            ArrayList<Object> scriptObjects = tinydb.getListObject("restaurantScripts", Script.class);
//
//            for(Object objs : scriptObjects){
//                listOfScripts.add((Script) objs);
//            }
//        }



        list1.setOnItemClickListener(
                new AdapterView.OnItemClickListener()
                {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View view,
                                            int position, long id) {
                        ScriptFragment scriptFragment = new ScriptFragment();
                        Bundle arguments = new Bundle();
                        arguments.putSerializable("processes", listOfScripts);
                        arguments.putSerializable("position",position);
                        arguments.putSerializable("id",id);

                        scriptFragment.setArguments(arguments);

                        android.support.v4.app.FragmentTransaction scriptfragmentTrans = getFragmentManager().beginTransaction();
                        scriptfragmentTrans.add(R.id.frame,scriptFragment);
                        scriptfragmentTrans.addToBackStack(null);
                        scriptfragmentTrans.commit();
                        Toast.makeText(getContext(), "Pressed!", Toast.LENGTH_LONG).show();
                    }
                }
        );

        return myView;
    }


    public void findScriptInstances(){

        helper=DatabaseHelper.getHelper(getActivity());
        scriptDefDao = helper.getScriptDefDao();
        subscriptsDao = helper.getSubScriptDao();
        taskDefDao = helper.getTaskDefinitionDao();
        scriptHasTasksDao = helper.getScriptHasTasksDao();
        localValuesDao = helper.getLocalValuesDao();

        scriptDefDao.queryRaw("delete from LocalValues;");


        try{
            ConfigReader config = new ConfigReader(getContext());

            /*get the keywords to search in the documents*/
            InputStream fis = getContext().getAssets().open(config.getStr(PROPERTIES.KEYWORDS_FILE));
            InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
            BufferedReader br = new BufferedReader(isr);
            String keywords="";
            String line;
            while ((line = br.readLine()) != null) {
                keywords=keywords+"\""+line+"\""+" OR ";
            }
            this.scriptKeywords=keywords.substring(0, keywords.length()-4);

            //Get the strong triggers and clues for triggers of the script
            JsonTriggerFactory = TriggersFactory.getTriggersFactory(TriggersFactory.json);
            scriptTriggers= JsonTriggerFactory.getTriggers(getContext());
            clues = JsonTriggerFactory.getClues(getContext());

            for (int i=0;i<scriptTriggers.getStrongTriggers().size();i++){
                String strongTrigger = scriptTriggers.getStrongTriggers().get(i);
                String [] strongArray = strongTrigger.split("<");
                triggers_Clues.put(strongTrigger, clues.getClues(strongArray[0].substring(1), strongArray[1].substring(0,strongArray[1].length()-2),getContext()));
                //printTriggersAndClues(triggers_Clues);
            }

            for (int i=0;i<scriptTriggers.getWeakTriggers().size();i++){
                String weakTrigger = scriptTriggers.getWeakTriggers().get(i);
                String [] weakArray = weakTrigger.split("<");
                triggers_Clues.put(weakTrigger, clues.getClues(weakArray[0].substring(1), weakArray[1].substring(0,weakArray[1].length()-2),getContext()));
                //printTriggersAndClues(triggers_Clues);
            }

            /*find the initial tasks that are running*/
            List <Task> tasksrunning= findTaskInstancesInDatabase(triggers_Clues);

            /*extract all the local properties from the tasks*/
            for (Task task:tasksrunning) {
                String taskName = task.getName();
                System.out.println("Task is: " + taskName);
                Object pid = task.getPid();
                if (pid instanceof Event) {
                    System.err.println("Task = " + taskName + ", Event = " + ((Event) pid).get_id()+" Script = ");

                } else if (pid instanceof Email) {
                    System.err.println("Task = " + taskName + ", Email = " + ((Email) pid).get_id());


                } else if (pid instanceof Payment) {
                    System.err.println("Task = " + taskName + ", Payment = " + ((Payment) pid).getName());
                }

                ArrayList<LocalProperties> taskLocals = helper.extractTaskLocals(taskName);


                if (taskLocals!=null){
                    for(LocalProperties w5h:taskLocals){
                        W5hLocals locals = JsonTriggerFactory.getLocals(getContext());
                        ArrayList<String> localValue = locals.getLocals(w5h.getW5h_value(), pid, getContext());
                        if (localValue.size()>0) {
                            for (String lValue:localValue) {

                                LocalValues w5hInfo = new LocalValues();
                                w5hInfo.setLocalProperties(w5h);
                                w5hInfo.setValue(lValue);
                                w5hInfo.setTask(task);
                                localValuesDao.create(w5hInfo);
                                task.addLocalValue(w5hInfo);
                            }
                        }
                    }
                }
            }



            ArrayList<ArrayList<Task>> tasks = mergeTasksByEventDate(tasksRunning);
            ArrayList<ArrayList<Task>> tasksThreads = mergeThreads(tasks);
            listOfScripts = createScriptPerTask(tasksThreads);



            CustomListAdapter adapter=new CustomListAdapter(getActivity(), listOfScripts, imgid);
            ListView list= myView.findViewById(R.id.restaurant_list);
            list.setAdapter(adapter);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        findScriptInstances();

    }

    public ArrayList<Script> createScriptPerTask(ArrayList<ArrayList<Task>> taskThreads) throws SQLException {

        ArrayList<Script> scripts = new ArrayList<Script>();
        for(ArrayList<Task> tasksRunning:taskThreads) {
            //put all tasks local values under the abstract who, what, where dimensions
            HashMap<String, HashSet<String>> map = new HashMap<String, HashSet<String>>();

            Script script = new Script();
            String scriptName = null;
            String ofType =null;
            for (Task task : tasksRunning) {
                scriptName = task.getScript().getName();
                ofType = task.getScript().getOfType();
                HashSet<String> values=null;
                for (LocalValues taskLocalValues : task.getLocalValues()) {
                    String w5hLabel = taskLocalValues.getLocalProperties().getW5h_label();
                    if (map.containsKey(w5hLabel)) {
                        values = map.get(w5hLabel);
                    }else{
                        if (w5hLabel != null) {
                            values = new HashSet<>();
                        }
                    }

                    if (taskLocalValues.getValue()!=null) {
                        //HashSet<String> values = map.get(w5hLabel);
                        if (w5hLabel.equalsIgnoreCase("who")) {
                            String whoValue = taskLocalValues.getValue();
                            String[] whoNames = whoValue.split("<");
                            if (whoNames.length > 1) {
                                if(whoNames[0].contains("\"")){
                                    values.add(whoNames[0].substring(1,whoNames[0].length()-2));
                                }else{
                                    values.add(whoNames[0]);
                                }
                                map.put(w5hLabel, values);
                            } else {
                                values.add(whoValue);
                                map.put(w5hLabel, values);
                            }
                        } else {
                            values.add(taskLocalValues.getValue());
                            map.put(w5hLabel, values);
                        }
                    }

                }
            }


            script.setScriptDefinition(helper.getTopScripts(scriptName, ofType));
            script.setTasks(tasksRunning);


            for (LocalProperties localProp : script.getScriptDefinition().getLocalProperties()) {
                HashSet<String> values = map.get(localProp.getW5h_label());
                if (values != null) {
                    for (String value : values) {
                        LocalValues scriptLocalValues = new LocalValues();
                        scriptLocalValues.setLocalProperties(localProp);
                        //scriptLocalValues.setTask(task);
                        scriptLocalValues.setValue(value);
                        script.addLocalValue(scriptLocalValues);
                    }
                }
            }

            //add local values in the definitions

            ArrayList<ScriptDefinition> scriptDefinitionList = script.getScriptDefinition().getSubscripts();
            for (ScriptDefinition subscriptDef : scriptDefinitionList) {
                Script subscript = new Script();
                subscript.setScriptDefinition(subscriptDef);


                for (LocalProperties localProp : subscriptDef.getLocalProperties()) {
                    HashSet<String> values = map.get(localProp.getW5h_label());
                    if (values != null) {
                        for (String value : values) {
                            LocalValues scriptLocalValues = new LocalValues();
                            scriptLocalValues.setLocalProperties(localProp);
                            //scriptLocalValues.setTask(task);
                            scriptLocalValues.setValue(value);
                            subscript.addLocalValue(scriptLocalValues);
                        }
                    }
                }
                script.addSubscript(subscript);
            }


            script.assignScore(getContext());
            scripts.add(script);

        }

        return scripts;
    }


    public ArrayList<ArrayList<Task>> mergeTasksByEventDate(List<Task> tasks){
        Log.d(TAG,"SIZE OF PROCESSES: " +tasks.size());
        ArrayList<ArrayList<Task>> listofMergedTasks = new ArrayList<ArrayList<Task>>();
        HashMap<Date, ArrayList<Task>> hashMap = new HashMap<Date, ArrayList<Task>>();

        for (Task task:tasks){
            Date extractedDate=null;
            if (task.getPid() instanceof Payment){
                try {
                    extractedDate = sdf.parse(sdf.format(((Payment) task.getPid()).getDate()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }else if (task.getPid() instanceof Email){
                if (((Email) task.getPid()).getSubjectDate()!=null) {
                    try {
                        extractedDate = sdf.parse(sdf.format(((Email) task.getPid()).getSubjectDate()));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }else{

                }
            }else if (task.getPid() instanceof Event){
                try {
                    extractedDate = sdf.parse(sdf.format(((Event) task.getPid()).getStartTime()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            if(extractedDate!=null) {
                if (!hashMap.containsKey(extractedDate)) {
                    ArrayList<Task> list = new ArrayList<Task>();
                    list.add(task);
                    hashMap.put(extractedDate, list);
                } else {
                    hashMap.get(extractedDate).add(task);
                }
            }else{
                ArrayList<Task> notMergedTask = new ArrayList<Task>();
                notMergedTask.add(task);
                listofMergedTasks.add(notMergedTask);
            }

        }


        for (Date date: hashMap.keySet()){
            listofMergedTasks.add(hashMap.get(date));
        }

        return listofMergedTasks;


//        Log.d(TAG,"SIZE OF PROCESSES after: " +hashMap.size());
//        for (Map.Entry entry : hashMap.entrySet()) {
//            Log.d(TAG,"Date: " +entry.getKey());
//            for (Task t:(List<Task>)entry.getValue()){
//                if (t.getPid() instanceof Event) {
//                    Log.d(TAG,"Event = " + ((Event) t.getPid()).getDescription());
//                }else if (t.getPid() instanceof Email){
//                    Log.d(TAG, "Email = "+((Email) t.getPid()).getSubject());
//                }else if (t.getPid() instanceof Payment){
//                    Log.d(TAG,"Payment = "+((Payment) t.getPid()).getName());
//                }
//            }
//        }

    }


    public void mergeScriptsByEventDate(List<Script> listOfScripts){
        Log.d(TAG,"SIZE OF PROCESSES: " +listOfScripts.size());
        HashMap<Date, List<Script>> hashMap = new HashMap<Date, List<Script>>();

		for (Script process:listOfScripts){
        	for (Task task:process.getTasks()){
                Date extractedDate=null;
                if (task.getPid() instanceof Payment){
                    try {
                        extractedDate = sdf.parse(sdf.format(((Payment) task.getPid()).getDate()));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }else if (task.getPid() instanceof Email){
                    if (((Email) task.getPid()).getSubjectDate()!=null) {
                        try {
                            extractedDate = sdf.parse(sdf.format(((Email) task.getPid()).getSubjectDate()));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }else{

                    }
                }else if (task.getPid() instanceof Event){
                    try {
                        extractedDate = sdf.parse(sdf.format(((Event) task.getPid()).getStartTime()));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                if(extractedDate!=null) {
                    if (!hashMap.containsKey(extractedDate)) {
                        List<Script> list = new ArrayList<Script>();
                        list.add(process);
                        hashMap.put(extractedDate, list);
                    } else {
                        hashMap.get(extractedDate).add(process);
                    }
                }
        	}
        }

        for (Date date: hashMap.keySet()){
            List<Script> mergedScripts = hashMap.get(date);
        }


//        Log.d(TAG,"SIZE OF PROCESSES after: " +hashMap.size());
//        for (Map.Entry entry : hashMap.entrySet()) {
//            Log.d(TAG,"Date: " +entry.getKey());
//            for (Task t:(List<Task>)entry.getValue()){
//                if (t.getPid() instanceof Event) {
//                    Log.d(TAG,"Event = " + ((Event) t.getPid()).getDescription());
//                }else if (t.getPid() instanceof Email){
//                    Log.d(TAG, "Email = "+((Email) t.getPid()).getSubject());
//                }else if (t.getPid() instanceof Payment){
//                    Log.d(TAG,"Payment = "+((Payment) t.getPid()).getName());
//                }
//            }
//        }

    }


	public ArrayList<ArrayList<Task>> mergeThreads(ArrayList<ArrayList<Task>> tasks){

        System.err.println("Total processes running before threading ="+tasks.size());

        ArrayList<ArrayList<Task>> listofMergedTasks = new ArrayList<ArrayList<Task>>();

		//hashmap of key:threadId and value:task
        HashMap<String,ArrayList<Task>> mergeTasksByThread = new HashMap();

        for (ArrayList<Task> tasksInAScript: tasks){
            String previous_thread_id=null;
            ArrayList<Task> tasklist = null;
            for (Task task:tasksInAScript) {
                if (task.getPid() instanceof Email) {
                    String key = ((Email) task.getPid()).getThreadId();
                    previous_thread_id=key;
                    ArrayList<Task> list = mergeTasksByThread.get(key);
                    if (list == null) {
                        list = new ArrayList<Task>();
                        list.add(task);
                        mergeTasksByThread.put(key, list);
                    }else{
                        list.add(task);
                    }
                }else {
                    if(previous_thread_id!=null){
                        ArrayList<Task> list = mergeTasksByThread.get(previous_thread_id);
                        list.add(task);
                        mergeTasksByThread.put(previous_thread_id, list);
                    }else{
                        if (tasklist==null){
                            tasklist = new ArrayList<>();
                            tasklist.add(task);
                        }else{
                            tasklist.add(task);
                        }

                    }
                }
            }
            if(tasklist!=null) {
                listofMergedTasks.add(tasklist);
            }
        }


        for (String thread_id: mergeTasksByThread.keySet()) {
            ArrayList<Task> mergedtasks = mergeTasksByThread.get(thread_id);
            listofMergedTasks.add(mergedtasks);
        }

		System.err.println("Total processes running after threading ="+listofMergedTasks.size());

//		Collections.sort(listOfScripts, new Comparator<Script>() {
//	        @Override public int compare(Script p1, Script p2) {
//	            return Float.compare(p2.getScore(),p1.getScore()); // Ascending
//	        }
//
//	    });

        return listofMergedTasks;
	}


    public List<Task> findTaskInstancesInDatabase(HashMap<Object, Object> triggers_Clues) throws SQLException{

        for (HashMap.Entry<Object, Object> entry : triggers_Clues.entrySet()) {
            String scriptType = ((String)entry.getKey()).replace("\"", "");;
            System.err.println("Script = "+scriptType);
            String[] scriptArray = scriptType.split("<");
            String scriptName=null;
            String typeOf=null;
            if (scriptArray!=null){
                scriptName = scriptArray[0];
                typeOf = scriptArray[1].substring(0,scriptArray[1].length()-1);
            }else{
                scriptName=scriptType;
            }
            Script script = new Script();
            script.setName(scriptName);
            script.setOfType(typeOf);
            List<HashMap<Object, Object>> values = (List<HashMap<Object, Object>>) entry.getValue();
            if (values!=null) {
                for (HashMap<Object, Object> value : values) {
                    for (HashMap.Entry<Object, Object> subtasks : value.entrySet()) {
                        String subtask = (String)subtasks.getKey();
                        //System.out.println("Subtask = " +subtask);
                        String query="select * ";
                        String fromClause=" from ";
                        String whereClause=" where ";
                        boolean foundKeywordsFile = false;
                        String keywordsSearchColumn="";
                        String fullTextQuery="";
                        String fromTable="";
                        HashMap<Object, Object> fromWhereValues = (HashMap<Object, Object>) subtasks.getValue();
                        for (HashMap.Entry<Object, Object> clues : fromWhereValues.entrySet()) {
                            String fromWhere = (String) clues.getKey();
                            //System.out.print("FromWhere = " + fromWhere);
                            if (fromWhere.equals("from")) {
                                for (int i = 0; i < ((ArrayList) clues.getValue()).size(); i++) {
                                    //System.out.println("From value = " + ((ArrayList) clues.getValue()).get(i));
                                    if (i == 0) {
                                        fromTable = ((ArrayList) clues.getValue()).get(i).toString();
                                        Log.d(TAG,"fromTable "+ fromTable);
                                        fromClause = fromClause + "`"+((ArrayList) clues.getValue()).get(i)+"`";
                                    } else {
                                        fromClause = fromClause + ",`" + ((ArrayList) clues.getValue()).get(i)+"`";
                                    }
                                }
                            } else if (fromWhere.equals("where")) {
                                HashMap<Object, Object> clue = (HashMap<Object, Object>) clues.getValue();
                                for (HashMap.Entry<Object, Object> cluesKeyValues : clue.entrySet()) {
                                    String clueKey = (String) cluesKeyValues.getKey();
                                    //System.out.println("AndOrOr = " + clueKey);
                                    Object clueValue = cluesKeyValues.getValue();
                                    if (clueKey.equals("and") || clueKey.equals("or")) {
                                        HashMap<Object, Object> valuesOfAndOr = (HashMap<Object, Object>) clueValue;
                                        whereClause = whereClause + " ( ";
                                        for (HashMap.Entry<Object, Object> valueOfAndOr : valuesOfAndOr.entrySet()) {
                                            String andOrKey = (String) valueOfAndOr.getKey();
                                            Object andOrValue = valueOfAndOr.getValue();
                                            Log.d(TAG,"clue key "+ andOrKey);
                                            if (andOrValue instanceof ArrayList) {
                                                for (int i = 0; i < ((ArrayList) andOrValue).size(); i++) {
                                                    String item = ((ArrayList) andOrValue).get(i).toString();
                                                    Log.d(TAG,"Clue value = " + item);
                                                    //whereClause = whereClause + " ( `" + andOrKey+"`";
                                                    if (i == 0) {
                                                        whereClause = whereClause + " ( `" + andOrKey+"`" + " LIKE '%" + item.toString().replace("\"", "") + "%'";
                                                    } else {
                                                        whereClause = whereClause + " or `" + andOrKey + "` LIKE '%" + item.toString().replace("\"", "") + "%'";
                                                    }

                                                }
                                                whereClause = whereClause + " )";

                                            } else {
                                                if (((JsonString)andOrValue).getString().equals("KEYWORDS_FILE")) {

                                                    //if (i == 0) {
                                                    whereClause = whereClause + " `" + andOrKey + "` MATCH '(" + scriptKeywords + ")'";
                                                    //whereClause = whereClause + " ( `" + andOrKey+"`" + " LIKE '%" + item.toString().replace("\"", "") + "%'";
                                                    //} else {
                                                    //    whereClause = whereClause + " or `" + andOrKey + "` MATCH '("+ scriptKeywords+")'";
                                                    //}

                                                    foundKeywordsFile = true;
                                                    //keywordsSearchColumn = andOrKey;
                                                    whereClause = whereClause + " )";

                                                } else {
                                                    whereClause = whereClause + " " + andOrKey;
                                                    if (andOrKey.contains("Payment") || andOrKey.contains("Category")){
                                                        whereClause = whereClause + " = " + andOrValue.toString().replace("\"", "");
                                                    }else {
                                                        //System.out.println("Clue value = " + andOrValue.toString().replace("\"", ""));
                                                        whereClause = whereClause + " = '" + andOrValue.toString().replace("\"", "") + "'";
                                                    }
                                                }

                                            }
                                            whereClause = whereClause + " " +clueKey ;
                                        }
                                    }
                                    whereClause=whereClause+")";
                                }
                            }
                        }
                        if (!foundKeywordsFile){
                            fromClause = fromClause.replace("\"", "");
                            if (fromClause.contains("Payment")){
                                query="select distinct Payment._id, Payment.name ";                            }
                            query = query + fromClause + whereClause;
                            query=query.substring(0,query.length()-4) +");";
                            System.out.println("QUERY = " + query);
                            try {
                                tasksRunning.addAll(queryDatabase(query,fromClause,subtask,script));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }else{
                            foundKeywordsFile=false;
                            whereClause=whereClause.substring(0,whereClause.length()-4) +")";
                            tasksRunning.addAll(fullTextSearch(whereClause,fromTable, subtask, script));
                        }

                    }
                }
            }
        }

        return tasksRunning;
    }

    public List<Task> fullTextSearch(String whereClause, String fromTable, String subtask, Script script) throws SQLException {

        fromTable=fromTable.toString().replace("\"", "");
        List<Task> tasks = new ArrayList<Task>();

        String query = "SELECT * FROM "+ fromTable +" WHERE \"_id\" IN (select \"_id\" from "+ fromTable + "_fts "+ whereClause +";"; //`"+ text_column +"` MATCH '("+ scriptKeywords+")')";
        query = query.replace(" or "," UNION select \"_id\" from " + fromTable + "_fts where ( " );
        System.out.println("FULLTEXTQUERY = " + query);
        //query = "SELECT * FROM Email WHERE \"_id\" IN (select \"_id\" from Email_fts  where  `textContent` MATCH '(\"restaurant\" OR \"dinner\" OR \"lunch\" )' UNION select \"_id\" from Email_fts  where `subject` MATCH '(\"restaurant\" OR \"dinner\" OR \"lunch\" )');";

        if (fromTable.equals("Email")){
            GenericRawResults<Email> rawResults = helper.getEmailDao().queryRaw(query,helper.getEmailDao().getRawRowMapper());
            if (rawResults!=null) {
                for (Email email : rawResults.getResults()) {
                    Task task = new Task();
                    task.setPid(email);
                    task.setName(subtask);
                    task.setScript(script);
                    tasks.add(task);
                }
            }
        }
        if (fromTable.equals("Payment")){
            GenericRawResults<Payment> rawResults = helper.getPaymentDao().queryRaw(query,helper.getPaymentDao().getRawRowMapper());
            for (Payment payment: rawResults.getResults()){
                Task task = new Task();
                task.setOid(payment.getId());
                task.setPid(payment);
                task.setName(subtask);
                task.setScript(script);
                tasks.add(task);
            }
        }
        if (fromTable.equals("Event")){
            GenericRawResults<Event> rawResults = helper.getEventDao().queryRaw(query,helper.getEventDao().getRawRowMapper());
            for (Event event: rawResults.getResults()){
                Task task = new Task();
                task.setOid(event.getId());
                task.setPid(event);
                task.setName(subtask);
                task.setScript(script);
                tasks.add(task);

            }
        }

        return tasks;
    }

    public List<Task> queryDatabase(String query, String fromTable, String subtask, Script script) throws SQLException {
        List<Task> tasks = new ArrayList<Task>();


        GenericRawResults<String[]> rawResults = helper.getPaymentDao().queryRaw(query);
       // List<String[]> results = null;
        if (rawResults!=null){
           // if (rawResults.getResults().size()>0){
                if (fromTable.contains("Payment")){
                    for (String[] tuple:rawResults.getResults()) {
                        Payment payment = new Payment();
                        payment.setId(tuple[0]);
                        payment.setName(tuple[1]);

                        String tempQuery = "select * from `Payment` where `_id`=" + tuple[0];
                        GenericRawResults<Payment> paymentData = helper.getPaymentDao().queryRaw(tempQuery, helper.getPaymentDao().getRawRowMapper());
//                        try {
//                            System.err.println("PAYMENTSFOUND = " + paymentData.getResults().size());
//                        } catch (SQLException e) {
//                            e.printStackTrace();
//                        }
                        try {
                            for (Payment fullpayment : paymentData.getResults()) {
                                Task task = new Task();
                                task.setOid(fullpayment.getId());
                                task.setPid(fullpayment);
                                task.setName(subtask);
                                task.setScript(script);
                                tasks.add(task);
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }


                    }
                }
          //  }
        }
        return tasks;
    }





    public void printTriggersAndClues(HashMap<Object,Object> triggers_Clues){

		for (HashMap.Entry<Object,Object> entry : triggers_Clues.entrySet()) {
			  String key = (String)entry.getKey();
			  System.out.println("Trigger = "+key);
			  List<HashMap<Object,Object>> values = (List<HashMap<Object,Object>>)entry.getValue();
			  if (values!=null){
				  for (HashMap<Object, Object> value:values){
					  for (HashMap.Entry<Object, Object> clues : value.entrySet()) {
						  String fromWhere = (String)clues.getKey();
						  System.out.println("From/Where = "+fromWhere);
                          if (fromWhere.equals("from")){
                              for (int i = 0; i < ((ArrayList) clues.getValue()).size(); i++)
                              System.out.println("Clue value = " + ((ArrayList) clues.getValue()).get(i));
                          }else {
                              HashMap<Object, Object> clue = (HashMap<Object, Object>) clues.getValue();
                              for (HashMap.Entry<Object, Object> cluesKeyValues : clue.entrySet()) {
                                  String clueKey = (String) cluesKeyValues.getKey();
                                  System.out.println("AndOrOr = " + clueKey);
                                  Object clueValue = cluesKeyValues.getValue();
                                  if (clueKey.equals("and") || clueKey.equals("or")) {
                                      HashMap<Object, Object> valuesOfAndOr = (HashMap<Object, Object>) clueValue;
                                      for (HashMap.Entry<Object, Object> valueOfAndOr : valuesOfAndOr.entrySet()) {
                                          String andOrKey = (String) valueOfAndOr.getKey();
                                          Object andOrValue = valueOfAndOr.getValue();
                                          System.out.println("clue key = " + andOrKey);
                                          if (andOrValue instanceof ArrayList) {
                                              for (int i = 0; i < ((ArrayList) andOrValue).size(); i++)
                                                  System.out.println("Clue value = " + ((ArrayList) andOrValue).get(i));
                                          } else {
                                              System.out.println("Clue value = " + andOrValue);
                                          }
                                      }
                                  }

                              }
                          }
					  }
				  }
			  }
		}

	}



    private class CustomListAdapter extends ArrayAdapter<Script> {

        private final Activity context;
        private final List<Script> itemname;
        private final Integer[] imgid;

        public CustomListAdapter(Activity context, List<Script> scripts, Integer[] imgid) {
            super(context, R.layout.restaurantsview, scripts);
            // TODO Auto-generated constructor stub
            this.context=context;
            this.itemname=scripts;
            this.imgid=imgid;
        }

        public View getView(int position,View view,ViewGroup parent) {


            LayoutInflater inflater=context.getLayoutInflater();
            View rowView=inflater.inflate(R.layout.restaurantsview, null,false);
            LinearLayout linearLayout = (LinearLayout) rowView.findViewById(R.id.linearLayout);

           // TextView txtTitle = (TextView) rowView.findViewById(R.id.item);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);


            Script script = itemname.get(position);//.getScriptDefinition();
            HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
                    //txtTitle.setText(itemname.get(position).getScore()+", "+String.valueOf(((Email)processTask.getPid()).get_id()));
            imageView.setImageResource(imgid[0]);
            ArrayList<LocalValues> localValues = script.getLocalValues();
            if (localValues != null) {
                for (LocalValues localValue : localValues) {
                    if (localValue != null) {
                        LocalProperties lp = localValue.getLocalProperties();
                        if (lp != null) {
                            String w5h_value = lp.getW5h_value();
                            if (map.containsKey(w5h_value)) {
                                ArrayList<String> values = map.get(w5h_value);
                                values.add(localValue.getValue());
                                map.put(w5h_value, values);
                            } else {
                                if (w5h_value != null) {
                                    ArrayList<String> values = new ArrayList<String>();
                                    values.add(localValue.getValue());
                                    map.put(w5h_value, values);
                                }
                            }
                        }
                    }
                }
            }

            for (String localLabel:map.keySet()) {
                StringBuilder sb = new StringBuilder();
                for (String localValue:map.get(localLabel)) {
                    sb.append(localValue);
                    sb.append(", ");
                }
                sb.delete(sb.length()-2,sb.length()-1);

                LinearLayout textLayout = new LinearLayout(context);
                textLayout.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                llp.setMargins(10, 5, 5, 0);
                textLayout.setLayoutParams(llp);

                TextView localTextView = new TextView(this.getContext());
                localTextView.setTextColor(Color.parseColor("#99CCFF"));
                localTextView.setText(getString(R.string.local, localLabel + " : "));

                TextView localValueTextView = new TextView(this.getContext());
                localValueTextView.setTextColor(Color.parseColor("#FFFFFF"));
                localValueTextView.setText(getString(R.string.local, sb.toString()));

                textLayout.addView(localTextView);
                textLayout.addView(localValueTextView);
                linearLayout.addView(textLayout);



            }



//                for (Task processTask : itemname.get(position).getTasks()) {
//                System.out.println("SIZE OF TASKS = "+itemname.get(position).getTasks().size());
//                if (processTask.getPid() instanceof Email){
//                    //txtTitle.setText(itemname.get(position).getScore()+", "+String.valueOf(((Email)processTask.getPid()).get_id()));
//                    imageView.setImageResource(imgid[0]);
//                    if (processTask.getLocalValues() != null) {
//                        for (LocalValues local : processTask.getLocalValues()) {
//                            if (local.getValue()!=null) {
//                                if (!local.getValue().toString().equalsIgnoreCase("[null]") && !local.getValue().toString().equalsIgnoreCase("[]")) {
//
//                                    LinearLayout textLayout = new LinearLayout(context);
//                                    textLayout.setOrientation(LinearLayout.HORIZONTAL);
//                                    LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//                                    llp.setMargins(10, 5, 5, 0);
//                                    textLayout.setLayoutParams(llp);
//
//                                    TextView localTextView = new TextView(this.getContext());
//                                    localTextView.setTextColor(Color.parseColor("#99CCFF"));
//                                    localTextView.setText(getString(R.string.local, local.getLocalProperties().getW5h_value() + " : "));
//
//                                    TextView localValueTextView = new TextView(this.getContext());
//                                    localValueTextView.setTextColor(Color.parseColor("#FFFFFF"));
//                                    localValueTextView.setText(getString(R.string.local, local.getValue().toString()));
//
//                                    textLayout.addView(localTextView);
//                                    textLayout.addView(localValueTextView);
//                                    linearLayout.addView(textLayout);
//                                }
//                            }
//                        }
//                    }
//
//                }else if (processTask.getPid() instanceof Payment) {
//                    //txtTitle.setText(itemname.get(position).getScore()+", "+((Payment) processTask.getPid()).getName());
//                    imageView.setImageResource(imgid[0]);
//                    if (processTask.getLocalValues() != null) {
//                        for (LocalValues local : processTask.getLocalValues()) {
//                            LinearLayout textLayout = new LinearLayout(context);
//                            textLayout.setOrientation(LinearLayout.HORIZONTAL);
//                            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//                            llp.setMargins(10, 5, 5, 0);
//                            textLayout.setLayoutParams(llp);
//
//                            TextView localTextView = new TextView(this.getContext());
//                            localTextView.setTextColor(Color.parseColor("#99CCFF"));
//                            localTextView.setText(getString(R.string.local, local.getLocalProperties().getW5h_value() + " : "));
//
//                            TextView localValueTextView = new TextView(this.getContext());
//                            localValueTextView.setTextColor(Color.parseColor("#FFFFFF"));
//                            localValueTextView.setText(getString(R.string.local, local.getLocalProperties().getW5h_value().toString()));
//
//                            textLayout.addView(localTextView);
//                            textLayout.addView(localValueTextView);
//                            linearLayout.addView(textLayout);
//
//                            localTextView = new TextView(this.getContext());
//                            llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//                            llp.setMargins(5, 5, 5, 5);
//                            localTextView.setLayoutParams(llp);
//                            localTextView.setText(getString(R.string.local, local.getLocalProperties().getW5h_label() + " : " + local.getValue().toString()));
//                            linearLayout.addView(localTextView);
//                        }
//                    }
//                }
//            }
            return rowView;
        };
    }


}


