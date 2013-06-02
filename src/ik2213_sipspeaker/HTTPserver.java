/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ik2213_sipspeaker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Prakash
 */
public class HTTPserver extends Thread{
   private static ServerSocket incommingRequestSocket;
   private static ConfigFileParser cParser; 
   
   public HTTPserver(String http_interface, int port, ConfigFileParser parser) throws IOException{
        initializeHTTPServer(http_interface,port);
        cParser = parser;        
   }
   
   public void run(){
       while(true){   
           try {   
                (new Thread(new ClientHandler(incommingRequestSocket.accept(), cParser))).start();
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ex) {
                    Logger.getLogger(HTTPserver.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (IOException ex) {
               Logger.getLogger(HTTPserver.class.getName()).log(Level.SEVERE, null, ex);
           }
       }
   }
   
   private static void initializeHTTPServer(String http_interface,int port) throws IOException{
        if(http_interface.compareTo("") == 0){
            incommingRequestSocket = new ServerSocket(port);
        }else {
           incommingRequestSocket = new ServerSocket (4711, 10, InetAddress.getByName(http_interface));
       }
   }
}
