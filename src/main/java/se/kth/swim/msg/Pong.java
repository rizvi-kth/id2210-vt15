package se.kth.swim.msg;

import java.util.Deque;
import java.util.Set;

import se.kth.swim.PiggybackEntry;
import se.sics.p2ptoolbox.util.network.NatedAddress;


public class Pong {
	// -- Riz

			private Deque<NatedAddress> piggyBackedJoinedNodes = null;
			private Deque<NatedAddress> piggyBackedDeadNodes = null;
			private Set<PiggybackEntry> piggyBackedSuspectedNodes = null;			
								
			
			public Pong()
			{}
			
			public Pong(Deque<NatedAddress> p_piggyBackedJoinedNodes,Deque<NatedAddress> p_piggyBackedDeadNodes,Set<PiggybackEntry> p_piggyBackedSuspectedNodes)
			{
				piggyBackedJoinedNodes= p_piggyBackedJoinedNodes;
				piggyBackedDeadNodes= p_piggyBackedDeadNodes;			
				piggyBackedSuspectedNodes = p_piggyBackedSuspectedNodes; 
			}
			
			public Deque<NatedAddress> getPiggyBackedJoinedNodes() {
				return piggyBackedJoinedNodes;
			}

			public Deque<NatedAddress> getPiggyBackedDeadNodes() {
				return piggyBackedDeadNodes;
			}
			
			public Set<PiggybackEntry> getPiggyBackedSuspectedNodes() {
				return piggyBackedSuspectedNodes;
			}



	// --
}
