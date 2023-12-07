package utils;

import es.ContextualEscapeStatus;
import es.EscapeStatus;
import ptg.ObjectNode;
import soot.SootMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KillCallerOnly {

	/*
	 * Meant to kill all the CallerOnly conditional values.
	 */
	public static Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> kill(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> solvedSummaries) {
		Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> _ret = new HashMap<>();
		for (Map.Entry<SootMethod, HashMap<ObjectNode, EscapeStatus>> entry : solvedSummaries.entrySet()) {
			SootMethod method = entry.getKey();
			HashMap<ObjectNode, EscapeStatus> map = entry.getValue();
			HashMap<ObjectNode, EscapeStatus> q = new HashMap<>();
			for (Map.Entry<ObjectNode, EscapeStatus> e : map.entrySet()) {
				ObjectNode obj = e.getKey();
				EscapeStatus es = e.getValue();
				q.put(obj, (es.isCallerOnly()) ? getEscape() : es);
			}
			_ret.put(method, q);
		}
		return _ret;
	}

	public static Map<SootMethod, HashMap<ObjectNode, ContextualEscapeStatus>> ckill(Map<SootMethod, HashMap<ObjectNode, List<ContextualEscapeStatus>>> solvedSummaries) {
		Map<SootMethod, HashMap<ObjectNode, ContextualEscapeStatus>> _ret = new HashMap<>();
		for (Map.Entry<SootMethod, HashMap<ObjectNode, List<ContextualEscapeStatus>>> entry : solvedSummaries.entrySet()) {
			SootMethod method = entry.getKey();
			HashMap<ObjectNode, List<ContextualEscapeStatus>> map = entry.getValue();
			HashMap<ObjectNode, ContextualEscapeStatus> q = new HashMap<>();
			for (Map.Entry<ObjectNode, List<ContextualEscapeStatus>> e : map.entrySet()) {
				ObjectNode obj = e.getKey();
				ContextualEscapeStatus es = (ContextualEscapeStatus) e.getValue();
				q.put(obj, (es.isCallerOnly()) ? getcEscape() : es);
			}
			_ret.put(method, q);
		}
		return _ret;
	}
	public static EscapeStatus getEscape(){
		EscapeStatus _ret = new EscapeStatus();
		_ret.setEscape();
		return _ret;
	}

	public static ContextualEscapeStatus getcEscape(){
		ContextualEscapeStatus _ret = new ContextualEscapeStatus();
		_ret.setEscape();
		return _ret;
	}
}
