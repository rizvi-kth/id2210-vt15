package se.kth.swim;


import se.sics.p2ptoolbox.util.network.NatedAddress;

public class ParentEntry {
	
	public ParentEntry(NatedAddress node) {
		super();
		this.nodeAdress = node;
	}
	
	public NatedAddress nodeAdress = null;
	public boolean waitingForPong = false;
	public int waitingForPongCount = 0;
	public int incurnationNumber = 0;
//	public boolean isHB = true;
	//public String nodeStatus = "LIVE"; // LIVE - SUSPECTED - DEAD 
	
	
	
}
