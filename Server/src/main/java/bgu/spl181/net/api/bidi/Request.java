package bgu.spl181.net.api.bidi;



import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Request {
    Runnable call;
    String name ;
    public String answer;
    public String broadcast;



    public Request(String data[] ,String message,String name){
        this.name = name;
        String type = getRequestType(data);
        switch (type) {
            case "balance info":
                call = ()->{
                    String balance = JsonHandler.getUser(this.name,"balance");
                    answer ="ACK balance " + balance;
                    int i = 0;
                };

                break;

            case "balance add":
                call = ()->{
                    String balance = JsonHandler.getUser(this.name,"balance");
                    int newBalance = Integer.parseInt(balance) + Integer.parseInt(data[3]);
                    int oldBalance = Integer.parseInt(balance);
                    balance = Integer.toString(newBalance);
                    JsonHandler.changeUser(this.name, "balance",balance);
                    answer =  "ACK balance "+newBalance +" added " + (newBalance -oldBalance);
                };
                break;

            case "info":
                call = ()-> {
                    if (hasMovieName(data, message)) {
                        String moviename = getMoiveName(message);
                        if(!JsonHandler.hasMovie(moviename)){
                            answer = "ERROR request info failed";
                            return;
                        }
                        String amountLeft = JsonHandler.getMovie(moviename, "availableAmount");
                        String price = JsonHandler.getMovie(moviename, "price");
                        ArrayList countries = JsonHandler.getCountry(moviename);
                        String temp = "ACK info " + "\"" + moviename + "\"" + " " + amountLeft + " " + price ;
                        for (Object word : countries) {
                            temp = temp + " " + "\"" + (String) word + "\"";
                        }
                        answer = temp;
                    } else {
                        String temp = "";
                        ArrayList<String> movies = JsonHandler.getAllMovies();
                        for(String word : movies){
                            temp = temp + " " + "\"" +word + "\"";
                        }
                        answer = temp;

                    }
                };
                break;

            case "rent":
                call = ()->{
                    String error = "ERROR request rent failed";
                    if(!hasMovieName(data,message)){//no moviename in message
                        answer = error;
                        return;
                    }

                    String moviename = getMoiveName(message);
                    if(!JsonHandler.hasMovie(moviename)){//no movie in json
                        answer = error;
                        return;
                    }

                    if(JsonHandler.userHasMovie(moviename,name)){//user already has that movie
                        answer = error;
                        return;
                    }

                    ArrayList<String> countries = JsonHandler.getCountry(moviename);
                    String country = JsonHandler.getUser(name,"country");
                    for(String item: countries){
                        if(item.compareTo(country)==0){//banned in user's country
                            answer = error;
                            return;
                        }
                    }

                    String moneyStr = JsonHandler.getUser(name,"balance");
                    String priceStr = JsonHandler.getMovie(moviename,"price");
                    int money = Integer.parseInt(moneyStr);
                    int price = Integer.parseInt(priceStr);
                    if(price>money){//user does not have enough money
                        answer = error;
                        return;
                    }

                    boolean gotMovie = JsonHandler.takeMovie(moviename);
                    if(!gotMovie){//not enough copies in store
                        answer = error;
                        return;
                    }
                    //if we got here then user can rent the movie
                    String id = JsonHandler.getMovie(moviename,"id");
                    JsonHandler.changeUser(name,"balance", Integer.toString(money - price));
                    JsonHandler.giveReturnMovie(name,moviename,id,true);//give user the movie
                    answer = "ACK rent " + "\"" + moviename + "\"" + " success";
                    String copies = JsonHandler.getMovie(moviename,"availableAmount");
                    broadcast = "BROADCAST movie " + "\"" + moviename + "\"" + " " + copies + " " + priceStr;

                };
                break;

            case "return":
                call = ()->{
                    String error = "ERROR request return failed";
                    if(!hasMovieName(data,message)){//no moviename in message
                        answer = error;
                        return;
                    }

                    String movieName = getMoiveName(message);
                    if(!JsonHandler.hasMovie(movieName)){//no movie in json
                        answer = error;
                        return;
                    }

                    if(!JsonHandler.userHasMovie(movieName,name)){//user dont have this movie
                        answer = error;
                        return;
                    }

                    //if we got here the movie exist and the user has the movie for sure
                    String id = JsonHandler.getMovie(movieName,"id");
                    JsonHandler.giveReturnMovie(name,movieName,id,false);//took the movie of the user
                    String amountStr = JsonHandler.getMovie(movieName,"availableAmount");
                    int amount = Integer.parseInt(amountStr);
                    amount++;
                    JsonHandler.changeMovie(movieName,"availableAmount",Integer.toString(amount));
                    answer = "ACK return " + "\"" + movieName + "\"" + " success";
                    String copies = JsonHandler.getMovie(movieName,"availableAmount");
                    String priceStr = JsonHandler.getMovie(movieName,"price");
                    broadcast = "BROADCAST movie " + "\"" + movieName + "\"" + " " + copies + " " + priceStr;
                };
                break;

            case "addmovie":
                call = ()-> {
                    String error = "ERROR request addmovie failed";
                    if (!isAdmin(name)) {//not admin
                        answer = error;
                        return;
                    }
                    ArrayList<String> expressions = getExpressions(message);
                    String movieName = expressions.get(0);
                    if(JsonHandler.hasMovie(movieName)){//movie already exist
                        answer = error;
                        return;
                    }
                    String temessage = new String(message);
                    for (String item:expressions) {//in case we have numbers in the movie name or even in some country name
                        temessage = temessage.replace(item,"");
                    }
                    ArrayList<String> numbers = getNumbers(temessage);
                    if(numbers.size() !=2){//bad message
                        answer = error;
                        return;
                    }
                    String amountStr = numbers.get(0);
                    String priceStr = numbers.get(1);
                    if(Integer.parseInt(amountStr) <=0 || Integer.parseInt(priceStr) <=0){//smaller then zero
                        answer = error;
                        return;
                    }
                    ArrayList<String> countries = new ArrayList<String>();
                    for(int i = 1; i<expressions.size();i++){
                        countries.add(new String(expressions.get(i)));
                    }
                    JsonHandler.addMovie(movieName,amountStr,priceStr,countries);
                    answer = "ACK addmovie " + "\"" +movieName + "\"" +" success";
                    String copies = JsonHandler.getMovie(movieName,"availableAmount");
                    broadcast = "BROADCAST movie " + "\"" + movieName + "\"" + " " + copies + " " + priceStr;
                };
                break;

            case "remmovie":
                call=()-> {
                    String error = "ERROR request remmovie failed";
                    if (!isAdmin(name)) {//not admin
                        answer = error;
                        return;
                    }

                    String movieName = getMoiveName(message);
                    if (!JsonHandler.hasMovie(movieName)) {//movie is not in the system
                        answer = error;
                        return;
                    }

                    boolean removedMovie = JsonHandler.removeMovie(movieName);
                    if (!removedMovie) {//sombody had a copy of that movie
                        answer = error;
                        return;
                    }
                    answer = "ACK remmovie " + "\"" + movieName + "\"" + " success";
                    broadcast = "BROADCAST movie " + "\"" + movieName + "\"" + " removed" ;
                };
                break;

            case "changeprice":
                call = ()->{
                    String error = "ERROR request changeprice failed";
                    if (!isAdmin(name)) {//not admin
                        answer = error;
                        return;
                    }

                    String movieName = getMoiveName(message);
                    if (!JsonHandler.hasMovie(movieName)) {//movie is not in the system
                        answer = error;
                        return;
                    }

                    String tempString = message.replace(movieName,"");
                    ArrayList<String> numbers = getNumbers(tempString);

                    if(numbers.size() !=1){//bad input
                        answer = error;
                        return;
                    }

                    int price = Integer.parseInt(numbers.get(0));
                    if(price <=0){//bad price
                        answer = error;
                        return;
                    }

                    JsonHandler.changeMovie(movieName,"price", numbers.get(0));
                    answer = "ACK changeprice " + "\"" +movieName + "\"" + " success";
                    String copies = JsonHandler.getMovie(movieName,"availableAmount");
                    String priceStr = JsonHandler.getMovie(movieName,"price");
                    broadcast = "BROADCAST movie " + "\"" + movieName + "\"" + " " + copies + " " + priceStr;
                };
                break;

            case ""://error
                System.out.println("something went wrong the request was not leagel");
                break;

        }

    }

    public void run(){
        call.run();
    }

    public static boolean isAdmin(String username){
        String type = JsonHandler.getUser(username,"type");
        return (type.compareTo("admin") == 0);
    }



    public static String getRequestType(String[] data){
        String ans = "";
        if(data.length < 2)
            return ans;
        else if(data[1].compareTo("balance") == 0){//balance assume user name is one word
            if(data[2].compareTo("info") == 0 && data.length == 3)
                ans =  data[1] +" "+data[2];//balance info
            else if( data.length == 4 && data[2].compareTo("add")== 0  && data[3].matches("\\d+"))
                ans =  data[1]+" " + data[2];
        }
        else if (data[1].compareTo("info") == 0 ){
            ans =  data[1];
        }
        else if(data[1].compareTo("rent") == 0){
            ans = data[1];
        }
        else if(data[1].compareTo("return") == 0){
            ans = data[1];
        }
        else if (data.length >= 5  && data[1].compareTo("addmovie") == 0){//add movie(admin)
            ans = data[1];
        }
        else if (data[1].compareTo("remmovie") == 0 ){//remmovie (admin)
            ans = data[1];
        }
        else if(data.length >= 4 && data[1].compareTo("changeprice") == 0){//changeprice (admin)
            if(data[data.length - 1].matches("[\\+-]?\\d+"))//make sure i have a price
                ans = data[1];
        }
        return ans;
    }

    public static boolean hasMovieName(String [] data,String message){
        boolean ans = false;
        Pattern MY_PATTERN = Pattern.compile("\"(.*?)\"");
        Matcher m = MY_PATTERN.matcher(message);
        ans = m.find();
        return ans;
    }

    public static String getMoiveName(String message){
        String ans = null;
        Pattern MY_PATTERN = Pattern.compile("\"(.*?)\"");
        Matcher m = MY_PATTERN.matcher(message);
        while (m.find()) {
            ans = m.group(1);
        }
        return ans;
    }

    public static ArrayList<String> getExpressions(String message){
        ArrayList<String> ans = new ArrayList<String>();
        Pattern MY_PATTERN = Pattern.compile("\"(.*?)\"");
        Matcher m = MY_PATTERN.matcher(message);
        while (m.find()) {
            ans.add (m.group(1));
        }
        return ans;
    }

    public static ArrayList<String> getNumbers(String message){
        ArrayList<String> ans = new ArrayList<String>();
        Pattern p = Pattern.compile("-?\\d+");
        Matcher m = p.matcher(message);
        while (m.find()) {
            ans.add(m.group());
        }
        return ans;
    }
}