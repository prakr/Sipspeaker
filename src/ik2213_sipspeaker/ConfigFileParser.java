/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ik2213_sipspeaker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fredrik<fnordl@kth.se> and prakash<prakashr@kth.se>
 */
public class ConfigFileParser {
    String sip_interface = "";
    String sip_user = "sipspeaker";
    int sip_port = 5060;
    String http_interface = "127.0.0.1";
    int http_port = 80;
    String default_audio = "default.wav";
    String customMessage_audio = "currentmessage.wav";
    String custom_message = "";
    String theFile;
    private String filename;
    byte[] buf; 
    public ConfigFileParser (String filename) throws IOException{
        this.filename = filename;
        initialize();
    }
    
    private void initialize() throws IOException{
        String s ;
        String []list;
        String Lval;
        String Rval;
        BufferedReader bf = null;
        try {
            bf = new BufferedReader(new FileReader(filename));
            File file = new File(filename);
            int size = (int) file.length();
            buf = new byte[size];
            FileInputStream fis = new FileInputStream(filename);
            fis.read(buf);
            
            theFile = new String(buf);
            //System.out.println(theFile);
        } catch (FileNotFoundException ex) {
            return ;
        }
        while((s = bf.readLine())!=null){
            s.trim();
            if( s.compareToIgnoreCase("") == 0 || s.charAt(0) == '#'){
                continue;
            }
            list = s.split("=");
            if(list.length < 2){
                continue;
            }
            Lval = list[0].trim();
            Rval = list[1].trim();
            if(Rval.compareToIgnoreCase("") == 0){
                continue;
            }
            if(Lval.compareToIgnoreCase("default_message") == 0){
                default_audio = Rval;
            }else if(Lval.compareToIgnoreCase("message_wav") == 0){
                customMessage_audio = Rval;
            }else if(Lval.compareToIgnoreCase("message_text") == 0){
                custom_message = Rval;
            }else if(Lval.compareToIgnoreCase("sip_interface") == 0){
                sip_interface = Rval;
            }else if(Lval.compareToIgnoreCase("sip_port") == 0){
                sip_port = new Integer (Rval);
            }else if(Lval.compareToIgnoreCase("sip_user") == 0){
                sip_user = Rval;
            }else if(Lval.compareToIgnoreCase("http_interface") == 0){
                http_interface = Rval;
            }else if(Lval.compareToIgnoreCase("http_port") == 0){
                http_port = new Integer (Rval);
            }
        }
    }
    
    public void writeMessageText(String msgTxt){
        String[] tmp = theFile.split("\r\n");
        try {
           FileOutputStream fos = new FileOutputStream(filename);
           String output = "";
           for(String tmp_string:tmp){
               if(tmp_string.contains("message_text = ")){
                   output = output + "message_text = " + msgTxt.trim() + "\r\n";
               }else{
                    output = output + tmp_string + "\r\n"; 
               }
           } 
           fos.write(output.getBytes());
           initialize();
        } catch (IOException ex) {
            Logger.getLogger(ConfigFileParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

