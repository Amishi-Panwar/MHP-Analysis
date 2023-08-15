package submit_a3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import soot.Unit;
import soot.jimple.Stmt;

public class PEGNode{
	
//	(object, unit, caller_method, special_property)
	HashSet<String> object;
	Unit unit;
	String threadId;
	String specialProperty;
	
	//M(n)-> Set of nodes that may run in parallel with n
	HashSet<PEGNode> M;
	
	//Out(n)-> MHP info propagated to successors of n
	HashSet<PEGNode> Out;
	
	//
	HashSet<PEGNode> NotifySucc;
	
	HashSet<PEGNode> Gen;
	
	HashSet<PEGNode> GenNotifyAll;
	
	HashSet<PEGNode> Kill;
	
	PEGNode(HashSet<String> _object, Unit _unit, String _threadId, String _specialProperty){
		
		object = _object;
		unit = _unit;
		threadId = _threadId;
		specialProperty = _specialProperty;
		M = new HashSet<>();
		Out = new HashSet<>();
		NotifySucc = new HashSet<>();
		Gen = new HashSet<>();
		GenNotifyAll = new HashSet<>();
		Kill = new HashSet<>();
	}
	
	
//	void printPEGNode() {
//		
//		System.out.println("Object:"+ this.object +"| Unit:"+ this.unit+"| ThreadId:"+this.threadId+"| SpecialProperty:"+ this.specialProperty +"| M:"+this.M.size()+"| gen"+ this.Gen.size());
//
//	}
}