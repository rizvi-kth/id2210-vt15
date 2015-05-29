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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.swim.msg.net.NetStatus;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.util.network.NatedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class AggregatorComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(AggregatorComp.class);
    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);
    // --Riz
    private List<Integer> convergedNodes = new ArrayList<Integer>();
    private List<Entry> convergedNodes2 = new ArrayList<Entry>();
    private Map<Integer,ArrayList> failureMap = new HashMap(); 
    
    // --

    private final NatedAddress selfAddress;

    public AggregatorComp(AggregatorInit init) {
        this.selfAddress = init.selfAddress;
        log.info("{} initiating...", new Object[]{selfAddress.getId()});

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleStatus, network);
    }

    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("{} starting...", new Object[]{selfAddress});
        }

    };
    private Handler<Stop> handleStop = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", new Object[]{selfAddress});
        }

    };

    private Handler<NetStatus> handleStatus = new Handler<NetStatus>() {

        @Override
        public void handle(NetStatus status) {
        	// -- Riz
        	
        	
        	// #### TAST-3-PHASE-2 - START : For scenario SwimScenarioP2T3_2 ************************
        	int DELETE_PIGGYBACK_QUEUE_FOR_SWIM =5;   // Set it the value you set DELETE_QUEUE_SIZE in swim component.
        	int FAILED_NODES_COUNT = 10;			  	// Set it tne value you set in SwimScenarioP2T3_2 for dead nodes count.
        	
        	if (FAILED_NODES_COUNT > DELETE_PIGGYBACK_QUEUE_FOR_SWIM){
	        	boolean _printFlag = false;
	        	if(status.getContent().deletedNodeList.size()>=DELETE_PIGGYBACK_QUEUE_FOR_SWIM)
	            {
	        		Entry _temp = null;
	        		for (Entry _e : convergedNodes2){
	        			if (_e.id == status.getHeader().getSource().getId()){
	        				_temp = _e;
	        				if (_e.deads.contains(status.getContent().deletedNodeList.getFirst().getId())){
	        					
	        				}
	        				else{
	        					_e.deads.add(status.getContent().deletedNodeList.getFirst().getId());
	        					if (_e.deads.size() >= FAILED_NODES_COUNT)
	        						_printFlag = true;        					
	        				}
	        			}        				
	        		}
	        		if (_temp == null){
	        			Entry _ee = new Entry(); 
	        			_ee.id = status.getHeader().getSource().getId();
	        			_ee.deads.add(status.getContent().deletedNodeList.getFirst().getId());
	        			convergedNodes2.add(_ee);
	        			_temp = _ee;
	
	        		}
	        		
	        		
	        		if (_printFlag == true)
	                 	log.info("Status from:{} " 
	                 							+ " deadQ" + ProcessSet(status.getContent().deletedNodeList) +
	                 							", deads-detected {}{} "
	                 							+ "  joined" + ProcessSet(status.getContent().joinedNodeList) +  "  suspect [ " 
	            							    + ProcessPiggyEntitySet(status.getContent().suspectedNodeList) + " ] Parents"
	            							    + ProcessParents(status.getHeader().getSource().getParents()) + " newNats"
	            							    + ProcessSet(status.getContent().newNATList), 
	                        new Object[]{status.getHeader().getSource(),            					 
	                 								_temp.deads.size(),_temp.deads}
	          				);
	            	
	            }
        	
        	}
        	else{
        		if(status.getContent().deletedNodeList.size()>=FAILED_NODES_COUNT)
            	{            	        
    	        	if(!convergedNodes.contains(status.getHeader().getSource().getId()))
    	        	{ 
    	        		convergedNodes.add(status.getHeader().getSource().getId());
    	        		log.info("Status from:{} - Vicinity of {}, dead" 
								+ ProcessSet(status.getContent().deletedNodeList) + "  joined" 
							    + ProcessSet(status.getContent().joinedNodeList) +  "  suspect [ " 
							    + ProcessPiggyEntitySet(status.getContent().suspectedNodeList) + " ] Parents"
							    + ProcessParents(status.getHeader().getSource().getParents()) + " newNats"
							    + ProcessSet(status.getContent().newNATList), 
							    new Object[]{status.getHeader().getSource(),            					 
									status.getContent().vicinityNodeList.size()}
				);
    	        		
    	        	}
            	}
        		
        	}
        	// #### TAST-3-PHASE-2 - END	**************************
        	
        	         
        	        
        	/*
        	//###  Filtering for Phase 1 Task 6*****************************
        	// Turn-on this logic to filter log information to observe convergence of failure detection for Phase 1 Task 6
        	
        	
        	int FAILED_NODES_COUNT = 10;			  	// Set it tne value you set in SwimScenarioP2T3_2 for dead nodes count.
        	
			if (status.getContent().deletedNodeList.size() >= FAILED_NODES_COUNT) 

			{
				if (!convergedNodes.contains(status.getHeader().getSource()
						.getId()))

				{
					convergedNodes.add(status.getHeader().getSource().getId());

					log.info(
							"Status from:{} - Vicinity of {} nodes:["
									+ ProcessViciniTyList(status.getContent().vicinityNodeList)
									+ "] dead"
									+ ProcessSet(status.getContent().deletedNodeList)
									+ "  joined"
									+ ProcessSet(status.getContent().joinedNodeList)
									+ "  suspect [ "
									+ ProcessPiggyEntitySet(status.getContent().suspectedNodeList)
									+ " ] newNats"
									+ ProcessSet(status.getContent().newNATList),
							new Object[] { status.getHeader().getSource(),

							status.getContent().vicinityNodeList.size() });
				}

			}
        			
        	//###  End of Filtering for Phase 1 Task 6*******************************   
        	*/
			
			
			
			
			
			/*
        	//### Filtering for Phase 1 Task 7**************************
            // Turn-on this logic block to filter log information for Phase 1 Task 7
        	// Turn-off the rest 
        	
        	int NETWORK_SIZE = 100;
        	int NUMBER_OF_FAILED_NODE = 10;
        	int _rest = NETWORK_SIZE - NUMBER_OF_FAILED_NODE;
        	        
        	for(NatedAddress _d: status.getContent().deletedNodeList){
        	if(failureMap.containsKey(_d.getId()))
        	{
        	if(!failureMap.get(_d.getId()).contains(status.getHeader().getSource().getId()))
        	{
        	(failureMap.get(_d.getId())).add(status.getHeader().getSource().getId());
        	}
        	}
        	else
        	{
        	failureMap.put(_d.getId(), new ArrayList<Integer>());
        	(failureMap.get(_d.getId())).add(status.getHeader().getSource().getId());
        	}
        	}
        	         
        	        Set<Integer> allKeys = failureMap.keySet();
        	         
        	        Iterator iterator = allKeys.iterator();
        	        while(iterator.hasNext())
        	        {
        	          Integer element = (Integer) iterator.next();
        	          if(!convergedNodes.contains(element.intValue()))
        	          {
        	 
        	          if(failureMap.get(element.intValue()).size()>=_rest) 
        	          {
        	          convergedNodes.add(element.intValue());
        	          log.info("Converged for failure of node - " 
        	+ element.intValue()); 
        	          
        	          }
        	          }
        	 
        	        }        	         
        	        
        	// ### End of Filtering for Phase 1 Task 7************************
            */
        	
        	
        	
        	// #### NORMAL - START
//        	log.info("Status from:{} - Vicinity of {} nodes:[" 
//		        								+ ProcessViciniTyList(status.getContent().vicinityNodeList) + "] dead" 
//		        								+ ProcessSet(status.getContent().deletedNodeList) + "  joined" 
//		        							    + ProcessSet(status.getContent().joinedNodeList) +  "  suspect [ " 
//		        							    + ProcessPiggyEntitySet(status.getContent().suspectedNodeList) + " ] Parents"
//		        							    + ProcessParents(status.getHeader().getSource().getParents()) + " newNats"
//		        							    + ProcessSet(status.getContent().newNATList), 
//		                    new Object[]{status.getHeader().getSource(),            					 
//		            					 status.getContent().vicinityNodeList.size()}
//		      				);
        	// #### NORMAL - END
        	
            // --
        }
    };

    public static class AggregatorInit extends Init<AggregatorComp> {

        public final NatedAddress selfAddress;

        public AggregatorInit(NatedAddress selfAddress) {
            this.selfAddress = selfAddress;
        }
    }
    
 // -- Riz

    public String ProcessSet(Set<NatEntity> nList)
    {
    	String st = " [";    	
    	for(NatEntity nd : nList){
    		String stat = "";
    		stat = " " + nd.nodeAdress.getId() + "-P"+ nd.nodeAdress.getParents().size() + "-" + nd.incurnationNumber;    		
    		st += stat;
    	}
    	return st + "]";
    	
    }
    
    
    public String ProcessParents(Set<NatedAddress> vList)
    {
    	String st = " ";    	
    	for(NatedAddress nd : vList){
    		
    		st += nd.getId() + " ";
    	}
    	st = "[" + st + "]"; 
    	return st;
    	    	
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
    
    public String ProcessViciniTyList(List<VicinityEntry> vList)
    {
    	String st = " ";    	
    	for(VicinityEntry nd : vList){
    		String stat = "";
    		if (nd.nodeStatus == "SUSPECTED"){
    			stat = "(S" + nd.waitingForPongCount + ")";
    		}
    		if (nd.nodeStatus == "DEAD"){
    			stat = "(D" + nd.waitingForPongCount + ")";
    		}
    		st += nd.nodeAdress.getId() + stat + " ";
    	}
    	return st;
    	    	
    }
    
    private class Entry{
    	public int id;
    	public List<Integer> deads = new ArrayList<Integer>();    	
    	
    }

    
    // --
}

