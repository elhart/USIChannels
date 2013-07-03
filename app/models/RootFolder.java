package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;

import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

@Entity
public class RootFolder extends Model{

	private static final long serialVersionUID = 1L;

	@Id
	public Long id;

	@Required
	public String path; 	//the root folder for the channels 

	public int updateRequest; 			//update the database if there is a change in the root folder

	
	public static Model.Finder<Long,RootFolder> find = new Finder<Long, RootFolder>(Long.class, RootFolder.class);


	public RootFolder(String path, int update) {
		this.path = path;
		this.updateRequest = update;
	}
	
	// Returns a list of all the elements available
	public static List<RootFolder> all() {
		return find.all();
	}

	// Insert a new element inside the database
	public static RootFolder addNew(RootFolder folder) {
		folder.save();
		return folder;
	}

	//	Deletes an app with a given id
	public static void delete(Long id) {
		find.ref(id).delete();
	}

	public static RootFolder get(Long id){
		return find.ref(id);
	}
	
	public static void setUpdate(Long id){
		RootFolder root = find.ref(id);
		root.updateRequest = 0;
		root.update();
	}
	
	public static boolean contains(Long id){
		
		RootFolder ds = RootFolder.find.byId(id);
		if(ds == null){
			return false;
		}else{
			return true;
		}//if else
	}

}

