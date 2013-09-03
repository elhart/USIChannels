package models;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;

import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

@Entity
public class Items extends Model{

	private static final long serialVersionUID = 1L;

	@Id
	public Long id;

	@Required
	public String name; 		//item name == items in the subfolder
	public String channel; 		//name of the channel the item belongs to
	public String path;			//path to the item
	public String app;			//app
	//public String type; 		//type of the item {image, video}
	//public String date;			//date when the item was added
	
	public static Model.Finder<Long,Items> find = new Finder<Long, Items>(Long.class, Items.class);

	public Items(String name, String channel, String app, String path){
		this.name = name;
		this.channel = channel;
		this.app = app;
		this.path = path;
		//this.type = type;
		//this.date = date;
	}
	
	// Returns a list of all the elements available
	public static List<Items> all() {
		return find.all();
	}

	// Insert a new element inside the database
	public static Items addNew(Items item) {
		item.save();
		return item;
	}

	//	Deletes an app with a given id
	public static void delete(Long id) {
		find.ref(id).delete();
	}

	public static Items get(Long id){
		return find.ref(id);
	}
	
	public static boolean contains(Long id){
		
		Items ds = Items.find.byId(id);
		if(ds == null){
			return false;
		}else{
			return true;
		}//if else
	}
	
	public static List<Items> getItemsByApp(String app){	
		return find.where()
				.ilike("app", app)
				.orderBy("id")
				.findList();
	}//getItemsByApp
	
	public static List<Items> getItemsByChannel(String channel){	
		return find.where()
				.ilike("channel", channel)
				.orderBy("id")
				.findList();
	}//getItemsByChannel
	
	public static List<Items> getItemsByName(String item){	
		return find.where()
				.ilike("name", item)
				.orderBy("id")
				.findList();
	}//getItemsByName
	
	public static Items getItemByAppChannelAndName(String app, String channel, String item){
		Items it = null;
		
		List<Items> list = new ArrayList<Items>();
		list = Items.getItemsByName(item);	
		
				
		//check if the item is in the db
		Iterator<?> ii = list.listIterator();
		while(ii.hasNext()){
			Items dbItem = (Items) ii.next();
			if(dbItem.app.equals(app) && dbItem.channel.equals(channel)){
				it = dbItem;
			}//if
		}//while(ii.hasNext())
		
		return it;	
	}//getItemByAppChannelAndName
	

}

