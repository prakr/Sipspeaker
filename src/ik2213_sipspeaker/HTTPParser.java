/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ik2213_sipspeaker;

/**
 *
 * @author Prakash
 */
public class HTTPParser {
   
    public HTTPMethods getMethod(String httpMessage) throws MethodNotSupportedException{
        String[] split = httpMessage.split("/");
        if(split[0].equals("GET") || split[0].equals("GET ")){
            return HTTPMethods.GET;
        }else if(split[0].equals("POST") || split[0].equals("POST ")){
            return HTTPMethods.POST;
        }else{
            System.out.println("Debugging@: " + httpMessage);
            throw new MethodNotSupportedException("No such method");
        }
    }
    
    public String getRequestedPage(String httpMessage){
        String[] split = httpMessage.split("/");
        if(split[1].equals(" HTTP") || split[1].equals("index.html HTTP")){
            return "INDEX";
        }else{
            return "NO SUCH PAGE";
        }
    }    
    
    public String getMessage(String httpMessage){
        String[] split = httpMessage.split("\n");
        //System.out.println(httpMessage);
        return split[split.length-1].replace("%40", "@").replace("+", " ").replace("%3F", "?");
        
    }
}
