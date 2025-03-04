package analyser;

import es.EscapeReason;
import es.EscapeState;
import es.EscapeStatus;
import handlers.*;
import ptg.*;
import resolver.SpeculativeResolver;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import utils.IllegalBCIException;

import java.util.*;

import branch.BranchUnits;
import branch.BranchUnits.Ifpart;


public class PostResolutionAnalyser extends BodyTransformer {
	// Store Branch Info Per method.
	public static Map<SootMethod, BranchUnits> BranchInfo;
	static Map<SootMethod, PointsToGraph> ptgs;
	
	public static Map<SootMethod, List<Map.Entry<Map<Integer, String>, Integer>>> finalBranchResult;
	// public static Map<SootMethod, Map.Entry<Map<Integer, String>, List<Integer>>> mergedBranchResult;
	public PostResolutionAnalyser ( Map<SootMethod, PointsToGraph> ptgs) {
		super();
		BranchInfo = new HashMap<>();
		finalBranchResult = new HashMap<>();
		PostResolutionAnalyser.ptgs = ptgs;
	}
	
	enum BranchType {
		IF, ELSEIF, ELSE
	}

	@Override
	protected void internalTransform(Body body, String phasename, Map<String, String> options) {
		// Don't Analyze Constructors.
		if(body.getMethod().getBytecodeSignature().contains("<init>")) {
			return;
        }
		// Current Method
		SootMethod curr_met = body.getMethod();
		//[Debug]
		System.out.println("==== Analyzing Method Name: " + curr_met.getBytecodeSignature() + " ==== " );
//		System.out.println(body);

		PatchingChain<Unit> units = body.getUnits();
		ExceptionalUnitGraph cfg = new ExceptionalUnitGraph(body);
		// Get the resolved values from Resolver:
		/*
			1. Check if the current method has a internal method which is escaping.
			2. Check if the method body has if statements.
			3. Then check for the conditions that is it escaping in one of the branch.
		 */

		// Check 1:  If Method has a escaping internal object.
		List<ObjectNode> objectList;
		objectList = checkIfEscapingObjectPresent(curr_met, SpeculativeResolver.MergedSummaries);
		for(ObjectNode o : objectList) {
			System.out.println(" Escaping Local Object: <"+ o.type + "," + o.ref + ">");
		}

		// Check 2: If Method has conditional "IF" Statement.
		boolean hasIF = false;
		hasIF = checkIfHasConditionalIFStatment(units);
		if(hasIF) {
			BranchUnits bu;
			bu = getBranchUnits(curr_met, units, cfg);
			if(bu != null && bu.ifpart.toString() != null && bu.elseifpart.toString() != null && bu.elsepart.toString() != null) {
				System.out.println("Branch Units: ");
				System.out.println(bu.ifpart.toString());
				System.out.println(bu.elseifpart.toString());
				System.out.println(bu.elsepart.toString() + " ");
			}
			// Store Branch Unit Per Method For Future References.
			BranchInfo.put(curr_met, bu);
			// Iterate Over all Objects Escaping and Find If all reasons that makes its escapes are inside this if-else block.
			Map<ObjectNode, List<Map<Integer, String>>> objectWiseResult = new HashMap<>();
			for(ObjectNode o : objectList) {
				System.out.println("\nChecking for Object: <"+ o.type + "," + o.ref + ">");
				List<Map<Integer, String>> forEachReason = new ArrayList<>();
				Local lo = getLocalObject(o, curr_met); 
				System.out.println("Received Corresponding Local Object: "+ lo.toString());
				List<EscapeReason> reasons = SpeculativeResolver.escapeReason.get(curr_met).get(o);
				for(EscapeReason er : reasons) {
					if(er == EscapeReason.escape_argument) {
						forEachReason = new ArrayList<>();
						Map<Integer, String> result; 
						result = checkforEscapingPlaces(lo, bu, er);
						if(!result.isEmpty()) {
							forEachReason.add(result);
						}
						for(Integer i : result.keySet()){
							System.out.println("ARG: AT BCI: "+ i + " IF: "+ result.get(i) + " IT ESCAPES.");
						}
					} else if(er == EscapeReason.escape_global) {
						forEachReason = new ArrayList<>();
						Map<Integer, String> result; 
						result = checkforEscapingPlaces(lo, bu, er);
						if(!result.isEmpty()) {
							forEachReason.add(result);
						}
						for(Integer i : result.keySet()){
							System.out.println("GLBL: AT BCI: "+ i + " IF: "+ result.get(i) + " IT ESCAPES.");
						}
					} else if(er == EscapeReason.escape_parameter) {
						forEachReason = new ArrayList<>();
						Map<Integer, String> result; 
						result = checkforEscapingPlaces(lo, bu, er);
						if(!result.isEmpty()) {
							forEachReason.add(result);
						}
						for(Integer i : result.keySet()){
							System.out.println("PARM: AT BCI: "+ i + " IF: "+ result.get(i) + " IT ESCAPES.");
						}
					} else if(er == EscapeReason.escape_return) {
						forEachReason = new ArrayList<>();
						Map<Integer, String> result; 
						result = checkforEscapingPlaces(lo, bu, er);
						if(!result.isEmpty()) {
							forEachReason.add(result);
						}
						for(Integer i : result.keySet()){
							System.out.println("RET: AT BCI: "+ i + " IF: "+ result.get(i) + " IT ESCAPES.");
						}
					}
					if(objectWiseResult.containsKey(o)) {
						objectWiseResult.get(o).addAll(forEachReason);
					} else {
						objectWiseResult.put(o, forEachReason);
					}
				}
				// Get the objects pointed by this object if they escape because of this object.
				List<ObjectNode> pointedBy = new ArrayList<>();
				pointedBy = GetObjectsPointedBy(o, curr_met);
				if(pointedBy.size() > 0) {
					System.out.println("Objects Pointed By: "+ pointedBy.toString());
					for(ObjectNode pb : pointedBy) {
						objectWiseResult.put(pb, objectWiseResult.get(o));
					}	
				}
			}
			for(ObjectNode o : objectWiseResult.keySet()) {
				System.out.println("Object: <"+ o.type + "," + o.ref + ">");
				for(Map<Integer, String> m : objectWiseResult.get(o)) {
					for(Integer i : m.keySet()) {
						System.out.println("AT BCI: "+ i + " IF: "+ m.get(i) + " IT ESCAPES.");
					}
				}
			}

			finalBranchResult.putIfAbsent(curr_met, new ArrayList<>());
			// Store the final results for this method.
			for(ObjectNode o : objectWiseResult.keySet()) {
				for(Map<Integer, String> m : objectWiseResult.get(o)) {
					Map.Entry<Map<Integer, String>, Integer> entry = new AbstractMap.SimpleEntry<Map<Integer, String>, Integer>(m, o.ref);
					finalBranchResult.get(curr_met).add(entry);
				}
			}
		} else {
			System.out.println("No Conditional IF Statement Found.");
		}
	}
	public static List<ObjectNode> checkIfEscapingObjectPresent(SootMethod m, Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> MergedSummaries) {
		List<ObjectNode> ret = new ArrayList<>();
		for(ObjectNode o : MergedSummaries.get(m).keySet()) {
			if(o.type == ObjectType.internal && MergedSummaries.get(m).get(o).doesEscape()) {
				ret.add(o);
			}
		}
		return ret;
	} 

	public static boolean checkIfHasConditionalIFStatment(PatchingChain<Unit> units) {
		for(Unit u : units) {
			if(u instanceof JIfStmt) {
				return true;
			}
		}
		return false;
	}

	public static BranchUnits getBranchUnits(SootMethod curr_met, PatchingChain<Unit> units, ExceptionalUnitGraph cfg) {
		Unit target = null;
		BranchUnits bu = new BranchUnits();
		bu.method = curr_met;
		int count = 0;
		int bci = 0;
		BranchType currentBranch = null;
	
		for (Unit unit : units) {

			if (unit instanceof JIfStmt) {
				// UnitBox targetBox = ((JIfStmt) unit).getTargetBox();
				// Unit targetUnit = targetBox.getUnit();
				IfStmt ifStmt = (IfStmt) unit;
				Unit targetUnit = ifStmt.getTarget();
				System.out.println("Unit: " + unit.toString() + " Target: " + targetUnit.toString());
				// if (targetUnit == null || targetUnit.toString().contains("return")) {
				// 	continue;
				// }
				// Check all predecessors of this if-statement
				boolean backEdge = false;
				for (Unit pred : cfg.getPredsOf(unit)) {
					// If a predecessor contains a goto to this unit → back edge found
					System.out.println("Pred Unit: " + pred + "Succ Unit: " + unit);
					if (pred instanceof GotoStmt) {
						GotoStmt gotoStmt = (GotoStmt) pred;
						if (gotoStmt.getTarget() == unit) {
							System.out.println("Back Edge Detected at: " + ifStmt);
							backEdge = true;
						}
					}
				}
				if(backEdge) {
					continue;
				}
				target = targetUnit;
				bci = utils.getBCI.get(unit); // Ensure valid BCI
				
				if (count == 0) {
					count++;
					currentBranch = BranchType.IF;
					bu.ifpart.BCI = bci;
					bu.ifpart.condition = ((JIfStmt) unit).getCondition();
				} else {
					currentBranch = BranchType.ELSEIF;
					BranchUnits.ElseIfpart temp = bu.new ElseIfpart();
					temp.BCI = bci;
					temp.condition = ((JIfStmt) unit).getCondition();
					bu.elseifpart.put(bci, temp);
				}
			}
	

			
			if (target != null && !unit.toString().equals(target.toString())) {
				if (currentBranch == BranchType.IF) {
					bu.ifpart.IfUnits.add(unit);
				} else if (currentBranch == BranchType.ELSEIF) {
					bu.elseifpart.get(bci).ElseIfUnits.add(unit);
				} else if (currentBranch == BranchType.ELSE) {
					bu.elsepart.ElseUnits.add(unit);
				}
			} else {
				if (currentBranch == BranchType.IF) {
					bu.ifpart.IfUnits.add(unit);
					Unit nextUnit = units.getSuccOf(unit);
					if(nextUnit instanceof JIfStmt) {
						currentBranch = BranchType.ELSEIF;
					} else {
						Unit prevUnit = units.getPredOf(unit);
						if(prevUnit instanceof JGotoStmt) {
							target = ((JGotoStmt) prevUnit).getTarget();
							// System.out.println("1. Target: " + target);
						}
						currentBranch = BranchType.ELSE;
					}
				} else if (currentBranch == BranchType.ELSEIF) {
					bu.elseifpart.get(bci).ElseIfUnits.add(unit);
					Unit nextUnit = units.getSuccOf(unit);
					if(nextUnit instanceof JIfStmt) {
						currentBranch = BranchType.ELSEIF;
					} else {
						Unit prevUnit = units.getPredOf(unit);
						if(prevUnit instanceof JGotoStmt) {
							target = ((JGotoStmt) prevUnit).getTarget();
							// System.out.println("2. Target: " + target);
						}
						currentBranch = BranchType.ELSE;
					}
				} else if (currentBranch == BranchType.ELSE) {
					target = null;
					currentBranch = null;
				}
			}
		} 
		return bu;
	}
	
	public static Local getLocalObject(ObjectNode obj, SootMethod currMethod) {
		Local ret = null;
		for(Local lo : ptgs.get(currMethod).vars.keySet()) {
			Set<ObjectNode> tmp = ptgs.get(currMethod).vars.get(lo);
			for(ObjectNode o : tmp){
				if(o.type == obj.type && o.ref == obj.ref) {
					return lo;
				}
			}

		}
		return ret;
	}

	public static Map<Integer, String>  checkforEscapingPlaces(Local lo, BranchUnits bu, EscapeReason er) {
		Map<Integer, String> result = new HashMap<>();
		// Handle Global Escape	
		if(er == EscapeReason.escape_global) {
			// Check Inside IF Part
			for(Unit u: bu.ifpart.IfUnits) {
				if(u instanceof JAssignStmt) {
					Value rhs = ((JAssignStmt)u).getRightOp();
					Value lhs = ((JAssignStmt)u).getLeftOp();
					if(rhs.toString().contains(lo.toString())) { 
						if (lhs instanceof StaticFieldRef) {
							result.put(bu.ifpart.BCI, "Taken");
						}
					}
				}
			}
			// Check Inside ElseIf Part
			for(Integer bci: bu.elseifpart.keySet()) {
				for(Unit u: bu.elseifpart.get(bci).ElseIfUnits) {
					if(u instanceof JAssignStmt) {
						Value rhs = ((JAssignStmt)u).getRightOp();
						Value lhs = ((JAssignStmt)u).getLeftOp();
						if(rhs.toString().contains(lo.toString())) {
							if (lhs instanceof StaticFieldRef) {
								result.put(bci, "Taken");
							}
						}
					}
				}
			}
			// Check Inside Else Part
			for(Unit u: bu.elsepart.ElseUnits) {
				if(u instanceof JAssignStmt) {
					Value rhs = ((JAssignStmt)u).getRightOp();
					Value lhs = ((JAssignStmt)u).getLeftOp();
					if(rhs.toString().contains(lo.toString())) {
						if (lhs instanceof StaticFieldRef) {
							result.put(bu.elsepart.BCI, "Taken");
						}
					}
				}
			}
		} 
		// Handle Argument Escape
		else if(er == EscapeReason.escape_argument) {
			// Check Inside IF Part
			for(Unit u: bu.ifpart.IfUnits) {
				if(u instanceof JAssignStmt) {
					Value rhs = ((JAssignStmt)u).getRightOp();
					Value lhs = ((JAssignStmt)u).getLeftOp();
					if(rhs.toString().contains(lo.toString())) { 
						if (lhs instanceof JInstanceFieldRef) {
							Local lhsBase = (Local) ((JInstanceFieldRef)lhs).getBase();
							if(lhsBase instanceof Local) {
//								System.out.println("Unit : "+ u + "LHS Base: "+ lhsBase);
								result.put(bu.ifpart.BCI, "Taken");
							}
						}
					}
				}
			}
			// Check Inside ElseIf Part
			for(Integer bci: bu.elseifpart.keySet()) {
				for(Unit u: bu.elseifpart.get(bci).ElseIfUnits) {
					if(u instanceof JAssignStmt) {
						Value rhs = ((JAssignStmt)u).getRightOp();
						Value lhs = ((JAssignStmt)u).getLeftOp();
						if(rhs.toString().contains(lo.toString())) {
							if (lhs instanceof JInstanceFieldRef) {
								Local lhsBase = (Local) ((JInstanceFieldRef)lhs).getBase();
								if(lhsBase instanceof Local) {
//									System.out.println("Unit : "+ u + "LHS Base: "+ lhsBase);
									result.put(bci, "Taken");
								}
							}
						}
					}
				}
			}
			// Check Inside Else Part
			for(Unit u: bu.elsepart.ElseUnits) {
				if(u instanceof JAssignStmt) {
					Value rhs = ((JAssignStmt)u).getRightOp();
					Value lhs = ((JAssignStmt)u).getLeftOp();
					if(rhs.toString().contains(lo.toString())) {
						if (lhs instanceof JInstanceFieldRef) {
							Local lhsBase = (Local) ((JInstanceFieldRef)lhs).getBase();
							if(lhsBase instanceof Local) {
//								System.out.println("Unit : "+ u + "LHS Base: "+ lhsBase);
								result.put(bu.elsepart.BCI, "Taken");
							}
						}
					}
				}
			}
		} 
		// Handle Parameter Escape
		else if(er == EscapeReason.escape_parameter) {
			// Check Inside IF Part
			for(Unit u: bu.ifpart.IfUnits) {
				if(u instanceof JInvokeStmt) {
					Value invokestmt = ((JInvokeStmt)u).getInvokeExpr();
					invokestmt.getUseBoxes().forEach(use -> {
						if(use.getValue().toString().contains(lo.toString())) {
							result.put(bu.ifpart.BCI, "Taken");
						}
					});
				}
			}
			// Check Inside ElseIf Part
			for(Integer bci: bu.elseifpart.keySet()) {
				for(Unit u: bu.elseifpart.get(bci).ElseIfUnits) {
					if(u instanceof JInvokeStmt) {
						Value invokestmt = ((JInvokeStmt)u).getInvokeExpr();
						invokestmt.getUseBoxes().forEach(use -> {
							if(use.getValue().toString().contains(lo.toString())) {
								result.put(bci, "Taken");
							}
						});
					}
				}
			}
			// Check Inside Else Part
			for(Unit u: bu.elsepart.ElseUnits) {
				if(u instanceof JInvokeStmt) {
					Value invokestmt = ((JInvokeStmt)u).getInvokeExpr();
					invokestmt.getUseBoxes().forEach(use -> {
						if(use.getValue().toString().contains(lo.toString())) {
							result.put(bu.elsepart.BCI, "Taken");
						}
					});
				}
			}
		} 
		// Handle Return Escape
		else if(er == EscapeReason.escape_return) {
			// Check Inside IF Part
			for(Unit u: bu.ifpart.IfUnits) {
				if(u instanceof JReturnStmt) {
					Value ret = ((JReturnStmt)u).getOp();
					if(ret.toString().contains(lo.toString())) {
							result.put(bu.ifpart.BCI, "Taken");
					}
				}
			}
			// Check Inside ElseIf Part
			for(Integer bci: bu.elseifpart.keySet()) {
				for(Unit u: bu.elseifpart.get(bci).ElseIfUnits) {
					if(u instanceof JReturnStmt) {
						Value ret = ((JReturnStmt)u).getOp();
						if(ret.toString().contains(lo.toString())) {
							result.put(bci, "Taken");
						}
					}
				}
			}
			// Check Inside Else Part
			for(Unit u: bu.elsepart.ElseUnits) {
				if(u instanceof JReturnStmt) {
					Value ret = ((JReturnStmt)u).getOp();
					if(ret.toString().contains(lo.toString())) {
						result.put(bu.elsepart.BCI, "Taken");
					}
				}
			}
		}
		return result;
	}

	public static List<ObjectNode> GetObjectsPointedBy(ObjectNode obj, SootMethod currMethod) {
		List<ObjectNode> ret = new ArrayList<>();
		if(ptgs.get(currMethod).fields.containsKey(obj)) {
			for(SootField f : ptgs.get(currMethod).fields.get(obj).keySet()) {
				for(ObjectNode o : ptgs.get(currMethod).fields.get(obj).get(f)) {
					boolean ifHasSameCVs = false;
					ifHasSameCVs = checkIfHasSameCVs(obj, o, currMethod);
					if(ifHasSameCVs) {
						ret.add(o);
					} 
				}
			}
		}
		return ret;
	}

	public static boolean checkIfHasSameCVs(ObjectNode obj1, ObjectNode obj2, SootMethod currMethod) {
		EscapeStatus e1 = StaticAnalyser.summaries.get(currMethod).get(obj1);
		EscapeStatus e2 = StaticAnalyser.summaries.get(currMethod).get(obj2);
		for(EscapeState es : e1.status) {
			if(!e2.status.contains(es)) {
				return false;
			}
		}
		return true;
	}
}
