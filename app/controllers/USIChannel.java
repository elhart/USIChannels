package controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import models.Apps;
import models.Channels;
import models.Items;
import models.RootFolder;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import com.ning.http.util.Base64;

import play.*;
import play.libs.Json;
import play.libs.F.Callback;
import play.libs.F.Callback0;
import play.mvc.*;
import play.mvc.WebSocket.Out;

import views.html.*;

import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class USIChannel extends Controller {
  
	// appName
	public static String appName = "USIChannels"; //internal app name - do not change!
	public static String appDisplayName = "USI Channels";
	
	// wsAddress
	//public static String wsAddress = "ws://pdnet.inf.unisi.ch:9010/usichannel/socket/";
	//public static String wsAddress = "ws://localhost:9010/usichannel/socket/";
	public static String wsAddress = "ws://uc-dev.inf.usi.ch:9010/usichannel/socket/";
	
	
	// web app address
	public static String webAppAddress = "http://pdnet.inf.unisi.ch/usiapps/";
			
	// levels of debug messages: 0-none, 1-events, 2-method calls, 3-in method code 
	public static int verbose = 3;

	
	//----------------------------------------------------------------
	//---- HTTP Request ----------------------------------------------
	//display size can be: small(600x1080), big(1320x1080), fullscreen(1920x1080)

    public static Result index(String app, String displayID) {
    	if(verbose == 1 || verbose == 2 || verbose == 3)
  		  Logger.info(appName+": a new display is connecting...id: "+displayID+"  app: "+app);
    	
    	if(displayID == null) displayID = "99";
    	
    	if(app != null){
	    	if(Apps.getByName(app) == null){ 
	    		return ok(index.render("Sorry, there is no such app as "+app+""));
	    	}else{
	    		return ok(usichannel.render(appDisplayName, app, displayID, wsAddress));
	    	 	  
	    	}
    	}else{
    		return ok(index.render("Sorry, choose an app first!"));
    	}  	
    	
  	  	
    }// index()
    
    //----------------------------------------------------------------
    // --- SCHEDULER
    public static void starScheduler(){
		Logger.info(appName+".scheduler.start() ---");
		//final ScheduledFuture<?> taskHandle = scheduler.scheduleAtFixedRate(task, 10, 10, SECONDS);
		folderWatchDog();
	}

	//should end with the application
	public static void stopScheduler(){
		Logger.info(appName+".scheduler.stop() ---");
		scheduler.shutdown();
	}

	public static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	final static Runnable task = new Runnable() {
		public void run() { 
			
			Logger.info(appName+".scheduler.task(): check for new images");
			//folderWatchDog();
			Logger.info("-------------------------------------------------");
		
		}//run
	};//task
	// --- scheduler
    //----------------------------------------------------------------
    
	//----------------------------------------------------------------
    // --- folderWatchDog
    public static void folderWatchDog(){
    	
    	readLocalFolder();
    	
    }//folderWatchDog
    //----------------------------------------
    
    // -- deleteAll
    public static void deleteAll(int updateFolder){
    	
		// update the db
		updateFolder = 1;
		if(updateFolder == 1){
			//delete all tables from the db
			
			//delete apps
			List<Apps> deleteApps = new ArrayList<Apps>();
			deleteApps = Apps.all();
			Iterator<?> dai = deleteApps.iterator();
			while(dai.hasNext()){
				Apps deleteApp = (Apps) dai.next();
				Apps.delete(deleteApp.id);
			}//while
			
			//delete channels
			List<Channels> deleteChannels = new ArrayList<Channels>();
			deleteChannels = Channels.all();
			Iterator<?> dci = deleteChannels.iterator();
			while(dci.hasNext()){
				Channels deleteChannel = (Channels) dci.next();
				Channels.delete(deleteChannel.id);
			}//while
			
			//delete items
			List<Items> deleteItems = new ArrayList<Items>();
			deleteItems = Items.all();
			Iterator<?> dii = deleteItems.iterator();
			while(dii.hasNext()){
				Items deleteItem = (Items) dii.next();
				Items.delete(deleteItem.id);
			}//while
			
			//change the update field
			RootFolder.setUpdate(1l);
			
			if(verbose == 3)
				Logger.info(appName+".deleteAll(): db deleted");
		}//if
    	
    }//deleteAll()
    //-------------
    
    public static String[] getItemsFromFile(String path){
    	File file = new File(path);
    	return file.list();
    }//getItemsFromFile(String path)
    
    // --- readLocalFolder
    public static void readLocalFolder(){
    	if(verbose == 2 || verbose == 3)
    		Logger.info(appName+".readLocalFolder --------------------------------------");  
    	
    	//read the root folder path from the db 
    	String path = "";
    	int updateFolder = 0;
    	if(RootFolder.contains(1l)){
    		path = RootFolder.get(1l).path;
    		updateFolder = RootFolder.get(1l).updateRequest;
    	}else{
    		//path = "../../Dropbox/Apps/USIApps/";
    		path = "/home/elhart/Dropbox/USIApps/";
    		RootFolder rf = new RootFolder(path,0);
    		RootFolder.addNew(rf);
    	}//if(RootFolder.contains(1l))
    	if(verbose == 3){
    		Logger.info(appName+".readLocalFolder.path = "+path);    	
    		Logger.info(appName+".readLocalFolder.update = "+updateFolder);
    	}
    	
    	//part of the update procedure: delete and read 
    	deleteAll(updateFolder);
    	
    	// --- apps ----
    	addAppsToDB(path);
    	
        
      //check if there are folders/items in the db that were deleted from the root folder
		
		//if there are any, then delete them form the db
		//send updates to clients (delete channel)
      
     //check if there are any items in the db that were deleted from the folder
		//if yes, delete them from the folder
		//send updates to clients in the channel (delete item)
	
        
        
        if(verbose == 2 || verbose ==3)
    		Logger.info(appName+".readLocalFolder --------------------------------------");
    }//readLocalFolder
    //----------------------------------------	
    
    public static void addAppsToDB(String path){
    	// get the list of all apps
    	String[] appsFromFile = getItemsFromFile(path);
    	
    	if(appsFromFile != null){
    		//for each app in the root folder
    		for(int a = 0; a < appsFromFile.length; a++){
    			if(!appsFromFile[a].startsWith(".")){
    				//add app to the db
    				addAppToDB(appsFromFile[a], webAppAddress+appsFromFile[a]+"/appicon/appicon");
    				
    				//get channels
    				addChannelsToDB(appsFromFile[a], path+appsFromFile[a]);
    				
    			}//if(!appsFromFile[a].startsWith("."))
    		}// for a
    	}//if(appsFromFile != null)
    }//addAppsToDB
    
    public static void addAppToDB(String app, String iconPath){	
    	Apps appExistsInDB = Apps.getByName(app);
    	if(appExistsInDB == null){//not in the DB, add it
    		if(verbose == 3)
    			Logger.info(appName+": addAppToDB(): "+app+": "+iconPath);
    		Apps newApp = new Apps(app,iconPath);
    		Apps.addNew(newApp);
		}else{//if(appExists == null)
			if(verbose == 3)
    			Logger.info(appName+": addAppToDB(): "+app+" is already in DB!!!");
		}
    }//addAppToDB()
    
    public static void addChannelsToDB(String app, String pathApp){
    	// get the list of all channels
    	String[] channelsFromFile = getItemsFromFile(pathApp);
    	
    	if(channelsFromFile != null){
    		//for each channel in the app folder
    		for(int c = 0; c < channelsFromFile.length; c++){
    			if(!channelsFromFile[c].startsWith(".") && !channelsFromFile[c].equals("appicon")){
    				//add channel to db
    				addChannelToDB(channelsFromFile[c], webAppAddress+app+"/"+channelsFromFile[c]+"/cover/cover", app);
    				
    				//add items to db
    				addItemsToDB(channelsFromFile[c], app, pathApp+"/"+channelsFromFile[c]);
    				
    			}//if
    		}// for c
    	}//if(channelsFromFile != null)
    	
    }//addChannelsToDB
    
    public static void addChannelToDB(String channel, String coverPath, String app){
    	Channels channelExistInDB = Channels.getChannelsByAppAndName(app, channel);
    	
    	if(channelExistInDB == null){//not in the DB, add it
    		if(verbose == 3)
    			Logger.info(appName+": addChannelToDB(): "+app+" "+channel+": "+coverPath);
    		Channels newChannel = new Channels(channel, 0l, coverPath, app);
    		Channels.addNew(newChannel);
    	}else{//if
    		if(verbose == 3)
    			Logger.info(appName+": addChannelToDB(): "+app+" "+channel+" is already in DB!!!");
    	}
    	
    	//TODO
    	//send update to clients
    	
    }//addChannelToDB
    
    public static void addItemsToDB(String channel, String app, String pathChannel){
    	// get the list of all items in the channel
    	String[] itemsFromFile = getItemsFromFile(pathChannel);
    	
    	if(itemsFromFile != null){
    		//for each item in the channel
    		for(int i = 0; i < itemsFromFile.length; i++){
    			if(!itemsFromFile[i].startsWith(".") && !itemsFromFile[i].equals("cover") && !itemsFromFile[i].contains("button")){
    				//add item to db
    				addItemToDB(itemsFromFile[i], channel, app);
    			}//if
    		}//for
    	}//if
    	
    }//addItemsToDB
    
    // --- add items to the db, first checks if the item is in the db
    public static void addItemToDB(String item, String channel, String app){
    			
		//if the item is not in the db, add it
		if(Items.getItemByAppChannelAndName(app, channel, item) == null){
			if(item.contains(".jpg")  || item.contains(".JPG")  ||
			   item.contains(".jpeg") || item.contains(".JPEG") ||
			   item.contains(".png")  || item.contains(".PNG")){
				
				if(verbose == 3)
					Logger.info(appName+": addItemToDB(): "+app+": "+channel+": "+item);
				//ADD NEW ITEM	
				Items ni = new Items(item, channel, app, webAppAddress+app+"/"+channel+"/"+item);
				Items.addNew(ni);
			}else{//if
				if(verbose == 3)
					Logger.info(appName+": addItemToDB(): "+app+": "+channel+": "+item+" not IMAGE!!!");
			}
		}else{//if
			if(verbose == 3)
				Logger.info(appName+": addItemToDB(): "+app+": "+channel+": "+item+" already in DB!!!");
		}
		
		
		//TODO
		//send update to clients in the channel (new item)
		
    }//public static void addItemToDB()
    //----------------------------------------
    
    //folderWatchDog -------------------------------------------------
    
    
    //----------------------------------------------------------------
    //---- send ws messages ------------------------------------------
    
    public static void sendWsMsgConnected(String did){
    	if(verbose == 1 || verbose == 2 || verbose ==3)
			Logger.info(appName+".sendWsMsgConnected displayID: "+did);
  		ObjectNode msg = Json.newObject();
		msg.put("kind", "displayConnected");
		msg.put("did", did);
		
		sendMsgToClientId(msg, did);
  		
  	}//sendChannelsAndItemsToClient
    
    public static void sendWsMsgAddChannel(String channel, int nItems, String cover, String did){
    	if(verbose == 1 || verbose == 2 || verbose ==3)
			Logger.info(appName+".sendWsMsgAddChannel displayID: "+did+" channel: "+channel);
  		ObjectNode msg = Json.newObject();
		msg.put("kind", "addChannel");
		msg.put("channel", channel);
		msg.put("nitems", nItems);
		msg.put("cover", cover);
		msg.put("did", did);
		
		sendMsgToClientId(msg, did);
  		
  	}//sendWsMsgAddChannel
    
    public static void sendWsMsgAddItem(String item, String itemName, String channel, String did){
    	if(verbose == 1 || verbose == 2 || verbose ==3)
			Logger.info(appName+".   sendWsMsgAddItem displayID: "+did+" channel: "+ channel+" item: "+itemName);
  		ObjectNode msg = Json.newObject();
		msg.put("kind", "addItem");
		msg.put("itemName", itemName);
		msg.put("channel", channel);
		msg.put("item", item);
		msg.put("did", did);
		
		sendMsgToClientId(msg, did);
  		
  	}//sendWsMsgAddItem
    
    public static void sendWsMsgChannelsAndItemsToClient(String did, String app){
    	List<Channels> readChannels = new ArrayList<Channels>();
    	readChannels = Channels.getChannelsByApp(app);
  		
    	Iterator<?> rci = readChannels.iterator();
    	while(rci.hasNext()){
    		Channels readChannel = (Channels) rci.next();
    		
    		//remove extension if the channel is a file
    		String channelName =  readChannel.name;
    		if(channelName.contains("."))
    			channelName = channelName.substring(0, channelName.length()-4);
    		
    		sendWsMsgAddChannel(channelName, (int)readChannel.nItems, readChannel.coverPath, did);
    		
    		List<Items> readItems = new ArrayList<Items>();
    		readItems = Items.getItemsByChannel(readChannel.name);  //TODO check for multiple channels in different apps
    		Iterator<?> rii = readItems.iterator();
    		while(rii.hasNext()){
    			Items readItem = (Items) rii.next();
    			
    			//remove extension
     			String rChannelName =  readItem.channel;
    	    	if(rChannelName.contains(".")){
    	    		rChannelName = rChannelName.substring(0, rChannelName.length()-4);
    	    	}//if
    	    	String rItemName = readItem.name;
    	    	if(rItemName.contains(".")){
    	    		if(rItemName.contains(".jpeg") || rItemName.contains(".JPEG")){
    	    			rItemName = rItemName.substring(0, rItemName.length()-5);
    	    		}else{
    	    			rItemName = rItemName.substring(0, rItemName.length()-4);
    	    		}
    	    	}
    			//sendWsMsgAddItem(encodeImage(readItem.path), rItemName, rChannelName, did);
    	    	sendWsMsgAddItem(readItem.path, rItemName, rChannelName, did);
    		}//while
    		
    	}//while
    	
  	}//sendWsMsgChannelsAndItemsToClient
    
    public static String encodeImage(String path){
    	String b64image = "";
    	
    	Logger.info("image path: "+path);
		File sourceimage = new File(path);
		Image image = null;
		try {
			image = ImageIO.read(sourceimage);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		  try {
		    ImageIO.write( (RenderedImage) image, "jpg", baos );
		  } catch( IOException ioe ) {}
		 
		  b64image = Base64.encode(baos.toByteArray());
    	
    	return b64image;
    }//encodeImage(String path)
    
    
    //send ws messages -----------------------------------------------
    
    //----------------------------------------------------------------
    //---- WS --------------------------------------------------------
   
    public static HashMap<String, Sockets> displaySockets = new HashMap<String, Sockets>();
    public static HashMap<WebSocket.Out<JsonNode>, String> displaySocketReverter = new HashMap<WebSocket.Out<JsonNode>, String>();
    
    public static WebSocket<JsonNode> webSocket() { 
  		return new WebSocket<JsonNode>() {

  			// Called when the Websocket Handshake is done.
  			public void onReady(WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out){

  				// For each event received on the socket
  				// --- onMessage ---
  				in.onMessage(new Callback<JsonNode>() { 
  					public void invoke(JsonNode event) {
  						String messageKind = event.get("kind").asText();						
  						
  						// --- displayReady
  						if(messageKind.equals("displayReady")){
  						//save the connection for later use
							if(!displaySockets.containsKey(event.get("displayID"))){
								String displayid = event.get("displayID").asText();
								String app = event.get("app").asText();
								
								if(verbose == 1 || verbose == 2 || verbose == 3)
									Logger.info(appName+".ws(): new display connected id: "+displayid);
								
								if(displayid != "null"){
									//register display
									displaySockets.put(event.get("displayID").asText(), new Sockets(out));
									displaySocketReverter.put(out, event.get("displayID").asText());	
								}//if
								
								//send connection message
								sendWsMsgConnected(displayid);
								
								//send channels and items to the client
	  							sendWsMsgChannelsAndItemsToClient(displayid,app);
								
							}//if
  	
  						}//displayReady 
  						//----------------------------------------
  						
  						// --- displayDisconnect
  						if(messageKind.equals("displayRemove")){
  					
  						
  						}//displayDisconnect
  						//----------------------------------------
  						
  						
  					}//invoke
  				});//in.onMessage
  				//----------------------------------------

  				// When the socket is closed.
  				// --- onClose
  				in.onClose(new Callback0() {
  					public void invoke() { 
  						String displayID = displaySocketReverter.get(out);
  						displaySocketReverter.remove(out);
  						displaySockets.remove(displayID);
  						if(verbose == 1 || verbose == 2 || verbose == 3)
  							Logger.info(appName+".ws(): planner "+displayID+" is disconnected.");
  					}//invoke
  				});//in.onClose
  				//----------------------------------------
  				
  			}//onReady
  		};//WebSocket<String>()
  	}//webSocket() { 

  	public static class Sockets {
  		public WebSocket.Out<JsonNode> wOut;

  		public Sockets(Out<JsonNode> out) {
  			this.wOut = out;
  		}
  	}//class
  	
  	public static void sendMsgToClients(ObjectNode msg){
		if(verbose == 1 || verbose == 2 || verbose ==3)
			Logger.info(appName+".ws().sendMsgToAllClients");
		//send to all clients
		Set<?> set = displaySockets.entrySet();
		Iterator<?> i = (Iterator<?>) set.iterator();
		while(i.hasNext()) {
			Map.Entry ds = (Map.Entry)i.next();
			sendMsgToClient(msg, displaySockets.get(ds.getKey()).wOut);
		}//while 
	}//sendMsgToClients
	
  	public static void sendMsgToClientId(ObjectNode msg, String displayID){
  		sendMsgToClient(msg, displaySockets.get(displayID).wOut);
  	}//sendMsgToClientId
  	
	private static void sendMsgToClient(ObjectNode msg, Out<JsonNode> out){
		if(verbose == 1 || verbose == 2 || verbose ==3){
			String displayID = displaySocketReverter.get(out);
			//Logger.info(appName+".ws().sendMsgToClient  displayID: "+displayID);
		}
		out.write(msg);
	}//sendMegToClient
  	
    
  	//WS --------------------------------------------------------------------
    
    
    
}//public class USIChannel extends Controller
