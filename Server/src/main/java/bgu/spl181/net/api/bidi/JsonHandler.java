package bgu.spl181.net.api.bidi;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.io.*;
import java.util.ArrayList;

public class JsonHandler {
    public static Gson gson = new Gson();
    public static String userPath = "Database/Clients.json";
    public static String moviePath = "Database/Movies.json";
    public static BufferedReader buff = null;
    public static ArrayList<LinkedTreeMap> jsonUser = null;
    public static ArrayList<LinkedTreeMap> jsonMovie = null;
    private static LinkedTreeMap<String, ArrayList> usersMap = null;
    private static LinkedTreeMap<String, ArrayList> moviesMap = null;



    public synchronized  static String getMovie(String moviename, String key){
        ReadFromJson();
        for (LinkedTreeMap<String , String> L : jsonMovie){
            if (((String)L.get("name")).compareTo(moviename) == 0)
                return L.get(key);
        }
        return null;
    }

    public synchronized static void addMovie(String movieName,String amount,String price,ArrayList<String> countries){
        int id = jsonMovie.size()+1;
        LinkedTreeMap<String, Object> M = new LinkedTreeMap<String,Object>();
        M.put("id",Integer.toString(id));
        M.put("name",movieName);
        M.put("price",price);
        M.put("bannedCountries",countries);
        M.put("availableAmount", amount);
        M.put("totalAmount",amount);
        jsonMovie.add(M);
        try {
            WriteToJson(moviePath,gson.toJson(moviesMap));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public synchronized static boolean removeMovie(String movieName){
        boolean ans = false;
        ReadFromJson();
        for (LinkedTreeMap<String , String> L : jsonMovie){
            if (((String)L.get("name")).compareTo(movieName) == 0) {
                String availableMount =L.get("availableAmount");
                String totalAmount = L.get("totalAmount");
                if(availableMount.compareTo(totalAmount)==0){
                    jsonMovie.remove(L);
                    try {
                        WriteToJson(moviePath,gson.toJson(moviesMap));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
            }
        }

        return ans;
    }

    public synchronized static void changeMovie(String movieName,String key,String value){
        ReadFromJson();
        for (LinkedTreeMap<String , String> L : jsonMovie){
            if (((String)L.get("name")).compareTo(movieName) == 0) {
                L.remove(key);
                L.put(key,value);
                try {
                    WriteToJson(moviePath,gson.toJson(moviesMap));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public synchronized static boolean takeMovie(String moviename){
        ReadFromJson();
        for (LinkedTreeMap<String , String> L : jsonMovie){
            if (((String)L.get("name")).compareTo(moviename) == 0){
                int amount = Integer.parseInt(L.get("availableAmount"));
                if (amount ==0)return false;
                amount --;
                L.remove("availableAmount");
                L.put("availableAmount",Integer.toString(amount));
                try {
                    WriteToJson(moviePath,gson.toJson(moviesMap));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }

        }
        return false;
    }

    public synchronized static boolean userHasMovie(String moviename,String username){
        ReadFromJson();
        boolean ans =false;
        for (LinkedTreeMap L : jsonUser){
            if (((String)L.get("username")).compareTo(username) == 0){
                ArrayList<LinkedTreeMap> movies = (ArrayList<LinkedTreeMap>) L.get("movies");
                for (LinkedTreeMap<String,String> item : movies){
                    if(item.get("name").compareTo(moviename)==0){
                        ans =true;
                    }
                }
            }
        }
        return ans;
    }

    //has movie in jason
    public synchronized static boolean hasMovie(String moviename){
        ReadFromJson();
        boolean ans =false;
        for (LinkedTreeMap<String , String> L : jsonMovie){
            if (((String)L.get("name")).compareTo(moviename) == 0)
                ans =true;
        }
        return ans;
    }

    public synchronized static ArrayList<String> getAllMovies(){
        ReadFromJson();
        ArrayList<String> ans = new ArrayList<>();
        for (LinkedTreeMap<String , String> L : jsonMovie){
            String temp = new String((String)L.get("name"));
            ans.add(temp);
        }
        return ans;
    }

    public synchronized  static ArrayList getCountry(String moviename){
        ReadFromJson();
        for (LinkedTreeMap<String,Object> L : jsonMovie){
            String temp = (String) L.get("name");
            if (temp.compareTo(moviename) == 0) {
                ArrayList ans = (ArrayList) (L.get("bannedCountries"));
                return ans;
            }
        }
        return null;
    }



    public synchronized static String getUser(String username,String key) {
        ReadFromJson();
        for (LinkedTreeMap<String , String> L : jsonUser){
            if (((String)L.get("username")).compareTo(username) == 0)
                return L.get(key);
        }
        return null;
    }

    public synchronized  static void giveReturnMovie(String username,String moviename,String movieId,boolean give) {
        ReadFromJson();
        for (LinkedTreeMap L : jsonUser){
            if (((String)L.get("username")).compareTo(username) == 0){
                ArrayList<LinkedTreeMap<String,String>> usersMovies  = (ArrayList<LinkedTreeMap<String,String>>)L.get("movies");
                if(give){
                    LinkedTreeMap<String,String> M = new LinkedTreeMap<String,String>();
                    M.put("id",movieId);
                    M.put("name",moviename);
                    usersMovies.add(M);
                }
                else{//remove the movie from the user
                    for(LinkedTreeMap<String,String> item:usersMovies){
                        if(item.get("name").compareTo(moviename) == 0 ){
                            usersMovies.remove(item);
                            break;
                        }
                    }
                }
            }

        }

        try {
            WriteToJson(userPath,gson.toJson(usersMap));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static void changeUser(String username,String key,String value){
        ReadFromJson();
        for (LinkedTreeMap<String , String> L : jsonUser){
            if (((String)L.get("username")).compareTo(username) == 0) {
                L.remove(key);
                L.put(key,value);
                try {
                    WriteToJson(userPath,gson.toJson(usersMap));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public synchronized  static boolean addUser(String[] data){
        boolean ans = false;
        ReadFromJson();
        LinkedTreeMap user = new LinkedTreeMap();
        user.put("username",data[0]);
        user.put("type","normal");
        user.put("password",data[1]);
        if (data.length == 3) {
            data[2] = Request.getMoiveName(data[2]);
            user.put("country", data[2]);
        }
        else
            user.put("country","");
        user.put("movies", new ArrayList<LinkedTreeMap>());
        user.put("balance", "0");

        try {
            buff = new BufferedReader(new FileReader(userPath));
        } catch (FileNotFoundException e){
            e.printStackTrace();
        }

        LinkedTreeMap<String, ArrayList> M =usersMap;
        M.get("users").add(user);

        String newJson = gson.toJson(M);
        ans = true;
        try {
            WriteToJson(userPath, newJson);
        } catch (IOException e) {
            e.printStackTrace();
            ans = false;
        }

        return ans;
    }




    private synchronized static void WriteToJson( String path, String newJason) throws IOException {
        File file = new File(path);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.append(newJason);
        fileWriter.flush();
    }

    private synchronized static void ReadFromJson(){
        try {
            buff = new BufferedReader(new FileReader(userPath));

        } catch (FileNotFoundException e){
            e.printStackTrace();
        }
        usersMap = ((LinkedTreeMap<String, ArrayList>)gson.fromJson(buff, Object.class));
        jsonUser = usersMap.get("users");
        try {
            buff = new BufferedReader(new FileReader(moviePath));

        } catch (FileNotFoundException e){
            e.printStackTrace();
        }
        moviesMap = ((LinkedTreeMap<String, ArrayList>)gson.fromJson(buff, Object.class));
        jsonMovie = moviesMap.get("movipricees");
    }

}