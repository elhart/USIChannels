package models;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;


import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

@Entity
public class Apps extends Model{

	private static final long serialVersionUID = 1L;

	@Id
	public Long id;

	@Required
	public String name; 		//app name == subfolder name within the root folder
	
	public String iconPath; 	//path to the app icon

	
	public static Model.Finder<Long,Apps> find = new Finder<Long, Apps>(Long.class, Apps.class);


	public Apps(String name, String iconPath) {
		this.name = name;
		this.iconPath = iconPath;
	}
	
	public Apps() {
		this.name = "";
		this.iconPath = "";
	}
	
	// Returns a list of all the elements available
	public static List<Apps> all() {
		return find.all();
	}
	
	public static List<Apps> allSort() {
		return find.where()
				.orderBy("id")
				.findList();
	}

	// Insert a new element inside the database
	public static Apps addNew(Apps channel) {
		channel.save();
		return channel;
	}

	//	Deletes an app with a given id
	public static void delete(Long id) {
		find.ref(id).delete();
	}

	public static Apps get(Long id){
		return find.ref(id);
	}
	
	public static boolean contains(Long id){
		
		Apps ds = Apps.find.byId(id);
		if(ds == null){
			return false;
		}else{
			return true;
		}//if else
	}
	
	public static List<Apps> getAllByName(String app){	
		return find.where()
				.ilike("name", app)
				.orderBy("id")
				.findList();
		
	}
	
	public static Apps getByName(String app){	
		Apps ch = null;
		
		List<Apps> list = new ArrayList<Apps>();
		list = Apps.getAllByName(app);		
				
		//check if the channel is in the db
		Iterator<?> ii = list.listIterator();
		while(ii.hasNext()){
			Apps dbItem = (Apps) ii.next();
			if(dbItem.name.equals(app)){
				ch = dbItem;
			}//if
		}//while(ii.hasNext())
		return ch;	
	}

}

