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
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.swim.croupier.CroupierPort;
import se.kth.swim.msg.Ping;
import se.kth.swim.msg.Ping2ndHand;
import se.kth.swim.msg.PingReq;
import se.kth.swim.msg.Pong;
import se.kth.swim.msg.NatNotify;
import se.kth.swim.msg.Pong2ndHand;
import se.kth.swim.msg.PongReq;
import se.kth.swim.msg.Status;
import se.kth.swim.msg.net.NetPing;
import se.kth.swim.msg.net.NetPing2ndHand;
import se.kth.swim.msg.net.NetPingReq;
import se.kth.swim.msg.net.NetPong;
import se.kth.swim.msg.net.NetPong2ndHand;
import se.kth.swim.msg.net.NetPongReq;
import se.kth.swim.msg.net.NetStatus;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.util.network.NatType;
import se.sics.p2ptoolbox.util.network.NatedAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicNatedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 * @author Rizvi Hasan <mrhasan@kth.se>
 */
public class SwimComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(SwimComp.class);
    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);
    // -- Riz
    private Positive<NatNotifyPort> NatNotify = requires(NatNotifyPort.class);
    // --
    
    //private final NatedAddress selfAddress;
    private NatedAddress selfAddress;
    private final Set<NatedAddress> bootstrapNodes;
    private final NatedAddress aggregatorAddress;

    private UUID pingTimeoutId;
    private UUID statusTimeoutId;

    private int receivedPings = 0;    
    
    //-- Riz   
    private static InetAddress localHost;
    private final Random rand;
    
    private int sentPings = 0;
    private int receivedPongs = 0;
    
    private int incurnationNumber = 0;
    
    private List<VicinityEntry> vicinityNodeList = new ArrayList<VicinityEntry>();
    private Deque<NatedAddress> joinedNodeList = new LinkedList<NatedAddress>();
    private Deque<NatedAddress> deletedNodeList = new LinkedList<NatedAddress>();
    private Set<PiggybackEntry> suspectedNodeList = new HashSet<PiggybackEntry>();
    
    
    private int JOIN_QUEUE_SIZE = 5;
    private int DELETE_QUEUE_SIZE = 5;
    private int PING_REQ_RANDOM_K = 2;    
    //--    

    public SwimComp(SwimInit init) {
        this.selfAddress = init.selfAddress;
        log.info("{} initiating...", selfAddress);
        this.bootstrapNodes = init.bootstrapNodes;
        this.aggregatorAddress = init.aggregatorAddress;
        
        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handlePing, network);
        subscribe(handlePingTimeout, timer);
        subscribe(handleStatusTimeout, timer);
        // -- Rizvi
        this.rand = new Random(init.seed);
        subscribe(handleNatNotify, NatNotify);
        subscribe(handlePong, network);
        subscribe(handlePingReq, network);
        subscribe(handlePongReq, network);
        subscribe(handlePing2ndHand, network);
        subscribe(handlePong2ndHand, network);        
        
        // --
    }
    
    
    // -- Riz

    private Handler<NatNotify> handleNatNotify = new Handler<NatNotify>() {

        @Override
        public void handle(NatNotify event) {
//            log.info("Old Parents {} :: ... ::: ... :: New Parents {}", new Object[]{selfAddress.getParents(), event.getChangedNatAddress().getParents() });            
        }

    };

    
    // --

    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("{} starting with  ...", new Object[]{selfAddress.getId()});
			//for (NatedAddress pt : bootstrapNodes){
			//	log.info("Partner >> {} ", new Object[]{pt.getId()});
			//}            
            log.info("{}'s Parent >> [{}] ", new Object[]{selfAddress.getId(),selfAddress.getParents()});
            
            //	-- Riz [ List the partners for this node ].
            
            if (!bootstrapNodes.isEmpty()) {
            	
            	// Transfer the bootstrapNodes to the vicinityNodeList            	
            	for (NatedAddress partnerAddress : bootstrapNodes) {
            		//log.info("Partners from boot: {} ", partnerAddress.getId());
            		vicinityNodeList.add( new VicinityEntry(partnerAddress));
            	}      
            	
            	for (java.util.Iterator<VicinityEntry> iterator = vicinityNodeList.iterator(); iterator.hasNext();)
            	{
            		log.info("{}'s partners from boot: {} ", selfAddress.getId(), ((VicinityEntry)iterator.next()).nodeAdress.getId());            		            		
            	}
            	schedulePeriodicPing();
            }                        
            schedulePeriodicStatus();            
            // --
            
        }

    };
    private Handler<Stop> handleStop = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", new Object[]{selfAddress.getId()});
            if (pingTimeoutId != null) {
                cancelPeriodicPing();
            }
            if (statusTimeoutId != null) {
                cancelPeriodicStatus();
            }
        }

    };

    private Handler<NetPing> handlePing = new Handler<NetPing>() {

        @Override
        public void handle(NetPing event) {
            //log.info("{} received ping from:{}", new Object[]{selfAddress.getId(), event.getHeader().getSource()});
            // -- Riz
            //log.info("{} received ping from:{} ", new Object[]{selfAddress.getId(), event.getHeader().getSource()});
            receivedPings++;
            
            // ADD PINGER: If the pinger is not in the vicinity-list - add him in the vicinityNodeList and joinedNodeList              
            if (!vicinityNodeList.contains(event.getHeader().getSource())){
            	AddUniqueToVicinity(event.getHeader().getSource());
                AddUniqueToJoinedList(event.getHeader().getSource());
            }
            
            // MARGE PIGGY-BACK
            MargeAllPiggyBacks(	event.getContent().getPiggyBackedSuspectedNodes(), 
            					event.getContent().getPiggyBackedJoinedNodes(),
            					event.getContent().getPiggyBackedDeadNodes());
            
            // UPDATE-VIEW: Update the vicinity node info according to the piggyback node info
            UpdateVicinityWithPiggybacks();
            
            /*             
            // MARGE PIGGY-BACK(SUSPECTED): If the piggy-backed suspected-nodes are merged with its suspected-nodes
            if (event.getContent().getPiggyBackedSuspectedNodes().size() > 0){
            	for(PiggybackEntry suspectedPiggyNode : event.getContent().getPiggyBackedSuspectedNodes()){
            		MargeUniqueToSuspectedList(suspectedPiggyNode);
            	}
            }
            
            // MARGE PIGGY-BACK(JOINED): If the piggy-backed new nodes are not in the vicinityNodeList - add them 
            if (event.getContent().getPiggyBackedJoinedNodes().size() > 0){
            	for(NatedAddress newPiggyBackedNode : event.getContent().getPiggyBackedJoinedNodes()){
            		if (newPiggyBackedNode != selfAddress ){
            			AddUniqueToVicinity(newPiggyBackedNode);
                		AddUniqueToJoinedList(newPiggyBackedNode);	
            		}            		            		
            	}
            }
            
            // MARGE PIGGY-BACK(DEAD): If the piggy-backed dead node are in the vicinityNodeList -  remove them
            if (event.getContent().getPiggyBackedDeadNodes().size() > 0){
            	for(NatedAddress deadPiggyBackedNode : event.getContent().getPiggyBackedDeadNodes()){
            		if (deadPiggyBackedNode != selfAddress ){
            			RemoveFromVicinityList(deadPiggyBackedNode);
            			AddUniqueToDeadList(deadPiggyBackedNode);
            			RemoveFromsuspectedNodeList(deadPiggyBackedNode);
            		}            		            		
            	}
            }
            */
            
            
            
            /*
            // UPDATE-VIEW: Update the vicinity node info according to the piggyback node info 
            for (PiggybackEntry _pn : suspectedNodeList){
            	for (VicinityEntry _vn : vicinityNodeList){
            		if(_pn.nodeAdress == _vn.nodeAdress && _vn.nodeStatus != "DEAD"){
            			if (_vn.nodeStatus != _pn.nodeStatus){
            				_vn.nodeStatus = _pn.nodeStatus;
	            			if(_pn.nodeStatus == "LIVE"){	            				
	            				_vn.waitingForPong = false;
	                			_vn.waitingForPongCount = 0;
	            			}else if(_pn.nodeStatus == "SUSPECTED"){	            				
	            				_vn.waitingForPong = true;                			
	            			}
            			}
            		}
            	}
            	
            }
            */
            
            trigger(new NetPong(selfAddress, event.getHeader().getSource(),new Pong(joinedNodeList, deletedNodeList, suspectedNodeList)), network);
            
                                    
            // --
        }

    };
    
    // Handling Pong
    private Handler<NetPong> handlePong = new Handler<NetPong>() {

        @Override
        public void handle(NetPong event) {        	
        	
            log.info("{} received pong from:{} with {} " 
            + ProcessSet(event.getContent().getPiggyBackedJoinedNodes()) + " new and {} " 
            + ProcessSet(event.getContent().getPiggyBackedDeadNodes()) 
            +  "dead nodes and suspect [ " 
            + ProcessPiggyEntitySet(event.getContent().getPiggyBackedSuspectedNodes()) + " ].", new Object[]{selfAddress.getId(), event.getHeader().getSource(),event.getContent().getPiggyBackedJoinedNodes().size(),event.getContent().getPiggyBackedDeadNodes().size()});
            receivedPongs++;
            
            //CHANGE STATUS: Change the status of the Ponger-node in vicinity-list as NOT waitingForPong  
            for (VicinityEntry _vNode: vicinityNodeList){
            	if (_vNode.nodeAdress == event.getSource()){            	
            		_vNode.waitingForPong = false;            		
            	    _vNode.waitingForPongCount = 0;
            	    if (_vNode.nodeStatus == "SUSPECTED"){            	    	
            	    	_vNode.nodeStatus = "LIVE";
            	    }
            	}            	
            }
            
            // MARGE PIGGY-BACK
            MargeAllPiggyBacks(	event.getContent().getPiggyBackedSuspectedNodes(), 
					event.getContent().getPiggyBackedJoinedNodes(),
					event.getContent().getPiggyBackedDeadNodes());

            // UPDATE-VIEW: Update the vicinity node info according to the piggyback node info
            UpdateVicinityWithPiggybacks();
            
            /*
            // MARGE PIGGY-BACK(SUSPECTED): If the piggy-backed suspected-nodes are merged with its suspected-nodes
            if (event.getContent().getPiggyBackedSuspectedNodes().size() > 0){
            	for(PiggybackEntry suspectedPiggyNode : event.getContent().getPiggyBackedSuspectedNodes()){
            		MargeUniqueToSuspectedList(suspectedPiggyNode);
            	}
            }
            
            // MARGE PIGGY-BACK(JOINED): If the piggy-backed new nodes are not in the vicinityNodeList - add them 
            if (event.getContent().getPiggyBackedJoinedNodes().size() > 0){
            	for(NatedAddress newPiggyBackedNode : event.getContent().getPiggyBackedJoinedNodes()){
            		if (newPiggyBackedNode != selfAddress ){
            			AddUniqueToVicinity(newPiggyBackedNode);
                		AddUniqueToJoinedList(newPiggyBackedNode);	
            		}            		            		
            	}
            }
            
            // MARGE PIGGY-BACK(DEAD): If the piggy-backed dead node are in the vicinityNodeList -  remove them
            if (event.getContent().getPiggyBackedDeadNodes().size() > 0){
            	for(NatedAddress deadPiggyBackedNode : event.getContent().getPiggyBackedDeadNodes()){
            		if (deadPiggyBackedNode != selfAddress ){
            			RemoveFromVicinityList(deadPiggyBackedNode);
            			AddUniqueToDeadList(deadPiggyBackedNode);
            			RemoveFromsuspectedNodeList(deadPiggyBackedNode);
            		}            		            		
            	}
            }
            
            // UPDATE-VIEW: Update the vicinity node info according to the piggyback node info 
            for (PiggybackEntry _pn : suspectedNodeList){
            	for (VicinityEntry _vn : vicinityNodeList){
            		if(_pn.nodeAdress == _vn.nodeAdress && _vn.nodeStatus != "DEAD"){
            			if (_vn.nodeStatus != _pn.nodeStatus){
            				_vn.nodeStatus = _pn.nodeStatus;
	            			if(_pn.nodeStatus == "LIVE"){	            				
	            				_vn.waitingForPong = false;
	                			_vn.waitingForPongCount = 0;
	            			}else if(_pn.nodeStatus == "SUSPECTED"){	            				
	            				_vn.waitingForPong = true;                			
	            			}
            			}
            		}
            	}            	
            }
            */
            
            
        }
    };


    private Handler<PingTimeout> handlePingTimeout = new Handler<PingTimeout>() {

        @Override
        public void handle(PingTimeout event) {            
            // -- Riz        	
        	
        	// FILTER: Filter out the waiting-for-pong nodes
        	// SUSPEND: Suspend nodes if too long waiting counter
        	// DEAD: Declare a node dead when too long suspended 
        	Set<NatedAddress> _pingCandidates = new HashSet<NatedAddress>(); 
        	Set<VicinityEntry> _justSuspectedList = new HashSet<VicinityEntry>();
        	VicinityEntry _justDead = null;
            for (VicinityEntry vNode : vicinityNodeList){
            	if (vNode.waitingForPong == true){
            		//if (vNode.nodeStatus == "LIVE"){
            			log.info("{} missed pong from {} by {} times", new Object[]{selfAddress.getId(),vNode.nodeAdress , vNode.waitingForPongCount });
            			vNode.waitingForPongCount ++;
            			
            		//}            			
            		// Suspecting in less than 2 cycle-time or (2*1000) millisecond
            		if (vNode.nodeStatus == "LIVE" && vNode.waitingForPongCount >= 2 ){            			
            			log.info("{} detected no response from {} and SUSPECTING... ", new Object[]{selfAddress.getId(), vNode.nodeAdress });
            			// Add to suspected list for dissamination
            			AddUniqueToSuspectedList(vNode);
            			vNode.nodeStatus = "SUSPECTED";
            			_justSuspectedList.add(vNode);
            			
            		}  
            		// Dead declare in less than 4 cycle-time or (4*1000) millisecond
            		if (vNode.nodeStatus == "SUSPECTED" && vNode.waitingForPongCount >= 30 ){
            			log.info("{} detected DEAD node {} ", new Object[]{selfAddress.getId(), vNode.nodeAdress });
            			vNode.nodeStatus = "DEAD";  
            			_justDead = vNode;
            		}            		
            	}
            	else{
            		_pingCandidates.add(vNode.nodeAdress);
            	}            		
            }
            //REMOVE-DEAD: Remove the DEAD
            if (_justDead != null){
            	// Add in dead-list to be piggy-backed.
    			AddUniqueToDeadList(_justDead.nodeAdress);
    			// Remove from vicinity.
    			RemoveFromVicinityList(_justDead.nodeAdress);    			
    			// Remove from joined-list
    			joinedNodeList.remove(_justDead.nodeAdress);
    			// Remove from suspected list
    			RemoveFromsuspectedNodeList(_justDead.nodeAdress);
    			
            }
            
    		// PING-REQ: Trigger a Ping-Req for this suspected node to random K nodes in vicinity list            
            for(VicinityEntry _justSuspected: _justSuspectedList){
	                        	
	            	// Piggyback the ping
	            	Ping _parasitePing = new Ping(joinedNodeList, deletedNodeList, suspectedNodeList);
	            	
	            	// Get all the live nodes
					Set<NatedAddress> _liveList = new HashSet<NatedAddress>();    				
					for (VicinityEntry vNode : vicinityNodeList){
						if (vNode != _justSuspected && vNode.nodeStatus == "LIVE"){
							_liveList.add(vNode.nodeAdress);
						}
					}
					log.info("{} going to ping-req among {} ", new Object[]{selfAddress.getId(), _liveList });
					
					
	    			if (PING_REQ_RANDOM_K > 0 && PING_REQ_RANDOM_K < _liveList.size()){
	    				// select random k nodes from vicinity list...
	    				for(int k=0 ; k < PING_REQ_RANDOM_K; k++ ){
	    					NatedAddress _randLive =  randomNode(_liveList);
	    					log.info("{} ping-req to random  {} ", new Object[]{selfAddress.getId(), _randLive });    					 
	    					trigger(new NetPingReq(selfAddress, _randLive, new PingReq(_justSuspected.nodeAdress, _parasitePing)), network);
	    					_liveList.remove(_randLive);
	    				}
	    			}    				
	    			else{
	    				// Send ping-req to all live nodes.
		            	for (NatedAddress _live : _liveList){
		            		log.info("{} ping-req to {} ", new Object[]{selfAddress.getId(), _live });
		            		trigger(new NetPingReq(selfAddress, _live, new PingReq(_justSuspected.nodeAdress,_parasitePing)), network);         			
		            	}
	    			}
        	
            }
        	
        	
        	// PING: ping the ping candidates 
        	if (!_pingCandidates.isEmpty() )
        	{
        		log.info("{} has ping candidates {} ", new Object[]{selfAddress.getId(),_pingCandidates.size()  });
        		// If the set with single element, ping that
        		if (_pingCandidates.size() == 1 ){
        			// Ping the node
        			goPingTheNode(_pingCandidates.iterator().next());
        		}
        		// Pick a random element to ping
        		else{
        			goPingTheNode(randomNode(_pingCandidates));
        		}        		
        	}
            //--
            
            
        }

    };
    
    // -- Riz
    
    // Handling PingReq
    private Handler<NetPingReq> handlePingReq = new Handler<NetPingReq>() {

        @Override
        public void handle(NetPingReq event) {      	
        	
        	log.info("{} received ping-req from:{} for {} ", new Object[]{selfAddress.getId(), event.getHeader().getSource(),event.getContent().GetTestSubjectNode()});        	
        	trigger(new NetPing2ndHand(selfAddress, event.getContent().GetTestSubjectNode(), new Ping2ndHand(event.getHeader().getSource(),event.getContent().getParasitePing())), network);
        	
        	
        }

    };
    
    // Handle Pong-Req
    private Handler<NetPongReq> handlePongReq = new Handler<NetPongReq>() {

        @Override
        public void handle(NetPongReq event) {      	
        	
        	log.info("{} received pong-req from:{} for {} ", new Object[]{selfAddress.getId(), event.getHeader().getSource(),event.getContent().GetTestSubjectNode()});        	
        	NatedAddress _suspectedNode = event.getContent().GetTestSubjectNode();
        	// check in the vicinity list if _suspectedNode is SUSPECTED then make it LIVE. 
//        	for (VicinityEntry partner : vicinityNodeList){
//        		if (partner.nodeAdress == _suspectedNode && partner.nodeStatus == "SUSPECTED"){
//        			partner.waitingForPong = false;
//        			partner.waitingForPongCount = 0;
//        			partner.nodeStatus = "LIVE";
//        		}
//        	}
        	
        	// MERGE PIGGY-BACKS
            MargeAllPiggyBacks(	event.getContent().getParasitePong().getPiggyBackedSuspectedNodes(), 
            					event.getContent().getParasitePong().getPiggyBackedJoinedNodes(),
            					event.getContent().getParasitePong().getPiggyBackedDeadNodes());
            
            // UPDATE-VIEW: Update the vicinity node info according to the piggyback node info
            UpdateVicinityWithPiggybacks();
        	
        	
        }

    };
    
    
    // Handling Ping2ndHand
    private Handler<NetPing2ndHand> handlePing2ndHand = new Handler<NetPing2ndHand>() {

        @Override
        public void handle(NetPing2ndHand event) {
        	
            log.info("{} received 2nd-hand-ping from:{} with caller {} ", new Object[]{selfAddress.getId(), event.getHeader().getSource(),event.getContent().GetTestRequesterNode()});
            // ADD PINGER: If the pinger is not in the vicinity-list - add him in the vicinityNodeList and joinedNodeList              
            if (!vicinityNodeList.contains(event.getHeader().getSource())){
            	AddUniqueToVicinity(event.getHeader().getSource());
                AddUniqueToJoinedList(event.getHeader().getSource());
            }
            
            // MERGE PIGGY-BACKS
            MargeAllPiggyBacks(	event.getContent().getParasitePing().getPiggyBackedSuspectedNodes(), 
            					event.getContent().getParasitePing().getPiggyBackedJoinedNodes(),
            					event.getContent().getParasitePing().getPiggyBackedDeadNodes());
            
            // UPDATE-VIEW: Update the vicinity node info according to the piggyback node info
            UpdateVicinityWithPiggybacks();
            
            Pong _parasitePong = new Pong(joinedNodeList, deletedNodeList, suspectedNodeList); 
            trigger(new NetPong2ndHand(selfAddress, event.getHeader().getSource(), new Pong2ndHand(event.getContent().GetTestRequesterNode(), _parasitePong )) ,network);            
        }
    };

    
    // Handling Pong2ndHand
    private Handler<NetPong2ndHand> handlePong2ndHand = new Handler<NetPong2ndHand>() {

        @Override
        public void handle(NetPong2ndHand event) {
        	
            log.info("{} received 2nd-hand-pong from:{} with caller {} ", new Object[]{selfAddress.getId(), event.getHeader().getSource(),event.getContent().GetTestRequesterNode()});
            trigger(new NetPongReq(selfAddress, event.getContent().GetTestRequesterNode(), new PongReq(event.getHeader().getSource(),event.getContent().getParasitePong())) ,network);            
        }
    };

    
    
    // --


    // Timer Status
    private Handler<StatusTimeout> handleStatusTimeout = new Handler<StatusTimeout>() {

        @Override
        public void handle(StatusTimeout event) {
            //log.info("{} sending status to aggregator:{}", new Object[]{selfAddress.getId(), aggregatorAddress});
            trigger(new NetStatus(selfAddress, aggregatorAddress, new Status(sentPings,receivedPings, receivedPongs, vicinityNodeList)), network);
        }
    };

    

    private void schedulePeriodicPing() {
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(1000, 1000);
        PingTimeout sc = new PingTimeout(spt);
        spt.setTimeoutEvent(sc);
        pingTimeoutId = sc.getTimeoutId();
        trigger(spt, timer);
    }
	
	
	private void cancelPeriodicPing() {
        CancelTimeout cpt = new CancelTimeout(pingTimeoutId);
        trigger(cpt, timer);
        pingTimeoutId = null;
    }

    private void schedulePeriodicStatus() {
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(10000, 10000);
        StatusTimeout sc = new StatusTimeout(spt);
        spt.setTimeoutEvent(sc);
        statusTimeoutId = sc.getTimeoutId();
        trigger(spt, timer);
    }

    private void cancelPeriodicStatus() {
        CancelTimeout cpt = new CancelTimeout(statusTimeoutId);
        trigger(cpt, timer);
        statusTimeoutId = null;
    }

    public static class SwimInit extends Init<SwimComp> {

        public final NatedAddress selfAddress;
        public final Set<NatedAddress> bootstrapNodes;
        public final NatedAddress aggregatorAddress;
        // -- Riz
        public final long seed;
        // --
        public SwimInit(NatedAddress selfAddress, Set<NatedAddress> bootstrapNodes, NatedAddress aggregatorAddress, long seed) {
            this.selfAddress = selfAddress;
            this.bootstrapNodes = bootstrapNodes;
            this.aggregatorAddress = aggregatorAddress;
            this.seed = seed;
        }
    }

    private static class StatusTimeout extends Timeout {

        public StatusTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }
    }

    private static class PingTimeout extends Timeout {

        public PingTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }
    }
    
    // -- Rizvi 
    
    // Add to Join-List
    protected void AddUniqueToJoinedList(NatedAddress node) {
		if (joinedNodeList.size() > 0){
    		if (!joinedNodeList .contains(node)){
    			joinedNodeList .addFirst(node);
    			if(joinedNodeList .size()> DELETE_QUEUE_SIZE)
    				joinedNodeList.pollLast();
    		}   			    		
    	}else{
    		joinedNodeList.add(node);
    	}
	}
    
 // Add to Dead-List
    protected void AddUniqueToDeadList(NatedAddress node) {
		if (deletedNodeList .size() > 0){
    		if (!deletedNodeList .contains(node)){
    			deletedNodeList .addFirst(node);
    			if(deletedNodeList .size()> JOIN_QUEUE_SIZE)
    				deletedNodeList .pollLast();
    		}   			    		
    	}else{
    		deletedNodeList .add(node);
    	}
	}
    
    // Add to VicinityList 
    protected void AddUniqueToVicinity(NatedAddress node) {

    	if (vicinityNodeList.size() > 0){
    		boolean _contains = false;
    		for(VicinityEntry entry : vicinityNodeList){
    			if (entry.nodeAdress.equals(node))
    				_contains = true;
    		}
    		if (!_contains )
    			vicinityNodeList.add(new VicinityEntry(node));
    		
   			    		
    	}else{
    		vicinityNodeList.add(new VicinityEntry(node));
    	}
		
	}
    
    // Add to local suspected list from the vicinity-list(local-view)
    protected void AddUniqueToSuspectedList(VicinityEntry vNode) {
    	PiggybackEntry _tempEntry = null;
    	for(PiggybackEntry _n : suspectedNodeList){
			if (_n.nodeAdress == vNode.nodeAdress){
				_tempEntry = _n; 
			}				
		}
    	
    	if (_tempEntry == null){
    		suspectedNodeList.add(new PiggybackEntry(vNode.nodeAdress,"SUSPECTED"));
    	}
    	else{    		 
    		_tempEntry.nodeStatus = "SUSPECTED";
    	}		
	}
    
    // Add to local suspected list from piggyback-suspected-list
    protected void MargeUniqueToSuspectedList(PiggybackEntry suspectedPiggyNode) {
    	if (suspectedPiggyNode.nodeAdress == selfAddress && suspectedPiggyNode.nodeStatus == "SUSPECTED" && suspectedPiggyNode.incCount == this.incurnationNumber){
    		this.incurnationNumber ++;
    		suspectedPiggyNode.incCount = this.incurnationNumber;
    		suspectedPiggyNode.nodeStatus = "LIVE";
    		suspectedNodeList.add(suspectedPiggyNode);
    	}
    	else{    		
	    	// Add in the suspected list based on highest incarnation-number
	    	PiggybackEntry _tempEntry = null;
	    	for(PiggybackEntry _n : suspectedNodeList){
				if (_n.nodeAdress == suspectedPiggyNode.nodeAdress){
					_tempEntry = _n; 
				}				
			}
	    	
	    	if (_tempEntry == null){
	    		suspectedNodeList.add(suspectedPiggyNode);
	    	}
	    	else{    		
	    		if ( suspectedPiggyNode.incCount > _tempEntry.incCount){
	    			_tempEntry.nodeStatus = suspectedPiggyNode.nodeStatus;
	    			_tempEntry.incCount = suspectedPiggyNode.incCount;
	    		}    		
	    	}
    	}
	}

    // Remove from VicinityList
    protected void RemoveFromVicinityList(NatedAddress deletedNode) {
    	if (vicinityNodeList.size() > 0){
    		VicinityEntry _temp = null;    		
    		for(VicinityEntry entry : vicinityNodeList){
    			if (entry.nodeAdress.equals(deletedNode)){
    				_temp = entry;    				
    			}    			
    		}
    		if (_temp != null){
    			vicinityNodeList.remove(_temp);
    		}    		
    	}		
	}
    
    // Remove form Suspected List
    protected void RemoveFromsuspectedNodeList(NatedAddress deletedNode) {
    	if (suspectedNodeList.size() > 0){
    		PiggybackEntry _temp = null;    		
    		for(PiggybackEntry entry : suspectedNodeList){
    			if (entry.nodeAdress.equals(deletedNode)){
    				_temp = entry;    				
    			}    			
    		}
    		if (_temp != null){
    			suspectedNodeList.remove(_temp);
    		}    		
    	}
		
	}
    
    protected void MargeAllPiggyBacks(
			Set<PiggybackEntry> piggyBackedSuspectedNodes,
			Deque<NatedAddress> piggyBackedJoinedNodes,
			Deque<NatedAddress> piggyBackedDeadNodes) {
		
    	
    	// MARGE PIGGY-BACK(SUSPECTED): If the piggy-backed suspected-nodes are merged with its suspected-nodes
        if (piggyBackedSuspectedNodes.size() > 0){
        	for(PiggybackEntry suspectedPiggyNode : piggyBackedSuspectedNodes){
        		MargeUniqueToSuspectedList(suspectedPiggyNode);
        	}
        }
        
        // MARGE PIGGY-BACK(JOINED): If the piggy-backed new nodes are not in the vicinityNodeList - add them 
        if (piggyBackedJoinedNodes.size() > 0){
        	for(NatedAddress newPiggyBackedNode : piggyBackedJoinedNodes){
        		if (newPiggyBackedNode != selfAddress ){
        			AddUniqueToVicinity(newPiggyBackedNode);
            		AddUniqueToJoinedList(newPiggyBackedNode);	
        		}            		            		
        	}
        }
        
        // MARGE PIGGY-BACK(DEAD): If the piggy-backed dead node are in the vicinityNodeList -  remove them
        if (piggyBackedDeadNodes.size() > 0){
        	for(NatedAddress deadPiggyBackedNode : piggyBackedDeadNodes){
        		if (deadPiggyBackedNode != selfAddress ){
        			RemoveFromVicinityList(deadPiggyBackedNode);
        			AddUniqueToDeadList(deadPiggyBackedNode);
        			RemoveFromsuspectedNodeList(deadPiggyBackedNode);
        		}            		            		
        	}
        }		
	}

    protected void UpdateVicinityWithPiggybacks(){
    	
    	// UPDATE-VIEW: Update the vicinity node info according to the piggyback node info 
        for (PiggybackEntry _pn : suspectedNodeList){
        	for (VicinityEntry _vn : vicinityNodeList){
        		if(_pn.nodeAdress == _vn.nodeAdress && _vn.nodeStatus != "DEAD"){
        			if (_vn.nodeStatus != _pn.nodeStatus){
        				_vn.nodeStatus = _pn.nodeStatus;
            			if(_pn.nodeStatus == "LIVE"){	            				
            				_vn.waitingForPong = false;
                			_vn.waitingForPongCount = 0;
            			}else if(_pn.nodeStatus == "SUSPECTED"){	            				
            				_vn.waitingForPong = true;                			
            			}
        			}
        		}
        	}
        	
        }
    	
    	
    }
    
    
    public String ProcessSet(Deque<NatedAddress> vList)
    {
    	String st = " ";    	
    	for(NatedAddress nd : vList){
    		
    		st += nd.getId() + " ";
    	}
    	st = "[" + st + "]"; 
    	return st;
    	    	
    }
    
    public String ProcessPiggyEntitySet(Set<PiggybackEntry> piggyBackedSuspectedNodes) {
    	String st = "";    	
    	for(PiggybackEntry nd : piggyBackedSuspectedNodes){
    		String stat = "";
    		stat = " " + nd.nodeAdress.getId() + "-"+ nd.nodeStatus + "-" + nd.incCount;    		
    		st += stat;
    	}
    	return st;
	}

    
    public String ProcessVicinityList(List<VicinityEntry> vList)
    {
    	String st = " ";
    	
    	for(VicinityEntry nd : vList){
    		String stat = "";
    		if (nd.nodeStatus == "SUSPECTED"){
    			stat = "(S" + nd.waitingForPongCount + ")";
    		}
    		st += nd.nodeAdress.getId() + stat + " ";
    	}
    	return st;
    	    	
    }
    
    private void goPingTheNode(NatedAddress pingPartner) {
			log.info("{} sending ping to partner:{} with vicinity - " + ProcessVicinityList(vicinityNodeList), new Object[]{selfAddress.getId(), pingPartner.getId()});
	    	//log.info("{} sending ping to partner:{} ", new Object[]{selfAddress.getId(), pingPartner.getId()});
	    	trigger(new NetPing(selfAddress, pingPartner, new Ping(joinedNodeList, deletedNodeList, suspectedNodeList)), network);
	    	sentPings++;
    	
	    	// Mark the node as waiting for ping in vicinity list
		    for (VicinityEntry partner : vicinityNodeList){
		      	if (partner.nodeAdress == pingPartner)
		      		partner.waitingForPong = true;
      }	
    }
    
    private NatedAddress randomNode(Set<NatedAddress> nodes) {
        int index = rand.nextInt(nodes.size());
        Iterator<NatedAddress> it = nodes.iterator();
        while(index > 0) {
            it.next();
            index--;
        }
        return it.next();
    }
    

    // --
}
