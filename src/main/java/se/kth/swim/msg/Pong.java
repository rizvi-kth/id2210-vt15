package se.kth.swim.msg;

import java.util.Deque;
import java.util.Set;

import se.kth.swim.NatEntity;
import se.kth.swim.PiggybackEntry;
import se.sics.p2ptoolbox.util.network.NatedAddress;


public class Pong {
	// -- Riz

			private Deque<NatedAddress> piggyBackedJoinedNodes = null;
			private Deque<NatedAddress> piggyBackedDeadNodes = null;
			private Set<PiggybackEntry> piggyBackedSuspectedNodes = null;
			private Set<NatEntity> piggyBackedNATEntities = null;
			private String heartBeatType = null; // HB (HeartBeat) or CB (CroupierBeat) 
								
			
			public Pong()
			{}
			
			public Pong(String p_heartBeatType)
			{
				heartBeatType = p_heartBeatType;		
			}
			
			public Pong(Deque<NatedAddress> p_piggyBackedJoinedNodes,
						Deque<NatedAddress> p_piggyBackedDeadNodes,
						Set<PiggybackEntry> p_piggyBackedSuspectedNodes,
						Set<NatEntity> p_piggyBackedNATEntities)
			{
				piggyBackedJoinedNodes= p_piggyBackedJoinedNodes;
				piggyBackedDeadNodes= p_piggyBackedDeadNodes;			
				piggyBackedSuspectedNodes = p_piggyBackedSuspectedNodes;
				piggyBackedNATEntities = p_piggyBackedNATEntities;
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

			public Set<NatEntity> getPiggyBackedNATEntities() {
				return piggyBackedNATEntities;
			}
			
			public String getHeartBeatType() {
				return heartBeatType;
			}




	// --
}
