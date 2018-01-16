package bgu.spl181.net.srv;

import bgu.spl181.net.api.MessageEncoderDecoder;
import bgu.spl181.net.api.bidi.BidiMessagingProtocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NonBlockingConnectionHandler<T> implements ConnectionHandler<T> {

    private static final int BUFFER_ALLOCATION_SIZE = 1 << 13; //8k
    private static final ConcurrentLinkedQueue<ByteBuffer> BUFFER_POOL = new ConcurrentLinkedQueue<>();

    private final BidiMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Queue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<>();//so called out
    private final SocketChannel channel;
    private final Reactor reactor;

    public NonBlockingConnectionHandler(
            MessageEncoderDecoder reader,
            BidiMessagingProtocol protocol,
            SocketChannel channel,
            Reactor reactor) {

        this.channel    = channel;
        this.encdec     = reader;
        this.protocol   = protocol;
        this.reactor    = reactor;
    }

    public Runnable continueRead() {//runs by the SelectorThread- makes input into a task
        ByteBuffer buffer  = leaseBuffer();
        boolean success = false;

        try {
            success = channel.read(buffer) != -1;
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (success) {
            buffer.flip();
            return () -> {//lambda to give the executor
                try {
                    while (buffer.hasRemaining()) {
                        T nextMessage = encdec.decodeNextByte(buffer.get());
                        if (nextMessage != null) {
                            protocol.process(nextMessage);//getting the answer to the message
                        }
                    }
                } finally {
                    releaseBuffer(buffer);
                }
            };
        } else {
            releaseBuffer(buffer);
            close();
            return null;
        }

    }

    public void close() {
        System.out.println("client disconnected");
        try {
            channel.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isClosed() {
        return !channel.isOpen();
    }

    public void continueWrite() {//committed by the SelectorThread
        while (!writeQueue.isEmpty()&!isClosed()) {
            try {
                ByteBuffer top = writeQueue.peek();
                channel.write(top);//deletes bytes from the buffer and write them to WriteQueue
                if (top.hasRemaining()) {//if we couldn't finish write the whole message
                    return;//thread will go to sleep until next time
                } else {//if we emptied the buffer
                    writeQueue.remove();//removes buffer itself from queue
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                close();
            }
        }

        if (writeQueue.isEmpty()) {//assuming we finished writing all what was in WriteQueue
            if (protocol.shouldTerminate()) close();
            else reactor.updateInterestedOps(channel, SelectionKey.OP_READ);//updating selector to notify only when READ happens
        }
    }

    private static ByteBuffer leaseBuffer() {
        ByteBuffer buff = BUFFER_POOL.poll();
        if (buff == null) {
            return ByteBuffer.allocateDirect(BUFFER_ALLOCATION_SIZE);
        }
        buff.clear();
        return buff;
    }

    private static void releaseBuffer(ByteBuffer buff) {
        BUFFER_POOL.add(buff);
    }

    @Override
    public void send(T msg) {// assuming msg not null
        writeQueue.add(ByteBuffer.wrap(encdec.encode(msg)));//adding the out message
        reactor.updateInterestedOps(channel, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

    }
}