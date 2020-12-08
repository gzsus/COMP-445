package UDP;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Packet {

    //  Packet length
    public static final int MIN_SIZE = 11;
    public static final int MAX_SIZE = 1024;

    //  Class attributes
    private int type;
    private long sequence;
    private InetAddress dest_address;
    private int dest_port;
    private byte[] payload;

    //  Constructor
    public Packet(int typ, long seq, InetAddress addr, int prt, byte[] pld) {
        this.type = typ;
        this.sequence = seq;
        this.dest_address = addr;
        this.dest_port = prt;
        this.payload = pld;
    }

    //  Accessors and Mutators
    public int get_type() {
        return type;
    }
    public long get_sequenceNumber() {
        return sequence;
    }
    public InetAddress get_address() {
        return dest_address;
    }
    public int get_port() {
        return dest_port;
    }
    public byte[] get_payload() { return payload; }
    public void set_type(int typ) {
        type = typ;
    }
    public void set_sequenceNumber(long seq) {
        sequence = seq;
    }
    public void set_address(InetAddress addr) {
        dest_address = addr;
    }
    public void set_port(int prt) {
        dest_port = prt;
    }
    public void set_payload(byte[] pld) {
        payload = pld;
    }

    //  Convert packet to a Buffer
    public ByteBuffer to_buffer() {
        ByteBuffer buf = ByteBuffer.allocate(MAX_SIZE).order(ByteOrder.BIG_ENDIAN);

        buf.put((byte) type);
        buf.putInt((int) sequence);
        buf.put(dest_address.getAddress());
        buf.putShort((short) dest_port);
        buf.put(payload);

        buf.flip();
        return buf;
    }

    public static Packet from_buffer(ByteBuffer buf) throws IOException {
        if (buf.limit() < MIN_SIZE || buf.limit() > MAX_SIZE) {
            throw new IOException("Invalid length");
        }

        int typ = Byte.toUnsignedInt(buf.get());
        long seq = Integer.toUnsignedLong(buf.getInt());
        byte[] host = new byte[]{buf.get(), buf.get(), buf.get(), buf.get()};
        InetAddress addr = InetAddress.getByAddress(host);
        int peer_port = Short.toUnsignedInt(buf.getShort());
        byte[] payld = new byte[buf.remaining()];
        ByteBuffer payload = buf.get(payld);


        Packet packet = new Packet(typ, seq,addr,peer_port,payld);

        return packet;
    }


    @Override
    public String toString() {
        return String.format("#%d peer=%s:%d, size=%d\t%s", sequence, dest_address, dest_port, payload.length, new String (payload) );
    }

}
