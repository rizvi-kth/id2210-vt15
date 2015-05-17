package se.kth.swim.msg.net;

import se.kth.swim.msg.Pong;
import se.sics.kompics.network.Header;
import se.sics.p2ptoolbox.util.network.NatedAddress;

/**
 * @author Md. Rizvi Hasan <mrhasan@kth.se>
 */

public class NetPong extends NetMsg<Pong> {

	public NetPong(NatedAddress src, NatedAddress dst) {
        super(src, dst, new Pong());
    }

    private NetPong(Header<NatedAddress> header, Pong content) {
        super(header, content);
    }
    
    
	public NetPong(NatedAddress src, NatedAddress dst, Pong content) {
		super(src, dst, content);
	}	
    

    @Override
    public NetMsg copyMessage(Header<NatedAddress> newHeader) {
        return new NetPong(newHeader, getContent());
    }

}
