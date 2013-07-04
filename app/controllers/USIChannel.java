package controllers;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

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
	public static String wsAddress = "ws://localhost:9010/usichannel/socket/";
			
	// levels of debug messages: 0-none, 1-events, 2-method calls, 3-in method code 
	public static int verbose = 3;

	
	//----------------------------------------------------------------
	//---- HTTP Request ----------------------------------------------
	//display size can be: small(600x1080), big(1320x1080), fullscreen(1920x1080)

    public static Result index(String displayID, String channel) {
    	if(verbose == 1 || verbose == 2 || verbose == 3)
  		  Logger.info(appName+": a new display is connecting...");
    	
    	if(displayID == null) displayID = "99";
    	if(channel == null || channel == "") channel = "USI Channels";
    	
    	folderWatchDog();
    	
  	  	return ok(usichannel.render(appDisplayName, channel, displayID, wsAddress));
  	  
    }// index()
    
    //----------------------------------------------------------------
    // --- SCHEDULER
    public static void starScheduler(){
		Logger.info(appName+".scheduler.start() ---");
		final ScheduledFuture<?> taskHandle = 
				scheduler.scheduleAtFixedRate(task, 10, 10, SECONDS);
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
    		//path = "../../Dropbox/Apps/USIDisplay/";
    		path = "/home/elhart/Dropbox/Apps/USIDisplay/";
    		RootFolder rf = new RootFolder(path,0);
    		RootFolder.addNew(rf);
    	}//if(RootFolder.contains(1l))
    	if(verbose == 3){
    		Logger.info(appName+".readLocalFolder.path = "+path);    	
    		Logger.info(appName+".readLocalFolder.update = "+updateFolder);
    	}
    	
    	// update the db
    	updateFolder = 1;
    	if(updateFolder == 1){
    		//delete all tables from the db
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
    			Logger.info(appName+".readLocalFolder.update: db updated");
    	}//if
    	
        // get the list of channels
    	File file = new File(path);
        String[] channels = file.list();
        
        // Read channels from the db
        //List<Channels> dbChannels = new ArrayList<Channels>();
        //dbChannels = Channels.all();

        if(channels != null){
        	//for each channel in the root folder
        	for(int i = 0; i < channels.length; i++) {
        		if(!channels[i].startsWith(".")){
        			
        			//get items in the channel
        			File channel = new File(path+channels[i]);
        			String[] channelItems = channel.list();
        			int channelListLength = 0;
        			if(channelItems != null){
        				channelListLength = channelItems.length;
        			}//if
        			        			
        			Channels dch = Channels.getChannelByName(channel.getName());	
    				if(dch == null){
    					if(verbose == 3)
    						Logger.info(appName+".readLocalFolder.channel: ----- "+channels[i]+" not in the db, add it");
            			
    					//channel cover
    					File channelCover = new File(path+channels[i]+"/cover/");
    					String coverPath = "na";
    					if(channelCover.exists()){
    						if(verbose == 3)
    							Logger.info(appName+".readLocalFolder.channelcover: "+channels[i]);
    						String[] coverList = channelCover.list();
    						if(coverList != null){
    							if(verbose == 3)
    								Logger.info(appName+".readLocalFolder.channelcover: "+channels[i]+" exists, length: "+coverList.length);
    							for(int c=0; c<coverList.length; c++){
    								if(!coverList[c].startsWith(".")){
    									coverPath = path+channels[i]+"/cover/"+coverList[c];
    									if(verbose == 3)
    										Logger.info(appName+".readLocalFolder.channelcover.path:"+coverPath);
    								}//if
    							}//for
    						}//if
   
    					}else if(channel.isFile()){//if channel is a file
    						coverPath = channel.getPath();
    					}
    					//add new channel to the db
    					Channels nh = new Channels(channels[i], channelListLength, coverPath);
    					Channels.addNew(nh);
    					//TODO
        				//send updates to clients (new channel)
    				}//if(dch == null)
    				
        			//for each item in the channel
        			if(channel.isDirectory()){
        				for(int j=0; j<channelItems.length; j++){
        					if(!channelItems[j].startsWith(".")){
        						if(!channelItems[j].equals("cover") && !channelItems[j].equals("Cover")){
        							if(channelItems[j].contains(".jpg") || channelItems[j].contains(".JPG") ||
        							   channelItems[j].contains(".png") || channelItems[j].contains(".PNG")){
        								checkAddItems(channels[i], channelItems[j], channel.getPath()+"/"+channelItems[j]);
        							}//if
        						}//if
        					}//if
        				}//for
        			}//if(channel.isDirectory())
        			if(channel.isFile()){
        				if(channel.getName().contains(".jpg") ||channel.getName().contains(".JPG") ||
        						channel.getName().contains(".png") || channel.getName().contains(".PNG")){
        					checkAddItems(channels[i], channel.getName(), channel.getPath());
        				}//if	
        			}//if(channel.isFile())
        		}//if
            }//for
        }else{
        	Logger.info(appName+".readLocalFolder.rootFolder = null");
    	}//else if(channels != null)
        
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
    
    // --- check and add items to the db
    public static void checkAddItems(String channel, String item, String path){
    	
		//get all items in the db for the channel
		List<Items> dbItems = new ArrayList<Items>();
		dbItems = Items.get(channel);
		//check if the item is in the db
		Iterator<?> ii = dbItems.listIterator();
		boolean itemFound = false;
		while(ii.hasNext()){
			Items dbItem = (Items) ii.next();
			if(dbItem.name.equals(item)){
				itemFound = true;
			}//if
		}//while(ii.hasNext())
			
		//if the item is not in the db add it
		if(!itemFound){
			if(verbose == 3)
				Logger.info(appName+"checkAddItems.item:                "+ channel+": "+item+" not in the db, add it");
			//ADD NEW ITEM	
			Items ni = new Items(item, channel, path);
			Items.addNew(ni);
		}//if(!itemFound)
		
		//TODO
		//send update to clients in the channel (new item)
		
    }//public static void checkAddItems()
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
    
    public static void sendWsMsgAddItem(String b64image, String itemName, String channel, String did){
    	if(verbose == 1 || verbose == 2 || verbose ==3)
			Logger.info(appName+".   sendWsMsgAddItem displayID: "+did+" channel: "+ channel+" item: "+itemName);
  		ObjectNode msg = Json.newObject();
		msg.put("kind", "addItem");
		msg.put("itemName", itemName);
		msg.put("channel", channel);
		msg.put("item", b64image);
		msg.put("did", did);
		
		sendMsgToClientId(msg, did);
  		
  	}//sendWsMsgAddItem
    
    public static void sendWsMsgChannelsAndItemsToClient(String did){
    	List<Channels> readChannels = new ArrayList<Channels>();
    	readChannels = Channels.all();
  		
    	Iterator<?> rci = readChannels.iterator();
    	while(rci.hasNext()){
    		Channels readChannel = (Channels) rci.next();
    		
    		//remove extension
    		String channelName =  readChannel.name;
    		if(channelName.contains("."))
    			channelName = channelName.substring(0, channelName.length()-4);
    		
    		if(readChannel.coverPath.equals("na")){
    			sendWsMsgAddChannel(channelName, (int)readChannel.nItems, "na", did);
    		}else{
    			sendWsMsgAddChannel(channelName, (int)readChannel.nItems, encodeImage(readChannel.coverPath), did);
    		}//if
    		
    		List<Items> readItems = new ArrayList<Items>();
    		readItems = Items.get(readChannel.name);
    		Iterator<?> rii = readItems.iterator();
    		while(rii.hasNext()){
    			Items readItem = (Items) rii.next();
    			
    			//remove extension
     			String rChannelName =  readItem.channel;
    	    	if(rChannelName.contains("."))
    	    		rChannelName = rChannelName.substring(0, rChannelName.length()-4);
    	    	String rItemName = readItem.name;
    	    	if(rItemName.contains("."))
    	    		rItemName = rItemName.substring(0, rItemName.length()-4);
    	    	
    			sendWsMsgAddItem(encodeImage(readItem.path), rItemName, rChannelName, did);
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
	  							sendWsMsgChannelsAndItemsToClient(displayid);
								
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
