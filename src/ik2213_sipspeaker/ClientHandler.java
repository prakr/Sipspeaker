/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ik2213_sipspeaker;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Prakash
 */
public class ClientHandler implements Runnable{
    private Socket clientSocket;
    private BufferedInputStream bffrdInput;
    private DataOutputStream dataOutput;
    private byte[] inputBytes;
    private String index_webpage;
    private String status_webpage;
    private String not_found = "HTTP/1.0 200 OK\r\nConnection: close\r\nContent-Type: text/html\r\n\r\n<html><body><h1>404 Not found</h1></body></html>\r\n";
    private HTTPParser parser;
    private ConfigFileParser cParser;
    
    public ClientHandler(Socket clientSocket, ConfigFileParser configParser) throws IOException{
        this.clientSocket = clientSocket;
        this.cParser = configParser;
        bffrdInput = new BufferedInputStream(this.clientSocket.getInputStream());
        dataOutput = new DataOutputStream(this.clientSocket.getOutputStream());
        inputBytes = new byte[2048];
        parser = new HTTPParser();
        index_webpage = "HTTP/1.0 200 OK\r\nConnection: close\r\nContent-Type: text/html\r\n\r\n<html><head><meta http-equiv='Content-Type' content='text/html; charset=ISO-8850-1'><title>IK2213 - Sipspeaker project</title></head><body><h1>Index</h1>"
                        + "Current message: " + cParser.custom_message + "</br></br>"
                        + "<form method='post' action='http://localhost:4711'>Message:     <input type='text' size =40 name='message'></br>"
                        + "<input type=submit value='Submit Post'></form></br>"
                        + "<form method='post' action='http://localhost:4711'><input type='hidden' name='default' value='default'><input type=submit value='Use default'></form></br></form></body></html>\r\n";
    }
    
    @Override
    public void run() {
        try {
            //System.out.println("in run");
            int nBytes = bffrdInput.read(inputBytes);
            handleHTTPMessage(new String(inputBytes, Charset.defaultCharset()));
            bffrdInput.close();
            dataOutput.close();
            clientSocket.close();
        } catch (IOException ex) {;
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MethodNotSupportedException ex) {
            System.out.println(ex.getMessage());
        }
    }
    
    private void handleHTTPMessage(String httpMessage) throws MethodNotSupportedException, IOException{
        
        switch(parser.getMethod(httpMessage)){
            case GET:
                handleHTTPGetMessage(httpMessage);
                break;
            case POST:
                handleHTTPPostMessage(httpMessage);
                break;
        }
    }
    
    private void handleHTTPGetMessage(String httpGetMessage){
        if(parser.getRequestedPage(httpGetMessage).equals("INDEX")){
            sendIndexResponse();
        }else{
            sendNotFound();
        }
    }
    
    private void handleHTTPPostMessage(String httpGetMessage){
        if(parser.getMessage(httpGetMessage).contains("message=")){
            String message = parser.getMessage(httpGetMessage).split("=")[1];
            TextSpeech.ConvertTextToSpeech(message, cParser.customMessage_audio, 0);
            MessageToBePlayed.setMessageFileName(cParser.customMessage_audio);
            cParser.writeMessageText(message);
            //Write to config file
            reloadPage();
            sendIndexResponse();
        }else if(parser.getMessage(httpGetMessage).contains("default=")){
            System.out.println("DEFAULT");
            MessageToBePlayed.setMessageFileName(cParser.default_audio);
            //Write to config file
            
            cParser.writeMessageText("");
            cParser.custom_message = "";
            reloadPage();
            sendIndexResponse();
        }else{
            sendNotFound();
        }
    }
    
    private void sendIndexResponse(){
        try {
            dataOutput.writeBytes(index_webpage);
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void sendNotFound() {
        try {
            dataOutput.writeBytes(not_found);
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
    
    private void reloadPage(){
        index_webpage = "HTTP/1.0 200 OK\r\nConnection: close\r\nContent-Type: text/html\r\n\r\n<html><head><meta http-equiv='Content-Type' content='text/html; charset=ISO-8850-1'><title>IK2213 - Sipspeaker project</title></head><body><h1>Index</h1>"
                        + "Current message: " + cParser.custom_message + "</br></br>"
                        + "<form method='post' action='http://localhost:4711'>Message:     <input type='text' size =40 name='message'></br>"
                        + "<input type=submit value='Submit Post'></form></br>"
                        + "<form method='post' action='http://localhost:4711'><input type='hidden' name='default' value='default'><input type=submit value='Use default'></form></br></form></body></html>\r\n";
    }
}
