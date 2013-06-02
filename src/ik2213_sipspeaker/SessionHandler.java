package ik2213_sipspeaker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.jlibrtp.*;
/**
 *
 * @author prakash<prakashr@kth.se> and fredrik<fnordl@kth.se>
 */
public class SessionHandler implements Runnable,RTPAppIntf{
    private RTPSession rtpSession = null;
    private DatagramSocket rtpSocket;
    private DatagramSocket rtcpSocket;
    private byte[] buf;
    private int rtpPortNumber;
    private String callID;
    private String branch;
    private String from;
    private long tag;
    private String to;
    private SessionStates sessionState;
    private boolean run = true;
    private int nBytesRead;
    private int packetCount;
    private String contact;
    private FileInputStream in;
    private byte[] outFile;
    private int ammountRead;
    private int offset;
    
    public SessionHandler(String callID, String branch, String from, String to, int rtpPortNumber, String contact) throws SocketException{
        this.callID = callID;
        this.branch = branch;
        this.from = from;
        this.to = to;
        this.rtpPortNumber = rtpPortNumber;
        buf = new byte[160];
        nBytesRead = 0;
        offset = 0;
        tag = (long) Double.parseDouble((from.split("="))[1]) + System.currentTimeMillis();
        sessionState = SessionStates.SESSION_INIT;
        this.contact = contact;
        outFile = new byte[52758];
        ammountRead = 0;
        try {
            in = new FileInputStream("default.wav");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SessionHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void addMessage(byte[] sipMessage){
        System.out.println("A sip message with this call-ID has arrived"); //Probably SIP BYE request.
    }
    
    @Override
    public void run(){
        try {
            rtpSocket = new DatagramSocket(rtpPortNumber);
            rtcpSocket = new DatagramSocket(rtpPortNumber+1); 
        } catch (SocketException ex) {
            Logger.getLogger(SessionHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        rtpSession = new RTPSession(rtpSocket, rtcpSocket);
        Participant p = new Participant(contact.split(":")[1], rtpPortNumber, rtpPortNumber+1);
        rtpSession.addParticipant(p);
        rtpSession.RTPSessionRegister(this, null, null);
        File soundFile = new File(MessageToBePlayed.getMessageFileName());
        if(!soundFile.exists()){
            System.err.println("Wave file not found.");
            return;
        }
        AudioInputStream audioInputStream = null;
        AudioFormat.Encoding encoding =  new AudioFormat.Encoding("PCMA");
        AudioFormat format = new AudioFormat(encoding, ((float) 8000.0), 8, 1, 2, ((float) 8000.0) ,false);
        rtpSession.payloadType(8);
        try {
            //audioInputStream = new AudioInputStream(new FileInputStream(soundFile), format, soundFile.length());
            //audioInputStream = AudioSystem.getAudioInputStream(new FileInputStream(soundFile));
            audioInputStream = AudioSystem.getAudioInputStream(soundFile);
        }catch(UnsupportedAudioFileException e1) {
            e1.printStackTrace();
            return;
        }catch(IOException e1) {
            e1.printStackTrace();
            return;
        }
        try {
            in.read(outFile);
        } catch (IOException ex) {
            Logger.getLogger(SessionHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        long timeStamp = 0;
        long seqN = 0;
        while(run){
            switch(getSessionSate()){
                case SESSION_UP:
                    if(!(nBytesRead == -1)){
                        readAudioStreamAndSendRTP(audioInputStream, p, timeStamp, seqN);
                    }else{
                        //Tell Sipspeaker to send Bye to peer.
                        setState(SessionStates.SESSION_DOWN);
                    }
                    break;
                case SESSION_DOWN:
                    run = false;
                    rtpSession.unregisterAVPFIntf();
                    rtpSession.endSession();
                    Sipspeaker.sendByeToClient(this, contact);
                    rtpSocket.close();
                    rtcpSocket.close();
                    break;
            }    
        }
    }
    
    private void readAudioStreamAndSendRTP(AudioInputStream audioInputStream, Participant p, long timestamp, long seqN){
        try {            
            nBytesRead = audioInputStream.read(buf);
            if(nBytesRead >= 0){
                rtpSession.sendData(buf, timestamp, seqN++);
                timestamp = timestamp + 160;
                Thread.sleep(20);
            }          
        } catch (InterruptedException ex) {
            Logger.getLogger(SessionHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SessionHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    } 
    
    public synchronized void setState(SessionStates state){
        sessionState = state;
    }
    
    public synchronized SessionStates getSessionSate(){
        return sessionState;
    }
    
    public String getBranch(){
        return branch;
    }
    
    public String getCallID(){
        return callID;
    }
    
    public String getFrom(){
        return from;
    }
    
    public long getTag(){
        return tag;
    }
    
    public String getTo(){
        return to;
    }
    
    public int getRTPPort(){
        return rtpPortNumber;
    }

    @Override
    public void receiveData(DataFrame df, Participant p) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void userEvent(int i, Participant[] ps) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int frameSize(int i) {
        //throw new UnsupportedOperationException("Not supported yet.");
        return 1;
    }
}
