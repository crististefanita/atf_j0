package atf.reporting.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/** Standalone JSON contract for POST/PUT report evidence. */
public final class ReportJsonBodyPolicy {
    public static final String NULL_DOCUMENT="null"; private static final ObjectMapper JSON=new ObjectMapper();
    private ReportJsonBodyPolicy() { }
    public static boolean appliesTo(String method){return method!=null && Set.of("POST","PUT").contains(method.trim().toUpperCase(Locale.ROOT));}
    public static boolean validDocument(String body){if(body==null||body.isBlank())return false; try{return JSON.readTree(body)!=null;}catch(Exception e){return false;}}
    public static String display(String body){return body==null||body.isBlank()?NULL_DOCUMENT:body;}
    public static List<String> validateWriteExchange(String requestBody,String responseBody){
        List<String> v=new ArrayList<>(); validate("request",requestBody,v); validate("response",responseBody,v); return List.copyOf(v);
    }
    private static void validate(String side,String body,List<String> v){if(body==null||body.isBlank()||NULL_DOCUMENT.equals(body.trim()))v.add(side+" JSON body is required for POST/PUT and must not be null"); else if(!validDocument(body))v.add(side+" JSON body is not valid JSON");}
}
