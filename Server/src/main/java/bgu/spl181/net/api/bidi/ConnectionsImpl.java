package bgu.spl181.net.api.bidi;

import bgu.spl181.net.srv.ConnectionHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl<T> implements Connections<T> {
    public Map<Integer, ConnectionHandler<T>> ClientList;

    public ConnectionsImpl (){
        ClientList = new ConcurrentHashMap<>();
    }

    @Override
    public boolean send(int connectionId, T msg) {
        if (!ClientList.containsKey(connectionId))
            return false;
        ClientList.get(connectionId).send(msg);
        return true;
    }

    @Override
    public void broadcast(T msg) {
        for (ConnectionHandler c : ClientList.values()){
            c.send(msg);
        }
    }


    @Override
    public void disconnect(int connectionId) {
        ClientList.remove(connectionId);
    }

    public void addClient (int id, ConnectionHandler<T> handler){
        ClientList.put(id, handler);
    }
}