/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.kth.swim.msg.net;

import se.kth.swim.msg.Ping2ndHand;
import se.sics.kompics.network.Header;
import se.sics.p2ptoolbox.util.network.NatedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class NetPing2ndHand extends NetMsg<Ping2ndHand> {

    public NetPing2ndHand(NatedAddress src, NatedAddress dst) {
        super(src, dst, new Ping2ndHand());
    }

    private NetPing2ndHand(Header<NatedAddress> header, Ping2ndHand content) {
        super(header, content);
    }
    
    // -- Riz
    public NetPing2ndHand(NatedAddress src, NatedAddress dst, Ping2ndHand content) {
        super(src, dst, content);
    }	
    // --
    

    @Override
    public NetMsg copyMessage(Header<NatedAddress> newHeader) {
        return new NetPing2ndHand(newHeader, getContent());
    }

}
