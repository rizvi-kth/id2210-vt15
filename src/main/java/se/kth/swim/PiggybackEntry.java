package se.kth.swim;


import se.sics.p2ptoolbox.util.network.NatedAddress;

public class PiggybackEntry {
	
	public PiggybackEntry(NatedAddress node) {
		super();
		this.nodeAdress = node;
	}
	
	public PiggybackEntry(NatedAddress node, String nodeStatus) {
		super();
		this.nodeAdress = node;
		this.nodeStatus = nodeStatus;
	}
	
	public NatedAddress nodeAdress = null;	
	public int incCount = 0;
	public String nodeStatus = "SUSPECTED"; // LIVE - SUSPECTED  
	
	
	
}
