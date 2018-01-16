package bgu.spl181.net.srv;

import bgu.spl181.net.api.MessageEncoderDecoder;
import bgu.spl181.net.api.bidi.BidiMessagingProtocol;
import bgu.spl181.net.api.bidi.Connections;
import bgu.spl181.net.impl.protocol.ServerConnections;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class Reactor<T> implements Server<T> {

    private final int port;
    private final Supplier<BidiMessagingProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> readerFactory;
    private final ActorThreadPool pool;
    private Selector selector;

    private Thread selectorThread;
    private final ConcurrentLinkedQueue<Runnable> selectorTasks = new ConcurrentLinkedQueue<>();
    private Connections<T> connections;
    private AtomicInteger counter = new AtomicInteger(0);

    public Reactor(
            int numThreads,
            int port,
            Supplier<BidiMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> readerFactory) {

        this.pool               = new ActorThreadPool(numThreads);
        this.port               = port;
        this.protocolFactory    = protocolFactory;
        this.readerFactory      = readerFactory;
        connections             = new ServerConnections<>();
    }

    @Override
    public void serve() {
        selectorThread = Thread.currentThread();

        try (Selector selector = Selector.open();
             ServerSocketChannel serverSock = ServerSocketChannel.open()) {

            this.selector = selector; //just to be able to close

            serverSock.bind(new InetSocketAddress(port));
            serverSock.configureBlocking(false);
            serverSock.register(selector, SelectionKey.OP_ACCEPT);//registering the ssc in the selector for ACCEPT event
            System.out.println("Server started");

            while (!Thread.currentThread().isInterrupted()) {

                selector.select();//wait until something happened
                runSelectionThreadTasks();//?????

                for (SelectionKey key : selector.selectedKeys()) {//

                    if (!key.isValid()) {//making sure socket is still valid and not deleted or something
                        continue;
                    } else if (key.isAcceptable()) {//if the event is ACCEPT
                        handleAccept(serverSock, selector);
                    } else {//if the event is READ or WRITE
                        handleReadWrite(key);
                    }
                }

                selector.selectedKeys().clear(); //clear the selected keys set so that we can know about new events
                //selected keys is only a copy and not the real one

            }

        } catch (ClosedSelectorException ex) {
            //do nothing - server was requested to be closed
        } catch (IOException ex) {
            //this is an error
            ex.printStackTrace();
        }

        System.out.println("server closed!!!");
        pool.shutdown();
    }

    /*package*/ void updateInterestedOps(SocketChannel chan, int ops) {//changing the status of READ to WRITE etc..
        final SelectionKey key = chan.keyFor(selector);
        if (Thread.currentThread() == selectorThread) {
            key.interestOps(ops);
        } else {
            selectorTasks.add(() -> {
                if(key!=null&&key.isValid())
                    key.interestOps(ops);
            });
            selector.wakeup();
        }
    }


    /**
     * @param serverChan
     * @param selector
     * @throws IOException
     * handled by selectorThread one a new client was registered to server
     */
    private void handleAccept(ServerSocketChannel serverChan, Selector selector) throws IOException {
        SocketChannel clientChan = serverChan.accept();//configuring channel
        clientChan.configureBlocking(false);
        int connectionId = counter.getAndIncrement();
        BidiMessagingProtocol<T> protocol = protocolFactory.get();

        final NonBlockingConnectionHandler<T> handler = new NonBlockingConnectionHandler<>(//defining new connection handler for the client
                readerFactory.get(),
                protocol,
                clientChan,
                this);//needs the reactor to change READ to WRIte etc..
        protocol.start(connectionId,connections,handler);
        clientChan.register(selector, SelectionKey.OP_READ, handler);//registering the clientChannel with READ
    }

    private void handleReadWrite(SelectionKey key) {
        NonBlockingConnectionHandler handler = (NonBlockingConnectionHandler) key.attachment();//attachment is what we decided to attach
        if (key.isReadable()) {
            Runnable task = handler.continueRead();//returns a runnable task which we can now give the executor
            if (task != null) {
                pool.submit(handler, task);//delivering the executable came back from reading back to the pool
            }
        }

        if (key.isValid() && key.isWritable()) {
            handler.continueWrite();
        }
    }

    private void runSelectionThreadTasks() {
        while (!selectorTasks.isEmpty()) {
            selectorTasks.remove().run();
        }
    }

    @Override
    public void close() throws IOException {
        selector.close();
    }

}