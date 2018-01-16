package bgu.spl181.net.srv;

import bgu.spl181.net.api.MessageEncoderDecoder;
import bgu.spl181.net.api.bidi.BidiMessagingProtocol;
import bgu.spl181.net.api.bidi.Connections;
import bgu.spl181.net.impl.protocol.ServerConnections;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public abstract class BaseServer<T> implements Server<T> {

    private final int port;
    private final Supplier<BidiMessagingProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
    private ServerSocket sock;
    private Connections connections;
    private AtomicInteger connectCount;

    public BaseServer(
            int port,
            Supplier<BidiMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory) {

        this.port               = port;
        this.protocolFactory    = protocolFactory;
        this.encdecFactory      = encdecFactory;
        this.sock               = null;
        this.connections        = new ServerConnections();
        this.connectCount       = new AtomicInteger(0);
    }

    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
            this.sock = serverSock; //just to be able to close
            System.out.println("Server started");

            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSock = serverSock.accept();
                BidiMessagingProtocol protocol = protocolFactory.get();
                BlockingConnectionHandler<T> handler = new BlockingConnectionHandler<T>(
                        clientSock,
                        encdecFactory.get(),
                        protocol);
                protocol.start(connectCount.getAndIncrement(),connections,handler);

                execute(handler);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        if (sock != null)
            sock.close();
    }

    protected abstract void execute(BlockingConnectionHandler<T>  handler);

}