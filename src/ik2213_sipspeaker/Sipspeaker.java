package ik2213_sipspeaker;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author prakash<prakashr@kth.se> and fredrik<fnordl@kth.se>
 */
public class Sipspeaker {
    private static HashMap<String, SessionHandler> map = new HashMap<String, SessionHandler>();
    private static ArrayList<String> removedList = new ArrayList<String>();
    private static byte[] buf = new byte[1024];
    private static DatagramPacket packet = new DatagramPacket(buf, buf.length);
    private static SIPParser sipParser = new SIPParser();
    private static SDPParser sdpParser = new SDPParser();
    private static DatagramSocket serverSocket;
    private static byte[] outBuf = new byte[1024];
    
    
    static String conf_file_name;
    static String sip_interface;
    static String sip_user;
    static int sip_port;
    static String http_interface;
    static int http_port;
    static int connection_id;
    
    
    
    public static void main(String[] arg) throws IOException{
        cmdInitializer(arg);
        serverSocket.setSoTimeout(20);
        while(true){
            try {
                serverSocket.receive(packet);
                byte[] sipMessage = packet.getData();
                switch(sipParser.getSIPMessageType(sipMessage)){
                    case INVITE:
                        System.out.println("INVITE");
                        handleSIPInvite(sipMessage);
                        break;
                    case BYE:
                        System.out.println("BYE");
                        handleSIPBye(sipMessage);
                        break;
                    case CANCEL:
                        handleSIPCancel(sipMessage);
                        break;
                    case STATUS:
                        handleSIPStatuses(sipMessage);
                        break;
                    case ACK:
                        handleSIPACK(sipMessage);
                        break;
                    default:
                        break;
                }
            } catch(SocketTimeoutException ste) {
                continue;
            }   
        }
    }
    
    private static void cmdInitializer(String[] args) throws IOException{
        conf_file_name = "sipspeaker.cfg";
        sip_interface = null;
        sip_user = null;
        sip_port = -1;
        http_interface = null;
        http_port = -1;		
        /* Parse the command line parameters */
        for(int i = 0 ; i < args.length;i=i+2){
            if(args[i].equalsIgnoreCase("-c") == true){
                conf_file_name = args[i+1];
            }else if(args[i].equalsIgnoreCase("-user") == true){
                String sip_uri = args[i+1];
                if(sip_uri.indexOf(":") != -1){
                    sip_port = new Integer(sip_uri.split(":")[1]);
                    sip_uri = sip_uri.split(":")[0];
                }else{
                    sip_port = 5060;
                }
                sip_user = sip_uri.split("@")[0];
                sip_interface = sip_uri.split("@")[1];
            }else if(args[i].equalsIgnoreCase("-http") == true){
                String http_bind_address = args[i+1];
                if(http_bind_address.indexOf(":") != -1){
                    http_port = new Integer(http_bind_address.split(":")[1]);
                    http_interface = http_bind_address.split(":")[0];
                }else{
                    http_port = 80;
                    http_interface = "0.0.0.0";
                }
            }else{
                System.out.println("wrong usage of commandline");
                System.exit(-1);
            }
        }

        ConfigFileParser configHandle = new ConfigFileParser(conf_file_name);
        if(sip_interface == null){
            sip_interface = configHandle.sip_interface;
        }
        if(sip_user == null){
            sip_user = configHandle.sip_user;
        }
        if(sip_port == -1){
            sip_port = configHandle.sip_port;
        }
        if(http_interface == null){
            http_interface = configHandle.http_interface;
        }
        if(http_port == -1){
            http_port = configHandle.http_port;
        }
        System.out.println(sip_interface);
        System.out.println(sip_user);
        System.out.println(sip_port);
        System.out.println(http_interface);
        System.out.println(http_port);
        //this is to test text to speech
        //TextSpeech text = new TextSpeech();
        //text.ConvertTextToSpeech("hi call me later",configHandle.default_audio);
        ////////////////
        if(sip_interface.compareTo("") == 0){
            serverSocket = new DatagramSocket(sip_port);   
        }
        else{
            serverSocket = new DatagramSocket(sip_port,InetAddress.getByName (sip_interface)); 
        }
        
        if(!"".equals(configHandle.custom_message)){
            TextSpeech.ConvertTextToSpeech
            (configHandle.custom_message, configHandle.customMessage_audio, 0);
            MessageToBePlayed.setMessageFileName(configHandle.customMessage_audio);
        }else{    
            File f = new File (configHandle.default_audio);
            if (f.exists() == false){
                TextSpeech.ConvertTextToSpeech
                ("hello , call me later",configHandle.default_audio, 0);
            }
            MessageToBePlayed.setMessageFileName(configHandle.default_audio);   
        }
         
         
       (new HTTPserver(http_interface, http_port, configHandle)).start();  
    }
    
    private synchronized static void handleSIPInvite(byte[] sipMessage) throws SocketException{
        String callID = sipParser.getCallID(sipParser.getCallIDIndex(SIPMessageTypes.INVITE), sipMessage);
        if(!sipParser.getTo(sipMessage).contains(sip_user)){
            System.out.println(sipParser.getTo(sipMessage));
            System.out.println(sip_user);
            System.out.println("404 Not found"); 
            generateAndSendBackNotFound(sipMessage);//Send back
            return;
        }
        if(map.containsKey(callID)){ //This call-ID exists and a handler thread has been created for this call session. I.e. the first invite response did not reach the client.
            map.get(callID).addMessage(sipMessage);//Probably won't do this, instead resend the first invite response.
        }else if(sipParser.isCallID(callID)){ //This is a legit call-ID and a handler threaed should be created to handle the call session.
            SessionHandler sch = new SessionHandler(callID, sipParser.getBranch(sipMessage), sipParser.getFrom(sipMessage), sipParser.getTo(sipMessage), sipParser.getRTPPortnumber(sipMessage), sipParser.getContact(sipMessage));
            map.put(callID, sch);
            generateAndSendBackTrying(sch);
            generateAndSendBackRinging(sch);
            generateAndSendBackSessionOK(sch, sipParser.getSessionOwnerAndName(sipMessage), sipParser.getSessionName(sipMessage));
        }else{ //Not a valid call-ID field
            System.out.println("Not a valid call-ID field");
            System.out.println("---------------------------------------");
            System.out.println(new String(sipMessage, Charset.defaultCharset()));
            System.out.println("---------------------------------------");
        }
    }
    
    private synchronized static void handleSIPBye(byte[] sipMessage){
        String callID = sipParser.getCallID(sipParser.getCallIDIndex(SIPMessageTypes.CANCEL), sipMessage);
        if(map.containsKey(callID) && map.get(callID).getSessionSate() == SessionStates.SESSION_UP){//This call-ID exists and a handler thread has been created for this call session.
            generateAndSendBackByeOK(map.get(callID)); 
            map.get(callID).setState(SessionStates.SESSION_DOWN);
            removedList.add(callID);
            map.remove(callID);
        }else if(sipParser.isCallID(callID)){ //This is a legit call-ID and a handler threaed should be created to handle the call session.
            //Do nothing since there is no session for this call-ID
        }else{ //Not a valid call-ID field
            System.out.println("Not a valid call-ID field");
            System.out.println("---------------------------------------");
            System.out.println(new String(sipMessage, Charset.defaultCharset()));
            System.out.println("---------------------------------------");
        }
    }
    
    private synchronized static void handleSIPCancel(byte[] sipMessage){
        String callID = sipParser.getCallID(sipParser.getCallIDIndex(SIPMessageTypes.CANCEL), sipMessage);
        if(map.containsKey(callID)){ //This call-ID exists and a handler thread has been created for this call session.
            generateAndSendBackOK(map.get(callID)); 
            generateAndSendBackRequestTerminated(map.get(callID));
            removedList.add(callID);
            map.remove(callID);
        }else if(sipParser.isCallID(callID)){ //This is a legit call-ID and a handler threaed should be created to handle the call session.
            //Do nothing since there is no session for this call-ID
        }else{ //Not a valid call-ID field
            System.out.println("Not a valid call-ID field");
            System.out.println("---------------------------------------");
            System.out.println(new String(sipMessage, Charset.defaultCharset()));
            System.out.println("---------------------------------------");
        }
    }
    
    private synchronized static void handleSIPStatuses(byte[] sipMessage){
        String sipStatusMessage = sipParser.getSIPStatus(sipMessage);
        if(sipStatusMessage.equals("487")){//Request Terminated
            
        }else if(sipStatusMessage.equals("200")){//OK
            
        }else{
            //Not valid or not supported status message
        }
    }
    
    private synchronized static void handleSIPACK(byte[] sipMessage){
        String callID = sipParser.getCallID(sipParser.getCallIDIndex(SIPMessageTypes.ACK), sipMessage);
        if(removedList.contains(callID)){
            System.out.println("Call fully terminated");
            removedList.remove(callID);
        }else if(map.containsKey(callID) && map.get(callID).getSessionSate() == SessionStates.SESSION_INIT){
            map.get(callID).setState(SessionStates.SESSION_UP);
            (new Thread(map.get(callID))).start();
        }
    }
    
    protected synchronized static void sendByeToClient(SessionHandler sch, String conatct){
        generateAndSendBackBye(sch, conatct);
        removedList.add(sch.getCallID());
        map.remove(sch.getCallID());
    }
    
    private static void generateAndSendBackTrying(SessionHandler sch){
        try {
            String outMessage = "SIP/2.0 100 Trying\r\n"+
                                "Via: SIP/2.0/UDP " + packet.getAddress().getHostAddress() + ";rport=5060;received=" + packet.getAddress().getHostAddress() + ";" + sch.getBranch() + "\r\n" +
                                "Content-Length: 0\r\n" +
                                sch.getCallID() + "\r\n" +  
                                "CSeq: 1 INVITE\r\n" +
                                sch.getFrom() + "\r\n" +
                                "Server: SJphone/1.60.299a/L (SJ Labs)\r\n" +
                                "To: \"unknown\"<" + sch.getTo() + ">;tag=" + sch.getTag() + "\r\n\r\n";
            System.out.println(outMessage);
            outBuf = outMessage.getBytes(Charset.defaultCharset());
            DatagramPacket out = new DatagramPacket(outBuf, outBuf.length, packet.getSocketAddress());
            serverSocket.send(out);
        } catch (IOException ex) {
            Logger.getLogger(Sipspeaker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void generateAndSendBackRinging(SessionHandler sch){
        try {
            String outMessage = "SIP/2.0 180 Ringing\r\n"+
                                "Via: SIP/2.0/UDP " + packet.getAddress().getHostAddress() + ";rport=5060;received=" + packet.getAddress().getHostAddress() + ";" + sch.getBranch() + "\r\n" +
                                "Content-Length: 0\r\n" +
                                "Contact: <" + sch.getTo() + ":5060>\r\n" +
                                sch.getCallID() + "\r\n" +  
                                "CSeq: 1 INVITE\r\n" +
                                sch.getFrom() + "\r\n" +
                                "Server: SJphone/1.60.299a/L (SJ Labs)\r\n" +
                                "To: \"unknown\"<" + sch.getTo() + ">;tag=" + sch.getTag() + "\r\n\r\n";
            System.out.println(outMessage);
            outBuf = outMessage.getBytes(Charset.defaultCharset());
            DatagramPacket out = new DatagramPacket(outBuf, outBuf.length, packet.getSocketAddress());
            serverSocket.send(out);
        } catch (IOException ex) {
            Logger.getLogger(Sipspeaker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void generateAndSendBackSessionOK(SessionHandler sch, String sessionOwnerAndName, String sessionName){
        try {
            String sdpMessage = sdpParser.parseSDPPacket(sessionOwnerAndName, (sch.getTo().split("@"))[1], sessionName, sch.getRTPPort());
            String outMessage = "SIP/2.0 200 OK\r\n"+
                                "Via: SIP/2.0/UDP " + packet.getAddress().getHostAddress() + ";rport=5060;received=" + packet.getAddress().getHostAddress() + ";" + sch.getBranch() + "\r\n" +
                                "Content-Length: " + sdpMessage.length() + "\r\n" +
                                "Contact: <" + sch.getTo() + ":5060>\r\n" +
                                sch.getCallID() + "\r\n" +
                                "Content-Type: application/sdp\r\n" +
                                "CSeq: 1 INVITE\r\n" +
                                sch.getFrom() + "\r\n" +
                                "Server: SJphone/1.60.299a/L (SJ Labs)\r\n" +
                                "To: \"unknown\"<" + sch.getTo() + ">;tag=" + sch.getTag() + "\r\n\r\n" +
                                sdpMessage;
            System.out.println(outMessage);
            outBuf = outMessage.getBytes(Charset.defaultCharset());
            DatagramPacket out = new DatagramPacket(outBuf, outBuf.length, packet.getSocketAddress());
            serverSocket.send(out);
        } catch (IOException ex) {
            Logger.getLogger(Sipspeaker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void generateAndSendBackByeOK(SessionHandler sch){
        try {
            String outMessage = "SIP/2.0 200 OK\r\n"+
                                "Via: SIP/2.0/UDP " + packet.getAddress().getHostAddress() + ";rport=5060;received=" + packet.getAddress().getHostAddress() + ";" + sch.getBranch() + "\r\n" +
                                "Content-Length: 0\r\n" +
                                sch.getCallID() + "\r\n" +  
                                "CSeq: 2 BYE\r\n" +
                                sch.getFrom() + "\r\n" +
                                "Server: SJphone/1.60.299a/L (SJ Labs)\r\n" +
                                "To: \"unknown\"<" + sch.getTo() + ">;tag=" + sch.getTag() + "\r\n\r\n";
            System.out.println(outMessage);
            outBuf = outMessage.getBytes(Charset.defaultCharset());
            DatagramPacket out = new DatagramPacket(outBuf, outBuf.length, packet.getSocketAddress());
            serverSocket.send(out);
        } catch (IOException ex) {
            Logger.getLogger(Sipspeaker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void generateAndSendBackBye(SessionHandler sch, String contact){
        try {
            String outMessage = "BYE " + contact + " SIP/2.0\r\n" +
                                "Via: SIP/2.0/UDP " + sch.getTo().split("@")[1] + ";rport;" + sch.getBranch() + "\r\n" +
                                "Content-Length: 0\r\n" +
                                sch.getCallID() + "\r\n" +  
                                "CSeq: 1 BYE\r\n" +
                                "From: \"unknown\"<" + sch.getTo() + ">;tag=" + sch.getTag() + "\r\n" +
                                "Max-Forwards: 70\r\n" +
                                sch.getFrom().replace("From: \"unknown\"", "To: ") + "\r\n" +
                                "User-Agent: SJphone/1.60.299a/L (SJ Labs)\r\n\r\n";
            System.out.println(outMessage);
            outBuf = outMessage.getBytes(Charset.defaultCharset());
            DatagramPacket out = new DatagramPacket(outBuf, outBuf.length, InetAddress.getByName(contact.split(":")[1]), sip_port);
            serverSocket.send(out);
        } catch (IOException ex) {
            Logger.getLogger(Sipspeaker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void generateAndSendBackOK(SessionHandler sch){
        try {
            String outMessage = "SIP/2.0 200 OK\r\n"+
                                "Via: SIP/2.0/UDP " + packet.getAddress().getHostAddress() + ";rport=5060;received=" + packet.getAddress().getHostAddress() + ";" + sch.getBranch() + "\r\n" +
                                "Content-Length: 0\r\n" +
                                sch.getCallID() + "\r\n" +  
                                "CSeq: 1 CANCEL\r\n" +
                                sch.getFrom() + "\r\n" +
                                "Server: SJphone/1.60.299a/L (SJ Labs)\r\n" +
                                "To: \"unknown\"<" + sch.getTo() + ">;tag=" + sch.getTag() + "\r\n\r\n";
            System.out.println(outMessage);
            outBuf = outMessage.getBytes(Charset.defaultCharset());
            DatagramPacket out = new DatagramPacket(outBuf, outBuf.length, packet.getSocketAddress());
            serverSocket.send(out);
        } catch (IOException ex) {
            Logger.getLogger(Sipspeaker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void generateAndSendBackRequestTerminated(SessionHandler sch){
        try {
            String outMessage = "SIP/2.0 487 Request Terminated\r\n"+
                                "Via: SIP/2.0/UDP " + packet.getAddress().getHostAddress() + ";rport=5060;received=" + packet.getAddress().getHostAddress() + ";" + sch.getBranch() + "\r\n" +
                                "Content-Length: 0\r\n" +
                                sch.getCallID() + "\r\n" +  
                                "CSeq: 1 INVITE\r\n" +
                                sch.getFrom() + "\r\n" +
                                "Server: SJphone/1.60.299a/L (SJ Labs)\r\n" +
                                "To: \"unknown\"<" + sch.getTo() + ">;tag=" + sch.getTag() + "\r\n\r\n";
            System.out.println(outMessage);
            outBuf = outMessage.getBytes(Charset.defaultCharset());
            DatagramPacket out = new DatagramPacket(outBuf, outBuf.length, packet.getSocketAddress());
            serverSocket.send(out);
        } catch (IOException ex) {
            Logger.getLogger(Sipspeaker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static void generateAndSendBackNotFound(byte[] sipMessage){
        try {
            
            String outMessage = "SIP/2.0 404 Not Found\r\n"+
                                "Via: SIP/2.0/UDP " + packet.getAddress().getHostAddress() + ";rport=5060;received=" + packet.getAddress().getHostAddress() + ";" + sipParser.getBranch(sipMessage) + "\r\n" +
                                "Content-Length: 0\r\n" +
                                sipParser.getCallID(sipParser.getCallIDIndex(SIPMessageTypes.INVITE), sipMessage) + "\r\n" +  
                                "CSeq: 1 INVITE\r\n" +
                                sipParser.getFrom(sipMessage) + "\r\n" +
                                "Server: SJphone/1.60.299a/L (SJ Labs)\r\n" +
                                "To: \"unknown\"<" + sipParser.getTo(sipMessage) + ">;tag=" + ((long) Double.parseDouble((sipParser.getFrom(sipMessage).split("="))[1]) + System.currentTimeMillis()) + "\r\n\r\n";
            System.out.println(outMessage);
            outBuf = outMessage.getBytes(Charset.defaultCharset());
            DatagramPacket out = new DatagramPacket(outBuf, outBuf.length, packet.getSocketAddress());
            serverSocket.send(out);
        } catch (IOException ex) {
            Logger.getLogger(Sipspeaker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
