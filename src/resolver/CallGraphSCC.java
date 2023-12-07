package resolver;
import soot.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.Targets;

import java.util.*;

public class CallGraphSCC {
	Stack<SootMethod> S = new Stack<SootMethod>();
	int index = 0;
	List<SootMethod> V;
    CallGraph cg;
    Map<SootMethod, Integer> indices = new HashMap<SootMethod, Integer>();
    Map<SootMethod, Integer> lowLinks = new HashMap<SootMethod, Integer>();
    List<Set<SootMethod>> order = new ArrayList<Set<SootMethod>>();
    Set<SootMethod> scc;
    
    public CallGraphSCC(CallGraph cg) {
        this.cg = cg;
        V = new ArrayList<SootMethod>();
        Iterator it = cg.sourceMethods();
        while(it.hasNext()) {
        	SootMethod m = (SootMethod) it.next();
        	if(m.getName().contains("main")) {
				dfsVisit(m);
			}

        }

//		for(SootMethod s : V ) {
//			if(!s.isJavaLibraryMethod()) {
//				System.out.println("Method is : "+ s);
//			}
//		}
//		System.out.println("===");
        /*SootMethod mainMethod = Scene.v().getMainMethod();
        dfsVisit(mainMethod);*/
        go();
		//eliminateLibraryMethods();
    }
    
    public void dfsVisit(SootMethod m) {
        if(V.contains(m))
        	return;
        V.add(m);
		Iterator<Edge> it = Scene.v().getCallGraph().edgesOutOf(m);
        while(it.hasNext()) {
			Edge edge = it.next();
			if(m.getName().equals("main")) {
				//System.out.println("edge value : "+ edge.tgt());
			}
        	if(!edge.isExplicit())
        		continue;
            SootMethod target = edge.tgt();
            if(target.hasActiveBody())
            	dfsVisit(target);
        }
    }



   /* public void dfsVisit(SootMethod m) {
        if(V.contains(m))
        	return;
        V.add(m);
        Iterator targets = new Targets(cg.edgesOutOf(m));
        while(targets.hasNext()) {
            SootMethod target = (SootMethod) targets.next();
            if(!target.isJavaLibraryMethod() && target.hasActiveBody())
            	dfsVisit(target);
        }
    }*/

    public void go() {
    	for(SootMethod m : V) {
			//System.out.println("Outside Method "+ m.getName());
    		if(!indices.containsKey(m)) {
				strongConnect(m);
			}
    	}
    }
    
    public void strongConnect(SootMethod v) {
    	// Set the depth index for v to the smallest unused index
    	indices.put(v, index);
    	lowLinks.put(v, index);
    	index++;
    	S.push(v);
    	// Consider successors of v
    	Iterator<Edge> it = Scene.v().getCallGraph().edgesOutOf(v);
    	while(it.hasNext()) {
    		Edge edge = it.next();
        	if(!edge.isExplicit())
        		continue;
        	
    		SootMethod w = edge.tgt();
    		
    		// If method calls itself, store this information for handling using fix-point method
//    		if(w.equals(v))
//    			CGTransform.callsItself.add(v);
    		
    		if(!indices.containsKey(w)) {
    			// Successor w has not yet been visited; recurse on it
    			strongConnect(w);
    			lowLinks.put(v, Math.min(lowLinks.get(v), lowLinks.get(w)));
    		}
    		else if(S.contains(w)){
    			// Successor w is in stack S and hence in the current SCC
    			lowLinks.put(v, Math.min(lowLinks.get(v), indices.get(w)));
    		}
    	}
    	
    	// If v is a root node, pop the stack and generate an SCC
    	//if(lowLinks.get(v) == indices.get(v)) {
    		scc = new HashSet<SootMethod>();
			scc.add(v);
    		SootMethod w;
    		do {
    			w = S.pop();
    			if(cg.edgesInto(w).hasNext()) {
					//System.out.println(" Method "+ w);
					scc.add(w);
				}

    		} while(!v.equals(w));
    		order.add(scc);
    	//}
    }

    public List<Set<SootMethod>> order() { 
    	return order; 
    }
    
    public void eliminateLibraryMethods() {
    	for(Set<SootMethod> scc : order) {
    		for(SootMethod m : scc)
    			//if(m.isJavaLibraryMethod())
    				scc.remove(m);
    	}
    }
    
}
