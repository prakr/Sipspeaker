package ik2213_sipspeaker;

/**
 *
 * @author prakash<prakashr@kth.se> & Fredrik<fnordl@kth.se>
 */
public class MessageToBePlayed {
    private static String messageFileName = "";
    
    public synchronized static String getMessageFileName(){
        return messageFileName;
    } 
    
    public synchronized static void setMessageFileName(String name){
        messageFileName = name;
    }
    
}
