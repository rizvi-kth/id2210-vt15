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

package se.kth.swim.msg;

import java.util.Deque;
import java.util.Set;

import se.kth.swim.NatEntity;
import se.kth.swim.PiggybackEntry;
import se.sics.p2ptoolbox.util.network.NatedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Ping {
	
	private Deque<NatedAddress> piggyBackedJoinedNodes = null;
	private Deque<NatedAddress> piggyBackedDeadNodes = null;
	private Set<PiggybackEntry> piggyBackedSuspectedNodes = null;
	private Set<NatEntity> piggyBackedNATEntities = null;
	private String heartBeatType = null; // HB (HeartBeat) or CB (CroupierBeat) 
	
		

	public Ping()
	{}
	
	public Ping(String p_heartBeatType)
	{
		heartBeatType = p_heartBeatType;		
	}
	
	
	public Ping(Deque<NatedAddress> p_piggyBackedJoinedNodes,
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

	
}
