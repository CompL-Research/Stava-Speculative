package utils;

import es.CallSite;
import main.Main;
import ptg.PointsToGraph;
import es.EscapeStatus;
import ptg.ObjectNode;
import ptg.ObjectType;
import resolver.ContextualResolver;
import soot.SootField;
import soot.SootMethod;

// import java.util.HashMap;
// import java.util.Map;
// import java.util.Queue;
import java.util.*;

public class Stats {
	int internal;
	int noEscape;
	int cv;
	static int total;
	double percentageNE;
	double percentageCV;
	int inlineNoEscape;
	int count;
	int totalCount;
	int[] cnt = {0,0,0};

	public Stats(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries, Map<SootMethod, PointsToGraph> ptg) {
		internal = 0;
		noEscape = 0;
		cv = 0;
		percentageNE = 0;
		percentageCV = 0;
		for (Map.Entry<SootMethod, HashMap<ObjectNode, EscapeStatus>> e : summaries.entrySet()) {
			//if(!e.getKey().isJavaLibraryMethod()) {
			//System
			//if(Main.ListofMethods.toString().contains(e.getKey().getBytecodeSignature().toString())) {
				//System.out.println("Stat for Method : "+e.getKey().getBytecodeSignature().toString());
				List<ObjectNode> cvObjects = new ArrayList<>();
				List<ObjectNode> escapeObjects = new ArrayList<>();
				for (Map.Entry<ObjectNode, EscapeStatus> ee : e.getValue().entrySet()) {
					if (ee.getKey().type == ObjectType.internal) {
						internal++;
						if (ee.getValue().doesEscape()) {
							if (ptg != null)
								escapeObjects.add(ee.getKey());
						} else if (ee.getValue().containsCV()) {
							if (ptg != null)
								cvObjects.add(ee.getKey());
							cv++;
	//						System.out.println(ee.getKey()+" of method:"+e.getKey().getBytecodeSignature()+" has a cv:"+ee.getValue());
						} else {
							noEscape++;
						}
					}
				}
				if (ptg != null) {
					int[] res = markFieldsCV(e.getKey(), e.getValue(), ptg.get(e.getKey()), cvObjects, escapeObjects);
					noEscape = res[0];
					cv = res[1];
				}
			//}
		}
		System.out.println("Stat Count: "+ cnt[0]+" "+cnt[1]+" "+cnt[2]);
		total = internal;
		percentageNE = (noEscape * 100.0 / total);
		percentageCV = (cv * 100.0 / total);
	}

	public Stats(Map<CallSite, HashMap<SootMethod, HashSet<Integer>>> inlinesummaries) {
		inlineNoEscape = 0;
		count = 0;
		totalCount = 0;
		HashMap<SootMethod, Integer> localCount = new HashMap<>();
		for(SootMethod m : ContextualResolver.solvedSummaries.keySet()) {
			count = 0;
			for(ObjectNode o: ContextualResolver.solvedSummaries.get(m).keySet()) {
				if(o.type == ObjectType.internal) {
					count++;
				}
			}
			localCount.put(m, count);
		}
		//ArrayList<SootMethod> visted = new ArrayList<>();
		for (Map.Entry<CallSite, HashMap<SootMethod, HashSet<Integer>>> i : inlinesummaries.entrySet()) {
			for (Map.Entry<SootMethod, HashSet<Integer>> j : i.getValue().entrySet()) {
				for(Integer k : j.getValue()) {
					inlineNoEscape++;
				}
				totalCount+= localCount.get(j.getKey());
			}
		}
		percentageNE = (inlineNoEscape * 100.0 / (totalCount));
	}

	private int[] markFieldsCV(SootMethod method, HashMap<ObjectNode, EscapeStatus> summary, PointsToGraph ptg,
				List<ObjectNode> cvo, List<ObjectNode> esco) {
		HashMap<ObjectNode, Integer> objects = new HashMap<>();
		for (ObjectNode obj: summary.keySet()) {
			objects.put(obj, 0);
		}
		bfs(objects, cvo, 1, ptg);
		bfs(objects, esco, 2, ptg);
		// int[] cnt = {0,0,0};
		for (ObjectNode obj: summary.keySet()) {
			if (obj.type == ObjectType.internal)
				cnt[objects.get(obj)]++;
		}
		return new int[]{cnt[0],cnt[1]};
	}

	private void bfs(HashMap<ObjectNode, Integer> objects, List<ObjectNode> cvo, Integer val, PointsToGraph ptg) {
		Queue<ObjectNode> q = new LinkedList<>();
		for (ObjectNode a:cvo) {
			q.add(a);
		}
		while (!q.isEmpty()) {
			ObjectNode u = q.poll();
			objects.put(u, val);
			if (ptg.fields.get(u) == null) continue;
			for (SootField sf : ptg.fields.get(u).keySet() ) {
				if (ptg.fields.get(u).get(sf) == null) continue;
				for (ObjectNode obn : ptg.fields.get(u).get(sf) ) {
					if (objects.get(obn) == val) continue;
					q.add(obn);
				}
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(totalCount!=0) {
			sb.append("Total Objects: " + (totalCount) + " ");
		}else {
			sb.append("Total Objects: " + total + " ");
		}

		if(noEscape != 0) {
			sb.append("NoEscape: " + noEscape + " ");
		} else {
			sb.append("NoEscape: " + inlineNoEscape + " ");
		}
		sb.append(String.format("(%.2f%%) ", percentageNE));
		sb.append("CV: " + cv + " ");
		sb.append(String.format("(%.2f%%) ", percentageCV));
		return sb.toString();
	}
}
