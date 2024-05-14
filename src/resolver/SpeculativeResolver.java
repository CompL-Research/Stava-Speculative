package resolver;

import Inlining.InlineCheck;
import analyser.StaticAnalyser;
import config.StoreEscape;
import counters.PolymorphicInvokeCounter;
import es.*;
import org.slf4j.LoggerFactory;
import ptg.ObjectNode;
import ptg.ObjectType;
import ptg.PointsToGraph;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.AbstractInstanceInvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import java.util.logging.*;
import java.util.*;
import java.util.logging.Formatter;

import static java.lang.System.exit;

/*
 *
 * Resolution:
 * Input: The resolution code takes the dependencies and intraprocedural points to graph as input.
 * Output: The output is the resolved values of the dependencies.
 *
 */

public class SpeculativeResolver extends Formatter {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(SpeculativeResolver.class);
    // Map for storing the static analysis result
    public static Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> existingSummaries;
    // Map for storing a copy of static analysis result
    public static Map<ObjectNode, EscapeStatus> copyexistingSummaries;
    // Map for storing all the conditional values
    public static Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> allcvs = new HashMap<>();
    // Map for storing the final resolved result
    public static Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> solvedSummaries;
    // Map for storing the previously solved result. This is used for fix-point implementation.
    public Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> previoussolvesummaries = new HashMap<>();
    // Map for storing the contextual summaries
    public static Map<SootMethod, HashMap<ObjectNode, List<ContextualEscapeStatus>>> solvedContextualSummaries;
    public static Map<SootMethod, HashMap<ObjectNode, List<ContextualEscapeStatus>>> solvedContextualSummaries2;
    // Map for storing the reason for an object to escape
    public static Map<SootMethod, HashMap<ObjectNode, List<EscapeState>>> reasonForEscape;

    // Map for storing the inline summaries for each callsite
    public static Map<CallSite, HashMap<SootMethod, HashSet<Integer>>> inlineSummaries;
    // Stores the points to graph for each method
    static Map<SootMethod, PointsToGraph> ptgs;
    // Stores the list of methods with noBCI
    List<SootMethod> noBCIMethods;
    public static int count = 0;

    public static Map<SootMethod, HashMap<ObjectNode, HashMap<EscapeState, EscapeStatus>>> CVfinalES;

    boolean debug = true;
    public static boolean printflag = true;
    int i = 0;
    int j = 0;
    public static Map<SootMethod, Map<ObjectNode, Boolean>> storedMergedStatus = new HashMap<>();
    public boolean fieldEscape = false;
    public boolean globalEscape = false;
    // Used for logging
    public static final Logger logger = Logger.getLogger(SpeculativeResolver.class.getName());
    // Mao for storing objects that have polymorphic callsites
    public static Map<SootMethod, List<ObjectNode>> IntrestingObjects = new HashMap<>();

    @Override
    public String format(LogRecord record) {
        return record.getMessage() + "\n";
    }

    public SpeculativeResolver(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> existingSummaries,
                               Map<SootMethod, PointsToGraph> ptgs,
                               List<SootMethod> escapingMethods) {
        // Set the logger to write logs to a file named debug.log
        FileHandler fh;
        try {
            fh = new FileHandler("../logs/debug.log");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);} catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating log file", e);
        }



        // We get three things from static analysis
        // 1. The summaries (Escape Statuses)
        // 2. Points to graph
        // 3. Methods which do not have bci

        for (Map.Entry<SootMethod, HashMap<ObjectNode, EscapeStatus>> ent : existingSummaries.entrySet()) {
            // if (!ent.getKey().isJavaLibraryMethod()) {
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
            // }
        }
        SpeculativeResolver.existingSummaries = existingSummaries;
        this.ptgs = ptgs;
        this.noBCIMethods = escapingMethods;
        /*
         * Debug Code
         */
        // if (debug) {
        // System.out.println(" 1. SUMMARIES : STATIC ANALYSIS");
        // for (SootMethod m : existingSummaries.keySet()) {
        // if (!m.isJavaLibraryMethod()) {
        // System.out.println("Method : " + m);
        // for (ObjectNode o : existingSummaries.get(m).keySet()) {
        // System.out.println(" For object : " + o);
        // System.out.println(" Summaries : ");
        // System.out.println(existingSummaries.get(m).get(o).status);
        // }
        // System.out.println("----------");
        // }
        // }
        // //System.out.println(this.existingSummaries);
        // System.out.println("***************************************");
        // System.out.println(" 2. POINTS TO GRAPH : STATIC ANALYSIS");
        // for (SootMethod method : this.ptgs.keySet()) {
        // if (!method.isJavaLibraryMethod()) {
        // System.out.println("Method : " + method);
        // System.out.println("Points to graph : ");
        // System.out.println(this.ptgs.get(method).toString());
        // }
        // }
        // System.out.println("----------");
        // }

        // Initializing the maps

        solvedSummaries = new HashMap<>();
        solvedContextualSummaries = new HashMap<>();
        solvedContextualSummaries2 = new HashMap<>();
        inlineSummaries = new HashMap<>();
        copyexistingSummaries = new HashMap<>();
        CVfinalES = new HashMap<>();
        reasonForEscape = new HashMap<>();

        for (Map.Entry<SootMethod, HashMap<ObjectNode, EscapeStatus>> entry : existingSummaries.entrySet()) {
            SootMethod method = entry.getKey();
            HashMap<ObjectNode, EscapeStatus> map = entry.getValue();
            HashMap<ObjectNode, ResolutionStatus> q = new HashMap<>();

            for (Map.Entry<ObjectNode, EscapeStatus> e : map.entrySet()) {
                ObjectNode obj = e.getKey();
                q.put(obj, ResolutionStatus.UnAttempted);
            }
//            resolutionStatus.put(method, q);

            solvedSummaries.put(method, new HashMap<>());
            solvedContextualSummaries.put(method, new HashMap<>());
            solvedContextualSummaries2.put(method, new HashMap<>());
        }
        /*
         * Next, we traverse all function calls and add mapping from caller to the
         * objects passed. We are just moving towards inter-procedural resolution :P
         *
         */
        // for (SootMethod sm : existingSummaries.keySet()) {
        //     solvedSummaries.put(sm, new HashMap<>());
        //     for(ObjectNode obj : existingSummaries.get(sm).keySet()) {
        //         //System.out.println("Object Type: "+ obj.toString());
        //         if(sm.isJavaLibraryMethod()) {
        //             solvedSummaries.get(sm).put(obj, new EscapeStatus(Escape.getInstance()));
        //         } else {
        //             solvedSummaries.get(sm).put(obj, new EscapeStatus(NoEscape.getInstance()));
        //         }
        //     }
        // }
        resolveSummaries();

        // for (CallSite c : inlineSummaries.keySet()) {
        //     // if (!c.methodName.isJavaLibraryMethod()) {
        //     if (!inlineSummaries.get(c).isEmpty()) {
        //         for (SootMethod s : inlineSummaries.get(c).keySet()) {
        //             // for(SootMethod sm: solvedSummaries.get())
        //             ArrayList<Integer> arr = new ArrayList<>();
        //             for (Map.Entry<ObjectNode, EscapeStatus> entry : solvedSummaries.get(s).entrySet()) {
        //                 ObjectNode obj = entry.getKey();
        //                 if (obj.type != ObjectType.internal)
        //                     continue;
        //                 EscapeStatus es = entry.getValue();
        //                 if (es.containsNoEscape())
        //                     arr.add(obj.ref);
        //             }
        //             for (Integer i : arr) {
        //                 inlineSummaries.get(c).get(s).add(i);
        //             }
        //         }
        //     }
        //     // }jh
        // }

        /*
         * Debug Code for printing the final result
         */

        // if (debug) {
        // System.out.println("Inline Summaries");
        // for (CallSite c : inlineSummaries.keySet()) {
        // if (!c.methodName.isJavaLibraryMethod()) {
        // if (!inlineSummaries.get(c).isEmpty()) {
        // System.out.println("CallSite : <" + c.methodName + "," + c.BCI + ">");
        // for (SootMethod s : inlineSummaries.get(c).keySet()) {
        // System.out.println(s + " can be inlined at bci " + c.BCI);
        // for (Integer i2 : inlineSummaries.get(c).get(s)) {
        // System.out.print(" with objects : " + i2);
        // }
        // System.out.println("");
        // }
        // System.out.println("");
        // }
        // }
        // }

//        // Final Result:
//        logger.info("\n **************FINAL RESULT ***************** \n");
//        for (SootMethod s : solvedSummaries.keySet()) {
//            if (!s.isJavaLibraryMethod()) {
//                logger.info("For method " + s);
//                for (ObjectNode o : solvedSummaries.get(s).keySet()) {
//                    logger.info("Object is " + o + " and its summary is " +
//                            solvedSummaries.get(s).get(o));
//               }
//                logger.info("*************************************** \n");
//            }
//        }
        for(SootMethod sm: reasonForEscape.keySet()) {
            for(ObjectNode o: reasonForEscape.get(sm).keySet()) {
                if(!reasonForEscape.get(sm).get(o).isEmpty()) {
                    System.out.println("Method: "+ sm + " Object: "+ o);
                    for(EscapeState e: reasonForEscape.get(sm).get(o)) {
                        System.out.println("Reason: "+ e);
                    }
                }
            }
        }

    }

    // Convert all <caller,<argument,x>> statements to the actual caller functions
    // and replace <argument,x>
    // to parameter passed.

    void resolveSummaries() {
        //Debug

        System.out.println(" Resolution of CV's");
        // CallGraph
        CallGraph cg = Scene.v().getCallGraph();
        // Get the list of methods for which static results are generated
        ArrayList<SootMethod> listofMethods = new ArrayList<>(existingSummaries.keySet());
        // Fix point over list of methods
        // This runs till there is no methods that needs to be processed.
        while (!listofMethods.isEmpty()) {
            // A temporary list of methods for fix-point implementation.
            ArrayList<SootMethod> tmpWorklistofMethods = new ArrayList<>();
            // For each method in lisofmethods data-structure process all the dependencies.
            for (SootMethod key : listofMethods) {
                // [DEBUG] Var to print the debug statements.
                debug = true;
                // Note: Key is the current method
                // Proceed only if the there are dependencies available from the phase 1: static analysis
                if (!existingSummaries.containsKey(key)) {
                    continue;
                }

                if(!reasonForEscape.containsKey(key)) {
                    reasonForEscape.put(key, new HashMap<>());
                }

                //
                for(CallSite c : PolymorphicInvokeCounter.polymorphicInvokes.keySet()) {
                    if (c.methodName.equals(key)) {
                        System.out.println("---------------------------------------------");
                        System.out.println("Found a method: " + c + " with polymorphic callsite.");
                        for(ObjectNode o : existingSummaries.get(key).keySet()) {
                            System.out.println("Object: " + o);
                            System.out.println("Summary: " + existingSummaries.get(key).get(o));
                            for(EscapeState e : existingSummaries.get(key).get(o).status) {
                                System.out.println("State: " + e);
                                if(e instanceof ConditionalValue && ((ConditionalValue) e).object.type == ObjectType.parameter){
                                    if(!PolymorphicInvokeCounter.polymorphicInvokes.isEmpty()){
                                        for(SootMethod m : PolymorphicInvokeCounter.polymorphicInvokes.get(c)) {
                                            if(((ConditionalValue) e).method.equals(m)) {
                                                System.out.println("Found a match");
                                                if(SpeculativeResolver.IntrestingObjects.containsKey(key)) {
                                                    if(SpeculativeResolver.IntrestingObjects.get(key).contains(o)) {
                                                        continue;
                                                    } else {
                                                        SpeculativeResolver.IntrestingObjects.get(key).add(o);
                                                    }
                                                } else {
                                                    SpeculativeResolver.IntrestingObjects.put(key, new ArrayList<>());
                                                    SpeculativeResolver.IntrestingObjects.get(key).add(o);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        System.out.println("---------------------------------------------");
                    }
                }
//                for(CallSite c: PolymorphicInvokeCounter.polymorphicInvokes.keySet()) {
//                    if(c.methodName.equals(key)) {
//                        System.out.println("Polymorphic call site found in : "+ key + " at BCI: "+ c.BCI);
//                        System.out.println("Possible Methods are: "+ PolymorphicInvokeCounter.polymorphicInvokes.get(c));
//                    }
//                }
                // DEBUG
//                if (key.getDeclaringClass().toString().contains("HandleCache") && key.toString().contains("cacheHandle")) {
//                    System.out.println("Debug will be printed for this: ");
//                    System.out.println(key.toString());
//                    debug = true;
//                }
//                logger.info("***************************************************************");
//                logger.info(" ********  Resolving Method: " + ++j + "." + key + "  ******** ");
//                logger.info("***************************************************************");
                System.out.println("***************************************************************");
                System.out.println(" ********  Resolving Method: " + ++j + "." + key + "  ******** ");
                System.out.println("***************************************************************");

                /* Get the objects in sorted order. --
                 * Sorted here means analyze parameters first and then any other type of objects.
                 */
                List<ObjectNode> listofobjects = sortedorder(key);

                // Debug
                // System.out.println("List of objects : "+ listofobjects.toString());

                // Create a copy of existing summary for further use.
                for (ObjectNode o : existingSummaries.get(key).keySet()) {
                    copyexistingSummaries.put(o, new EscapeStatus());
                    for (EscapeState e : existingSummaries.get(key).get(o).status) {
                        copyexistingSummaries.get(o).status.add(e);
                    }
                }
                // what is this for?
                Map<ObjectNode, EscapeStatus> methodInfo = copyexistingSummaries;
                // HashMap<ObjectNode, EscapeStatus> solvedMethodInfo =
                // solvedSummaries.get(key);
                // If the result given by the static analyzer does not have the current method
                // then continue.
                // for the current method key for all the objects
                if (!methodInfo.isEmpty() && !listofobjects.isEmpty()) {
                    for (ObjectNode obj : listofobjects) {
                        // Check if the object is already marked as Escaping
                        // if(solvedSummaries.containsKey(key) &&
                        // solvedSummaries.get(key).containsKey(obj)
                        // &&solvedSummaries.get(key).get(obj).doesEscape()) {
                        // continue;
                        // }
                        // if not escaping then proceed.
                        if (key.toString().contains("<init>") &&
                                obj.type == ObjectType.parameter && obj.ref == -1) {
                            solvedSummaries.get(key).put(obj, new EscapeStatus(NoEscape.getInstance()));
                            continue;
                        }
                        boolean nothandled = true;
                        HashMap<EscapeState, EscapeStatus> allresolvedstatusforthisobject = new HashMap<>();
                        HashSet<EscapeStatus> allresolvedstatusforthisobject2 = new HashSet<>();
                        EscapeStatus status = methodInfo.get(obj);
                        HashSet<EscapeState> newStates = new HashSet<>();
                        if(!reasonForEscape.get(key).containsKey(obj)) {
                            reasonForEscape.get(key).put(obj, new ArrayList<>());
                        }
                        if(!reasonForEscape.get(key).containsKey(obj)) {
                            reasonForEscape.get(key).put(obj, new ArrayList<>());
                        }
                        for (EscapeState state : status.status) {
                            HashSet<EscapeStatus> resolvedStatuses = new HashSet<>();
                            System.out.println(" --> Current method is : " + key + "  and  Object : " + obj);
                            System.out.println(" -----> Conditional value for object is : " + state);
//                            logger.info(" ** Current method is : " + key + "  and  Object : " + obj);
//                            logger.info(" ** Conditional value for object is : " + state);
                            if (state instanceof ConditionalValue) {

                                ConditionalValue cstate = (ConditionalValue) state;
                                /*
                                 * Handling parameter dependencies of type <class:methodname,<parameter,number>>
                                 * As such we are deleting the dependency.
                                 */
                                if (cstate.object.type == ObjectType.parameter) {
                                    System.out.println("-------> CV is of type parameter");
//                                    logger.info("CV is of type parameter");
                                    // Method on which this CV depends
                                    SootMethod sm = cstate.getMethod();
                                    // Object on which this current object (obj) depends
                                    ObjectNode o = cstate.object;
                                    System.out.println("-------> Sent to Method : " + sm + " and object as : " + o);

                                    //logger.info(" Sent to Method : " + sm + " and object as : " + o);

                                    /*
                                     * Get the callsite and check if the dependency is for field.
                                     * 
                                     * Get the callsite and for this callsite check if there is resolved value in
                                     * the contextual summary.
                                     * if not merged then can directly use the resolved value of the object.
                                     */

                                    CallSite c = new CallSite(key, cstate.BCI);
                                    System.out.println("-------> CallSite is : " + c.toString());
                                    //logger.info("CallSite is : " + c.toString());

                                    /*
                                     * Check which object to look into:
                                     * if the dependency has field dependency the map the correct object for in the callee
                                     * Get the list of object and based on the resolved value of those resolve the dependency.
                                     */
                                    List<ObjectNode> objects = null;
                                    if (cstate.fieldList != null) {
                                        System.out.println("-------> Field Access");
                                        //logger.info("Field Access");
                                        Iterator<Edge> iter = cg.edgesOutOf(key);
                                        while (iter.hasNext()) {
                                            Edge edge = iter.next();
                                            if (edge.getTgt().equals(sm)) {
                                                try {
                                                    objects = GetParmObjects(o, cstate.object.ref, sm,
                                                            cstate.fieldList);
                                                    System.out.println("-------> Field Access : Object Received" + objects.toString());
//                                                    logger.info("Field Access : Object Received" + objects.toString());
                                                } catch (Exception e) {
                                                    throw e;
                                                }
                                            }
                                        }
                                    }

                                    if (objects != null) {
                                        for (ObjectNode mappedobject : objects) {
                                            System.out.println("-------> Objects are not null: Mapped Object " + mappedobject.toString());
                                            //logger.info("Objects are not null: Mapped Object " + mappedobject.toString());
                                            if (solvedContextualSummaries.containsKey(sm)
                                                    && solvedContextualSummaries.get(sm).containsKey(mappedobject)) {
                                                System.out.println("-------> Value:" + solvedContextualSummaries.get(sm).get(mappedobject));
                                                //logger.info("Value:"+ solvedContextualSummaries.get(sm).get(mappedobject));
                                                for (ContextualEscapeStatus ces : solvedContextualSummaries.get(sm)
                                                        .get(mappedobject)) {
                                                    if (ces.cescapestat.containsKey(c) && ces.doesEscape(c)) {
                                                        System.out.println("-------> Escaping in Parameter");
                                                        //logger.info("Escaping in Parameter");
                                                        solvedSummaries.get(key).put(obj,
                                                                new EscapeStatus(Escape.getInstance()));
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        /* 
                                         * == LOGIC ==:
                                         * Came here because the <objects> list was empty.
                                         * Now: 
                                         * 
                                         * First check for check if the parameter for this dependency has a contextual info
                                         *  - if yes then proceed with the result in the contextual summary.
                                         * 
                                         * Second if the above is not true then check for the result in solved summary which will
                                         * have the merged value from all.
                                         *  - if yes then proceed with the result in the solved summary.
                                         * 
                                         * Third if the result is not found the in either of the summary and till now object has not 
                                         * escaped the store NOESCAPE and resolved value for this dependency.
                                         */

                                        // Case 1: Check for result in contextual summary.
                                        if (solvedContextualSummaries.containsKey(sm)
                                                && solvedContextualSummaries.get(sm).containsKey(o)) {
                                            boolean checkflag = false;
                                            if(true) {
                                                System.out.println("-------> Not a field, sCS Value: "+ solvedContextualSummaries.get(sm).get(o));
                                            }
                                            for (ContextualEscapeStatus ces : solvedContextualSummaries.get(sm)
                                                    .get(o)) {
                                                 System.out.println("-------> ces value : "+ ces.toString());
                                                if (ces.cescapestat.containsKey(c)) {
                                                    checkflag = true;
                                                    if (ces.doesEscape(c)) {
                                                         System.out.println("-------> Escaping in Parameter");
                                                        solvedSummaries.get(key).put(obj,
                                                                new EscapeStatus(Escape.getInstance()));
                                                    } else {
                                                        solvedSummaries.get(key).put(obj,
                                                                new EscapeStatus(NoEscape.getInstance()));
                                                    }
                                                }
                                            }
                                            // if the context was not there in contextual summary then look in to merged summary.
                                            if(!checkflag) {
                                                if (solvedSummaries.get(sm).containsKey(o)) {
                                                    if (solvedSummaries.get(sm).get(o).doesEscape()) {
                                                        solvedSummaries.get(key).put(obj,
                                                                new EscapeStatus(Escape.getInstance()));
                                                                if(true) {
                                                                    System.out.println("-------> No Contextual Summary: ESCAPE");
                                                                }
                                                    } else if (!solvedSummaries.get(sm).get(o).doesEscape()) {
                                                        solvedSummaries.get(key).put(obj,
                                                            new EscapeStatus(NoEscape.getInstance()));
                                                            if(true) {
                                                                System.out.println("-------> No Contextual Summary: NOESCAPE");
                                                            }
                                                    }
                                                    else {
                                                    // if the parameter object was not there in the solvedSummaries as well.
                                                    solvedSummaries.get(key).put(obj,
                                                        new EscapeStatus(NoEscape.getInstance()));
                                                        if(true) {
                                                            System.out.println("-------> No Contextual Summary: NOESCAPE");
                                                        }
                                                    }
                                                }
                                            } 
                                        } else {
                                            // Case 2: if the object is not there in the contextual summary 
                                            if(solvedSummaries.containsKey(sm)) {
                                                if (solvedSummaries.get(sm).containsKey(o)) {
                                                    if (solvedSummaries.get(sm).get(o).doesEscape()) {
                                                        solvedSummaries.get(key).put(obj,
                                                                new EscapeStatus(Escape.getInstance()));
                                                                if(debug) {
                                                                    System.out.println("-------> No Contextual Summary: ESCAPE");
                                                                }
                                                    } else if (!solvedSummaries.get(sm).get(o).doesEscape()) {
                                                        solvedSummaries.get(key).put(obj,
                                                            new EscapeStatus(NoEscape.getInstance()));
                                                            if(debug) {
                                                                System.out.println("-------> No Contextual Summary: NOESCAPE");
                                                            }
                                                    }
                                                } else {
                                                    // if the parameter object was not there in the solvedSummaries as well.
                                                    solvedSummaries.get(key).put(obj,
                                                        new EscapeStatus(NoEscape.getInstance()));
                                                        if(debug) {
                                                            System.out.println("-------> No Contextual Summary: NOESCAPE");
                                                        }
                                                }
                                            }
                                        }
                                        if(debug) {
                                            System.out.println("-------> No Contextual Summary");
                                        }
                                    }

                                    allresolvedstatusforthisobject.put(cstate, solvedSummaries.get(key).get(obj));
                                    allresolvedstatusforthisobject2.add(solvedSummaries.get(key).get(obj));
                                    if (true) {
                                        System.out.println("-------> Resolution is: " + solvedSummaries.get(key).get(obj));
                                    }
                                    if(solvedSummaries.get(key).get(obj).doesEscape()) {
                                        reasonForEscape.get(key).get(obj).add(cstate);
                                    }
                                    continue;
                                }

                                /*
                                 * Handling return dependencies of type liv<class:methodname,<return,number>>
                                 * As such we are deleting the dependency.
                                 */
                                else if (cstate.object.type == ObjectType.returnValue) {
                                    SootMethod sm = cstate.getMethod();
                                    ObjectNode o = cstate.object;
                                    System.out.println("-------> CV is of type return and returned from method : " + sm +
                                            " and object as : " + o);
//                                    logger.info(" CV is of type return and returned from method : " + sm +
//                                            " and object as : " + o);

                                    /*
                                     * We have two types of return type dependencies
                                     * 1. return o1; 2. o1 = foo();
                                     * Case 1: if the object is returned from the method;
                                     * The method in conditional value is same as the current method in this case.
                                     * First directly mark the Object as escaping
                                     */
                                    if (sm.equals(key)) {
                                        // Mark as Escaping
                                        // System.out.println("1. Escaping in Return");
                                        solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                                        // Get the CallGraph
                                        CallGraph c = Scene.v().getCallGraph();
                                        Iterator<Edge> itr = c.edgesInto(key);
                                        while (itr.hasNext()) {
                                            Edge e = itr.next();
                                            // For all the objects in the caller check which object has a return
                                            // dependency on the current method.
                                            // Mark the corresponding object also ESCAPING and delete the dependency,
                                            if (allcvs.containsKey(e.src())) {
                                                for (ObjectNode object : allcvs.get(e.src()).keySet()) {
                                                    Iterator<EscapeState> it = allcvs.get(e.src()).get(object).status
                                                            .iterator();
                                                    while (it.hasNext()) {
                                                        EscapeState es = it.next();
                                                        if (es instanceof ConditionalValue) {
                                                            if (((ConditionalValue) es).object.type == ObjectType.returnValue
                                                                    &&
                                                                    ((ConditionalValue) es).getMethod().equals(key) &&
                                                                    ((ConditionalValue) es).object.ref == cstate.object.ref) {
                                                                if (solvedSummaries.get(e.src()).containsKey(object)) {
                                                                    solvedSummaries.get(e.src()).get(object)
                                                                            .addEscapeStatus(new EscapeStatus(
                                                                                    Escape.getInstance()));
                                                                    // System.out.println("returned from to object : "+
                                                                    // object + " in method : "+ e.src() );
                                                                    existingSummaries.get(e.src()).get(object).status
                                                                            .remove(es);
                                                                } else {
                                                                    // System.out.println("returned from to object : "+
                                                                    // object + " in method : "+ e.src() );
                                                                    // System.out.println("2. Escaping in Return :
                                                                    // object"+ object.toString());
                                                                    solvedSummaries.get(e.src()).put(object,
                                                                            new EscapeStatus(Escape.getInstance()));
                                                                    existingSummaries.get(e.src()).get(object).status
                                                                            .remove(es);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        nothandled = false;
                                        // allresolvedstatusforthisobject.add(solvedSummaries.get(key).get(obj));
                                        allresolvedstatusforthisobject.put(cstate, solvedSummaries.get(key).get(obj));
                                        allresolvedstatusforthisobject2.add(solvedSummaries.get(key).get(obj));

                                    } else {
                                        // System.out.println("Here the control should never reach, as all these
                                        // dependencies should have been deleted by now");
                                        // TODO: Check if still reachable for some benchmark
                                        if (!solvedSummaries.get(key).containsKey(obj)) {
                                            solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                                        } else {
                                            solvedSummaries.get(key).get(obj).status.add(Escape.getInstance());
                                        }
                                        // allresolvedstatusforthisobject.add(new EscapeStatus(Escape.getInstance()));
                                        allresolvedstatusforthisobject.put(cstate, solvedSummaries.get(key).get(obj));
                                        allresolvedstatusforthisobject2.add(solvedSummaries.get(key).get(obj));

                                    }
                                    if (debug) {
                                        System.out.println("-------> Resolution is: " + solvedSummaries.get(key).get(obj));
                                    }
                                    // System.out.println(" Object is : " + obj + " and its Summary is : " +
                                    // solvedSummaries.get(key).get(obj));
                                    if(solvedSummaries.get(key).get(obj).doesEscape()) {
                                        reasonForEscape.get(key).get(obj).add(cstate);
                                    }
                                    continue;
                                }
                                /*
                                 * Handling global dependencies of type <class:methodname,<global,number>>
                                 * In this case the object should always ESCAPE
                                 * If the object is of type parameter then propagate to other (caller)
                                 */
                                else if (cstate.object.type == ObjectType.global) {
                                    // System.out.println("1. Escaping in Global");
                                    solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                                    // allresolvedstatusforthisobject.add(solvedSummaries.get(key).get(obj));
                                    allresolvedstatusforthisobject.put(cstate, solvedSummaries.get(key).get(obj));
                                    allresolvedstatusforthisobject2.add(solvedSummaries.get(key).get(obj));
                                    // Tell to all its caller
                                    if (obj.type == ObjectType.parameter) {
                                        Iterator<Edge> iter = cg.edgesInto(key);
                                        while (iter.hasNext()) {
                                            Edge edge = iter.next();
                                            List<ObjectNode> objects = GetObjects(edge.srcUnit(), cstate.object.ref,
                                                    edge.src(), cstate.fieldList);
                                            if (objects != null) {
                                                for (ObjectNode o : objects) {
                                                    // System.out.println("Object received is global escape at parameter
                                                    // is :"+ o.toString() );
                                                    // System.out.println("2. Escaping in Global");
                                                    solvedSummaries.get(edge.src()).put(o,
                                                            new EscapeStatus(Escape.getInstance()));
                                                }
                                            }
                                        }
                                    }
                                    nothandled = false;
                                    if (debug) {
                                        System.out.println("-------> Resolution is: " + solvedSummaries.get(key).get(obj));
                                    }
                                    if(solvedSummaries.get(key).get(obj).doesEscape()) {
                                        reasonForEscape.get(key).get(obj).add(cstate);
                                    }
                                    continue;
                                }
                                /*
                                 * Handling caller dependencies of type <<caller,argument>, number>
                                 * Resolve the dependency for each context (caller) separately.
                                 */
                                int parameternumber = cstate.object.ref;

                                if (StoreEscape.MarkStoreEscaping && StoreEscape.ReduceParamDependence) {
                                    newStates.add(state);
                                    continue;
                                }
                                if (parameternumber < 0) {
                                    newStates.add(state);
                                    // continue;
                                }
                                // Get all the callers of the current method (current method: "Key" which define
                                // the object having caller, arg dependency)
                                if (debug) {
                                    System.out.println("Reached point 2");
                                }
                                Iterator<Edge> iter = cg.edgesInto(key);
                                solvedContextualSummaries.get(key).put(obj, new ArrayList<>());
                                solvedContextualSummaries2.get(key).put(obj, new ArrayList<>());
                                i = 0;
                                int numberofcaller = 0;
                                if (debug) {
                                    if (iter.hasNext()) {
                                        System.out.println("" + iter.getClass().toString());
                                    } else {
                                        System.out.println("Callee is null : " + iter.getClass().toString());
                                    }
                                }
                                if (!iter.hasNext()) {
                                    solvedSummaries.get(key).put(obj,
                                            new EscapeStatus(Escape.getInstance()));
                                    // allresolvedstatusforthisobject.put(cstate,
                                    // solvedSummaries.get(key).get(obj));
                                    // allresolvedstatusforthisobject2.add(solvedSummaries.get(key).get(obj));
                                    // continue;
                                }
                                while (iter.hasNext()) {
                                    if (debug) {
                                        System.out.println("Reached point 3");
                                    }
                                    EscapeStatus resolvedEscapeStatus = new EscapeStatus(); // This will store the
                                    // escape status for a
                                    // particular context.
                                    ContextualEscapeStatus ctemp = new ContextualEscapeStatus();
                                    boolean mappedobjectescape = false;
                                    boolean localStoredInparamtersfield = false;
                                    boolean mappedinternalescaping = false;
                                    boolean polymerge = false;
                                    globalEscape = false;
                                    fieldEscape = false;
                                    ctemp.cescapestat = new HashMap<>();

                                    parameternumber = cstate.object.ref;
                                    Edge edge = iter.next();
                                    // System.out.println(" \n " + ++i + ". Called from : " + edge.src().getName());
                                    // System.out.println(key+" "+obj+" "+cstate+" " + +parameternumber + "
                                    // "+edge.src() );
                                    // System.out.println("Edge type:" + edge.kind() + " " + key+ "
                                    // "+edge.srcUnit()+" "+edge.src());

                                    if (parameternumber >= 0) {
                                        if (edge.kind() == Kind.REFL_CONSTR_NEWINSTANCE) {
                                            parameternumber = 0;
                                        } else if (edge.kind() == Kind.REFL_INVOKE) {
                                            parameternumber = 1;
                                        }
                                    }
                                    if (debug) {
                                        System.out.println("Reached point 4");
                                    }
                                    List<ObjectNode> objects;
                                    try {
                                        // System.out.println("Src: "+ edge.srcUnit() + "parameter number : "+
                                        // parameternumber + "edge source: "+ edge.src() + "cstate fieldstate : "+
                                        // cstate.fieldList);
                                        objects = GetObjects(edge.srcUnit(), parameternumber, edge.src(),
                                                cstate.fieldList);
                                        if (debug) {
                                            System.out.println("1. Object Received is + " + objects);
                                        }
                                    } catch (Exception e) {
                                        // System.out.println("Cond: " + cstate + " " + cstate.object + " " +
                                        // cstate.object.ref + " " + parameternumber);
                                        throw e;
                                    }

                                    /*
                                     * Dependency is of type <caller,argument>
                                     * We got the corresponding object back in caller. (We are creating a context
                                     * here)
                                     * First check if the mapped object is escaping. (This can happen as object
                                     * might have been escaping due to its own dependency)
                                     ** In that case mark this object as escaping (Also store as contextual result
                                     * for a particular callsite)
                                     * Secondly check if the caller dependency arg count doesn't match with current
                                     * object's count (eg: <parameter,1> = <caller,<arg,0>>)
                                     ** and current object is also of type parameter that means we are storing the
                                     * current object into parameter field. In that case based on the
                                     ** lifetime of object decide the escape status.
                                     * Third if current object escapes and not escaping due to merging of multiple
                                     * caller context then mark all it's caller to escape as well.
                                     * Fourth check for global escaping.
                                     **
                                     */
                                    if (debug) {
                                        System.out.println("Reached point 5");
                                    }
                                    if (objects == null || objects.isEmpty()) {
                                        if (debug) {
                                        System.out.println("Object received is null");
                                        }
                                        if (solvedSummaries.containsKey(key)
                                                && solvedSummaries.get(key).containsKey(obj)
                                                && !solvedSummaries.get(key).get(obj).doesEscape()) {
                                            if (cstate.object.type == ObjectType.argument
                                                    && obj.type == ObjectType.internal) {
                                                // System.out.println("1. Escaping in Caller,arg where object is null");
                                                solvedSummaries.get(key).get(obj)
                                                        .addEscapeStatus(new EscapeStatus(Escape.getInstance()));
                                            } else if (cstate.object.type == ObjectType.argument
                                                    && cstate.object.ref != obj.ref
                                                    && obj.type == ObjectType.parameter) {
                                                List<ObjectNode> objects2 = GetObjects(edge.srcUnit(), obj.ref,
                                                        edge.src(), null);
                                                // System.out.println("2. Object Received is : "+ objects2);
                                                if (objects2 != null) {
                                                    for (ObjectNode o2 : objects2) {
                                                        solvedSummaries.get(edge.src()).put(o2,
                                                                new EscapeStatus(Escape.getInstance()));
                                                        // System.out.println("Stored Escape");
                                                    }
                                                }
                                            } else {
                                                if (!solvedSummaries.get(key).get(obj).doesEscape()) {
                                                    solvedSummaries.get(key).get(obj)
                                                            .addEscapeStatus(new EscapeStatus(NoEscape.getInstance()));
                                                }
                                            }
                                        } else {
                                            if (!solvedSummaries.get(key).containsKey(obj)) {
                                                if (cstate.object.type == ObjectType.argument
                                                        && obj.type == ObjectType.internal) {
                                                    // System.out.println("1. Escaping in Caller,arg where object is
                                                    // null");
                                                    solvedSummaries.get(key).put(obj,
                                                            new EscapeStatus(NoEscape.getInstance()));
                                                } else {
                                                    solvedSummaries.get(key).put(obj,
                                                            new EscapeStatus(NoEscape.getInstance()));
                                                }
                                            } else {
                                                if (!solvedSummaries.get(key).get(obj).doesEscape()) {
                                                    solvedSummaries.get(key).put(obj,
                                                            new EscapeStatus(NoEscape.getInstance()));
                                                }
                                            }
                                        }

                                    } else {
                                        // We got the set of objects mapped to current object (obj)
                                        for (ObjectNode x : objects) {
                                            /*
                                             * Case 1: Mapped Object is Escaping
                                             * If the mapped object is already marked as ESCAPE mark the current object
                                             * as escaping
                                             * for the current context. (Context = <MethodName, bci>)
                                             */

                                            if (debug) {
                                                System.out
                                                        .println("Object received are not null: " + objects.toString());
                                            }
                                            if (solvedSummaries.get(edge.src()) != null) {
                                                if (solvedSummaries.get(edge.src()).get(x) != null) {
                                                    // System.out.println("1. Value of solved summaries for is "+
                                                    // edge.src() + " is :" +
                                                    // solvedSummaries.get(edge.src()).get(x).status);
                                                    if (solvedSummaries.get(edge.src()).get(x).doesEscape()) {
                                                        if (reasonForEscape.containsKey(edge.src()) && reasonForEscape.get(edge.src()).containsKey(x)) {
                                                            for (EscapeState e : reasonForEscape.get(edge.src()).get(x)) {
                                                                if (e instanceof ConditionalValue) {
                                                                    System.out.println("Reason for escape: "+ e);
                                                                    if (((ConditionalValue) e).object.type != ObjectType.parameter) {
                                                                        resolvedEscapeStatus = new EscapeStatus(Escape.getInstance());
                                                                        mappedobjectescape = true;
                                                                        if (debug) {
                                                                            System.out.println("Escaping at Case 1 in <caller,arg>: Type Non Parameter");
                                                                        }
                                                                    } else if (((ConditionalValue) e).object.type == ObjectType.parameter &&
                                                                            !((ConditionalValue) e).method.getName().toString().equals(key.getName().toString())) {
                                                                        resolvedEscapeStatus = new EscapeStatus(Escape.getInstance());
                                                                        mappedobjectescape = true;
                                                                        if (debug) {
                                                                            System.out.println("Escaping at Case 1 in <caller,arg>: Type Parameter");
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        if(!mappedobjectescape) {
                                                            resolvedEscapeStatus = new EscapeStatus(NoEscape.getInstance());
                                                            polymerge = true;
                                                            if (debug) {
                                                                System.out.println("Non Escaping at Case 1 in <caller,arg> -- Poly Merge");
                                                            }
                                                        }

                                                    }
                                                }
                                            }
                                            /*
                                             * Case 2: Handling Field Case
                                             * If the current object is local and have caller dependency, mark it as
                                             * escaping.
                                             * <parameter.field = local obj;>
                                             */
                                            if (cstate.object.type == ObjectType.argument
                                                    && obj.type == ObjectType.internal) {
                                                resolvedEscapeStatus = new EscapeStatus(Escape.getInstance());
                                                localStoredInparamtersfield = true;
                                                if (debug) {
                                                    System.out.println("Escaping at Case 2 in <caller,arg>");
                                                }
                                            }
                                            /*
                                             * Case 3: Handling store when both are parameters
                                             * Get the objects if both are paremeters map the dependency in caller.
                                             * else resolve based on the life time of object.
                                             * <parameter.field = parameter;>
                                             */

                                            if (cstate.object.type == ObjectType.argument
                                                    && cstate.object.ref != obj.ref
                                                    && obj.type == ObjectType.parameter) {
                                                List<ObjectNode> objects2;
                                                // System.out.println("CV is : "+ cstate.toString());
                                                objects2 = GetObjects(edge.srcUnit(), obj.ref, edge.src(),
                                                        cstate.fieldList);
                                                // System.out.println("Second object received is : "+
                                                // objects2.toString());
                                                if (objects != null && objects2 != null) {
                                                    if (solvedSummaries.containsKey(edge.src()) &&
                                                            solvedSummaries.get(edge.src()).containsKey(x) &&
                                                            solvedSummaries.get(edge.src()).get(x).doesEscape()) {
                                                        resolvedEscapeStatus = new EscapeStatus(Escape.getInstance());
                                                        mappedinternalescaping = true;

                                                        // for(ObjectNode o2 : objects2) {
                                                        // solvedSummaries.get(edge.src()).put(o2, new
                                                        // EscapeStatus(Escape.getInstance()));
                                                        // }
                                                        // // In this the possibility of that <parameter,0> may have
                                                        // <caller,arg,1> dependency which might be causing it to
                                                        // escape.
                                                        // // In that case we have map it to its actual object
                                                    }
                                                    // System.out.println("First Object was : "+ x.toString());
                                                    if (x.type == ObjectType.parameter
                                                            || x.type == ObjectType.external) {
                                                        for (ObjectNode o2 : objects2) {
                                                            // System.out.println("Second Object Receilved is : "+
                                                            // o2.toString());
                                                            if (o2.type == ObjectType.internal) {
                                                                // System.out.println("Marking as escaping");
                                                                solvedSummaries.get(edge.src()).put(o2,
                                                                        new EscapeStatus(Escape.getInstance()));
                                                                // System.out.println("Escaping at Case 3 in
                                                                // <caller,arg>");
                                                            } else if (o2.type == ObjectType.parameter) {
                                                                // if(solvedSummaries.containsKey(edge.))
                                                                if (!existingSummaries.get(edge.src()).get(o2).status
                                                                        .contains(cstate)) {
                                                                    existingSummaries.get(edge.src()).get(o2).status
                                                                            .add(cstate);
                                                                    if (!tmpWorklistofMethods.contains(edge.src())) {
                                                                        tmpWorklistofMethods.add(edge.src());
                                                                        resolvedEscapeStatus = new EscapeStatus(
                                                                                Escape.getInstance());
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                            }
                                            /*
                                             * Case 4: If the current object has a global dependency then mark the
                                             * mapped object
                                             * and all its dependencies as Escaping.
                                             */
                                            if (SpeculativeResolver.allcvs.containsKey(key)) {
                                                for (EscapeState e : SpeculativeResolver.allcvs.get(key)
                                                        .get(obj).status) {
                                                    if (e instanceof ConditionalValue) {
                                                        if (((ConditionalValue) e).object.type == ObjectType.global) {
                                                            // System.out.println("Object received is global escape at
                                                            // argument is :" + x.toString());
                                                            resolvedEscapeStatus = new EscapeStatus(
                                                                    Escape.getInstance());
                                                            solvedSummaries.get(edge.src()).put(x,
                                                                    new EscapeStatus(Escape.getInstance()));
                                                            HashSet<ObjectNode> visited = new HashSet<>();
                                                            propagateStatustofields(edge.src(), x, visited);
                                                            globalEscape = true;
                                                        }
                                                    }
                                                }
                                            }
                                            /*
                                             * Case 5: If the mapped object is already marked as ESCAPE no need to
                                             * proceed forward
                                             * We directly mark the object as escaping.
                                             */
                                            if (!mappedobjectescape && !mappedinternalescaping
                                                    && !localStoredInparamtersfield && !globalEscape && !polymerge) {
                                                // System.out.println("Came here");
                                                resolvedEscapeStatus = new EscapeStatus(NoEscape.getInstance());
                                            }
                                        }
                                    }
                                    // Storing the context for each call-site
                                    if (edge.srcUnit() != null) {
                                        if (utils.getBCI.get(edge.srcUnit()) > -1) {
                                            CallSite c = new CallSite(edge.src(), utils.getBCI.get(edge.srcUnit()));
                                            if (resolvedEscapeStatus.containsNoEscape()) {
                                                if (ctemp.cescapestat != null) {
                                                    ctemp.cescapestat.put(c, NoEscape.getInstance());
                                                    if (!solvedContextualSummaries.get(key).get(obj).contains(ctemp))
                                                        solvedContextualSummaries.get(key).get(obj).add(ctemp);
                                                    solvedContextualSummaries2.get(key).get(obj).add(ctemp);
                                                    // System.out.println(" Stored : for method "+ key +
                                                    // ctemp.cescapestat.toString());
                                                }
                                            } else {
                                                if (ctemp.cescapestat != null) {
                                                    ctemp.cescapestat.put(c, Escape.getInstance());
                                                    if (!solvedContextualSummaries.get(key).get(obj).contains(ctemp))
                                                        solvedContextualSummaries.get(key).get(obj).add(ctemp);
                                                    solvedContextualSummaries.get(key).get(obj).add(ctemp);
                                                    // System.out.println("Stored : for method "+ key +
                                                    // ctemp.cescapestat.toString());
                                                }
                                            }
                                        }
                                    }
                                    // System.out.println("Value of solved summaries : " +
                                    // resolvedEscapeStatus.toString());
                                    resolvedStatuses.add(resolvedEscapeStatus);
                                    nothandled = false;
                                    numberofcaller++;
                                }
                                // Taking the merge for the call-sites for local object
                                if (obj.type != ObjectType.parameter) {
                                    for (EscapeStatus e : resolvedStatuses) {
                                        // System.out.println("Escape Status Value : "+ e.toString());
                                        boolean escapeExitFlag = false;
                                        if (e != null) {
                                            if (e.status.size() != 1) {
                                                // System.out.println("Error in size");
                                                exit(-1);
                                            } else {
                                                for (EscapeState es : e.status) {
                                                    if (es.equals(Escape.getInstance())) {
                                                        solvedSummaries.get(key).put(obj,
                                                                new EscapeStatus(Escape.getInstance()));
                                                        if (numberofcaller > 1) {
                                                            if (storedMergedStatus.containsKey(key)) {
                                                                if (!storedMergedStatus.get(key).containsKey(obj)) {
                                                                    storedMergedStatus.get(key).put(obj, true);
                                                                }
                                                            } else {
                                                                storedMergedStatus.put(key, new HashMap<>());
                                                                storedMergedStatus.get(key).put(obj, true);
                                                            }
                                                        }
                                                        // System.out.println(" Meet for all callsites resolved value
                                                        // for object : " + obj + " Status :" +
                                                        // solvedMethodInfo.get(obj));
                                                        escapeExitFlag = true;
                                                        break;
                                                    } else if (es.equals(NoEscape.getInstance())) {
                                                        // System.out.println("Came Inside meet");
                                                        if (solvedSummaries.containsKey(key)
                                                                && !solvedSummaries.get(key).containsKey(obj)) {
                                                            solvedSummaries.get(key).put(obj,
                                                                    new EscapeStatus(NoEscape.getInstance()));
                                                        }
                                                        if (numberofcaller > 1) {
                                                            if (storedMergedStatus.containsKey(key)) {
                                                                if (!storedMergedStatus.get(key).containsKey(obj)) {
                                                                    storedMergedStatus.get(key).put(obj, true);
                                                                }
                                                            } else {
                                                                storedMergedStatus.put(key, new HashMap<>());
                                                                storedMergedStatus.get(key).put(obj, true);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            if (escapeExitFlag) {
                                                break;
                                            }
                                        }
                                    }
                                } else if (obj.type == ObjectType.parameter && cstate.object.ref != obj.ref) {
                                    solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                                } else {
                                    if (!solvedSummaries.get(key).containsKey(obj)) {
                                        solvedSummaries.get(key).put(obj, new EscapeStatus(NoEscape.getInstance()));
                                    }
                                }
                                allresolvedstatusforthisobject.put(cstate, solvedSummaries.get(key).get(obj));
                                allresolvedstatusforthisobject2.add(solvedSummaries.get(key).get(obj));
                                if (debug) {
                                    System.out.println("Resolution is: " + solvedSummaries.get(key).get(obj));
                                }
                                if(solvedSummaries.get(key).get(obj).doesEscape()) {
                                    reasonForEscape.get(key).get(obj).add(cstate);
                                }
                            } else {
                                if (state instanceof Escape) {
                                    if (!solvedSummaries.get(key).containsKey(obj)) {
                                        solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                                        nothandled = false;
                                    } else if (solvedSummaries.get(key).containsKey(obj)) {
                                        solvedSummaries.get(key).get(obj).status.add(Escape.getInstance());
                                        nothandled = false;
                                    }
                                    allresolvedstatusforthisobject.put(state, solvedSummaries.get(key).get(obj));
                                    allresolvedstatusforthisobject2.add(solvedSummaries.get(key).get(obj));
                                } else if (state instanceof NoEscape) {
                                    if (solvedSummaries.get(key).containsKey(obj)) {
                                        if (!solvedSummaries.get(key).get(obj).doesEscape()) {
                                            solvedSummaries.get(key).put(obj, new EscapeStatus(NoEscape.getInstance()));
                                            allresolvedstatusforthisobject.put(state,new EscapeStatus(NoEscape.getInstance()));
                                            allresolvedstatusforthisobject2.add(new EscapeStatus(NoEscape.getInstance()));
                                        }
                                    } else {
                                        solvedSummaries.get(key).put(obj, new EscapeStatus(NoEscape.getInstance()));
                                    }
                                    nothandled = false;
                                }
                                if (debug) {
                                    System.out.println("Resolution is: " + solvedSummaries.get(key).get(obj));
                                }
                                continue;
                            }
                            // allresolvedstatusforthisobject.add(solvedSummaries.get(key).get(obj))
                        }
                        if (nothandled) {
                            // System.out.println(" Reached where the status was not CV ");
                            if (solvedSummaries.get(key).containsKey(obj)
                                    && solvedSummaries.get(key).get(obj).doesEscape()) {

                            } else {
                                solvedSummaries.get(key).put(obj, new EscapeStatus(NoEscape.getInstance()));
                            }
                            // GenerateGraphFromSummary();
                            // FindSCC(key, obj);
                            // allresolvedstatusforthisobject.put(state, solvedSummaries.get(key).get(obj));
                            // allresolvedstatusforthisobject.add(solvedSummaries.get(key).get(obj));
                            // System.out.println(" Value for object : " + obj + " Status :" +
                            // solvedSummaries.get(key).get(obj));
                        }
                        // Take a meet for the case when multiple CV's are there and one all may
                        // different types (parm,ret and caller<arg>)
                        for (EscapeStatus es : allresolvedstatusforthisobject2) {
                            if (es != null) {
                                for (EscapeState e : es.status) {
                                    if (e.equals(Escape.getInstance())) {
                                        solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                                        // System.out.println(" Meet for all the CV's : " +
                                        // solvedSummaries.get(key).get(obj).status);
                                        break;
                                    }
                                }
                            }
                        }
                        // for (EscapeState es : allresolvedstatusforthisobject.keySet()) {
                        // for (EscapeState e : allresolvedstatusforthisobject.get(es).status) {
                        // if (e.equals(Escape.getInstance())) {
                        // solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                        // System.out.println(" Meet for all the CV's : " +
                        // solvedSummaries.get(key).get(obj).status);
                        // break;
                        // }
                        // }
                        // }
                        //
                        // if(solvedSummaries.get(key).get(obj).equals(Escape.getInstance())) {
                        // System.out.println("all: "+ allresolvedstatusforthisobject.toString());
                        // System.out.println("1. Reaching here");
                        //System.out.println("Current Method is : "+ key);
                        //System.out.println("Object is : "+ obj);
                        if(CVfinalES.containsKey(key)) {
                            if(CVfinalES.get(key).containsKey(obj)) {
                                for(EscapeState e: allresolvedstatusforthisobject.keySet()) {
                                    CVfinalES.get(key).get(obj).put(e, allresolvedstatusforthisobject.get(e));
                                    //System.err.println("Escape State is : " + e.toString() + " and its value is : "+ allresolvedstatusforthisobject.get(e).toString());
                                }
                            } else {
                                CVfinalES.get(key).put(obj, new HashMap<>());
                                for(EscapeState e: allresolvedstatusforthisobject.keySet()) {
                                    CVfinalES.get(key).get(obj).put(e, allresolvedstatusforthisobject.get(e));
                                    //System.err.println("Escape State is : " + e.toString() + " and its value is : "+ allresolvedstatusforthisobject.get(e).toString());
                                }
                            }
                        } else {
                            CVfinalES.put(key, new HashMap<>());
                            CVfinalES.get(key).put(obj, new HashMap<>());
                            for(EscapeState e: allresolvedstatusforthisobject.keySet()) {
                                CVfinalES.get(key).get(obj).put(e, allresolvedstatusforthisobject.get(e));
                                //System.err.println("Escape State is : " + e.toString() + " and its value is : "+ allresolvedstatusforthisobject.get(e).toString());
                            }
                        }



                        for (EscapeState e1 : allresolvedstatusforthisobject.keySet()) {
                            // System.out.println("2. Reaching here");
                            if (allresolvedstatusforthisobject.get(e1) != null) {
                                // System.out.println("3. Reaching here");
                                for (EscapeState e2 : allresolvedstatusforthisobject.get(e1).status) {
                                    // System.out.println("4. Reaching here");
                                    if (e2.equals(Escape.getInstance())) {
                                        // System.out.println("5. Reaching here");
                                        // solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                                        if (e1 instanceof ConditionalValue) {
                                            // System.out.println("6. Reaching here");
                                            if (((ConditionalValue) e1).object.type != ObjectType.argument) {
                                                // System.out.println("7. Reaching here");
                                                if (solvedContextualSummaries.containsKey(key)
                                                        && solvedContextualSummaries.get(key).containsKey(obj)) {
                                                    // System.out.println("8. Reaching here");
                                                    for (ContextualEscapeStatus es1 : solvedContextualSummaries.get(key)
                                                            .get(obj)) {
                                                        // System.out.println("9. Reaching here");
                                                        for (CallSite c : es1.cescapestat.keySet()) {
                                                            es1.cescapestat.put(c, Escape.getInstance());
                                                            // System.out.println(" UPDATING CONTEXTUAL SUMMARY for : "
                                                            // + c.toString());
                                                        }
                                                    }
                                                }
                                            } else {
                                                if (((ConditionalValue) e1).object.ref != obj.ref) {
                                                    // System.out.println("7.1 Reaching here");
                                                    if (solvedContextualSummaries.containsKey(key)
                                                            && solvedContextualSummaries.get(key).containsKey(obj)) {
                                                        // System.out.println("8.1 Reaching here");
                                                        for (ContextualEscapeStatus es1 : solvedContextualSummaries
                                                                .get(key).get(obj)) {
                                                            // System.out.println("9.1 Reaching here");
                                                            for (CallSite c : es1.cescapestat.keySet()) {
                                                                es1.cescapestat.put(c, Escape.getInstance());
                                                                // System.out.println(" .1 UPDATING CONTEXTUAL SUMMARY
                                                                // for : " + c.toString());
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // }
                        if (debug) {
                            System.out.println("Final Resolved Value in this iteration is : " + solvedSummaries.get(key).get(obj));
                        }

                        // for (EscapeStatus es : allresolvedstatusforthisobject) {
                        // if (es != null) {
                        // for (EscapeState e : es.status) {
                        // if (e.equals(Escape.getInstance())) {
                        // solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                        //// System.out.println(" Meet for all the CV's : " +
                        // solvedSummaries.get(key).get(obj).status);
                        // break;
                        // }
                        // }
                        // }
                        // }

                        // assert
//                        if (previoussolvesummaries.get(key).get(obj).doesEscape()
//                                && !solvedSummaries.get(key).get(obj).doesEscape()) {
//                            System.out.println("Things Going Wrong Here");
//                            System.out.println("Method is : " + key.toString() + "Object is : " + obj.toString());
//                            System.out.println(
//                                    "CV's for the object are : " + existingSummaries.get(key).get(obj).toString());
//                            System.out
//                                    .println("Resolved Status for CV's: " + allresolvedstatusforthisobject.toString());
//                        }

                        // Find the methods which need to be reanalyzed due to change in the current
                        // method
                        // (object passed as parameter might be escaping now)
                        // System.out.println("1. Coming here");
                        if (solvedSummaries.get(key).containsKey(obj) && previoussolvesummaries.containsKey(key)
                                && previoussolvesummaries.get(key).containsKey(obj)) {
                            // System.out.println("2. Coming here");
                            if (!solvedSummaries.get(key).get(obj).status
                                    .equals(previoussolvesummaries.get(key).get(obj).status)) {
                                for (EscapeState e : allcvs.get(key).get(obj).status) {
                                    // System.out.println("3. Coming here");
                                    if (e instanceof ConditionalValue) {
                                        ConditionalValue cv = (ConditionalValue) e;
                                        // Add those methods whose parameter value are now escaping
                                        if (cv.object.type == ObjectType.parameter) {
                                            SootMethod sm = cv.getMethod();
                                            // System.out.println("CV of parameter/return type : " + cv.toString());
                                            if (!tmpWorklistofMethods.contains(sm)) {
                                                tmpWorklistofMethods.add(sm);
                                                // System.out.println("Added method " + sm + "to get re-analyzed in
                                                // paramter/return");
                                            }
                                        } else if (cv.object.type == ObjectType.argument) {
                                            Iterator<Edge> iter = cg.edgesInto(key);
                                            while (iter.hasNext()) {
                                                Edge edge = iter.next();
                                                if (!tmpWorklistofMethods.contains(edge.src())) {
                                                    tmpWorklistofMethods.add(edge.src());
                                                    // System.out.println("Added method " + edge.src() + "to get
                                                    // re-analyzed in argument");
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            previoussolvesummaries.get(key).put(obj, solvedSummaries.get(key).get(obj));
                        }
                        // System.out.println(" Value for object : " + obj + " Status :" +
                        // solvedSummaries.get(key).get(obj));

                        if (obj.type == ObjectType.internal)
                            InlineCheck.inlineinfo(key, obj);
                        // HashSet<ObjectNode> visited = new HashSet<>();
                        // propagateStatustofields(key, obj, visited);
                    }
                }
                //System.out.println("Solved Summaries: "+ solvedSummaries.toString());
                for(SootMethod sm : solvedSummaries.keySet()) {
                    System.out.println("Method is : "+ sm.toString());
                    System.out.println("Objects are : "+ solvedSummaries.get(sm).toString());
                }
                System.out.println("");
                for(SootMethod sm : solvedContextualSummaries.keySet()) {
                    System.out.println("Method is : "+ sm.toString());
                    System.out.println("Objects are : "+ solvedContextualSummaries.get(sm).toString());
                }
                //System.out.println("Values in solvedContextualSummaries : "+ solvedContextualSummaries.toString());
            }
            System.out.println();
            listofMethods.clear();
            listofMethods.addAll(tmpWorklistofMethods);
//            System.out.println("Methods getting re-analyzed " + listofMethods.toString());
            tmpWorklistofMethods.clear();
        }
    }

    public void propagateStatustofields(SootMethod key, ObjectNode x, HashSet<ObjectNode> visited) {
        if (visited.contains(x))
            return;
        if (!this.ptgs.get(key).fields.containsKey(x))
            return;
        // System.out.println("Propagate Called");
        visited.add(x);
        for (SootField f : this.ptgs.get(key).fields.get(x).keySet()) {
            Set<ObjectNode> tmpobj = this.ptgs.get(key).fields.get(x).get(f);
            for (ObjectNode ob : tmpobj) {
                solvedSummaries.get(key).put(ob, new EscapeStatus(Escape.getInstance()));
                // System.out.println("Object is : "+ ob.toString());
                if (!visited.contains(ob))
                    propagateStatustofields(key, ob, visited);
            }
        }
    }

    private List<ObjectNode> sortedorder(SootMethod key) {
        List<ObjectNode> sortedmap = new ArrayList<>();
        for (ObjectNode o : existingSummaries.get(key).keySet()) {
            if (o.type == ObjectType.parameter)
                sortedmap.add(o);
        }
        for (ObjectNode o : existingSummaries.get(key).keySet()) {
            if (o.type != ObjectType.parameter)
                sortedmap.add(o);
        }
        return sortedmap;
    }

    // EscapeState CreateNewEscapeState(ObjectNode obj, ConditionalValue state,
    // SootMethod src) {
    // return new ConditionalValue(src, obj, state.fieldList, state.isReal);
    // }

    public static List<ObjectNode> GetObjects(Unit u, int num, SootMethod src, List<SootField> fieldList) {
        List<ObjectNode> objs = new ArrayList<>();
        InvokeExpr expr;
        if (u instanceof JInvokeStmt) {
            expr = ((JInvokeStmt) u).getInvokeExpr();
            // System.out.println("1"+ expr.toString());
        } else if (u instanceof JAssignStmt) {
            expr = (InvokeExpr) (((JAssignStmt) u).getRightOp());
            // System.out.println("2"+ expr.toString());
        } else {
            // System.out.println("3"+ u.toString());
            return null;
        }
        Value arg = null;
        try {
            if (num >= 0) {
                // System.out.println("Num : "+ num + " expr is : "+ expr.toString());
                if (expr.getArgCount() > num)
                    arg = expr.getArg(num);
            } else if (num == -1 && (expr instanceof AbstractInstanceInvokeExpr)) {
                arg = ((AbstractInstanceInvokeExpr) expr).getBase();
                // System.out.println("For -1 : expr is "+ expr + "arg value is : "+
                // arg.toString());
            } else
                return null;

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

        if (arg != null) {
            if (!(arg instanceof Local))
                return objs;
            else if (((Local) arg).getType() instanceof PrimType)
                return objs;
        }
        try {
            // System.out.println("Reaching here with source: "+ src.toString() + "and arg :
            // "+ arg);
            if (ptgs.containsKey(src)) {
                if (ptgs.get(src).vars.containsKey(arg)) {
                    // System.out.println("Reached Inside");
                    for (ObjectNode o : ptgs.get(src).vars.get(arg)) {
                        // System.out.println("Object is : "+ o.toString());
                        if (fieldList != null) {
                            // System.out.println("Reaching inside as field list is : "+
                            // fieldList.toString());
                            Set<ObjectNode> obj = new HashSet<>();
                            Set<ObjectNode> obj1 = new HashSet<>();
                            obj1.add(o);
                            for (SootField f : fieldList) {
                                // System.out.println("Field is "+ f.toString());
                                obj = getfieldObject(src, obj1, f);
                                if (obj.isEmpty()) {
                                    // objs.add(o);
                                    return null;
                                }
                                // System.out.println("Obj is "+ obj.toString());
                                obj1 = obj;
                            }
                            if (obj != null) {
                                objs.addAll(obj);
                                return objs;
                            } else {
                                return null;
                            }

                        } else {
                            objs.add(o);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // System.out.println(src + " " + arg + " " + u + " " + num);
            e.printStackTrace();
            exit(0);
        }
        // System.out.println("Obj has "+ objs.toString());
        return objs;
    }

    public static List<ObjectNode> GetParmObjects(ObjectNode ob, int num, SootMethod tgt, List<SootField> fieldList) {
        List<ObjectNode> objs = new ArrayList<>();
        try {
            // System.out.println("Reaching here with source: "+ tgt.toString() + "and
            // fieldlist : "+ fieldList);
            if (fieldList != null) {
                if (ptgs.containsKey(tgt)) {
                    if (ptgs.get(tgt).fields.containsKey(ob)) {
                        // System.out.println("Reached Inside");
                        Set<ObjectNode> obj = new HashSet<>();
                        Set<ObjectNode> obj1 = new HashSet<>();
                        obj1.add(ob);
                        for (SootField f : fieldList) {
                            // System.out.println("Object is : "+ ob.toString());
                            // System.out.println("Reaching inside as field list is : "+
                            // fieldList.toString());
                            // System.out.println("Field is "+ f.toString());
                            obj = getfieldObject(tgt, obj1, f);
                            if (obj.isEmpty()) {
                                // objs.add(o);
                                return null;
                            }
                            // System.out.println("Obj is "+ obj.toString());
                            obj1 = obj;
                        }
                        if (obj != null) {
                            objs.addAll(obj);
                            return objs;
                        } else {
                            return null;
                        }
                    }
                }
            } else {
                objs.add(ob);
            }
        } catch (Exception e) {
            // System.out.println(src + " " + arg + " " + u + " " + num);
            e.printStackTrace();
            exit(0);
        }
        // System.out.println("Obj has "+ objs.toString());
        return objs;
    }

//    public static List<ObjectNode> GetParmNoFieldObjects(ObjectNode ob, int num, SootMethod tgt) {
//        List<ObjectNode> objs = new ArrayList<>();
//        try {
//            // System.out.println("Reaching here with source: "+ tgt.toString() + "and
//            // fieldlist : "+ fieldList);
////            if (fieldList != null) {
//                if (ptgs.containsKey(tgt)) {
//                    if (ptgs.get(tgt).containsKey(ob)) {
//                        // System.out.println("Reached Inside");
//                        Set<ObjectNode> obj = new HashSet<>();
//                        Set<ObjectNode> obj1 = new HashSet<>();
//                        obj1.add(ob);
//                        if (obj != null) {
//                            objs.addAll(obj);
//                            return objs;
//                        } else {
//                            return null;
//                        }
//                    }
//                }
////            } else {
////                objs.add(ob);
////            }
//        } catch (Exception e) {
//            // System.out.println(src + " " + arg + " " + u + " " + num);
//            e.printStackTrace();
//            exit(0);
//        }
//        // System.out.println("Obj has "+ objs.toString());
//        return objs;
//    }






    public static Set<ObjectNode> getfieldObject(SootMethod src, Set<ObjectNode> obj, SootField f) {
        Set<ObjectNode> tmpobj = new HashSet<>();
        for (ObjectNode o : obj) {
            if (ptgs.get(src).fields.containsKey(o)) {
                if (ptgs.get(src).fields.get(o).containsKey(f)) {
                    tmpobj = ptgs.get(src).fields.get(o).get(f);
                }
            }
        }
        return tmpobj;
    }

    boolean isstoredinfield(ObjectNode obj, EscapeState es) { // Is assigned to this or parameter.
        if (obj.type == ObjectType.internal) {
            if (es instanceof ConditionalValue) {
                if (((ConditionalValue) es).object.type == ObjectType.argument)
                    return true;
            }
        }
        return false;
    }

    // void resolver(HashMap<ObjectNode, EscapeStatus> solvedMethodInfo, ObjectNode
    // obj, SootMethod m, EscapeState es) {
    // if(es instanceof ConditionalValue) {
    // if (isstoredinfield(obj, es)) {
    // solvedMethodInfo.put(obj, new EscapeStatus(Escape.getI22nstance()));
    // fieldEscape = true;
    // return;
    // }
    // }
    // solvedMethodInfo.put(obj, new EscapeStatus(NoEscape.getInstance()));
    // }
}
