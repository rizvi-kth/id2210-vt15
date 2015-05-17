package se.kth.swim;


import se.sics.kompics.PortType;
import se.kth.swim.msg.NatNotify;;


public class NatNotifyPort extends PortType {
	{
	
		indication(NatNotify.class);
	}
}
