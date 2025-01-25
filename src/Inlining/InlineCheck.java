package Inlining;

import es.*;
import ptg.ObjectNode;
import ptg.ObjectType;
import ptg.PointsToGraph;
import resolver.SpeculativeResolver;
import soot.Scene;
import soot.SootField;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.*;

public class InlineCheck {
    public static void inlineinfo(SootMethod key, ObjectNode obj) {
        /*
         * We got two parameters
         *      i) the current method and
         *     ii) the current object (type of object is local always)
         * Our objective is to find out that from all the places from which this method is can be
         * called and if we inline the current method there then can this object be stack
         * allocated on the caller's stack or not.
         */
        // Get all the callers of this method
        System.out.println(" == Inline Checking == ");
        System.out.println("Current Method and Object: "+ key.toString() + obj.toString());
        CallGraph cg = Scene.v().getCallGraph();
        Iterator<Edge> it = cg.edgesInto(key); // Get all the methods that call the current method.
        while (it.hasNext()) {
            HashSet<Integer> setofbj = new HashSet<>();
            Edge edge = it.next();

            // get the context (Remember: Context is a tuple consisting of method-name and bci)
            CallSite c = new CallSite(edge.src(), utils.getBCI.get(edge.srcUnit()));
            // Get the CV's generated at static analysis time and check if it is of type conditional values
            if (!SpeculativeResolver.inlineSummaries.containsKey(c)) {
                SpeculativeResolver.inlineSummaries.put(c, new HashMap<>());
                SpeculativeResolver.inlineSummaries.get(c).put(key, new HashSet<>());
                //System.out.println("Added Context : "+ c);
            } else if(!SpeculativeResolver.inlineSummaries.get(c).containsKey(key)) {
                SpeculativeResolver.inlineSummaries.get(c).put(key, new HashSet<>());
                //System.out.println("Added Method " + key + " in context "+ c);
            }
            if (SpeculativeResolver.allcvs.containsKey(key)) {
                if (SpeculativeResolver.allcvs.get(key).containsKey(obj)) {
                    // Want to process argument dependency first.
                    List<EscapeState> sortedorderlist = sortedorder(key, obj);
                    for (EscapeState e : sortedorderlist) {
                        boolean flag = true;
                        if (e instanceof ConditionalValue) {
                            // Only go ahead if the CV type is argument which means the object depends upon the formal parameter
                            if (((ConditionalValue) e).object.type == ObjectType.argument) {
                                //System.out.println("Reached inside Argument");
                                flag = CheckEscapeStatus(key, obj);
                                if (flag) {
                                    //get the corresponding parameter
                                    List<ObjectNode> objs = SpeculativeResolver.GetObjects(edge.srcUnit(), ((ConditionalValue) e).object.ref, edge.src(), ((ConditionalValue) e).fieldList);
                                    //tmpobj= getParameterObject(key, obj, e) ;
                                    if(objs != null) {
                                        for (ObjectNode tmpobj : objs) {
                                            System.out.println("TmpObj is  : " + tmpobj + "from : " + edge.src());
                                            boolean tmpflag = true;
                                            // Check if the contextual summaries is present in the map
                                            if (SpeculativeResolver.PassedCallsiteValues2.containsKey(edge.src())) {
                                                if (SpeculativeResolver.PassedCallsiteValues2.get(edge.src()).containsKey(tmpobj)) {
                                                    // Now for each context for this object find out the status
                                                    //System.out.println("No parameter or parameter doesn't escape :  inside Argument");
                                                    for (ContextualEscapeStatus ces : SpeculativeResolver.PassedCallsiteValues2.get(edge.src()).get(tmpobj)) {
                                                        if (ces.cescapestat.containsKey(c)) {
                                                            EscapeState es = ces.cescapestat.get(c);
                                                            // Check for the escape status if it doesnot escape in the context then only add to the set of object
                                                            //System.out.println("Escape state value : " + es);
                                                            if (es instanceof NoEscape && !CheckGlobalEscape(key, obj)) {
                                                                // For the first time the context is being added to the inline summaries
                                                                // create a new entry for this context and this object as the first object in the set
                                                                SpeculativeResolver.inlineSummaries.get(c).get(key).add(obj.ref);
                                                                System.out.println("Added object : " + obj + " inside argument");
                                                                tmpflag = false;
                                                            } else {
                                                                // Suppose earlier when the object was not escaping we added the object in list for a particular context now it escapes
                                                                // We need to remove the object form the list
                                                                if (SpeculativeResolver.inlineSummaries.get(c).containsKey(key) &&
                                                                        SpeculativeResolver.inlineSummaries.get(c).get(key).contains(obj.ref)) {
                                                                    System.out.println("1. Deleting object : "+ obj  +" inside argument");
                                                                    SpeculativeResolver.inlineSummaries.get(c).get(key).remove(obj.ref);
                                                                }
                                                                tmpflag = false;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            if (tmpflag) {
//                                                System.out.println("Contextual Summary: "+ SpeculativeResolver.PassedCallsiteValues.toString());
                                                if (SpeculativeResolver.MergedSummaries.containsKey(edge.src())) {
                                                    if (SpeculativeResolver.MergedSummaries.get(edge.src()).containsKey(tmpobj)) {
                                                        // Now for each context for this object find out the status
                                                        //System.out.println("No parameter or parameter doesn't escape :  inside Argument");
                                                        for (EscapeState es : SpeculativeResolver.MergedSummaries.get(edge.src()).get(tmpobj).status) {
                                                            if (es instanceof NoEscape && !CheckGlobalEscape(key, obj)) {
                                                                // For the first time the context is being added to the inline summaries
                                                                // create a new entry for this context and this object as the first object in the set
                                                                SpeculativeResolver.inlineSummaries.get(c).get(key).add(obj.ref);
                                                                System.out.println("Added object : " + obj + " inside argument");
                                                            } else {
                                                                // Suppose earlier when the object was not escaping we added the object in list for a particular context now it escapes
                                                                // We need to remove the object form the list
                                                                if (SpeculativeResolver.inlineSummaries.get(c).containsKey(key) &&
                                                                        SpeculativeResolver.inlineSummaries.get(c).get(key).contains(obj.ref)) {
                                                                    System.out.println("2. Deleting object : "+ obj  +" inside argument");
                                                                    SpeculativeResolver.inlineSummaries.get(c).get(key).remove(obj.ref);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                } else {
                                    if (SpeculativeResolver.inlineSummaries.get(c).containsKey(key)) {
                                        if(SpeculativeResolver.inlineSummaries.get(c).get(key).contains(obj.ref)) {
                                            System.out.println("3. Deleting object : "+ obj  +" outside");
                                            SpeculativeResolver.inlineSummaries.get(c).get(key).remove(obj.ref);
                                        }
                                    }
                                }
                            }
                            // Similarly check for the return dependency
                            else if (((ConditionalValue) e).object.type == ObjectType.returnValue) {
                                /* First checking in case of return dependency if object is passed as argument to other function
                                 * if the object has parameter dependency and doesn't escape in the called method the flag = ture;
                                 * else flag will be false.
                                 */
                                flag = CheckEscapeStatus(key, obj);
                                //System.out.println("Reached inside return with flag "+ flag);
                                if (flag && !CheckGlobalEscape(key, obj)) {
                                        SpeculativeResolver.inlineSummaries.get(c).get(key).add(obj.ref);
                                        System.out.println("Added object : "+ obj  +" inside return");
                                } else {
                                    if (SpeculativeResolver.inlineSummaries.get(c).containsKey(key) &&
                                            SpeculativeResolver.inlineSummaries.get(c).get(key).contains(obj.ref)) {
                                        System.out.println("Deleting object : "+ obj  +" inside return");
                                        SpeculativeResolver.inlineSummaries.get(c).get(key).remove(obj.ref);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static ObjectNode getParameterObject(SootMethod key, EscapeState e) {
        if(SpeculativeResolver.allcvs.containsKey(key)) {
            for (ObjectNode o : SpeculativeResolver.allcvs.get(key).keySet()) {
                if(e instanceof ConditionalValue) {
                    if(o.type == ObjectType.parameter && ((ConditionalValue) e).object.ref == o.ref) {
                        return o;
                    }
                }
            }
        }
        return null;
    }

    public static boolean CheckEscapeStatus(SootMethod key, ObjectNode obj) {
        for (EscapeState es : SpeculativeResolver.allcvs.get(key).get(obj).status) {
            if (es instanceof ConditionalValue) {
                if (((ConditionalValue) es).object.type == ObjectType.parameter) {
                    if (SpeculativeResolver.MergedSummaries.containsKey(((ConditionalValue) es).getMethod())) {
                            if (!SpeculativeResolver.MergedSummaries.get(((ConditionalValue) es).getMethod()).isEmpty()) {
                                //ObjectNode oj = new ObjectNode(((ConditionalValue) es).object.ref, ObjectType.parameter);
                                ObjectNode oj = getParameterObject(((ConditionalValue) es).getMethod(), es);
                                if (SpeculativeResolver.MergedSummaries.get(((ConditionalValue) es).getMethod()).containsKey(oj) &&
                                        SpeculativeResolver.MergedSummaries.get(((ConditionalValue) es).getMethod()).get(oj).doesEscape()) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        return true;
    }

    public static boolean CheckGlobalEscape(SootMethod key, ObjectNode obj) {
        if (SpeculativeResolver.allcvs.get(key).containsKey(obj)) {
            for (EscapeState e : SpeculativeResolver.allcvs.get(key).get(obj).status) {
                boolean flag = true;
                if (e instanceof ConditionalValue) {
                     if(((ConditionalValue) e).object.type == ObjectType.global)
                         return true;
                }
            }
        }
        return false;
    }
    public static boolean CheckFieldEscape(SootMethod key, ObjectNode obj) {
        if (SpeculativeResolver.allcvs.get(key).containsKey(obj)) {
            for (EscapeState e : SpeculativeResolver.allcvs.get(key).get(obj).status) {
                boolean flag = true;
                if (e instanceof ConditionalValue) {
                    if(((ConditionalValue) e).object.type == ObjectType.argument)
                        return true;
                }
            }
        }
        return false;
    }

    public static List<EscapeState> sortedorder(SootMethod key, ObjectNode obj) {
        List<EscapeState> sortedmap = new ArrayList<>();
        EscapeStatus status = SpeculativeResolver.allcvs.get(key).get(obj);

        for (EscapeState es : status.status) {
            if (es instanceof ConditionalValue) {
                if (((ConditionalValue) es).object.type == ObjectType.argument) {
                    sortedmap.add(es);
                }
            }
        }
        for (EscapeState es : status.status) {
            if (es instanceof ConditionalValue) {
                if (((ConditionalValue) es).object.type == ObjectType.returnValue) {
                    sortedmap.add(es);
                }
            }
        }
        return sortedmap;
    }

}
