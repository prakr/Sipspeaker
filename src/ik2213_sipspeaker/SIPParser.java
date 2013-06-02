package ik2213_sipspeaker;

import java.nio.charset.Charset;

/**
 *
 * @author fredrik<fnordl@kth.se> and prakash<prakashr@kth.se>
 */
public class SIPParser {
    private String callID = "Call-ID";
    
    public SIPMessageTypes getSIPMessageType(byte[] sipMessage){
        String[] tmp = ((new String(sipMessage, Charset.defaultCharset())).split("\r\n")[0]).split(" ");
        if(tmp[0].equals("INVITE")){
            return SIPMessageTypes.INVITE;
        }else if(tmp[0].equals("CANCEL")){
            return SIPMessageTypes.CANCEL;
        }else if(tmp[0].equals("BYE")){
            return SIPMessageTypes.BYE;
        }else if(tmp[0].equals("ACK")){
            return SIPMessageTypes.ACK;
        }else if(tmp[0].equals("SIP/2.0")){
            return SIPMessageTypes.STATUS;
        }else{
            return SIPMessageTypes.ERROR;
        }
    }
    
    public String getSIPStatus(byte[] sipMessage){
        String[] tmp = ((new String(sipMessage, Charset.defaultCharset())).split("\r\n")[0]).split(" ");
        if(tmp.length > 2){
            return tmp[1];
        }
        return "INSERT EXCEPTION HERE BECAUSE THE STATUS FORMAT IS WRONG";
    }
    
    public int getCallIDIndex(SIPMessageTypes smt){
        switch(smt){
            case INVITE:
                return 4;
            case CANCEL:
                return 3;
            case ACK:
                return 3;
            default:
                return 0;
        }
    }
    
    public String getCallID(int callIDIndex, byte[] sipMessage){
        String[] tmp = (new String(sipMessage, Charset.defaultCharset())).split("\r\n");
        if(tmp.length > callIDIndex){
            return tmp[callIDIndex];
        }else{
            return "INSERT EXCEPTION HERE BECAUSE THE CALL-ID FORMAT IS WRONG";
        }
    }
    
    public boolean isCallID(String s){
        String[] m  = s.split(":");
        if(m[0].equals(callID)){
            return true;
        }else{
            return false;
        }
    }
    
    public String getBranch(byte[] sipMessage){
        String[] tmp = (new String(sipMessage, Charset.defaultCharset())).split("\r\n");
        if(tmp.length > 2){
            String[] tmp2 = tmp[1].split(";");
            if(tmp2.length > 2){
                return tmp2[2];
            }
        }
        return null; //NO BRANCH AVAILABLE, RETURN EXCEPTION
    }
    
    public String getFrom(byte[] sipMessage){
        String[] tmp = (new String(sipMessage, Charset.defaultCharset())).split("\r\n");
        if(tmp.length > 7){
            return tmp[7];
        }
        return null; //NO FROM AVAILABLE, RETURN EXCEPTION
    }
    
    public String getContact(byte[] sipMessage){
        String[] tmp = (new String(sipMessage, Charset.defaultCharset())).split("\r\n");
        if(tmp.length > 3){
            return (tmp[3].split("<"))[1].replace(">", "");
        }
        return null; //NO FROM AVAILABLE, RETURN EXCEPTION
    }
    
    public String getTo(byte[] sipMessage){
        String[] tmp = (new String(sipMessage, Charset.defaultCharset())).split("\r\n");
        if(tmp.length > 9){
            String[] tmp2 = tmp[9].split(" ");
            if(tmp2.length > 1){
                return tmp2[1].replace("<", "").replace(">", "");
            }
        }
        return null; //NO TO AVAILABLE, RETURN EXCEPTION
    }
    
    public int getRTPPortnumber(byte[] sipMessage){
        String[] tmp = (new String(sipMessage, Charset.defaultCharset())).split("\r\n");
        if(tmp.length > 19){
            String[] tmp2 = tmp[18].split(" ");
            if(tmp2.length > 2){
                return Integer.parseInt(tmp2[1]);
            }
        }
        return 0; //THROW EXCEPTION HERE BECAUSE THE FORMAT FOR THIS FIELD IS WRONG
    }
    
    public String getSessionOwnerAndName(byte[] sipMessage){
        String[] tmp = (new String(sipMessage, Charset.defaultCharset())).split("\r\n");
        if(tmp.length > 13){
            String[] tmp2 = tmp[13].split(" ");
            if(tmp2.length > 2){
                return tmp2[2];
            }
        }
        return null; //THROW EXCEPTION HERE BECAUSE THE FORMAT FOR THIS FIELD IS WRONG
    }
    
    public String getSessionName(byte[] sipMessage){
        String[] tmp = (new String(sipMessage, Charset.defaultCharset())).split("\r\n");
        if(tmp.length > 14){
            return tmp[14];
        }
        return null; //THROW EXCEPTION HERE BECAUSE THE FORMAT FOR THIS FIELD IS WRONG
    }
}
