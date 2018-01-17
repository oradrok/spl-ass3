package bgu.spl181.net.api.bidi;

import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageHandler {


    public static boolean register (String[] data){
        if(JsonHandler.getUser(data[0],"password")!=null)return false;

        if (data.length == 2)
            return JsonHandler.addUser(data);

        if (data.length == 3 && data[2].substring(0, 8).compareTo("country=") == 0){
            data[2] = data[2].substring(8);
            return JsonHandler.addUser(data);
        }
        return false;
    }

    public static boolean login(String[] data) {
        if (data.length > 2)
            return false;
        else {
            boolean ans = false;
            String password = JsonHandler.getUser(data[0],"password");
            if (password != null && password.compareTo(data[1]) == 0)
                ans = true;
            return ans;
        }
    }

    public static Request request(String[] data,String name,String message){
        Request r = new Request(data,message,name);
        if(r.call != null)
            r.run();
        else r.answer = "ERROR 404 ileagel request";
        return r;
    }

}