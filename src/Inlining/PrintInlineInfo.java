package Inlining;

import es.CallSite;
import fj.P;
import main.Main;
import soot.SootMethod;
import utils.GetListOfRecaptureObjects;

import java.util.*;

public class PrintInlineInfo {

    public static List<CallSite> getSortedCallSites (SootMethod method,
                                          Map<CallSite,HashMap<SootMethod, HashSet<Integer>>> inlineSummary) {
//            System.out.println("Inside getSortedCallSites");
            List<Integer> c1 = new ArrayList<>();
            List<CallSite> c2 = new ArrayList<>();
            List<CallSite> c3 = new ArrayList<>();
            for(CallSite cs : inlineSummary.keySet()) {
                if(cs.methodName.equals(method)) {
                        c2.add(cs); // c2 hold both method name and BCI like: <SpecOpt: void foo(SpecOpt)>,91>
                        c1.add(cs.BCI); // c1 holds 91
//                        System.out.println("Method is :"+ method.toString());
                }
            }
//            System.out.println("C1: "+ c1.toString());
            Collections.sort(c1);
//            System.out.println("After SORTING C1: "+ c1.toString());
            for(Integer i : c1) {
                for(CallSite c : c2) {
                    if(c.BCI == i)
                        c3.add(c);
                }
            }
            return c3;
    }

    public static String get(CallSite c, HashMap<SootMethod, HashSet<Integer>> inlineSummary) {
        StringBuilder _ret = new StringBuilder();
        boolean flag = false;
        for(Map.Entry<SootMethod, HashSet<Integer>> e : inlineSummary.entrySet()) {
            if (!e.getValue().isEmpty())
                flag  = true;
        }
        if(!flag)
            return _ret.toString();
        if(Main.i == 0){
            _ret.append("");
        } else{
            _ret.append(" | ");
        }
//        System.out.println("Inside get: "+ c.toString());
        _ret.append(c.BCI);
        int k = 0;
        for(Map.Entry<SootMethod, HashSet<Integer>> e : inlineSummary.entrySet()) {
            if(e.getValue().isEmpty())
                continue;
            if(k==0) {
                _ret.append(" ");
                k++;
            } else {
                _ret.append(" ! ");
            }
            _ret.append(transformFuncSignature(e.getKey().getBytecodeSignature()));
            ArrayList<Integer> arr = new ArrayList<>(e.getValue());
            Collections.sort(arr);
            _ret.append("[");
            int j= 1;
            StringJoiner joiner = new StringJoiner(",");
            arr.forEach(item -> joiner.add(item.toString()));
            _ret.append(joiner.toString());
            _ret.append("]");
            //_ret.append(">");
            Main.i++;
        }
        //_ret.append("}");
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
