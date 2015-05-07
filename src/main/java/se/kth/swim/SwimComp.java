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

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.swim.msg.Pong;
import se.kth.swim.msg.Status;
import se.kth.swim.msg.net.NetPing;
import se.kth.swim.msg.net.NetPong;
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
import se.sics.p2ptoolbox.util.network.NatedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 * @author Rizvi Hasan <mrhasan@kth.se>
 */
public class SwimComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(SwimComp.class);
    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);

    private final NatedAddress selfAddress;
    private final Set<NatedAddress> bootstrapNodes;
    private final NatedAddress aggregatorAddress;

    private UUID pingTimeoutId;
    private UUID statusTimeoutId;

    private int receivedPings = 0;    
    
    //-- Riz   
    private int sentPings = 0;
    private int receivedPongs = 0;
    
    private List<VicinityEntry> vicinityNodeList = new ArrayList<VicinityEntry>();
    private Deque<NatedAddress> joinedNodeList = new LinkedList<NatedAddress>();
    private Deque<NatedAddress> deletedNodeList = new LinkedList<NatedAddress>();
    private int JOIN_QUEUE_SIZE = 5;
    private int DELETE_QUEUE_SIZE = 5;
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
        subscribe(handlePong, network);
//        subscribe(handlePingReq, network);
//        subscribe(handlePongReq, network);
//        subscribe(handlePing2ndHand, network);
//        subscribe(handlePong2ndHand, network);        
        
        // --
    }

    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("{} starting with  ...", new Object[]{selfAddress.getId()});
			//for (NatedAddress pt : bootstrapNodes){
			//	log.info("Partner >> {} ", new Object[]{pt.getId()});
			//}            
            log.info("Parent >> [{}] ", new Object[]{selfAddress.getParents()});
            
            //	-- Riz [ List the partners for this node ].
            
            if (!bootstrapNodes.isEmpty()) {
            	
            	// Transfer the bootstrapNodes to the vicinityNodeList            	
            	for (NatedAddress partnerAddress : bootstrapNodes) {
            		//log.info("Partners from boot: {} ", partnerAddress.getId());
            		vicinityNodeList.add( new VicinityEntry(partnerAddress));
            	}      
            	
            	for (java.util.Iterator<VicinityEntry> iterator = vicinityNodeList.iterator(); iterator.hasNext();)
            	{
            		log.info("Partners from boot: {} ", ((VicinityEntry)iterator.next()).nodeAdress.getId());            		            		
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
            
            // PIGGY-BACK AND PONG:  Piggyback pong with Newly joined nodes 
            //log.info("{} replying pong to:{}", new Object[]{selfAddress.getId(), event.getHeader().getSource()});            
            Set<NatedAddress> _piggyBackedJoinedNodes = new HashSet<NatedAddress>();
            Set<NatedAddress> _piggyBackedDeadNodes = new HashSet<NatedAddress>();
            for (NatedAddress newNode : joinedNodeList) 
            	_piggyBackedJoinedNodes.add(newNode);      
            for (NatedAddress newNode : deletedNodeList) 
            	_piggyBackedDeadNodes.add(newNode);      
            trigger(new NetPong(selfAddress, event.getHeader().getSource(),new Pong(_piggyBackedJoinedNodes,_piggyBackedDeadNodes )), network);
            
            // ADD PINGER: If the pinger is not in the vicinity-list - add him in the vicinityNodeList and joinedNodeList
            // Reason behind adding the pinger in the joinedNodeList after triggering the pong is:
            //   The pinger itself doesn't need to be piggy-backed back to itself.  
            if (!vicinityNodeList.contains(event.getHeader().getSource())){
            	AddUniqueToVicinity(event.getHeader().getSource());
                AddUniqueToJoinedList(event.getHeader().getSource());
            }                        
            // --
        }

    };
    
    // Handling Pong
    private Handler<NetPong> handlePong = new Handler<NetPong>() {

        @Override
        public void handle(NetPong event) {        	
        	
            log.info("{} received pong from:{} with {} " + ProcessSet(event.getContent().getPiggyBackedJoinedNodes()) + " new and {} " + ProcessSet(event.getContent().getPiggyBackedDeadNodes()) +  "dead nodes.", new Object[]{selfAddress.getId(), event.getHeader().getSource(),event.getContent().getPiggyBackedJoinedNodes().size(),event.getContent().getPiggyBackedDeadNodes().size()});
            receivedPongs++;
            
            //CHANGE STATUS: Change the status of the node in vicinity-list as NOT waitingForPong  
            for (VicinityEntry _vNode: vicinityNodeList){
            	if (_vNode.nodeAdress == event.getSource()){            	
            		_vNode.waitingForPong = false;            		
            	    _vNode.waitingForPongCount = 0;
            	    if (_vNode.nodeStatus == "SUSPECTED"){
            	    	_vNode.nodeStatus = "LIVE";
            	    }
            	}            	
            }
            
            // MARGE PIGGY-BACK: If the piggy-backed new nodes are not in the vicinityNodeList - add them 
            if (event.getContent().getPiggyBackedJoinedNodes().size() > 0){
            	for(NatedAddress newPiggyBackedNode : event.getContent().getPiggyBackedJoinedNodes()){
            		if (newPiggyBackedNode != selfAddress ){
            			AddUniqueToVicinity(newPiggyBackedNode);
                		AddUniqueToJoinedList(newPiggyBackedNode);	
            		}            		            		
            	}
            }
            // MARGE PIGGY-BACK: If the piggy-backed dead node are in the vicinityNodeList -  remove them
            if (event.getContent().getPiggyBackedDeadNodes().size() > 0){
            	for(NatedAddress deadPiggyBackedNode : event.getContent().getPiggyBackedDeadNodes()){
            		if (deadPiggyBackedNode != selfAddress ){
            			RemoveFromVicinityList(deadPiggyBackedNode);
            			AddUniqueToDeadList(deadPiggyBackedNode);            			                			
            		}            		            		
            	}
            }
            
        }
    };


    private Handler<PingTimeout> handlePingTimeout = new Handler<PingTimeout>() {

        @Override
        public void handle(PingTimeout event) {            
            // -- Riz
        	// FILTER: Filter out the waiting-for-pong nodes
        	// SUSPEND: Suspend nodes if too long waiting counter
        	Set<NatedAddress> _pingCandidates = new HashSet<NatedAddress>(); 
        	VicinityEntry _justSuspected = null;
        	VicinityEntry _justDead = null;
            for (VicinityEntry vNode : vicinityNodeList){
            	if (vNode.waitingForPong == true){
            		//if (vNode.nodeStatus == "LIVE"){
            			log.info("{} missed pong from {} by {} times", new Object[]{selfAddress.getId(),vNode.nodeAdress , vNode.waitingForPongCount });
            			vNode.waitingForPongCount ++;
            			
            		//}            			
            		// Suspecting in less than 2 cycle-time or (2*1000) millisecond
            		if (vNode.nodeStatus == "LIVE" && vNode.waitingForPongCount >= 2 ){            			
            			log.info("{} detected no response from {} ", new Object[]{selfAddress.getId(), vNode.nodeAdress });
            			vNode.nodeStatus = "SUSPECTED";
            			_justSuspected = vNode;
            			
            		}  
            		// Dead declare in less than 3 cycle-time or (3*1000) millisecond
            		if (vNode.nodeStatus == "SUSPECTED" && vNode.waitingForPongCount >= 4 ){
            			log.info("{} detected dead node {} ", new Object[]{selfAddress.getId(), vNode.nodeAdress });
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
    			// Remove from vicinity.
    			RemoveFromVicinityList(_justDead.nodeAdress);
    			// Add in dead-list to be piggy-backed.
    			AddUniqueToDeadList(_justDead.nodeAdress);
            }
            
            
            
    		// PING-REQ: Trigger a Ping-Req for this suspected node to random K nodes in vicinity list
//            if (_justSuspected != null){
//            	for (VicinityEntry vNode : vicinityNodeList){
//            		if (vNode != _justSuspected && vNode.nodeStatus == "LIVE"){
//            			// select random k nodes ...
//            			// ..
//            			trigger(new NetPingReq(selfAddress, vNode.nodeAdress, new PingReq(_justSuspected.nodeAdress)), network);            			
//            		}            			
//            	}
//            }
        	
        	
        	// PING: ping the ping candidates 
        	if (!_pingCandidates.isEmpty() )
        	{
        		log.info("{} has ping cnadidates {} ", new Object[]{selfAddress.getId(),_pingCandidates.size()  });
        		// If the set with single element, ping that
        		if (_pingCandidates.size() == 1 ){
        			// Ping the node
        			goPingTheNode(_pingCandidates.iterator().next());
        		}
        		// Pick a random element to ping
        		else{
        			int size = _pingCandidates.size();
        			int item = new Random().nextInt(size); // In real life, the Random object should be rather more shared than this
        			log.info("{} has ping cnadidates {}, going to ping {}th ", new Object[]{selfAddress.getId(),size,item  });
        			int i = 0;        			
        			for(NatedAddress pingPartner : _pingCandidates)
        			{
        			    if (i == item){        			    	
        			    	// Ping the node
        			    	goPingTheNode(pingPartner);
        			    	break;
        			    }   
        			    i = i + 1;
        			}
        		}        		
        	}
            //--
            
            
        }

    };


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

        public SwimInit(NatedAddress selfAddress, Set<NatedAddress> bootstrapNodes, NatedAddress aggregatorAddress) {
            this.selfAddress = selfAddress;
            this.bootstrapNodes = bootstrapNodes;
            this.aggregatorAddress = aggregatorAddress;
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
    
    public String ProcessSet(Set<NatedAddress> vList)
    {
    	String st = " ";    	
    	for(NatedAddress nd : vList){
    		
    		st += nd.getId() + " ";
    	}
    	st = "[" + st + "]"; 
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
    	trigger(new NetPing(selfAddress, pingPartner), network);
    	sentPings++;
    	
    	// Mark the node as waiting for ping in vicinity list
      for (VicinityEntry partner : vicinityNodeList){
      	if (partner.nodeAdress == pingPartner)
      		partner.waitingForPong = true;
      }	
    }
    

    // --
}
