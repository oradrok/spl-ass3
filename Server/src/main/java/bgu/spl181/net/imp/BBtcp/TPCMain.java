package bgu.spl181.net.impl.BBtpc;

import bgu.spl181.net.api.MessageEncoderDecoder;
import bgu.spl181.net.api.bidi.BidiMessageEncoderDecoder;
import bgu.spl181.net.api.bidi.BidiMessagingProtocol;
import bgu.spl181.net.api.bidi.BidiProtocol;
import bgu.spl181.net.srv.BaseServer;
import bgu.spl181.net.srv.BlockingConnectionHandler;
import bgu.spl181.net.srv.Server;

import java.io.IOException;
import java.util.function.Supplier;

public class TPCMain extends BaseServer<String>{


    public TPCMain(int port, Supplier<BidiMessagingProtocol<String>> protocolFactory, Supplier<MessageEncoderDecoder<String>> encdecFactory) {
        super(port, protocolFactory, encdecFactory);
    }

    public static void main (String[] args) throws IOException{

        if (args.length != 1) {
            System.err.println("Usage: java BidiServer <port number>");
            System.exit(1);
        }
        Supplier<BidiMessagingProtocol<String>> supplier1 = new Supplier<BidiMessagingProtocol<String>>() {
            @Override
            public BidiMessagingProtocol<String> get() {
                return new BidiProtocol();}
        };
        Supplier<MessageEncoderDecoder<String>> supplier2 = new Supplier<MessageEncoderDecoder<String>>() {
            @Override
            public MessageEncoderDecoder<String> get() {
                return new BidiMessageEncoderDecoder<String>();
            }
        };
        Server<String> server = Server.threadPerClient(Integer.parseInt(args[0]), supplier1, supplier2);
        server.serve();


    }

    @Override
    protected void execute(BlockingConnectionHandler<String> handler) {
        handler.run();
    }
}