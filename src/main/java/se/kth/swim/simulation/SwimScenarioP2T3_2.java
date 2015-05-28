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
package se.kth.swim.simulation;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.javatuples.Pair;

import se.kth.swim.AggregatorComp;
import se.kth.swim.HostComp;
import se.kth.swim.croupier.CroupierConfig;
import se.sics.p2ptoolbox.simulator.cmd.OperationCmd;
import se.sics.p2ptoolbox.simulator.cmd.impl.ChangeNetworkModelCmd;
import se.sics.p2ptoolbox.simulator.cmd.impl.SimulationResult;
import se.sics.p2ptoolbox.simulator.cmd.impl.StartAggregatorCmd;
import se.sics.p2ptoolbox.simulator.cmd.impl.StartNodeCmd;
import se.sics.p2ptoolbox.simulator.cmd.impl.KillNodeCmd;
import se.sics.p2ptoolbox.simulator.core.network.NetworkModel;
import se.sics.p2ptoolbox.simulator.core.network.impl.DeadLinkNetworkModel;
import se.sics.p2ptoolbox.simulator.core.network.impl.DisconnectedNodesNetworkModel;
import se.sics.p2ptoolbox.simulator.core.network.impl.UniformRandomModel;
import se.sics.p2ptoolbox.simulator.dsl.SimulationScenario;
import se.sics.p2ptoolbox.simulator.dsl.adaptor.Operation;
import se.sics.p2ptoolbox.simulator.dsl.adaptor.Operation1;
import se.sics.p2ptoolbox.simulator.dsl.distribution.ConstantDistribution;
import se.sics.p2ptoolbox.simulator.dsl.distribution.extra.BasicIntSequentialDistribution;
import se.sics.p2ptoolbox.simulator.dsl.distribution.extra.GenIntSequentialDistribution;
import se.sics.p2ptoolbox.util.network.NatType;
import se.sics.p2ptoolbox.util.network.NatedAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicNatedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 * @author Md. Rizvi Hasan <mrhasan@kth.se>
 */
public class SwimScenarioP2T3_2 {

    private static long seed;
    private static InetAddress localHost;

    private static CroupierConfig croupierConfig = new CroupierConfig(10, 5, 2000, 1000); 
    static {
        try {
            localHost = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
    }

    //Make sure that your dead link set reflect the nodes in your system
    private static final Map<Integer, Set<Pair<Integer, Integer>>> deadLinksSets = new HashMap<Integer, Set<Pair<Integer, Integer>>>();

    static {
        Set<Pair<Integer, Integer>> deadLinks;

        deadLinks = new HashSet<Pair<Integer, Integer>>();
        deadLinks.add(Pair.with(10, 16));
        deadLinks.add(Pair.with(16, 10));
        deadLinksSets.put(1, deadLinks);

        /*       
        deadLinks = new HashSet<Pair<Integer, Integer>>();
        deadLinks.add(Pair.with(10, 12));
        deadLinks.add(Pair.with(12, 10));
        deadLinks.add(Pair.with(13, 10));
        deadLinksSets.put(2, deadLinks);
        */
    }

    //Make sure disconnected nodes reflect your nodes in the system
    private static final Map<Integer, Set<Integer>> disconnectedNodesSets = new HashMap<Integer, Set<Integer>>();

    static {
        Set<Integer> disconnectedNodes;
        
        disconnectedNodes = new HashSet<Integer>();
        disconnectedNodes.add(16);
        disconnectedNodesSets.put(1, disconnectedNodes);

        disconnectedNodes = new HashSet<Integer>();
        disconnectedNodes.add(10);
        disconnectedNodes.add(12);
        disconnectedNodesSets.put(2, disconnectedNodes);
    }

    static Operation1<StartAggregatorCmd, Integer> startAggregatorOp = new Operation1<StartAggregatorCmd, Integer>() {

        @Override
        public StartAggregatorCmd generate(final Integer nodeId) {
            return new StartAggregatorCmd<AggregatorComp, NatedAddress>() {
                private NatedAddress aggregatorAddress;

                @Override
                public Class getNodeComponentDefinition() {
                    return AggregatorComp.class;
                }

                @Override
                public AggregatorComp.AggregatorInit getNodeComponentInit() {
                    aggregatorAddress = new BasicNatedAddress(new BasicAddress(localHost, 23456, nodeId));
                    return new AggregatorComp.AggregatorInit(aggregatorAddress);
                }

                @Override
                public NatedAddress getAddress() {
                    return aggregatorAddress;
                }

            };
        }
    };

    static Operation1<StartNodeCmd, Integer> startNodeOp = new Operation1<StartNodeCmd, Integer>() {

        @Override
        public StartNodeCmd generate(final Integer nodeId) {
            return new StartNodeCmd<HostComp, NatedAddress>() {
                private NatedAddress nodeAddress;

                @Override
                public Class getNodeComponentDefinition() {
                    return HostComp.class;
                }

                @Override
                public HostComp.HostInit getNodeComponentInit(NatedAddress aggregatorServer, Set<NatedAddress> bootstrapNodes) {
                    if (nodeId % 2 == 0) {
                        //open address
                        nodeAddress = new BasicNatedAddress(new BasicAddress(localHost, 12345, nodeId));
                    } else {
                        //nated address
                        nodeAddress = new BasicNatedAddress(new BasicAddress(localHost, 12345, nodeId), NatType.NAT, bootstrapNodes);
                    }
                    /**
                     * we don't want all nodes to start their pseudo random
                     * generators with same seed else they might behave the same
                     */
                    long nodeSeed = seed + nodeId;
                    return new HostComp.HostInit(nodeAddress, bootstrapNodes, aggregatorServer, nodeSeed, croupierConfig);
                }

                @Override
                public Integer getNodeId() {
                    return nodeId;
                }

                @Override
                public NatedAddress getAddress() {
                    return nodeAddress;
                }

                @Override
                public int bootstrapSize() {
                    return 5;
                }

            };
        }
    };

    static Operation1<KillNodeCmd, Integer> killNodeOp = new Operation1<KillNodeCmd, Integer>() {

        public KillNodeCmd generate(final Integer nodeId) {
            return new KillNodeCmd() {
                public Integer getNodeId() {
                    return nodeId;
                }
            };
        }

    };

    //Usable NetworkModels:
    //1. UniformRandomModel
    //parameters: minimum link latency, maximum link latency
    //by default Simulator starts with UniformRandomModel(50, 500), so minimum link delay:50ms, maximum link delay:500ms
    //2. DeadLinkNetworkModel
    //composite network model that can be built on any other network model
    //parameters: network model, set of dead links (directed links)
    //Pair<1,2> means if node 1 will try to send a message to node 2, the simulator will drop this message, since this is a dead link
    //3. DisconnectedNodesNetworkModel
    //composite network model that can be built on any other network model
    //parameters: network model, set of disconnected nodes
    //a disconnected node will not be able to send or receive messages
    static Operation1<ChangeNetworkModelCmd, Integer> disconnectedNodesNMOp = new Operation1<ChangeNetworkModelCmd, Integer>() {

        @Override
        public ChangeNetworkModelCmd generate(Integer setIndex) {
            NetworkModel baseNetworkModel = new UniformRandomModel(50, 500);
            NetworkModel compositeNetworkModel = new DisconnectedNodesNetworkModel(setIndex, baseNetworkModel, disconnectedNodesSets.get(setIndex));
            return new ChangeNetworkModelCmd(compositeNetworkModel);
        }
    };
    
    static Operation1<ChangeNetworkModelCmd, Integer> reConnectedNodesNMOp = new Operation1<ChangeNetworkModelCmd, Integer>() {

        @Override
        public ChangeNetworkModelCmd generate(Integer setIndex) {
            NetworkModel baseNetworkModel = new UniformRandomModel(50, 500);
//            NetworkModel compositeNetworkModel = new DisconnectedNodesNetworkModel(setIndex, baseNetworkModel, disconnectedNodesSets.get(setIndex));
            
            return new ChangeNetworkModelCmd(baseNetworkModel);
        }
    };

    static Operation1<ChangeNetworkModelCmd, Integer> deadLinksNMOp = new Operation1<ChangeNetworkModelCmd, Integer>() {

        @Override
        public ChangeNetworkModelCmd generate(Integer setIndex) {
            NetworkModel baseNetworkModel = new UniformRandomModel(50, 500);
            NetworkModel compositeNetworkModel = new DeadLinkNetworkModel(setIndex, baseNetworkModel, deadLinksSets.get(setIndex));
            return new ChangeNetworkModelCmd(compositeNetworkModel);
        }
    };

    static Operation<SimulationResult> simulationResult = new Operation<SimulationResult>() {

        public SimulationResult generate() {
            return new SimulationResult() {

                @Override
                public void setSimulationResult(OperationCmd.ValidationException failureCause) {
                    SwimSimulationResult.failureCause = failureCause;
                }
            };
        }
    };

    //Operations require Distributions as parameters
    //1.ConstantDistribution - this will provide same parameter no matter how many times it is called
    //2.BasicIntSequentialDistribution - on each call it gives the next int. Works more or less like a counter
    //3.GenIntSequentialDistribution - give it a vector. It will draw elements from it on each call. 
    //Once out of elements it will give null. 
    //So be carefull for null pointer exception if you draw more times than elements
    //check se.sics.p2ptoolbox.simulator.dsl.distribution for more distributions
    //you can implement your own - by extending Distribution
    public static SimulationScenario simpleBoot(final long seed) {
        SwimScenarioP2T3_2.seed = seed;
        SimulationScenario scen = new SimulationScenario() {
            {
                StochasticProcess startAggregator = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, startAggregatorOp, new ConstantDistribution(Integer.class, 0));
                    }
                };

                StochasticProcess startPeers = new StochasticProcess() {
                    {                    	
                                              
                        eventInterArrivalTime(constant(1000));                      
//                        Integer[] _nodes = new Integer[]{10,16,18,21,30,32,39,40,};
                        raise(100, startNodeOp, new BasicIntSequentialDistribution(10));
                        
                        
                    }
                };
                
                StochasticProcess startPublicPeers = new StochasticProcess() {
                    {
                    	
                    	int total_nodes=70;  // Tested 70, 50, 20 according to private peers
                    	int start_id, node_count=0;
                    	Integer[] nodeIdList = new Integer[total_nodes];                      
                        for (start_id=2; node_count<total_nodes; node_count++,start_id+=2)
                        	nodeIdList[node_count] = start_id;
                                              
                        eventInterArrivalTime(constant(1000));                      
                        raise(nodeIdList.length, startNodeOp, new GenIntSequentialDistribution(nodeIdList));                        
                        
                    }
                };
                
                StochasticProcess startPrivatePeers = new StochasticProcess() {
                    {
                    	
                    	int total_nodes=30;  // Tested 30, 50, 80 according to public peers
                    	int start_id, node_count=0;
                    	Integer[] nodeIdList = new Integer[total_nodes];                      
                        for (start_id=1; node_count<total_nodes; node_count++,start_id+=2)
                        	nodeIdList[node_count] = start_id;
                                              
                        eventInterArrivalTime(constant(1000));                      
                        raise(nodeIdList.length, startNodeOp, new GenIntSequentialDistribution(nodeIdList));                        
                        
                    }
                };
                
                StochasticProcess joinPeers = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));                          
//                        raise(1, startNodeOp, new GenIntSequentialDistribution(new Integer[]{18}));
                        raise(5, startNodeOp, new GenIntSequentialDistribution(new Integer[]{20,21,80,51,10}));
                        //raise(100, startNodeOp, new BasicIntSequentialDistribution(10));
                        
                    }
                };

                StochasticProcess killPeers = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
//                        raise(1, killNodeOp, new GenIntSequentialDistribution(new Integer[]{25}));
//                        raise(5, killNodeOp, new GenIntSequentialDistribution(new Integer[]{20,21,30,71,10}));                        
//                        raise(10, killNodeOp, new GenIntSequentialDistribution(new Integer[]{20,21,30,51,10,
//                        																	 45,27,19,75,91}));                        
                        raise(20, killNodeOp, new GenIntSequentialDistribution(new Integer[]{21,13,05,27,19,
                        																	 20,30,40,50,60,
                        																	 22,32,42,52,62,
                        																	 26,36,46,56,66
                        																	 }));                        
//                        raise(30, killNodeOp, new GenIntSequentialDistribution(new Integer[]{21,13,05,27,19,
//																							 40,42,46,48,44,
//																							 60,62,66,68,64,
//																							 20,22,26,28,24,
//																							 50,52,56,58,54,
//																							 70,72,76,78,74
//																							 }));                        

                    }
                };
                
                StochasticProcess killPeers2 = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, killNodeOp, new ConstantDistribution(Integer.class, 16));                        
                    }
                };

                StochasticProcess deadLinks1 = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, deadLinksNMOp, new ConstantDistribution(Integer.class, 1));
                    }
                };

                StochasticProcess disconnectedNodes1 = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, disconnectedNodesNMOp, new ConstantDistribution(Integer.class, 1));
                    }
                };
                
                StochasticProcess reincurnate = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, reConnectedNodesNMOp, new ConstantDistribution(Integer.class, 0));
                    }
                };

                StochasticProcess fetchSimulationResult = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, simulationResult);
                    }
                };

                startAggregator.start();
//                startPeers.startAfterTerminationOf(1000, startAggregator);
                startPublicPeers.startAfterTerminationOf(1000, startAggregator);
                startPrivatePeers.startAfterTerminationOf(1000, startPublicPeers);
//                deadLinks1.startAfterTerminationOf(10000,startPeers);
//                disconnectedNodes1.startAfterTerminationOf(10000, deadLinks1);
//                joinPeers.startAfterStartOf(10000, deadLinks1);
//                reincurnate.startAfterTerminationOf(10000, joinPeers);               
                killPeers.startAfterTerminationOf(9*1000, startPrivatePeers);
//                killPeers2.startAfterTerminationOf(1*10000, killPeers);
                
                fetchSimulationResult.startAfterTerminationOf(900*1000, killPeers);
                terminateAfterTerminationOf(60*1000, fetchSimulationResult);

            }
        };

        scen.setSeed(seed);

        return scen;
    }
}
