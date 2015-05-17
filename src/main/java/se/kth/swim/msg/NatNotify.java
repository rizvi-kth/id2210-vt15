package se.kth.swim.msg;

import se.sics.kompics.KompicsEvent;
import se.sics.p2ptoolbox.util.network.NatedAddress;

public class NatNotify implements KompicsEvent {
	private NatedAddress ChangedNatAddress;
	
	public NatNotify(NatedAddress changedNatAddress) {
		super();
		ChangedNatAddress = changedNatAddress;
	}
	
	public NatedAddress getChangedNatAddress() {
		return ChangedNatAddress;
	}
	
}
