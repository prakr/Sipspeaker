/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ik2213_sipspeaker;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.SingleFileAudioPlayer;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioFileFormat.*;
/**
 *
 * @author fredrik<fnordl@kth.se> and prakash<prakashr@kth.se>
 */
public class TextSpeech {
    
   public static void ConvertTextToSpeech(String text, String audio,int type){


        Voice voice;
    	SingleFileAudioPlayer str;
        FileInputStream input;
        try{ 
            voice=VoiceManager.getInstance().getVoice("kevin16"); 
            if(voice!=null){
                System.out.println("not empty");
                 voice.allocate();
            }   
           // voice = new Voice();
            System.out.println(audio.split("\\.")[0]);
            str = new SingleFileAudioPlayer (audio.split("\\.")[0],Type.WAVE);  
            voice.setAudioPlayer(str);
            if (type == 1){
        	    InputStream inputStream = new FileInputStream(text);
                    voice.speak(inputStream);
        	}
        	else{           	
        	   voice.speak(text);        			
        	}
        	voice.deallocate();
                str.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    
    
    
}
