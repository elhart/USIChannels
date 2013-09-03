

import controllers.USIChannel;
import play.*;

public class Global extends GlobalSettings {

  @Override
  public void onStart(Application app) {
    Logger.info("Glogal: OnStart: Application has started...");
    USIChannel.starScheduler();
    
  }  
  
  @Override
  public void onStop(Application app) {
    Logger.info("Global: OnStop: Application shutdown...");
  }  
    
}