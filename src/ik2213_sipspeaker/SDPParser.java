package ik2213_sipspeaker;

/**
 *
 * @author Fredrik Nordlund<fnordl@kth.se> and Prakash<prakashr@kth.se>
 */
public class SDPParser {
    public String parseSDPPacket(String sessionIDandOwner, String sessionInterface, String sessionName, int rtpPort){
        return  "v=0\r\n" +
                "o=- " + sessionIDandOwner + " " + sessionIDandOwner + " IN IP4 " + sessionInterface + "\r\n" +
                sessionName + "\r\n" +
                "c=IN IP4 " + sessionInterface + "\r\n" +
                "t=0 0\r\n" +
                "a=direction:active\r\n" +
                "m=audio " + rtpPort + " RTP/AVP 8\r\n" +
                "a=rtpmap:8 PCMA/8000\r\n";
    }
}
