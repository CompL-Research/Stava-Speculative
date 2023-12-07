package resolver;

import Inlining.InlineCheck;
import config.StoreEscape;
import es.*;
import ptg.ObjectNode;
import ptg.ObjectType;
import ptg.PointsToGraph;
import handlers.*;
import soot.SootField;
import soot.SootMethod;
import soot.Scene;
import soot.Unit;
import soot.Value;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
// import javafx.util.Pair;
import java.util.*;

import static java.lang.System.exit;
/*
 *
 * Some notes:
 * 1. We just need to mark object in the function returned as escaping and nothing more, if some object is coming from
 *      callee then it should be marked as escaping in the callee. It is not the responsiblity of caller.
 *
 */

class StandardObject3 {
    private SootMethod method;
    private ObjectNode obj;

    public StandardObject3(SootMethod m, ObjectNode o){
        this.method = m;
        this.obj = o;
    }
    public SootMethod getMethod() {
        return this.method;
    }
    public ObjectNode getObject() {
        return this.obj;
    }
    public String toString() {
        return "("+method+","+obj+")";
    }
}

public class ContextualResolverOriginal {
    public static Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> existingSummaries; // map for storing the static analysis result
    public static Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> allcvs = new HashMap<>();
    public static Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> solvedSummaries; // map for storing the final resolved result
    public Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> previoussolvesummaries = new HashMap<>();
    public static Map<SootMethod, HashMap<ObjectNode, List<ContextualEscapeStatus>>> solvedContextualSummaries;
    public Map<SootMethod, HashMap<ObjectNode, StandardObject3>> objMap;
    HashMap<SootMethod, HashMap<ObjectNode, ResolutionStatus>> resolutionStatus;
    public static Map<CallSite, HashMap<SootMethod, HashSet<Integer>>> inlineSummaries;
    Map<SootMethod, PointsToGraph> ptgs; // Stores the points to graph for each method
    Map<StandardObject3, Set<StandardObject3>> graph;
    Map<StandardObject3, Set<StandardObject3>> revgraph;
    List<SootMethod> noBCIMethods;
    List<StandardObject3> reverseTopoOrder;
    boolean debug = false;
    public static boolean printflag = true;
    int i = 0;
    int j = 0;

    public ContextualResolverOriginal(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> existingSummaries,
                              Map<SootMethod, PointsToGraph> ptgs,
                              List<SootMethod> escapingMethods) {
        // We get three things from static analysis
        // 1. The summaries (Escape Statuses)
        // 2. Points to graph
        // 3. Methods which do not have bci

        for (Map.Entry<SootMethod, HashMap<ObjectNode, EscapeStatus>> ent : existingSummaries.entrySet()) {
            if (!ent.getKey().isJavaLibraryMethod()) {
                allcvs.put(ent.getKey(), new HashMap<>());
                previoussolvesummaries.put(ent.getKey(), new HashMap<>());
                for (Map.Entry<ObjectNode, EscapeStatus> entry : existingSummaries.get(ent.getKey()).entrySet()) {
                    allcvs.get(ent.getKey()).put(entry.getKey(), new EscapeStatus());
                    previoussolvesummaries.get(ent.getKey()).put(entry.getKey(), new EscapeStatus());
                    for (EscapeState e : existingSummaries.get(ent.getKey()).get(entry.getKey()).status) {
                        allcvs.get(ent.getKey()).get(entry.getKey()).status.add(e);
                        previoussolvesummaries.get(ent.getKey()).get(entry.getKey()).status.add(e);
                    }
                }
            }
        }
        ContextualResolver.existingSummaries = existingSummaries;
        this.ptgs = ptgs;
        this.noBCIMethods = escapingMethods;

        /*
         * Debug Code
         */
        if (debug) {
            System.out.println(" 1. SUMMARIES : STATIC ANALYSIS");
            for (SootMethod m : existingSummaries.keySet()) {
                if (!m.isJavaLibraryMethod()) {
                    System.out.println("Method : " + m);
                    for (ObjectNode o : existingSummaries.get(m).keySet()) {
                        System.out.println(" For object : " + o);
                        System.out.println(" Summaries : ");
                        System.out.println(existingSummaries.get(m).get(o).status);
                    }
                    System.out.println("----------");
                }
            }
            //System.out.println(this.existingSummaries);
            System.out.println("***************************************");
            System.out.println(" 2. POINTS TO GRAPH : STATIC ANALYSIS");
            for (SootMethod method : this.ptgs.keySet()) {
                if (!method.isJavaLibraryMethod()) {
                    System.out.println("Method : " + method);
                    System.out.println("Points to graph : ");
                    System.out.println(this.ptgs.get(method).toString());
                }
            }
            System.out.println("----------");
        }

        //Initializing the maps
        this.objMap = new HashMap<>();
        solvedSummaries = new HashMap<>();
        solvedContextualSummaries = new HashMap<>();
        this.resolutionStatus = new HashMap<>();
        inlineSummaries = new HashMap<>();
        this.reverseTopoOrder = new ArrayList<>();
        this.graph = new HashMap<>();
        this.revgraph = new HashMap<>();

        for (Map.Entry<SootMethod, HashMap<ObjectNode, EscapeStatus>> entry : existingSummaries.entrySet()) {
            SootMethod method = entry.getKey();
            HashMap<ObjectNode, EscapeStatus> map = entry.getValue();
            HashMap<ObjectNode, ResolutionStatus> q = new HashMap<>();
            HashMap<ObjectNode, StandardObject3> tobj = new HashMap<>();
            for (Map.Entry<ObjectNode, EscapeStatus> e : map.entrySet()) {
                ObjectNode obj = e.getKey();
                q.put(obj, ResolutionStatus.UnAttempted);
                StandardObject3 x = new StandardObject3(method, obj);
                tobj.put(obj, x);
                this.graph.put(x, new HashSet<>());
                this.revgraph.put(x, new HashSet<>());
            }
            resolutionStatus.put(method, q);
            this.objMap.put(method, tobj);
            solvedSummaries.put(method, new HashMap<>());
            solvedContextualSummaries.put(method, new HashMap<>());

        }
        /*
         * Next, we traverse all function calls and add mapping from caller to the
         * objects passed. We are just moving towards inter-procedural resolution :P
         *
         */
        AddCallerSummaries();

        for (CallSite c : inlineSummaries.keySet()) {
            if (!c.methodName.isJavaLibraryMethod()) {
                if (!inlineSummaries.get(c).isEmpty()) {
                    for (SootMethod s : inlineSummaries.get(c).keySet()) {
                        //for(SootMethod sm: solvedSummaries.get())
                        ArrayList<Integer> arr = new ArrayList<>();
                        for (Map.Entry<ObjectNode, EscapeStatus> entry : solvedSummaries.get(s).entrySet()) {
                            ObjectNode obj = entry.getKey();
                            if (obj.type != ObjectType.internal)
                                continue;
                            EscapeStatus es = entry.getValue();
                            if (es.containsNoEscape()) arr.add(obj.ref);
                        }
                        for (Integer i : arr) {
                            inlineSummaries.get(c).get(s).add(i);
                        }
                    }
                }
            }
        }



        /*
         * Debug Code for printing the final result
         */

        if (debug) {
            System.out.println("Inline Summaries");
            for (CallSite c : inlineSummaries.keySet()) {
                if (!c.methodName.isJavaLibraryMethod()) {
                    if (!inlineSummaries.get(c).isEmpty()) {
                        System.out.println("CallSite  : <" + c.methodName + "," + c.BCI + ">");
                        for (SootMethod s : inlineSummaries.get(c).keySet()) {
                            System.out.println(s + " can be inlined at bci " + c.BCI);
                            for (Integer i2 : inlineSummaries.get(c).get(s)) {
                                System.out.print(" with objects : " + i2);
                            }
                            System.out.println("");
                        }
                        System.out.println("");
                    }
                }
            }
            System.out.println(" **************FINAL RESULT ***************** ");
            for (SootMethod s : solvedSummaries.keySet()) {
                if (!s.isJavaLibraryMethod()) {
                    System.out.println("For method " + s);
                    for (ObjectNode o : solvedSummaries.get(s).keySet()) {
                        System.out.println("Object is " + o + " and its summary is " + solvedSummaries.get(s).get(o));
                    }
                }
            }
        }
    }

    void printGraph(Map<StandardObject3, Set<StandardObject3>> graph) {
        System.out.println("Printing graph: ");
        for (StandardObject3 u : graph.keySet()) {
            System.out.print(u + ": ");
            for (StandardObject3 v : graph.get(u)) {
                System.out.print(v + ",");
            }
            System.out.println();
        }
        System.out.println();
    }

    // Convert all <caller,<argument,x>> statements to the actual caller functions and replace <argument,x>
    // to parameter passed.

    void AddCallerSummaries() {
        ArrayList<SootMethod> listofMethods = new ArrayList<>();
        CallGraph cg = Scene.v().getCallGraph();
//        System.out.println("Callgraph : "+ cg);
        CallGraphSCC cgscc = new CallGraphSCC(cg);
        //System.out.println("Call Graph : "+ cgscc.order);
        if (debug) System.out.println("List of methods to be analyzed: ");
        i = 0;
        for (Set<SootMethod> scc : cgscc.order()) {
            for (SootMethod m : scc) {
                if (!m.isJavaLibraryMethod()) {
                    System.out.println(++i + ". " + m);
                    listofMethods.add(m);
                }
            }
        }
        while (!listofMethods.isEmpty()) {
            ArrayList<SootMethod> tmpWorklistofMethods = new ArrayList<>();
            for (SootMethod key : listofMethods) {
//                if (!(key.isJavaLibraryMethod() || key.getName().startsWith("java.") || key.getName().startsWith("jdk.") ||
//                        key.getName().startsWith("sun.") || key.getName().startsWith("org.") || key.getName().startsWith("com.") ||
//                        key.getName().startsWith("javax."))) {
                System.out.println(" ********  Resolving Method: " + ++j + "." + key + "  ******** ");
                HashMap<ObjectNode, EscapeStatus> methodInfo = existingSummaries.get(key);
                HashMap<ObjectNode, EscapeStatus> solvedMethodInfo = this.solvedSummaries.get(key);
                if (!existingSummaries.containsKey(key)) {
                    continue;
                }
                //printflag = !key.isJavaLibraryMethod();
                if (!methodInfo.isEmpty()) {
                    for (ObjectNode obj : methodInfo.keySet()) {
                        boolean callercontext = false;
                        HashSet<EscapeStatus> allresolvedstatusforthisobject = new HashSet<>();
                        EscapeStatus status = methodInfo.get(obj);
                        HashSet<EscapeState> newStates = new HashSet<>();
                        for (EscapeState state : status.status) {
                            HashSet<EscapeStatus> resolvedStatuses = new HashSet<>();
                            if (printflag) {
                                System.out.println(" \nCurrent method is : " + key + "  and  Object : " + obj);
                                System.out.println("  Conditional value for object is : " + state);
                            }
                            if (state instanceof ConditionalValue) {
                                ConditionalValue cstate = (ConditionalValue) state;
                                if (cstate.object.type != ObjectType.argument) {
                                    newStates.add(state);
                                    // Handling parameter dependencies of type < Class:Methodname, <parameter, number> >
                                    if (cstate.object.type == ObjectType.parameter) {
                                        if (printflag) System.out.println("  CV is of type parameter");
                                        SootMethod sm = cstate.getMethod(); // the method on which this CV depends
                                        ObjectNode o = cstate.object;  // the object on which this current object (obj) depends
                                        if (printflag)
                                            System.out.println("  Sent to Method : " + sm + " and object as : " + o);
                                        if (this.solvedSummaries.get(sm) != null) {
                                            if (this.solvedSummaries.get(sm).get(o) != null) {
                                                if (this.solvedSummaries.get(sm).get(o).doesEscape()) {
                                                    //System.out.println("  CV ESCAPE DIRECTLY");
                                                    this.solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                                                } else if (this.solvedSummaries.get(sm).get(o).containsNoEscape()) {
                                                    //System.out.println("  CV DOES NOT ESCAPE");
                                                    this.solvedSummaries.get(key).put(obj, new EscapeStatus(NoEscape.getInstance()));
                                                }
                                            }
                                        }
                                        allresolvedstatusforthisobject.add(this.solvedSummaries.get(key).get(obj));
                                        if (printflag)
                                            System.out.println("  Object is : " + obj + " and its Summary is : " + solvedSummaries.get(key).get(obj));
                                        continue;
                                    } else if (cstate.object.type == ObjectType.returnValue) {
                                        if (printflag) System.out.println("  CV is of type return");
                                        SootMethod sm = cstate.getMethod();
                                        ObjectNode o = cstate.object;
                                        if (printflag)
                                            System.out.println("  returned from method : " + sm + " and object as : " + o);
                                        if (sm.equals(key)) {
                                            this.solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                                            CallGraph c = Scene.v().getCallGraph();
                                            Iterator<Edge> itr = c.edgesInto(key);
                                            while (itr.hasNext()) {
                                                Edge e = itr.next();
                                                if (existingSummaries.containsKey(e.src())) {
                                                    for (ObjectNode object : existingSummaries.get(e.src()).keySet()) {
                                                        Iterator<EscapeState> it = existingSummaries.get(e.src()).get(object).status.iterator();
                                                        while (it.hasNext()) {
                                                            EscapeState es = it.next();
                                                            if (es instanceof ConditionalValue) {
                                                                if (((ConditionalValue) es).object.type == ObjectType.returnValue &&
                                                                        ((ConditionalValue) es).getMethod().equals(key) &&
                                                                        ((ConditionalValue) es).object.ref == cstate.object.ref) {
                                                                    //System.out.println("  In method : "+ e.src() + " for object : "+ object.toString());
                                                                    //System.out.println("  CV is :"+ es.toString());
                                                                    if (solvedSummaries.get(key).get(obj).doesEscape()) {
                                                                        if (solvedSummaries.get(e.src()).containsKey(object)) {
                                                                            solvedSummaries.get(e.src()).get(object).setEscape();
                                                                            it.remove();
                                                                        }
                                                                    } else {
                                                                        it.remove();
                                                                        if (printflag)
                                                                            System.out.println("  Deleting the CV : " + e.toString() + " in class : " + e.src() + " for object : " + object);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            //System.out.println("  Never Reached");
                                            this.solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                                        }
                                        allresolvedstatusforthisobject.add(this.solvedSummaries.get(key).get(obj));
                                        if (printflag)
                                            System.out.println("  Object is : " + obj + " and its Summary is : " + solvedSummaries.get(key).get(obj));
                                        // checking for inline
                                        continue;
                                    }
                                    if (printflag) System.out.println("Not Handled");
                                    continue;
                                }
                                int parameternumber = cstate.object.ref;

                                if (StoreEscape.MarkStoreEscaping && StoreEscape.ReduceParamDependence) {
                                    newStates.add(state);
                                    continue;
                                }
                                if (parameternumber < 0) {
                                    newStates.add(state);
                                    continue;
                                }
                                // Get all the callers of the current method (current method: "Key" which define the object having caller, arg dependency)
                                Iterator<Edge> iter = cg.edgesInto(key);
                                this.solvedContextualSummaries.get(key).put(obj, new ArrayList<>());
                                i = 0;
                                while (iter.hasNext()) {
                                    ContextualEscapeStatus ctemp = new ContextualEscapeStatus();
                                    ctemp.cescapestat = new HashMap<>();
                                    boolean already_es_flag = false;
                                    callercontext = true;
                                    HashSet<EscapeState> newStates2 = new HashSet<>();
                                    newStates2.add(state);
                                    parameternumber = cstate.object.ref;
                                    Edge edge = iter.next();
                                    if (printflag) {
                                        System.out.println(" \n " + ++i + ". Called from : " + edge.src().getName());
                                    }
                                    // System.out.println(key+" "+obj+" "+cstate+" " + +parameternumber + " "+edge.src() );
                                    // System.out.println("Edge type:" + edge.kind() + " " + key+ " "+edge.srcUnit()+" "+edge.src());
                                    if (parameternumber >= 0) {
                                        if (edge.kind() == Kind.REFL_CONSTR_NEWINSTANCE) {
                                            parameternumber = 0;
                                        } else if (edge.kind() == Kind.REFL_INVOKE) {
                                            parameternumber = 1;
                                        }
                                    }
                                    List<ObjectNode> objects;
                                    try {
                                        objects = GetObjects(edge.srcUnit(), parameternumber, edge.src(), cstate.fieldList);
                                    } catch (Exception e) {
                                        System.out.println("Cond: " + cstate + " " + cstate.object + " " + cstate.object.ref + " " + parameternumber);
                                        throw e;
                                    }
                                    if (objects == null) {
                                        System.err.println("  Objects are null!.");
                                    } else {
                                        // We got the objects mapped to this
                                        for (ObjectNode x : objects) {
                                            if (printflag) {
                                                System.out.println("  Object Received is :" + x);
                                                System.out.println("  Escape status of " + x + " is " + existingSummaries.get(edge.src()).get(x));
                                            }
                                            // If the mapped object is already marked as ESCAPE no need to proceed forward
                                            if (existingSummaries.get(edge.src()) != null) {
                                                if (existingSummaries.get(edge.src()).get(x) != null) {
                                                    if (existingSummaries.get(edge.src()).get(x).doesEscape()) {
                                                        CallSite c = new CallSite();
                                                        //System.out.println("  Reached where already the mapped object is resolved to Escaping ");
                                                        this.solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                                                        c.methodName = edge.src();
                                                        c.BCI = utils.getBCI.get(edge.srcUnit());
                                                        solvedContextualSummaries.get(key).get(obj).add(new ContextualEscapeStatus(c, Escape.getInstance()));
                                                        already_es_flag = true;
                                                        solvedMethodInfo.put(obj, new EscapeStatus(Escape.getInstance()));
                                                    }
                                                }
                                            }
                                            if (this.solvedSummaries.get(edge.src()) != null) {
                                                if (this.solvedSummaries.get(edge.src()).get(x) != null) {
                                                    if (printflag)
                                                        System.out.println("Value of solved summaries : " + this.solvedSummaries.get(edge.src()).get(x).status);
                                                    if (this.solvedSummaries.get(edge.src()).get(x).doesEscape()) {
                                                        CallSite c = new CallSite();
                                                        //System.out.println("  Reached where already the mapped object is resolved to Escaping ");
                                                        this.solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                                                        c.methodName = edge.src();
                                                        c.BCI = utils.getBCI.get(edge.srcUnit());
                                                        solvedContextualSummaries.get(key).get(obj).add(new ContextualEscapeStatus(c, Escape.getInstance()));
                                                        already_es_flag = true;
                                                        solvedMethodInfo.put(obj, new EscapeStatus(Escape.getInstance()));
                                                    }
                                                }
                                            } else {
                                                newStates2.add(CreateNewEscapeState(x, cstate, edge.src()));
                                            }
                                        }
                                    }
                                    // If object has CV
                                    if (!already_es_flag) {
                                        solvedMethodInfo.put(obj, new EscapeStatus());
                                        solvedMethodInfo.get(obj).status = newStates2;
                                        solvedMethodInfo.put(obj, new EscapeStatus(NoEscape.getInstance()));
                                        //GenerateGraphFromSummary();
                                        //System.out.println("After Generate Graph from Summary value for Obj : " + obj + " is " + this.solvedSummaries.get(key).get(obj));
                                        //FindSCC(key, obj);
                                        //resolve(newStates2);
                                        if (printflag)
                                            System.out.println("  After \"FindSCC\" method call : resolved value for Obj : " + obj + " is " + this.solvedSummaries.get(key).get(obj));
                                    }
                                    resolvedStatuses.add(solvedSummaries.get(key).get(obj));
                                        /* This code deletes the dependency in the caller method for that particular object
                                        for eg: foo() {                             bar(p1) {
                                                  bar(a);  // o1 doesn't escape          p1 doesn't escape for o1 but escape for o2
                                                  bar(b);  // o2 doesn't escape                 ....
                                                }                                   }
                                        o1 and o2 will have <bar, parameter,0> dependency whereas p1 will have <caller,arg> dependency.
                                        Now as the escape status for p1 in context o1 should get reflected to o1, similarly for o2,
                                        So if p1 resolves to ESCAPE in o2 case just make the corresponding object in the caller also ESCAPE as other
                                        dependencies for that object doesn't matter, but if p1 DOESNOT ESCAPE we delete the corresponding CV in the caller.
                                        For eg: in foo we will be deleting the <bar, parameter,0> for o1 but marking ESCAPE for o2.
                                        */
                                    //deleting the dependency for this call-site/context
                                    if (objects != null) {
                                        // For all the objects in the source method
                                        for (ObjectNode o : existingSummaries.get(edge.src()).keySet()) {
                                            // Iterate over all the statuses of the object.
                                            Iterator<EscapeState> itr = existingSummaries.get(edge.src()).get(o).status.iterator();
                                            while (itr.hasNext()) {
                                                EscapeState e = itr.next();
                                                if (e instanceof ConditionalValue) {
                                                    // If the status is of type parameter
                                                    if (((ConditionalValue) e).object.type == ObjectType.parameter &&
                                                            ((ConditionalValue) e).getMethod().equals(key) &&
                                                            ((ConditionalValue) e).object.ref == cstate.object.ref) {
                                                        // If the object does not have field in it
                                                        if (cstate.fieldList == null && ((ConditionalValue) e).fieldList == null) {
                                                            //System.out.println("Reaching in non-field one");
                                                            // if the current object does not escape then delete the corresponding status in the caller's object.
                                                            if (!solvedSummaries.get(key).get(obj).doesEscape()) {
                                                                itr.remove();
                                                                if (printflag)
                                                                    System.out.println("  Deleting the CV : " + e.toString() + " in class : " + edge.src() + " for object : " + o);
                                                            } else if (solvedSummaries.get(key).get(obj).doesEscape()) {
                                                                // if the object escapes directly store esacping for the caller object as well.
                                                                solvedSummaries.get(edge.src()).put(o, new EscapeStatus(Escape.getInstance()));
                                                                existingSummaries.get(edge.src()).put(o, new EscapeStatus(Escape.getInstance()));
                                                                if (printflag)
                                                                    System.out.println("  Deleting the CV : " + e.toString() + " in class : " + edge.src() + " for object : " + o);
                                                            }
                                                        } else if (cstate.fieldList != null && ((ConditionalValue) e).fieldList != null) {
                                                            if (ConditionalValue.comaparefieldlist(cstate.fieldList, ((ConditionalValue) e).fieldList)) {
                                                                //System.out.println("Reaching in field one");
                                                                // if the current object does not escape then delete the corresponding status in the caller's object.
                                                                if (!solvedSummaries.get(key).get(obj).doesEscape()) {
                                                                    itr.remove();
                                                                    if (printflag)
                                                                        System.out.println("  Deleting the CV : " + e.toString() + " in class : " + edge.src() + " for object : " + o);
                                                                } else if (solvedSummaries.get(key).get(obj).doesEscape()) {
                                                                    // if the object escapes directly store esacping for the caller object as well.
                                                                    solvedSummaries.get(edge.src()).put(o, new EscapeStatus(Escape.getInstance()));
                                                                    existingSummaries.get(edge.src()).put(o, new EscapeStatus(Escape.getInstance()));
                                                                    if (printflag)
                                                                        System.out.println("  Deleting the CV : " + e.toString() + " in class : " + edge.src() + " for object : " + o);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // debug code
                                    for (SootMethod m : solvedSummaries.keySet()) {
                                        if (!m.isJavaLibraryMethod() && m.equals(key)) {
                                            //System.out.println(" Current Method : " + m);
                                            if (printflag)
                                                System.out.println("  Object is : " + obj + " and its current summary is : " + this.solvedSummaries.get(m).get(obj));
                                        }
                                    }
                                    // Storing the context for each call-site
                                    CallSite c = new CallSite();
                                    c.methodName = edge.src();
                                    c.BCI = utils.getBCI.get(edge.srcUnit());
                                    if (solvedSummaries.get(key).get(obj).containsNoEscape()) {
                                        if (ctemp.cescapestat != null) {
                                            ctemp.cescapestat.put(c, NoEscape.getInstance());
                                            if (!solvedContextualSummaries.get(key).get(obj).contains(ctemp))
                                                solvedContextualSummaries.get(key).get(obj).add(ctemp);
                                            //System.out.println("  Stored : for method "+ key + ctemp.cescapestat.toString());
                                        }
                                    } else {
                                        if (ctemp.cescapestat != null) {
                                            ctemp.cescapestat.put(c, Escape.getInstance());
                                            if (!solvedContextualSummaries.get(key).get(obj).contains(ctemp))
                                                solvedContextualSummaries.get(key).get(obj).add(ctemp);
                                            //System.out.println("Stored : for method "+ key + ctemp.cescapestat.toString());
                                        }
                                    }
                                }
                                // Taking the merge for the call-sites for local object
                                for (EscapeStatus e : resolvedStatuses) {
                                    boolean escapeExitFlag = false;
                                    if (e != null) {
                                        if (e.status.size() != 1) {
                                            if (printflag) System.out.println("Error in size");
                                            exit(-1);
                                        } else {
                                            for (EscapeState es : e.status) {
                                                if (es.equals(Escape.getInstance())) {
                                                    solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                                                    //System.out.println("  Meet for all callsites resolved value for object : " + obj + " Status :" + solvedMethodInfo.get(obj));
                                                    escapeExitFlag = true;
                                                    break;
                                                }
                                            }
                                        }
                                        if (escapeExitFlag) {
                                            break;
                                        }
                                    }
                                }
                            } else {
                                newStates.add(state);
                                if (state instanceof Escape) {
                                    solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                                } else {
                                    solvedMethodInfo.put(obj, new EscapeStatus(NoEscape.getInstance()));
                                }
                                //GenerateGraphFromSummary();
                                //FindSCC(key, obj);

                            }
                            allresolvedstatusforthisobject.add(solvedSummaries.get(key).get(obj));
                        }
                        if (!callercontext) {
                            System.out.println("  Reached where the status was not CV ");
                            solvedMethodInfo.put(obj, new EscapeStatus());
                            solvedMethodInfo.get(obj).status = newStates;
                            //GenerateGraphFromSummary();
                            //FindSCC(key, obj);
                            allresolvedstatusforthisobject.add(solvedSummaries.get(key).get(obj));
                            System.out.println("  Value for object : " + obj + " Status :" + solvedMethodInfo.get(obj));
                        }
                        // Take a meet for the case when multiple CV's are there and one all may different types (parm,ret and caller<arg>)
                        for (EscapeStatus es : allresolvedstatusforthisobject) {
                            if (es != null) {
                                for (EscapeState e : es.status) {
                                    if (e.equals(Escape.getInstance())) {
                                        solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                                        System.out.println("  Meet for all the CV's  : " + solvedSummaries.get(key).get(obj).status);
                                        break;
                                    } else {
                                        System.out.println("Reaching here");
                                        for (EscapeState esp : solvedSummaries.get(key).get(obj).status) {
                                            if (esp instanceof ConditionalValue) {
                                                if (((ConditionalValue) esp).object.type == ObjectType.parameter) {
                                                    solvedSummaries.get(key).put(obj, new EscapeStatus(NoEscape.getInstance()));
                                                    System.out.println("  Meet for all the CV's  : " + solvedSummaries.get(key).get(obj).status);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // Find the methods which need to be reanalyzed due to change in caller method
                        // (object passed as parameter might be escaping now)
                        if (solvedMethodInfo.containsKey(obj) && previoussolvesummaries.containsKey(key) && previoussolvesummaries.get(key).containsKey(obj)) {
                            if (!solvedMethodInfo.get(obj).status.equals(previoussolvesummaries.get(key).get(obj).status)) {
                                for (EscapeState e : allcvs.get(key).get(obj).status) {
                                    if (e instanceof ConditionalValue) {
                                        ConditionalValue cv = (ConditionalValue) e;
                                        // Add those methods whose parameter value are now escaping
                                        if (cv.object.type == ObjectType.parameter || cv.object.type == ObjectType.returnValue) {
                                            SootMethod sm = cv.getMethod();
                                            if (printflag)
                                                System.out.println("CV of parameter/return type : " + cv.toString());
                                            if (!sm.getName().equals("<init>") && !tmpWorklistofMethods.contains(sm)) {
                                                tmpWorklistofMethods.add(sm);
                                                if (printflag) System.out.println("Added " + sm);
                                            }
                                        }
                                    }

                                }
                            }
                            previoussolvesummaries.get(key).put(obj, solvedMethodInfo.get(obj));
                        }
//                            for(EscapeState es : solvedMethodInfo.get(obj).status) {
//                                if(es instanceof ConditionalValue) {
//                                    if(((ConditionalValue) es).object.type == ObjectType.parameter)
//                                        solvedMethodInfo.put(obj, new EscapeStatus(NoEscape.getInstance()));
//                                }
//                            }

                        // DEBUG CODE
                        if (printflag)
                            System.out.println("  Value for object : " + obj + " Status :" + solvedMethodInfo.get(obj));
                        if (obj.type == ObjectType.internal)
                            InlineCheck.inlineinfo(key, obj);
                    }
                }
                // Update the field status
                PointsToGraph p = this.ptgs.get(key);
//                    System.out.println("Points to graph : " + p.toString());
//                    System.out.println("Fields : "+ p.fields);
                for (ObjectNode o : p.fields.keySet()) {
                    if (solvedMethodInfo.get(o).doesEscape()) {
                        for (SootField f : p.fields.get(o).keySet()) {
                            for (ObjectNode ob : p.fields.get(o).get(f)) {
                                solvedMethodInfo.put(ob, new EscapeStatus(Escape.getInstance()));
                            }
                        }
                    }
                }


                // DEBUG CODE
                if (true) {
                    System.out.println("======= RESOLUTION OF METHOD COMPLETED ======= ");
                    if (true) {
                        System.out.println("---- Resolved Value (SolvedMethodInfo) -------");
                        for (ObjectNode o : solvedMethodInfo.keySet()) {
                            System.out.println("Object " + o + " Escape Status : " + solvedMethodInfo.get(o));
                        }
                        System.out.println("---- Contextual Summaries -------- ");
                        for (SootMethod m : solvedContextualSummaries.keySet()) {
                            if (!m.isJavaLibraryMethod()) {
                                System.out.println("Method : " + m);
                                for (ObjectNode o : solvedContextualSummaries.get(m).keySet()) {
                                    System.out.println("Object : " + o + "Summary : " + solvedContextualSummaries.get(m).get(o).toString());
                                }
                            }
                        }
                        System.out.println("-------- Overall Summaries --------");
                        for (SootMethod m : solvedSummaries.keySet()) {
                            if (!m.isJavaLibraryMethod()) {
                                System.out.println("Method : " + m);
                                for (ObjectNode o : solvedSummaries.get(m).keySet()) {
                                    System.out.println("Object : " + o + "Summary : " + solvedSummaries.get(m).get(o));
                                }
                            }
                        }
                    }
                }
                //}
            }
//            listofMethods.clear();
//            System.out.println();
//            System.out.println("Methods getting re-analyzed are : ");
//            for( SootMethod s: tmpWorklistofMethods) {
//                System.out.println(s);
//                listofMethods.add(s);
//            }
//            tmpWorklistofMethods.clear();

            System.out.println();
            System.out.println("Methods getting re-analyzed ");
            listofMethods.removeIf(s -> !tmpWorklistofMethods.contains(s));
            tmpWorklistofMethods.clear();
        }
    }

    EscapeState CreateNewEscapeState(ObjectNode obj, ConditionalValue state, SootMethod src) {
        return new ConditionalValue(src, obj, state.fieldList, state.isReal);
    }

    List<ObjectNode> GetObjects(Unit u, int num, SootMethod src, List<SootField> fieldList) {
        List<ObjectNode> objs = new ArrayList<>();
        InvokeExpr expr;
        if (u instanceof JInvokeStmt) {
            expr = ((JInvokeStmt) u).getInvokeExpr();
        } else if (u instanceof JAssignStmt) {
            expr = (InvokeExpr) (((JAssignStmt) u).getRightOp());
        } else {
            //System.out.println(u);
            return null;
        }
        Value arg;
        try {
            if (num >= 0)
                arg = expr.getArg(num);
            else if (num == -1 && (expr instanceof AbstractInstanceInvokeExpr))
                arg = ((AbstractInstanceInvokeExpr) expr).getBase();
            else return null;

        } catch (Exception e) {
            System.err.println(u + " " + num + " " + expr);
            CallGraph cg = Scene.v().getCallGraph();
            Iterator<Edge> iter = cg.edgesOutOf(u);
            while (iter.hasNext()) {
                Edge edg = iter.next();
                System.err.println("EXT: " + edg.tgt() + " " + edg.kind());
            }
            throw e;
        }

        if (!(arg instanceof Local))
            return objs;
        else if (((Local) arg).getType() instanceof PrimType)
            return objs;
        try {
            for (ObjectNode o : this.ptgs.get(src).vars.get(arg)) {
                if (fieldList != null) {
                    Set<ObjectNode> obj = new HashSet<>();
                    Set<ObjectNode> obj1 = new HashSet<>();
                    obj1.add(o);
                    for (SootField f : fieldList) {
                        obj = getfieldObject(src, obj1, f);
                        if (obj == null)
                            break;
                        obj1 = obj;
                    }
                    if (obj != null) {
                        objs.addAll(obj);
                        return objs;
                    } else {
                        objs.add(o);
                    }

                } else {
                    objs.add(o);
                }
            }
        } catch (Exception e) {
            System.out.println(src + " " + arg + " " + u + " " + num);
            e.printStackTrace();
            exit(0);
        }
        // System.out.println("Unit: "+u+" : "+objs);
        return objs;
    }

    Set<ObjectNode> getfieldObject(SootMethod src, Set<ObjectNode> obj, SootField f) {
        Set<ObjectNode> tmpobj = new HashSet<>();
        for (ObjectNode o : obj) {
            if (this.ptgs.get(src).fields.containsKey(o)) {
                if (this.ptgs.get(src).fields.get(o).containsKey(f)) {
                    tmpobj = this.ptgs.get(src).fields.get(o).get(f);
                }
            }
        }
        return tmpobj;
    }


    StandardObject3 getSObj(SootMethod method, ObjectNode obj) {
        if (objMap.get(method) == null) {
            objMap.put(method, new HashMap<>());
        }
        StandardObject3 objx = objMap.get(method).get(obj);
        if (objx == null)
            objMap.get(method).put(obj, new StandardObject3(method, obj));

        return objMap.get(method).get(obj);
    }

    void GenerateGraphFromSummary() {
        for (SootMethod key : solvedSummaries.keySet()) {
            HashMap<ObjectNode, EscapeStatus> methodInfo = solvedSummaries.get(key);

            for (ObjectNode obj : methodInfo.keySet()) {
                EscapeStatus status = methodInfo.get(obj);
                Set<StandardObject3> target = new HashSet<>();
                for (EscapeState state : status.status) {
                    if (state instanceof ConditionalValue) {
                        ConditionalValue cstate = (ConditionalValue) state;

                        if (cstate.method != null) {
                            try {
                                /* StandardObject3 objx = getSObj(cstate.method,cstate.object);
                                 * target.add(objx);
                                 * Actually figure out, if above code is sufficient or not.
                                 * If not, use the below one.
                                 * getObjs() actually find all the objects pointed by obj along with
                                 * its fields of obj pointed by conditional value.
                                 */
                                Set<StandardObject3> objx = getObjs(cstate);
                                System.out.println("objx :" + objx);
                                target.addAll(objx);
                                for (StandardObject3 x : objx) {
                                    if (this.graph.get(x) == null) {
                                        this.graph.put(x, new HashSet<>());
                                    }
                                }
                            } catch (Exception e) {
                                // System.err.println(cstate.method+" "+cstate.object);
                                System.err.println("Generated Grpah Summary : " + e);
                                continue;
                            }
                        } else {
                            // System.err.println(key+" "+obj+" Method NULL: "+cstate);
                        }
                    }
                }
                this.graph.put(objMap.get(key).get(obj), target);
            }
        }
//        System.out.println("After ");
//        for(SootMethod s : solvedSummaries.keySet()) {
//            if(!s.isJavaLibraryMethod()) {
//                System.out.println("For method " + s);
//                for (ObjectNode o : solvedSummaries.get(s).keySet()) {
//                    System.out.println("Object is " + o + " and its summary is " + solvedSummaries.get(s).get(o));
//                }
//            }
//        }
//        System.out.println("  Graph : "+ this.graph);


        if (StoreEscape.MarkParamReturnEscaping == false)
            for (StandardObject3 srcobj : this.graph.keySet()) {
                if (srcobj.getObject().type == ObjectType.external)
                    for (StandardObject3 tgtobj : this.graph.get(srcobj)) {
                        if (isReturnedFromDifferentFunction(srcobj, tgtobj)) {
                            HashSet<ObjectNode> retObjs = JReturnStmtHandler.returnedObjects.get(tgtobj.getMethod());
                            if (retObjs == null) continue;
                            for (ObjectNode retobj : retObjs) {
                                this.graph.get(objMap.get(tgtobj.getMethod()).get(retobj)).add(srcobj);
                            }
                            // this.graph.get(tgtobj).add(srcobj);
                        }
                    }
            }

        for (SootMethod method : this.ptgs.keySet()) {
            Map<ObjectNode, Map<SootField, Set<ObjectNode>>> fieldMap = this.ptgs.get(method).fields;
            for (ObjectNode obj : fieldMap.keySet()) {
                StandardObject3 sobj = getSObj(method, obj);

                for (SootField field : fieldMap.get(obj).keySet()) {
                    for (ObjectNode tobj : fieldMap.get(obj).get(field)) {
                        StandardObject3 tsobj = getSObj(method, tobj);
                        this.graph.get(tsobj).add(sobj);
                    }
                }
            }
        }
        /*
         *  Find the objects passed and the respective dummy nodes, and match every field.
         *
         */
        List<StandardObject3[]> toAlter = new ArrayList<>();
        for (StandardObject3 obj1 : this.graph.keySet()) {
            for (StandardObject3 obj2 : this.graph.get(obj1)) {
                if (obj2.getObject().type == ObjectType.parameter) {
                    toAlter.add(new StandardObject3[]{obj1, obj2});
                }
            }
        }

        for (StandardObject3[] obj : toAlter) {
            matchObjs(obj[0], obj[1]);
        }

        for (StandardObject3 key : this.graph.keySet()) {
            for (StandardObject3 val : this.graph.get(key)) {
                if (!this.revgraph.containsKey(val))
                    this.revgraph.put(val, new HashSet<>());
                this.revgraph.get(val).add(key);
            }
        }
        //printGraph(this.graph);
        //printGraph(this.revgraph);
    }

    private void matchObjs(StandardObject3 obj1, StandardObject3 obj2) {
//        if(obj1.getMethod().getName().equals("<init>")){
//            System.out.println("Inside match obj trap");
//        } else {
        try {
            Map<SootField, Set<ObjectNode>> fieldMap1 = this.ptgs.get(obj1.getMethod()).fields.get(obj1.getObject()); // fieldMap of obj1.
            Map<SootField, Set<ObjectNode>> fieldMap2 = null;
            //System.out.println("Object 1 : "+ obj1.getObject() +" "+ obj1.getMethod());
            //System.out.println("Object 2 : "+ obj2.getObject() +" "+ obj2.getMethod());
            if (obj2.getMethod() != null) {
                if (this.ptgs.containsKey(obj2.getMethod())) {
                    fieldMap2 = this.ptgs.get(obj2.getMethod()).fields.get(obj2.getObject()); // fieldMap of obj1.
                }
            }
            if (fieldMap1 == null || fieldMap2 == null)
                return;

            for (SootField f : fieldMap1.keySet()) {
                if (fieldMap1.get(f) == null || fieldMap2.get(f) == null) {
                    continue;
                }
                for (ObjectNode o1s : fieldMap1.get(f)) {
                    for (ObjectNode o2s : fieldMap2.get(f)) {
                        StandardObject3 sobj1 = getSObj(obj1.getMethod(), o1s);
                        StandardObject3 sobj2 = getSObj(obj2.getMethod(), o2s);
                        // System.err.println(sobj1+"-><-"+sobj2);
                        if (this.graph.get(sobj1).contains(sobj2) && this.graph.get(sobj2).contains(sobj1))
                            continue;
                        else {
                            if (!this.graph.get(sobj1).contains(sobj2))
                                this.graph.get(sobj1).add(sobj2);
                            if (!this.graph.get(sobj2).contains(sobj1))
                                this.graph.get(sobj2).add(sobj1);
                            matchObjs(sobj1, sobj2);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Object obj1 : " + obj1 + "Object obj2 : " + obj2);
            System.err.println("MATCH OBJECT" + e);
            System.err.println(obj1 + " " + obj2);
            e.printStackTrace();
            exit(0);
        }
        //}
    }

    // Take a CV
    private Set<StandardObject3> getObjs(ConditionalValue cv) {
        Iterable<ObjectNode> _ret = new LinkedHashSet<ObjectNode>();
        Collection<ObjectNode> c = (Collection<ObjectNode>) _ret;
        // <m, <parameter,0>.f.g> or <m,<returnValue,0>.f.g>
        LinkedList<ObjectNode> workList = new LinkedList<ObjectNode>();
        PointsToGraph ptg;
        // Get the points-to-set for the method on which CV is dependent on.
        ptg = this.ptgs.get(cv.getMethod());
        if (ptg == null || cv.object.equals(new ObjectNode(0, ObjectType.returnValue))) {
//			System.out.println("the method of "+ cv.toString() + " doesn't have a ptg defined!");
            HashSet<StandardObject3> x = new HashSet<>();
            x.add(getSObj(cv.getMethod(), cv.object));
            return x;
//			throw new IllegalArgumentException("the method of "+ cv.toString() + " doesn't have a ptg defined!");
        }
        c.add(cv.object);

        workList.addAll(c);

        LinkedList<ObjectNode> temp;
        LinkedList<ObjectNode> workListNext = new LinkedList<ObjectNode>();
        if (cv.fieldList != null) {
            Iterator<SootField> i = cv.fieldList.iterator();
            while (i.hasNext()) {
                SootField f = i.next();
                Iterator<ObjectNode> itr = workList.iterator();
                while (itr.hasNext()) {
                    ObjectNode o = itr.next();
                    if (ptg.fields.containsKey(o) && ptg.fields.get(o).containsKey(f)) {
                        for (ObjectNode obj : ptg.fields.get(o).get(f)) {
                            if (!c.contains(obj)) c.add(obj);
                        }
                        workListNext.addAll(ptg.fields.get(o).get(f));
                    }
                }
                workList.clear();
                temp = workListNext;
                workListNext = workList;
                workList = temp;
            }
        }
        // return _ret;
        Set<StandardObject3> fnobjs = new HashSet<>();
        for (ObjectNode x : c) {
            fnobjs.add(getSObj(cv.getMethod(), x));
        }
        return fnobjs;
    }

    void FindSCC(SootMethod sm, ObjectNode o) {

        HashMap<StandardObject3, Boolean> used = new HashMap<>();
        List<List<StandardObject3>> components = new ArrayList<>();
        // Here get all the CV on which this unit is directly on indirectly dependent upon
        for (StandardObject3 u : this.graph.keySet()) {
//            if(!u.getMethod().isJavaLibraryMethod()) {
//                System.out.println("FIND SCC : "+ u.toString());
//                System.out.println("FIND SCC 2: "+ used.containsKey(u) + " and "+ used.get(u));
//            }
            if (used.containsKey(u) && used.get(u) == true)
                continue;
            dfs1(u, used);
        }

        used = new HashMap<>();

        for (int i = 0; i < reverseTopoOrder.size(); i++) {
            StandardObject3 u = reverseTopoOrder.get(reverseTopoOrder.size() - 1 - i);
//            if(!u.getMethod().isJavaLibraryMethod()) {
//               System.out.println("Object : "+ u + "in method : "+ u.getMethod().getName() + " and "+ used.containsKey(u) +  " : "+ used.get(u));
//            }
            if (used.containsKey(u) && used.get(u) == true)
                continue;
            List<StandardObject3> component = new ArrayList<>();
            dfs2(u, used, component);
            components.add(component);
            //System.out.println("1. " + u.getMethod() + "2. "+ u.getObject() + "3. " + sm + "4. "+ o.toString());
            //System.out.println("Component :" + component);
            if (u.getMethod().equals(sm) && u.getObject().equals(o)) {
                //System.out.println("Getting here");
                resolve(component);
            }
//          for (StandardObject3 s : component) {
//              if(!s.getMethod().isJavaLibraryMethod()) {
//                  System.out.println("SCC Component :" +component);
//              }
//              if(!s.getMethod().equals(sm)  ) {
//
//              }
//            }
        }
    }

    void dfs1(StandardObject3 u, HashMap<StandardObject3, Boolean> used) {
        // Store this unit
        used.put(u, true);
//        if(!u.getMethod().isJavaLibraryMethod())
//            System.out.println(" In dfs1 : "+ u.toString());
        if (this.revgraph.get(u) != null) {
            for (StandardObject3 v : this.revgraph.get(u)) {
                if (used.containsKey(v) && used.get(v) == true)
                    continue;
                dfs1(v, used);
            }
        }
        reverseTopoOrder.add(u);
    }


    void dfs2(StandardObject3 u, HashMap<StandardObject3, Boolean> used, List<StandardObject3> component) {
        used.put(u, true);
        component.add(u);
        if (this.graph.get(u) != null) {
            for (StandardObject3 v : this.graph.get(u)) {
                if (used.containsKey(v) && used.get(v) == true)
                    continue;
                dfs2(v, used, component);
            }
        }
    }

    boolean isReturnObject(StandardObject3 s) {
        if (s.getObject().equals(new ObjectNode(0, ObjectType.returnValue)))
            return true;
        return false;
    }

    /*
     *  Two issues: First return statements have a function name as
     *  null. and this code simply ignores such statements. - Resolved
     *
     *  How to check code with mutiple return statements.
     *
     *  Second, return statements should be marked as escaping iff object
     *  returned is allocated in that function.
     */
    boolean isEscapingObject(StandardObject3 sobj) {

        HashMap<ObjectNode, EscapeStatus> ess = this.existingSummaries.get(sobj.getMethod());
        //
        //HashMap<ObjectNode, EscapeStatus> ess2 = this.solvedSummaries.get(sobj.getMethod());

        // System.err.println("isEscaping Object: "+ess+" Object: "+sobj);
        if (ess == null)
            return false;

        if (this.noBCIMethods.contains(sobj.getMethod())) {
            //System.out.println("Returned from no bci method");
            return true;
        }

        EscapeStatus es = ess.get(sobj.getObject());
        //EscapeStatus es2 = ess2.get(sobj.getObject());
        // System.err.println("isEscaping Object: "+es+" Object: "+sobj);
        if ((es != null && es.doesEscape())) { //|| (es2 != null && es2.doesEscape())) {
            //System.out.println("es is escaping.");
            // SetComponent(component, Escape.getInstance());
            return true;
        }
        return isAssignedToThis(sobj);
    }

    boolean isReturnedFromDifferentFunction(StandardObject3 sobj, StandardObject3 nxt) {
        if (this.noBCIMethods.contains(nxt.getMethod())) {
            return false;
        }
        if (sobj.getMethod() != nxt.getMethod()) {
            if (isReturnObject(nxt))
                return true;
        }
        return false;
    }

    boolean isAssignedToThis(StandardObject3 sobj) { // Is assigned to this or parameter.
        HashMap<ObjectNode, EscapeStatus> objEs = this.solvedSummaries.get(sobj.getMethod());
        if (objEs == null)
            return false;
        EscapeStatus es = objEs.get(sobj.getObject());

        if (es == null)
            return false;

        if (sobj.getObject().type == ObjectType.parameter || sobj.getObject().type == ObjectType.argument)
            return false;

        // if (sobj.getObject().type != ObjectType.internal)
        //     return false;

        // If any internal object is assigned to the parameter then it is escaping.
        // Whereas, same argument is not valid for external object,
        // But if any external object is assigned to static variable,
        // then it is escaping.

//        if (sobj.getObject().type == ObjectType.internal) {
//            for (EscapeState e : es.status) {
//                if (e instanceof ConditionalValue) {
//                    ConditionalValue cv = (ConditionalValue) e;
//                    if (cv.method == null && cv.object.type == ObjectType.argument )//&& cv.object.ref == -1)
//                        return true;
//                }
//            }
//        }
//        else {
//            for (EscapeState e : es.status) {
//                if (e instanceof ConditionalValue) {
//                    ConditionalValue cv = (ConditionalValue) e;
//                    if (cv.method == null && cv.object.type == ObjectType.argument && cv.object.ref == -1)
//                        return true;
//                }
//            }
//        }
        return false;
    }

    boolean isEscapingParam(StandardObject3 sobj) {
        for (StandardObject3 nxt : this.graph.get(sobj)) {
            if (isReturnObject(nxt))
                continue;
            if (isEscapingObject(nxt))
                return true;
        }
        return false;
    }

    void resolve(List<StandardObject3> component) {
        boolean resolveflag = true;
        List<EscapeState> conds = new ArrayList<>();
        for (StandardObject3 sobj : component) {
//            if(!sobj.getMethod().isJavaLibraryMethod()) {
//                System.out.println("Sobj : "+ sobj.toString());
//            }
            if (sobj.getMethod().isJavaLibraryMethod()) {
                resolveflag = false;
            }
            if (isReturnObject(sobj)) {
                //System.out.println("Identified as return obj: "+sobj);
                SetComponent(component, Escape.getInstance());
                return;
            }
            if (isEscapingObject(sobj)) {
                //System.out.println("Identified as escaping obj: "+sobj);
                SetComponent(component, Escape.getInstance());
                return;
            }
            if (StoreEscape.MarkParamReturnEscaping == false)
                if (sobj.getObject().type == ObjectType.parameter) {
                    if (isEscapingParam(sobj)) {
                        //System.out.println("Identified as escaping param: "+sobj);
                        SetComponent(component, Escape.getInstance());
                        return;
                    }
                    continue;
                }

            for (StandardObject3 nxt : this.graph.get(sobj)) {
                try {
                    if (StoreEscape.MarkParamReturnEscaping == false)
                        if (isReturnedFromDifferentFunction(sobj, nxt)) {
                            //System.out.println("Returned from different func: "+ sobj);
                            continue;
                        }
                    if (isEscapingObject(nxt)) {
                        //System.out.println("Escaping obj: "+nxt);
                        SetComponent(component, Escape.getInstance());
                        return;
                    }
                } catch (Exception e) {
                    // System.err.println(this.solvedSummaries.get(nxt.getMethod())+" "+nxt.getMethod()+" "+nxt.getObject());
                    // System.err.println(e);
                    // throw e;
                }

            }
        }
        if (resolveflag) {
            //System.out.println("Component in resolve : " + component);
            SetComponent(component, NoEscape.getInstance());
        }
    }

    void SetComponent(List<StandardObject3> comp, EscapeState es) {
        //System.out.println("comp:"+comp+" : "+es);
        for (StandardObject3 s : comp) {
            if (solvedSummaries.get(s.getMethod()) != null)
                solvedSummaries.get(s.getMethod()).put(s.getObject(), new EscapeStatus(es));
        }
    }

//    void resolver(HashSet<EscapeState> component) {
//        boolean resolveflag = true;
//        List<EscapeState> conds = new ArrayList<>();
//        for (StandardObject3 sobj : component) {
////            if(!sobj.getMethod().isJavaLibraryMethod()) {
////                System.out.println("Sobj : "+ sobj.toString());
////            }
//            if (sobj.getMethod().isJavaLibraryMethod()) {
//                resolveflag = false;
//            }
//            if (isReturnObject(sobj)) {
//                //System.out.println("Identified as return obj: "+sobj);
//                SetComponent(component, Escape.getInstance());
//                return;
//            }
//            if (isEscapingObject(sobj)) {
//                //System.out.println("Identified as escaping obj: "+sobj);
//                SetComponent(component, Escape.getInstance());
//                return;
//            }
//            if (StoreEscape.MarkParamReturnEscaping == false)
//                if (sobj.getObject().type == ObjectType.parameter) {
//                    if (isEscapingParam(sobj)) {
//                        //System.out.println("Identified as escaping param: "+sobj);
//                        SetComponent(component, Escape.getInstance());
//                        return;
//                    }
//                    continue;
//                }
//
//            for (StandardObject3 nxt : this.graph.get(sobj)) {
//                try {
//                    if (StoreEscape.MarkParamReturnEscaping == false)
//                        if (isReturnedFromDifferentFunction(sobj, nxt)) {
//                            //System.out.println("Returned from different func: "+ sobj);
//                            continue;
//                        }
//                    if (isEscapingObject(nxt)) {
//                        //System.out.println("Escaping obj: "+nxt);
//                        SetComponent(component, Escape.getInstance());
//                        return;
//                    }
//                } catch (Exception e) {
//                    // System.err.println(this.solvedSummaries.get(nxt.getMethod())+" "+nxt.getMethod()+" "+nxt.getObject());
//                    // System.err.println(e);
//                    // throw e;
//                }
//
//            }
//        }
//        if (resolveflag) {
//            //System.out.println("Component in resolve : " + component);
//            SetComponent(component, NoEscape.getInstance());
//        }
//    }
}
