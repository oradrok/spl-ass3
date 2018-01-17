package bgu.spl181.net.api.bidi;


public interface BidiMessagingProtocol<T>  {

    void start(int connectionId, Connections<T> connections);

    void process(T message);

    /**
     * @return true if the connection should be terminated
     */
    boolean shouldTerminate();
}