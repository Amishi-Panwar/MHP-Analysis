package submit_a3;

import java.util.HashSet;

public class Iteration {

	PEG peg;
	Iteration(PEG _peg){
		peg = _peg;
	}
	
	int iterate() {
		int update = 0;
    	for (PEGNode node: peg.graph.keySet()) {

    		// Update M
    		HashSet<PEGNode> updatedM = peg.computeM(node);
    		
    		if(!(updatedM.equals(node.M))) {
    			node.M = updatedM;
    			
    			for(PEGNode m : updatedM) {
    				m.M.add(node);
    			}
    			update = 1;
    		}
    		
    		
    		// Update GEN
    		HashSet<PEGNode> updatedGen = peg.computeGEN(node); 
    		
    		if(!(updatedGen.equals(node.Gen))) {
    			node.Gen = updatedGen;
    			update = 1;
    		}
    		
    		
    		// Update KILL
    		HashSet<PEGNode> updatedKill = peg.computeKILL(node); 
    		
    		if(!(updatedKill.equals(node.Kill))) {
    			node.Kill = updatedKill;
    			update = 1;
    		}
    		

    		// Update OUT
    		HashSet<PEGNode> updatedOut = peg.computeOUT(node); 
    		
    		if(!(updatedOut.equals(node.Out))) {
    			node.Out = updatedOut;
    			update = 1;
    		}
    		
    		// Update notifySuccs
    		HashSet<PEGNode> updatedNotifySuccs = peg.computeNotifySuccesor(node);
    		
    		if(!(updatedNotifySuccs.equals(node.NotifySucc))) {
    			node.NotifySucc = updatedNotifySuccs;
    			
    			peg.makeEdgeForNotifySuccs(node);
    			update = 1;
    		}
    		
    		
    		// Update GENNotifyALL
    		HashSet<PEGNode> updatedGenNotifyAll =  peg.computeGenNotifyAll(node);
    		
    		if(!(updatedGenNotifyAll.equals(node.GenNotifyAll))) {
    			node.GenNotifyAll = updatedGenNotifyAll;
    			update = 1;
    		}

        }
		return update;
	}
}

