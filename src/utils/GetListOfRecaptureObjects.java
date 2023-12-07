package utils;

import es.CallSite;
import soot.SootMethod;

import java.util.*;

public class GetListOfRecaptureObjects {
	public static String get(CallSite c, HashMap<SootMethod, HashSet<Integer>> inlineSummary) {
        StringBuilder _ret = new StringBuilder();
        _ret.append("{");
        _ret.append(c.BCI);
        for(Map.Entry<SootMethod, HashSet<Integer>> e : inlineSummary.entrySet()) {
            if(e.getValue().isEmpty())
                continue;
            _ret.append("<");
            _ret.append(transformFuncSignature(e.getKey().getBytecodeSignature()));
            //Collections.sort(e.getValue());
            _ret.append("[");
            int j= 1;
            StringJoiner joiner = new StringJoiner(",");
            e.getValue().forEach(item -> joiner.add(item.toString()));
            _ret.append(joiner.toString());
            _ret.append("]");
            _ret.append(">");
        }
        _ret.append("}");


//        for(Map.Entry<SootMethod, HashSet<Integer>> entry: recaptureSummary.entrySet()){
//            if(entry.getValue().isEmpty())
//                continue;
//            summaryMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
//        }
//        _ret.append('[');
//
//        for(Map.Entry<SootMethod, ArrayList<Integer>> e : summaryMap.entrySet()) {
//            if(e.getValue().isEmpty())
//                continue;
//            _ret.append("{");
//            _ret.append(e.getKey().toString());
//            _ret.append("<");
//            if(e.getValue().isEmpty())
//                continue;
//            _ret.append(transformFuncSignature(e.getKey().getBytecodeSignature()));
//            Collections.sort(e.getValue());
//            // _ret.append(" ");
//            _ret.append("[");
//            for(Integer i : e.getValue()) {
//                _ret.append(i.toString() + ",");
//            }
//            if(_ret.length()>0)
//                _ret.delete(_ret.length()-1, _ret.length());
//            _ret.append("]");
//            // _ret.append(e.getValue().toString());
//            _ret.append(">");
//            // if(_ret.length()>1)
//            //     _ret.delete(_ret.length()-2, _ret.length());
//            _ret.append("},");
//        }
//
//        // for(Map.Entry<InvokeSite, ArrayList<Integer>> entry: summaryMap.entrySet()){
//        //     Collections.sort(entry.getValue());
//        //     _ret.append(transformFuncSignature(entry.getKey().getMethod().getBytecodeSignature()));
//        //     _ret.append(" <");
//        //     _ret.append(entry.getKey().getSite());
//        //     _ret.append("> ");
//        //     _ret.append(entry.getValue().toString());
//        //     _ret.append(", ");
//        // }
//        if(_ret.length()>1)
//            _ret.delete(_ret.length()-2, _ret.length());
//        _ret.append(']');
//
		return _ret.toString();
	}

    static String transformFuncSignature(String inputString) {
		StringBuilder finalString = new StringBuilder();
		for(int i=1;i<inputString.length()-1;i++) {
			if(inputString.charAt(i) == '.')
				finalString.append('/');
			else if(inputString.charAt(i) == ':')
				finalString.append('.');
			else if(inputString.charAt(i) == ' ')
				continue;
			else finalString.append(inputString.charAt(i));
		}
		return finalString.toString();
	}
}
