package se.kth.swim.msg;

import java.util.Set;
import se.sics.p2ptoolbox.util.network.NatedAddress;


public class Pong {
	// -- Riz

			private Set<NatedAddress> piggyBackedJoinedNodes = null;
			private Set<NatedAddress> piggyBackedDeadNodes = null;
			
			
			public Pong()
			{}
			
			public Pong(Set<NatedAddress> p_piggyBackedJoinedNodes,Set<NatedAddress> p_piggyBackedDeadNodes)
			{
				piggyBackedJoinedNodes= p_piggyBackedJoinedNodes;
				piggyBackedDeadNodes= p_piggyBackedDeadNodes;				
			}
			
			public Set<NatedAddress> getPiggyBackedJoinedNodes() {
				return piggyBackedJoinedNodes;
			}

			public Set<NatedAddress> getPiggyBackedDeadNodes() {
				return piggyBackedDeadNodes;
			}


	// --
}
