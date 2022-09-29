package kanarious.musicnow;

import java.util.ArrayList;

public class IdTracker {
    private static int ID = 1;
    private static ArrayList<Integer> currentIds = new ArrayList<>(); //Current Held IDs
    private static ArrayList<Integer> freeIds = new ArrayList<>(); //Returned IDs that can be borrowed again

    public static int getID(){
        //Check if there any IDs to recycle
        if(freeIds.size() > 0){
            int id = freeIds.get(0);
            freeIds.remove(0);
            currentIds.add(id);
            return id;
        }
        //Generate new ID
        else{
            int id = ID;
            currentIds.add(ID);
            ID = ID + 1;
            return id;
        }
    }

    public static int getID(int id){
        //Check if there are any ID's in-use
        if(currentIds.size() > 0){
            //Check if desired ID is already in-use
            if(currentIds.contains(id)){
                return getID();
            }
        }
        //ID not in use therefore register desired id and return it for use
        currentIds.add(id);
        return id;
    }

    public static void freeID(int id){
        if(currentIds.contains(id)){
            int index = currentIds.indexOf(id);
            currentIds.remove(index);
            freeIds.add(id);
        }
    }

    public static void clearIDs(){
        currentIds.clear();
        freeIds.clear();
    }

}
