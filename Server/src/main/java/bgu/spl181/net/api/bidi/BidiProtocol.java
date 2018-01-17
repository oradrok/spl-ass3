package bgu.spl181.net.api.bidi;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class BidiProtocol implements BidiMessagingProtocol<String> {
    private static ConcurrentHashMap<String, AtomicBoolean> logger  = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Integer> loggerId      = new ConcurrentHashMap<>();
    private boolean shouldTerminate = false;
    private String name;
    private boolean isLoggedIn;
    private int connectionId;
    private Connections connections;


    @Override
    public void start(int connectionId, Connections connections) {
        this.connectionId   = connectionId;
        this.connections    = connections;
        this.isLoggedIn     = false;
    }

    @Override
    public void process(String message) {
        String msg          = "";
        String dataholder   = "";

        if (message.length() >= 8 && message.substring(0, 8).equals("REGISTER")) {
            msg         = "REGISTER";
            dataholder  = message.substring(9);
        } else if (message.length() >= 5 && message.substring(0, 5).equals("LOGIN")) {
            msg         = "LOGIN";
            dataholder  = message.substring(6);
        } else if (message.length() >= 7  && message.substring(0, 7).equals("REQUEST")) {
            msg         = "REQUEST";
            dataholder  = message;
        } else if (message.length() >= 7 && message.substring(0, 7).equals("SIGNOUT")) {
            msg         = "SIGNOUT";
            dataholder  = message.substring(7);
        }

        switch (msg) {
            case "SIGNOUT":
                signout();
                break;

            case "REGISTER":
                register(dataholder);
                break;

            case "LOGIN":
                login(dataholder);
                break;

            case "REQUEST":
                request(dataholder);
                break;
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    private void signout(){
        if(isLoggedIn) {
            synchronized (logger){
                logger.get(name).set(false);
                logger.remove(name);
                connections.send(connectionId, "ACK signout succeeded");
                connections.disconnect(connectionId);//get him out of connections
            }
        }
        else connections.send(connectionId, "ERROR signout "+  "failed");
    }

    private void register(String dataholder){
            String[] data = dataholder.split(" ");
            boolean success;

            if (isLoggedIn || logger.get(data[0]) == null)//only read from logger so no problem in here
                success = MessageHandler.register(data);
            else success = false;

            if(success){
                connections.send(connectionId, "ACK registration succeeded");
            }
            else
                connections.send(connectionId, "ERROR registration failed");
    }

    private void login(String dataholder){
        String[] data = dataholder.split(" ");
        boolean success;

        synchronized (logger) {
            if (isLoggedIn || (logger.get(data[0]) != null && logger.get(data[0]).get()))
                success  = false;
            else success = MessageHandler.login(data);
            if (success) {
                if (logger.get(data[0]) != null)
                    logger.get(data[0]).set(true);
                else{
                    logger.put(data[0], new AtomicBoolean(true));
                    loggerId.put(data[0], connectionId);
                }

                this.name = data[0];
                this.isLoggedIn = true;
                connections.send(connectionId, "ACK login succeeded");
            } else
                connections.send(connectionId, "ERROR login failed");
        }
    }

    private void request(String dataholder){
        String[] data = dataholder.split(" ");
        
        if(!isLoggedIn){
            String error = new String ("ERROR request " + Request.getRequestType(data) + " failed");
            connections.send(connectionId, error);
        }
        else {
            Request r = MessageHandler.request(data, this.name, message);
            String answer = r.answer;
            String broadcast = r.broadcast;
            connections.send(connectionId,answer);


            if(broadcast != null){
                for (String k : logger.keySet()){
                    connections.send(loggerId.get(k), broadcast);
                }
            }
        }
    }

}