package submit_a3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.internal.JEnterMonitorStmt;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.sets.P2SetVisitor;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

//to store node with edgeType in adjacency List
class NodeAndEdgeType{
	PEGNode node;
	String type;
	NodeAndEdgeType(PEGNode _node, String _type){
		node = _node;
		type = _type;
	}
}

public class PEG {
	//PEG
	HashMap<PEGNode, List<NodeAndEdgeType>> graph;
	HashMap<PEGNode, List<NodeAndEdgeType>> predGraph;
	List<SootMethod> allMethods;
	
	List<PEGNode> allBeginNodes = new ArrayList<>();
	
	PEG(List<SootMethod> _allMethods){
		allMethods = _allMethods;
		graph = new HashMap<>();
		predGraph = new HashMap<>();
	}
	
	void constructPredGraph() {
		
		for(PEGNode node: graph.keySet()) {
			predGraph.put(node, new ArrayList<>());
		}
		
		for(Map.Entry<PEGNode,List<NodeAndEdgeType>> entry: graph.entrySet()) {
			PEGNode node = entry.getKey();
			List<NodeAndEdgeType> successors = entry.getValue();
			
			for(NodeAndEdgeType succAndEdge: successors) {
				NodeAndEdgeType predecessor = new NodeAndEdgeType(node, succAndEdge.type);
				
				if(!predGraph.containsKey(succAndEdge.node)){
					List<NodeAndEdgeType> predList = new ArrayList<>();
					predList.add(predecessor);
					predGraph.put(succAndEdge.node, predList);
				}else {
					predGraph.get(succAndEdge.node).add(predecessor);
				}
				
			}
		}
	}
	
	void DFSUtil(PEGNode n, Set<PEGNode> visited) {
		visited.add(n);
		

		Iterator<NodeAndEdgeType> it;
		if(graph.containsKey(n)) {
			it = graph.get(n).listIterator();
	        while (it.hasNext()) {
	        	NodeAndEdgeType succ = it.next();
	            if (!visited.contains(succ.node))
	                DFSUtil(succ.node, visited);
	        }
		}
	}
	
	void DFS(PEGNode src) {
		Set<PEGNode> visited = new HashSet<>();
		DFSUtil(src,visited);
	}
	
	//return pointsToSet of local variable from spark
	HashSet<String> findPointsToList(Local l){
		PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
		PointsToSet ptSet = pta.reachingObjects(l);
		PointsToSetInternal pti = (PointsToSetInternal)ptSet;
		
		
		HashSet<String> p2set = new HashSet<>();
		P2SetVisitor vis = new P2SetVisitor(){

            @Override
            public void visit(Node n){
             /* Do something with node n*/
            	String str = new String(""+n.getType()+n.getNumber());
            	
            	p2set.add(str);
	     
            }
    	};
    	pti.forall(vis);
    	return p2set;
	}
	
	//return waiting predecessors of node
	PEGNode waitingPredecessor(PEGNode n){
		
		//traverse predecessor graph to check if it has node with "waiting" property
		for(NodeAndEdgeType ne: predGraph.get(n)){
			if(ne.node.specialProperty.equals("waiting")) {
				return ne.node;
			}
		}
		return null;
	}
	
	//returns start predecessor of node
	List<PEGNode> startPred(PEGNode n){
		
		List<PEGNode> startPredList = new ArrayList<>();
		
		//traverse predecessor graph to check if it has node with "start" property
		for(NodeAndEdgeType ne: predGraph.get(n)){
			if(ne.node.specialProperty.equals("start")) {
				startPredList.add(ne.node);
			}
		}
		return startPredList;
	}
	
	//returns notify successors of node 
	HashSet<PEGNode> computeNotifySuccesor(PEGNode n){
		
		HashSet<PEGNode> notifySuccList = new HashSet<>();
		
		//check if there is waiting node in M(n) set
		for(PEGNode nodeInMSet: n.M) {
			
			//if waiting node found
			if(nodeInMSet.specialProperty.equals("waititng")) {
				
				//find its notifiedEntryNode
				PEGNode notifiedEntryNode;
				
				//successor of waiting node is notified-entry node
				List<NodeAndEdgeType> succ = graph.get(nodeInMSet);
				notifiedEntryNode = succ.get(0).node;
				
				//if notified-entry found, check if intersection is not null of p2set of waiting and notifedentry node
				if(notifiedEntryNode.specialProperty.equals("notifiedentry")) {
					if(n.object!=null) {
						
						if(hasIntersection(n.object, notifiedEntryNode.object))
							notifySuccList.add(notifiedEntryNode);
						
					}
				}
			}
		}
		return notifySuccList;
	}
	
    void makeEdgeForNotifySuccs(PEGNode n) {
    	
    	for(PEGNode m : n.NotifySucc) 
    		predGraph.get(m).add(new NodeAndEdgeType(n, "notify to  notifiedentry")); 
    	
    	for(PEGNode m : n.NotifySucc) 	
    		graph.get(n).add(new NodeAndEdgeType(m, "notify to  notifiedentry")); 
    	
    	
    }
	
	
    //return all nodes of thread
   HashSet<PEGNode> N(String threadID){
    	
       	HashSet<PEGNode>allNodesOfThread = new HashSet<>();

        for(PEGNode node: graph.keySet())
            if(node.threadId.equals(threadID))
            	allNodesOfThread.add(node);

        return allNodesOfThread;
    }
	
  
    boolean hasIntersection(HashSet<String> set1, HashSet<String> set2) {
    	for(String obj: set1) {
    		if(set2.contains(obj))
    			return true;
    	}
    	return false;
    }
    
    HashSet<PEGNode> getIntersectionOfPEGNodeSet(HashSet<PEGNode> set1, HashSet<PEGNode> set2) {
    	HashSet<PEGNode> intersection = new HashSet<>();
    	for(PEGNode n: set1) {
    		if(set2.contains(n)) {
    			intersection.add(n);
    		}
    	}
    	return intersection;
    }
    
    
    HashSet<PEGNode> computeGenNotifyAll(PEGNode n){
    	
    	HashSet<PEGNode> genNotifyAllList = new HashSet<>();
    	
    	if(n.specialProperty.equals("notifiedentry")) {
    		for(PEGNode m: graph.keySet()) {
    			//check if m is notified-entry node and has common objects
    			if(m.specialProperty.equals("notifiedentry") && hasIntersection(m.object,n.object)) {
    				
    				//check if waiting predecessors of m is in M set of n
    				if(waitingPredecessor(n).M.contains(waitingPredecessor(m))) {
    					for(PEGNode r: graph.keySet()) {
    						if(r.specialProperty.equals("notifyall")) {
    							
    							//if r is M set of both the waiting predecessors of m and n, then add m to list
    							HashSet<PEGNode> intersection = getIntersectionOfPEGNodeSet(waitingPredecessor(m).M, waitingPredecessor(n).M);
    							if(intersection.contains(r)) {
    								genNotifyAllList.add(m);
    							}
    						}
    					}
    				}
    			}
    		}
    	}
    	return genNotifyAllList;
    }
    
    //check if unit is start unit
    boolean isStartUnit(Unit u){
    	if(u!=null) {
	    	Stmt s = (Stmt)u;
			if(s.containsInvokeExpr() && s.getInvokeExpr().getMethod().getName().toString().equals("start")){
				return true;
			}
    	}
		return false;
    }
    
  //check if unit is join unit
    boolean isJoinUnit(Unit u) {
    	if(u!=null) {
	    	Stmt s = (Stmt)u;
			if(s.containsInvokeExpr() && s.getInvokeExpr().getMethod().getName().toString().equals("join")){
				return true;
			}
    	}
		return false;
    }
    
  //check if unit is enterMonitor statement of synchronized block
    boolean isLockEntryUnit(Unit u) {
    	if(u!=null) {
	    	Stmt s = (Stmt)u;
	        if (s instanceof EnterMonitorStmt) {
	            return true;
	        }
    	}
        return false;
    }
    
  //check if unit is exitMonitor statement of synchronized block
    boolean isLockExitUnit(Unit u) {
    	if(u!=null) {
	    	Stmt s = (Stmt)u;
	        if (s instanceof ExitMonitorStmt) {
	            return true;
	        }
    	}
        return false;
    }
    
    //check if it is notify unit
    boolean isNotifyUnit(Unit u) {
    	if(u!=null) {
	    	Stmt s = (Stmt)u;
			if(s.containsInvokeExpr() && s.getInvokeExpr().getMethod().getName().toString().equals("notify")){
				return true;
			}
    	}
		return false;

    }

    //check if it is notifyAll unit
    boolean isNotifyAllUnit(Unit u) {
    	if(u!=null) {
	    	Stmt s = (Stmt)u;
			if(s.containsInvokeExpr() && s.getInvokeExpr().getMethod().getName().toString().equals("notifyAll")){
				return true;
			}
    	}
		return false;
    }

  //check if it is wait unit
    boolean isWaitUnit(Unit u) {
    	if(u!=null) {
	    	Stmt s = (Stmt)u;
			if(s.containsInvokeExpr() && s.getInvokeExpr().getMethod().getName().toString().equals("wait")){
				return true;
			}
    	}
		return false;
    }
    
    
    HashSet<PEGNode> waitingNodesWithSameObject(String obj) {
    	
    	HashSet<PEGNode> waitingNodesWithSameObjectList = new HashSet<>();
    	
    	for(PEGNode node : graph.keySet()) 
    		if(node.specialProperty.equals("waiting") && node.object.contains(obj)) 
    			waitingNodesWithSameObjectList.add(node);
    
    	return waitingNodesWithSameObjectList;
    	
    }
    
 // returns the list of begin nodes corresponding to all objs of start node
    HashSet<PEGNode> beginNodesOfStart(PEGNode n) {

        HashSet<PEGNode> beginNodesOfStartList = new HashSet<>();

        for(NodeAndEdgeType successor: graph.get(n))
            if(successor.node.specialProperty.equals("begin"))
            	beginNodesOfStartList.add(successor.node);

        return beginNodesOfStartList;
    }
    
    HashSet<PEGNode> GEN(PEGNode n){
    	
    	//if n is start node, add begin nodes of all objects given by p2set
    	if(isStartUnit(n.unit))
    		return beginNodesOfStart(n);
    	
    	if(n.specialProperty.equals("notifyAll") ||n.specialProperty.equals("notify")){
    		return computeNotifySuccesor(n);
    	}
    	if(isLockExitUnit(n.unit)) {
    		for(PEGNode _n: N(n.threadId)) {
    			if(isLockEntryUnit(_n.unit)) {
    				if(hasIntersection(n.object, _n.object)) {
    					return computeKILL(_n);
    				}
    			}
    		}
    	}
    	return new HashSet<>();
    }
    
    HashSet<PEGNode> setUnion(HashSet<PEGNode> set1, HashSet<PEGNode> set2){
    	HashSet<PEGNode> union = new HashSet<>();
    	union.addAll(set1);
    	union.addAll(set2);
    	return union;
    }
    
    HashSet<PEGNode> setDifference(HashSet<PEGNode> set1, HashSet<PEGNode> set2){
    	
    	HashSet<PEGNode> diffSet = new HashSet<>();
    	for(PEGNode node: set1) {
    		if(!set2.contains(node)) {
    			diffSet.add(node);
    		}
    	}
    	return diffSet;
    }
    

    Value getBaseVar(Unit u) {

        Stmt s = (Stmt) u;
        VirtualInvokeExpr vi = (VirtualInvokeExpr) s.getInvokeExpr();
        return vi.getBase();

    }
    Type getBaseVarClass(Unit u) {

        Stmt s = (Stmt) u;
        VirtualInvokeExpr vi = (VirtualInvokeExpr) s.getInvokeExpr();
        return vi.getBase().getType();

    }
    //returns OUT of node
    HashSet<PEGNode> computeOUT(PEGNode n) {
    	return setDifference(setUnion(n.M, GEN(n)), computeKILL(n));
    }
   
    
    HashSet<PEGNode> computeM(PEGNode n){
    	HashSet<PEGNode> newMSet = new HashSet<>();
    	if(n.specialProperty.equals("begin")) {
    		for(PEGNode startNode: startPred(n)) {
    			newMSet = setUnion(newMSet, startNode.Out);
    		}
    		
    		newMSet = setDifference(newMSet, N(n.threadId));
    		return setUnion(newMSet, n.M);
    		
    	}else if(n.specialProperty.equals("notifiedentry")) {
    		
    		for(PEGNode notifyNode: notifiedEntryPred(n)) {
    			newMSet = setUnion(newMSet, notifyNode.Out);
    		}
    		
    		newMSet =  getIntersectionOfPEGNodeSet(newMSet, waitingPredecessor(n).Out);
    		newMSet = setUnion(newMSet, n.GenNotifyAll);
    		
    		return setUnion(newMSet, n.M);
    	}
    	
    	HashSet<PEGNode> localPredList = new HashSet<>();
    	for(NodeAndEdgeType ne: predGraph.get(n)) {
    		if(ne.type.equals("local"))
    			localPredList.add(ne.node);
    	}
    	
    	for(PEGNode localPred: localPredList) {
    		newMSet = setUnion(newMSet, localPred.Out);
    	}
    	return setUnion(newMSet, n.M);
    }

    
    HashSet<PEGNode> computeGEN(PEGNode n){

    	HashSet<PEGNode> newGenSet = new HashSet<>();
        if (isStartUnit(n.unit))
            return beginSuccOfStart(n); 

        if(n.specialProperty.equals("notifyall") || n.specialProperty.equals("notify"))
        	return n.NotifySucc; 
       
        if(isLockExitUnit(n.unit)) 
        	for(PEGNode _n : N(n.threadId)) { 
        		if(isLockEntryUnit(_n.unit)) {
        			if(hasIntersection(n.object, _n.object))
        				return _n.Kill; 
        		}
        	}
        
        return newGenSet; 
        
    }
    
    
    void computeMonitorUtil(PEGNode n, Set<PEGNode> visited, HashSet<PEGNode> ans) {
		visited.add(n);
		
		Iterator<NodeAndEdgeType> it;
		if(graph.containsKey(n)) {
			it = graph.get(n).listIterator();
	        while (it.hasNext()) {
	        	NodeAndEdgeType succ = it.next();
	        	if(isLockExitUnit(succ.node.unit))
	        		break;
	            if (!visited.contains(succ.node)) {
	            	ans.add(succ.node);
	            	computeMonitorUtil(succ.node, visited,ans);
	            }
	        }
		}
	}
	
	void computeMonitor(PEGNode src, HashSet<PEGNode> ans) {
		Set<PEGNode> visited = new HashSet<>();
		computeMonitorUtil(src,visited,ans);
	}
    
    HashSet<PEGNode> computeKILL(PEGNode n){
    	
    	HashSet<PEGNode> waitingNodesToKill = new HashSet<>();
    	
    	//if n is join with single object then we can kill all the statements of thread
    	if(isJoinUnit(n.unit) && n.object.size() == 1) {
    		//to check if it is true
    		return N(n.threadId);

    	}
    	
    	if(isLockEntryUnit(n.unit) || n.specialProperty.equals("notifiedentry")) {
    		if(n.object.size()==1) {
    			
    			HashSet<PEGNode> montiorObjList = new HashSet<>();
    			computeMonitor(n, montiorObjList);
    			return montiorObjList;
    		}
    	}
    	
    	if(n.specialProperty.equals("notifyall")) {
    		
    		for(String obj: n.object) {
    			waitingNodesToKill.addAll(waitingNodesWithSameObject(obj));
    		}
    		return waitingNodesToKill;
    	}
    	
    	if(n.specialProperty.equals("notify")) {
  
    		for(String obj: n.object) {
    			if(waitingNodesWithSameObject(obj).size() == 1)
    				waitingNodesToKill.addAll(waitingNodesWithSameObject(obj));
    		}
    		return waitingNodesToKill;
    	}
    	return waitingNodesToKill;
    }
  
    HashSet<PEGNode> beginSuccOfStart(PEGNode n) {

    	HashSet<PEGNode> beginSuccOfStartList = new HashSet<>();
        for (NodeAndEdgeType successor: graph.get(n)) {
            if (successor.node.specialProperty.equals("begin")) {
            	beginSuccOfStartList.add(successor.node);
            }
        }
        return beginSuccOfStartList;

    }
    
    //returns list of notifiedEntryPredecessor list
    HashSet<PEGNode> notifiedEntryPred(PEGNode n){
    	
    	HashSet<PEGNode> notifiedEntryPredList = new HashSet<>();
    	for(NodeAndEdgeType ne: predGraph.get(n)) {
    		if(ne.node.specialProperty.equals("notifyall") || ne.node.specialProperty.equals("notify"))
    			notifiedEntryPredList.add(ne.node);
    	}
    	
    	return notifiedEntryPredList;
    }
 
    void construct_peg(SootMethod mainMethod, String threadId, PEGNode callerNode){
		
		//create cfg for method
    	UnitGraph g = new BriefUnitGraph(mainMethod.getActiveBody());
    	
    	HashSet<String> thisVarP2set = new HashSet<>();
    	if(callerNode.unit!=null) {
    		thisVarP2set = findPointsToList((Local)getBaseVar(callerNode.unit));
    	}
    	
    	//Create a mapping from unit to its node
    	HashMap<Unit,PEGNode> unitNodeMapping = new HashMap<>();
    	
    	//add PEGNodes in peg graph 
    	for(Unit u: g){

	    		PEGNode node = new PEGNode(new HashSet<>(thisVarP2set), u , threadId, "");
	    		
	    		//create unit to newNode mapping
	    		unitNodeMapping.put(u, node);
	    		
	    		//put newNode in peg graph with empty adjacency list
	    		graph.put(node, new ArrayList<>());
	    	
    	}
    	
    	//add begin node
    	//Taking 1st node of heads list as first node
    	for(Unit u : g.getHeads()) {
    		
    			PEGNode beginNode = new PEGNode(new HashSet<>(thisVarP2set), null , threadId, "begin");
    			
    			List<NodeAndEdgeType> beginList = new ArrayList<>();
    			
    			beginList.add(new NodeAndEdgeType(unitNodeMapping.get(u),"local"));
    			graph.put(beginNode , beginList);
    			
    			//add edge from caller node to begin node
    	    	if(!threadId.equals("main")) {
    	    		graph.get(callerNode).add(new NodeAndEdgeType(beginNode,"startedge"));
    	    	}

    	    	allBeginNodes.add(beginNode);
    	    	break;
    	}
    	
    	//from start call, add peg of new called thread
    	for(Unit u: g){

    		Stmt s = (Stmt)u;
    		if(isStartUnit(u)){

    			Value baseVar = getBaseVar(u);
    			Type baseClass = getBaseVarClass(u);
    			HashSet<String> p2set = findPointsToList((Local)baseVar);
    			PEGNode node = unitNodeMapping.get(u);
    			node.object = p2set;
    			node.specialProperty = "start";

    			for(SootMethod method: allMethods){
    				if(method.getDeclaringClass().getName().equals(baseClass.toString())){
    					construct_peg(method, baseVar.toString(),node);
    				}
    			}
    			
    		}else if(isWaitUnit(u)) {

    			Value baseVar = getBaseVar(u);
			  
    	        HashSet<String> p2set = findPointsToList((Local)baseVar);
    			
    			
    	        PEGNode node = unitNodeMapping.get(u);
    			node.object = p2set;
    			node.specialProperty = "wait";
    			
    			//divide cfg wait node to peg three nodes 
    			//waiting node
    			PEGNode waitingNode = new PEGNode(p2set,null,threadId,"waiting");
    			
    			//notified-entry Node
    			PEGNode notifiedEntryNode = new PEGNode(p2set,null,threadId,"notifiedentry");
    			
    			//create unit to newNode mapping
        		unitNodeMapping.put(u, node);
        		
        		//put waiting,notified-entry node in peg graph with empty adjacency list
        		graph.put(waitingNode, new ArrayList<>());
        		graph.put(notifiedEntryNode, new ArrayList<>());

        		//add edge from notifiedEntry node to successors of cfg wait node
        		List<Unit> successorsOfWait = g.getSuccsOf(u);
        		for(Unit succs: successorsOfWait) {
        			PEGNode destNode = unitNodeMapping.get(succs);
        			graph.get(notifiedEntryNode).add(new NodeAndEdgeType(destNode,"normal"));
        		}
        		graph.get(node).add(new NodeAndEdgeType(waitingNode, "local"));
        		graph.get(waitingNode).add(new NodeAndEdgeType(notifiedEntryNode, "watiing"));
        		
        		
    		}else if(isJoinUnit(u)) {
    			Value baseVar =  getBaseVar(u);
                unitNodeMapping.get(u).object = findPointsToList((Local) baseVar); 
    		}else if(isNotifyUnit(u)) {
    			Value baseVar =  getBaseVar(u);
                unitNodeMapping.get(u).object = findPointsToList((Local) baseVar); 
    		}else if(isNotifyAllUnit(u)) {
    			Value baseVar =  getBaseVar(u);
                unitNodeMapping.get(u).object = findPointsToList((Local) baseVar); 
    		}else if(isLockEntryUnit(u)) {
    			Value baseVar = ((EnterMonitorStmt) s).getOp();
    			unitNodeMapping.get(u).object = findPointsToList((Local) baseVar); 
    		}else if(isLockExitUnit(u)) {
    			Value baseVar = ((ExitMonitorStmt) s).getOp();
    			unitNodeMapping.get(u).object = findPointsToList((Local) baseVar); 
    		}

    	}
    	
    	//add internal edges to nodes
    	for(Unit u: g){
    		
    		PEGNode srcNode = unitNodeMapping.get(u);
    		//get succ of unit
    		if(!isWaitUnit(u)) {
	    		List<Unit> successors = g.getSuccsOf(u);
	    		
	    		for(Unit s: successors){
	    			
	    			PEGNode destNode = unitNodeMapping.get(s);
	    			
	    			graph.get(srcNode).add(new NodeAndEdgeType(destNode,"local"));
	    		}
    		}
    	}
    	
    	//add end node
    	for(Unit u: g){
    		if(g.getSuccsOf(u).isEmpty()) {
    			
    			PEGNode endNode = new PEGNode(null, null , threadId, "end");
    			List<NodeAndEdgeType> endList = new ArrayList<>();
    			
    			endList.add(new NodeAndEdgeType(endNode,"endedge"));
    			graph.put(unitNodeMapping.get(u) , endList);
    		}	
    	}

	}

}
