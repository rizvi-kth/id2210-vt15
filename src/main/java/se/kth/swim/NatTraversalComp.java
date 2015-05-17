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
package se.kth.swim;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.swim.croupier.CroupierConfig;
import se.kth.swim.croupier.CroupierPort;
import se.kth.swim.croupier.msg.CroupierSample;
import se.kth.swim.croupier.util.Container;
import se.kth.swim.msg.PingReq;
import se.kth.swim.msg.net.NetHeartbeatPing;
import se.kth.swim.msg.net.NetHeartbeatPong;
import se.kth.swim.msg.net.NetMsg;
import se.kth.swim.msg.net.NetPing;
import se.kth.swim.msg.net.NetPingReq;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Header;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.util.network.NatType;
import se.sics.p2ptoolbox.util.network.NatedAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicNatedAddress;
import se.sics.p2ptoolbox.util.network.impl.RelayHeader;
import se.sics.p2ptoolbox.util.network.impl.SourceHeader;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class NatTraversalComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(NatTraversalComp.class);
    private Negative<Network> local = provides(Network.class);
    private Positive<Network> network = requires(Network.class);
    private Positive<CroupierPort> croupier = requires(CroupierPort.class);
    // -- Riz
    private Positive<Timer> timer = requires(Timer.class);
    private UUID heartbeatPingTimeoutId;
    private Negative<NatNotifyPort> NatNotify = provides(NatNotifyPort.class);
    private static InetAddress localHost;
    private NatedAddress selfAddress;
    private Set<ParentEntry> myParents = new HashSet<ParentEntry>();
    static {
        try {
            localHost = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
    }
    // --
    
    private final Random rand;

    public NatTraversalComp(NatTraversalInit init) {
        this.selfAddress = init.selfAddress;
        log.info("{} {} initiating...", new Object[]{selfAddress.getId(), (selfAddress.isOpen() ? "OPEN" : "NATED")});

        this.rand = new Random(init.seed);
        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleIncomingMsg, network);
        subscribe(handleOutgoingMsg, local);
        subscribe(handleCroupierSample, croupier);
        // -- Riz
        subscribe(handleHeartBeatPingTimeout, timer);
        subscribe(handleHeartbeatPing, network);
        subscribe(handleHeartbeatPong, network);
        
        // --
    }

    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("{} starting...", new Object[]{selfAddress.getId()});
            for(NatedAddress _node : selfAddress.getParents()){
            	myParents.add(new ParentEntry(_node));
            }
            log.info("{} starting... with patents {}", new Object[]{selfAddress.getId(),selfAddress.getParents()});
            
            schedulePeriodicPing();
        }

    };
    private Handler<Stop> handleStop = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", new Object[]{selfAddress.getId()});
            if (heartbeatPingTimeoutId != null) {
                cancelPeriodicPing();
            }
        }

    };

    private Handler<NetMsg<Object>> handleIncomingMsg = new Handler<NetMsg<Object>>() {

        @Override
        public void handle(NetMsg<Object> msg) {
            log.trace("{} received msg:{}", new Object[]{selfAddress.getId(), msg});
            Header<NatedAddress> header = msg.getHeader();
            if (header instanceof SourceHeader) {
                if (!selfAddress.isOpen()) {
                    throw new RuntimeException("source header msg received on nated node - nat traversal logic error");
                }
                SourceHeader<NatedAddress> sourceHeader = (SourceHeader<NatedAddress>) header;
                if (sourceHeader.getActualDestination().getParents().contains(selfAddress)) {
                    log.info("{} relaying message for:{}", new Object[]{selfAddress.getId(), sourceHeader.getSource()});
                    RelayHeader<NatedAddress> relayHeader = sourceHeader.getRelayHeader();
                    trigger(msg.copyMessage(relayHeader), network);
                    return;
                } else {
                    log.warn("{} received weird relay message:{} - dropping it", new Object[]{selfAddress.getId(), msg});
                    return;
                }
            } else if (header instanceof RelayHeader) {
                if (selfAddress.isOpen()) {
                    throw new RuntimeException("relay header msg received on open node - nat traversal logic error");
                }
                RelayHeader<NatedAddress> relayHeader = (RelayHeader<NatedAddress>) header;
                log.info("{} delivering relayed message:{} from:{}", new Object[]{selfAddress.getId(), msg, relayHeader.getActualSource()});
                Header<NatedAddress> originalHeader = relayHeader.getActualHeader();
                trigger(msg.copyMessage(originalHeader), local);
                return;
            } else {
                log.info("{} delivering direct message:{} from:{}", new Object[]{selfAddress.getId(), msg, header.getSource()});
                trigger(msg, local);
                return;
            }
        }

    };

    private Handler<NetMsg<Object>> handleOutgoingMsg = new Handler<NetMsg<Object>>() {

        @Override
        public void handle(NetMsg<Object> msg) {
            log.trace("{} sending msg:{}", new Object[]{selfAddress.getId(), msg});
            Header<NatedAddress> header = msg.getHeader();
            if(header.getDestination().isOpen()) {
                log.info("{} sending direct message:{} to:{}", new Object[]{selfAddress.getId(), msg, header.getDestination()});
                trigger(msg, network);
                return;
            } else {
                if(header.getDestination().getParents().isEmpty()) {
                    throw new RuntimeException("nated node with no parents");
                }
                NatedAddress parent = randomNode(header.getDestination().getParents());
                SourceHeader<NatedAddress> sourceHeader = new SourceHeader(header, parent);
                log.info("{} sending message:{} to relay:{}", new Object[]{selfAddress.getId(), msg, parent});
                trigger(msg.copyMessage(sourceHeader), network);
                return;
            }
        }

    };
    
    private Handler handleCroupierSample = new Handler<CroupierSample>() {
        @Override
        public void handle(CroupierSample event) {
            //log.info("{} croupier public nodes:{}", selfAddress.getBaseAdr(), event.publicSample);
            //use this to change parent in case it died
            // -- Riz
            if (!selfAddress.isOpen()){
            	log.info("{} croupier public nodes:{}", selfAddress.getBaseAdr(), event.publicSample);
            	// CHECK CURRENT PARENTS: Check the status of the current parents
            	NatedAddress _deadParent = null;
            	for (ParentEntry _p : myParents){
            		if(_p.waitingForPong && _p.waitingForPongCount > 2){
            			log.info("{} detected dead parent :{}", selfAddress.getId(), _p.nodeAdress);
            			_deadParent = _p.nodeAdress;
            		}            			
            	}
            	
	            // PARENTS-FROM-CROUPIER: Get the parents form the Croupier
	            Set<NatedAddress> _parents = new HashSet<NatedAddress>();
	            Iterator _i = event.publicSample.iterator();
	            while(_i.hasNext()){
	            	Container<NatedAddress, Object> _a = (Container<NatedAddress, Object>) _i.next();
	            	if (_a.getSource() != _deadParent){
	            		log.info("Possible parents {} ", _a.getSource());
		            	_parents.add(_a.getSource());	
	            	}
	            	
	            }            
	            // NEW-ADDRESS: Create new address with new parents  
	            NatedAddress _nodeAddress = new BasicNatedAddress(new BasicAddress(localHost, 12345, selfAddress.getId()), NatType.NAT, _parents);
	            
	            trigger(new se.kth.swim.msg.NatNotify(_nodeAddress), NatNotify);
            }
            // --
            
        }
    };
    
    
    
    // -- Riz

    private Handler<NetHeartbeatPong> handleHeartbeatPong = new Handler<NetHeartbeatPong>() {

        @Override
        public void handle(NetHeartbeatPong event) {
            log.info("{} received heartbeat back from:{}", new Object[]{selfAddress.getId(), event.getHeader().getSource()});
            // UPDATE PARENT STATUS: Change the parent status as alive
            NatedAddress _liveParent = event.getHeader().getSource();
            for (ParentEntry _p : myParents){
            	if (_p.nodeAdress == _liveParent){
					_p.waitingForPong = false;
					_p.waitingForPongCount=0;
					
            	}
			}
            
        }
    };

    
    
    private Handler<NetHeartbeatPing> handleHeartbeatPing = new Handler<NetHeartbeatPing>() {

        @Override
        public void handle(NetHeartbeatPing event) {        	
            log.info("{} received heartbeat from:{}", new Object[]{selfAddress.getId(), event.getHeader().getSource()});
            // HEART-BEAT-BACK HATED: Heart-beat ping the parents
            trigger(new NetHeartbeatPong(selfAddress, event.getHeader().getSource()), network);
        }
    };
    
    private Handler<HeartbeatPingTimeout> handleHeartBeatPingTimeout = new Handler<HeartbeatPingTimeout>() {

        @Override
        public void handle(HeartbeatPingTimeout event) {            
        	// HEART-BEAT PARENT: Heart-beat ping the parents  
   			if ((!selfAddress.isOpen()) && (!myParents.isEmpty())){
   				for (ParentEntry _p : myParents){
   					log.info("{} heartbeat to parent {} ", new Object[]{selfAddress.getId(),_p.nodeAdress});
   					trigger(new NetHeartbeatPing(selfAddress, _p.nodeAdress), network);
   					_p.waitingForPong = true;
   					_p.waitingForPongCount++;
   				}
   			}            
        }

    };
    

    
    private static class HeartbeatPingTimeout extends Timeout {

        public HeartbeatPingTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }
    }
    
    private void schedulePeriodicPing() {
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(1000, 1000);
        HeartbeatPingTimeout sc = new HeartbeatPingTimeout(spt);
        spt.setTimeoutEvent(sc);
        heartbeatPingTimeoutId = sc.getTimeoutId();
        trigger(spt, timer);
    }

    private void cancelPeriodicPing() {
        CancelTimeout cpt = new CancelTimeout(heartbeatPingTimeoutId );
        trigger(cpt, timer);
        heartbeatPingTimeoutId  = null;
    }
    // --
    private NatedAddress randomNode(Set<NatedAddress> nodes) {
        int index = rand.nextInt(nodes.size());
        Iterator<NatedAddress> it = nodes.iterator();
        while(index > 0) {
            it.next();
            index--;
        }
        return it.next();
    }

    public static class NatTraversalInit extends Init<NatTraversalComp> {

        public final NatedAddress selfAddress;
        public final long seed;

        public NatTraversalInit(NatedAddress selfAddress, long seed) {
            this.selfAddress = selfAddress;
            this.seed = seed;
        }
    }
}
