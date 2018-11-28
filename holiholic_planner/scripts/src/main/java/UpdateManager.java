import org.apache.commons.io.IOUtils;
import org.json.*;

import java.io.*;
import java.net.*;
import java.util.*;


/* ActionFactory - Creates a new instance given the command line arguments
 *
 */
class ActionFactory {
    static Action getAction(String[] args) {
        if (args == null || args.length == 0)
            return null;

        if (args[0].equals("updateDistance")) {
            return new UpdateDistanceAction(args);
        } else if (args[0].equals("updateRestaurants")){
            return new UpdateRestaurantsAction(args);
        }
        return null;
    }
}

public class UpdateManager {
    public static void main(String[] args) {
        Action action = ActionFactory.getAction(args);
        action.execute();
    }
}
