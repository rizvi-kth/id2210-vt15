package se.kth.swim;


import se.sics.p2ptoolbox.util.network.NatedAddress;

public class VicinityEntry {
	
	public VicinityEntry(NatedAddress node) {
		super();
		this.nodeAdress = node;
	}
	
	public NatedAddress nodeAdress = null;
	public boolean waitingForPong = false;
	public int waitingForPongCount = 0;
	public String nodeStatus = "LIVE"; // LIVE - SUSPECTED - DEAD
	//public int incurnationCount = 0;
	
}
