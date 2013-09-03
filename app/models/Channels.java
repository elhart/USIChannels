package models;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;


import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

@Entity
public class Channels extends Model{

	private static final long serialVersionUID = 1L;

	@Id
	public Long id;

	@Required
	public String name; 		//channel name == subfolder name within the root folder

	public long nItems; 		//number of items in each channel/folder
	
	public String coverPath; 	//path to the cover image, 
	
	public String app;			//application to which the channel belongs

	
	public static Model.Finder<Long,Channels> find = new Finder<Long, Channels>(Long.class, Channels.class);


	public Channels(String name, long nItems, String coverPath, String app){
		this.name = name;
		this.nItems = nItems;
		this.coverPath = coverPath;
		this.app = app;
	}
	
	public Channels() {
		this.name = "";
		this.nItems = 0L;
		this.coverPath = "";
		this.app = "";
	}
	
	// Returns a list of all the elements available
	public static List<Channels> all() {
		return find.all();
	}
	
	public static List<Channels> allSort() {
		return find.where()
				.orderBy("id")
				.findList();
	}

	// Insert a new element inside the database
	public static Channels addNew(Channels channel) {
		channel.save();
		return channel;
	}

	//	Deletes an app with a given id
	public static void delete(Long id) {
		find.ref(id).delete();
	}

	public static Channels get(Long id){
		return find.ref(id);
	}
	
	public static boolean contains(Long id){
		
		Channels ds = Channels.find.byId(id);
		if(ds == null){
			return false;
		}else{
			return true;
		}//if else
	}
	
	public static List<Channels> getChannelsByName(String channel){	
		return find.where()
				.ilike("name", channel)
				.orderBy("id")
				.findList();
		
	}
	
	public static List<Channels> getChannelsByApp(String app){
		return find.where()
				.ilike("app", app)
				.orderBy("id")
				.findList();
	}
	
	public static Channels getChannelsByAppAndName(String app, String channel){
		Channels ch = null;
		
		List<Channels> list = new ArrayList<Channels>();
		list = Channels.getChannelsByApp(app);		
				
		//check if the channel is in the db
		Iterator<?> ii = list.listIterator();
		while(ii.hasNext()){
			Channels dbItem = (Channels) ii.next();
			if(dbItem.name.equals(channel)){
				ch = dbItem;
			}//if
		}//while(ii.hasNext())
		return ch;	
	}
	
	public static Channels getChannelByName(String channel){	
		Channels ch = null;
		
		List<Channels> list = new ArrayList<Channels>();
		list = Channels.getChannelsByName(channel);		
				
		//check if the channel is in the db
		Iterator<?> ii = list.listIterator();
		while(ii.hasNext()){
			Channels dbItem = (Channels) ii.next();
			if(dbItem.name.equals(channel)){
				ch = dbItem;
			}//if
		}//while(ii.hasNext())
		return ch;	
	}

}

