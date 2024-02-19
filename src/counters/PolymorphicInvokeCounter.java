package counters;
import es.CallSite;
import handlers.JAssignStmt.InvokeStmt;
import main.CHATransform;
import ptg.ObjectNode;
import soot.*;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import utils.getBCI;
import java.util.*;

public class PolymorphicInvokeCounter extends BodyTransformer {
    public static HashMap<CallSite, List<SootMethod>> polymorphicInvokes = new HashMap<>();

    @Override
    protected void internalTransform(Body body, String phasename, Map<String, String> options) {
        //System.out.println("Reached PolymorphicInvokeCounter");
//        System.out.println("Body: " + body);
        PatchingChain<Unit> units = body.getUnits();
        for (Unit u : units) {
            if (u instanceof JAssignStmt) {
//                  System.out.println("Unit is "+ u);
//                JAssignStmt stmt = (JAssignStmt) u;
//                Value rhs = stmt.getRightOp();
//                System.out.println("RHS: " + rhs);
//                if (rhs instanceof InvokeStmt) {
//                    CallGraph cg = Scene.v().getCallGraph();
//                    Iterator<Edge> iedges = cg.edgesOutOf(u);
//                    List<Edge> edges = new ArrayList<>();
//                    if (!iedges.hasNext()) {
//                        iedges = CHATransform.getCHA().edgesOutOf(u);
//                    }
//                    while(iedges.hasNext()) {
//                        edges.add(iedges.next());
//                    }
//                    if(!edges.isEmpty()) {
//                        //System.out.println("Current Method: " + body.getMethod());
//                        //System.out.println("Current Unit: " + u);
//                        for (Edge edge : edges) {
//                            SootMethod method = edge.tgt();
//                            //System.out.println("Target Method: " + method + " Kind: " + edge.kind() + " at BCI : "+ getBCI.get(u));
//                        }
//                    }
//
//                }
            } else if (u instanceof  JInvokeStmt) {
                boolean flag = false;
                CallGraph cg = Scene.v().getCallGraph();
                Iterator<Edge> iedges = cg.edgesOutOf(u);
                List<Edge> edges = new ArrayList<>();
                if (!iedges.hasNext()) {
                    iedges = CHATransform.getCHA().edgesOutOf(u);
                }
                while(iedges.hasNext()) {
                    edges.add(iedges.next());
                }
                if(!edges.isEmpty()) {
                    int count = 0;
                    for (Edge edge : edges) {
                        count++;
                    }
                    if(count > 1 ) {
                        polymorphicInvokes.put(new CallSite(body.getMethod(), getBCI.get(u)), new ArrayList<>());
                        ArrayList methods  = new ArrayList();
                        for (Edge edge : edges) {
                            SootMethod method = edge.tgt();
                            methods.add(method);
                        }
                        System.out.println("BCI: "+ getBCI.get(u));
                        polymorphicInvokes.get(new CallSite(body.getMethod(), getBCI.get(u))).addAll(methods);
                        flag = true;
                    }
                }

            }
        }
    }
}