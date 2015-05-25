package se.kth.swim;

import se.sics.p2ptoolbox.util.network.NatedAddress;

public class NatEntity {
	
	public NatEntity(NatedAddress nodeAdress) {
		super();
		this.nodeAdress = nodeAdress;
	}
	
	public NatedAddress nodeAdress = null;
	public int incurnationNumber = 0;

}
