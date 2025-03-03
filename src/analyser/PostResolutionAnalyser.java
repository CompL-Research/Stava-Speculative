package analyser;

import es.EscapeReason;
import es.EscapeStatus;
import handlers.*;
import ptg.*;
import resolver.SpeculativeResolver;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import utils.IllegalBCIException;
import soot.Local;
import java.util.*;

import branch.BranchUnits;
import branch.BranchUnits.Ifpart;


public class PostResolutionAnalyser extends BodyTransformer {
	// Store Branch Info Per method.
	public static Map<SootMethod, BranchUnits> BranchInfo;
	static Map<SootMethod, PointsToGraph> ptgs;
	public static Map<SootMethod, Map.Entry<Map<Integer, String>, List<Integer>>> branchResult;
	public PostResolutionAnalyser ( Map<SootMethod, PointsToGraph> ptgs) {
		super();
		BranchInfo = new HashMap<>();
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
		System.out.println("Method Name: " + curr_met.getBytecodeSignature());
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
			System.out.println("Escaping Object: <"+ o.type + "," + o.ref + ">");
		}

		// Check 2: If Method has conditional "IF" Statement.
		boolean hasIF = false;
		hasIF = checkIfHasConditionalIFStatment(units);
		if(hasIF) {
			BranchUnits bu;
			bu = getBranchUnits(curr_met, units);
			if(bu != null && bu.ifpart != null ) {
				System.out.println("Branch Units: ");
				System.out.println(bu.ifpart.toString());
				System.out.println(bu.elseifpart.toString());
				System.out.println(bu.elsepart.toString() + " ");
			}

			for(ObjectNode o : objectList) {
				Local lo = getLocalObject(o, curr_met); 
				System.out.println("Received Local Object: "+ lo.toString());
				List<EscapeReason> reasons = SpeculativeResolver.escapeReason.get(curr_met).get(o);
				for(EscapeReason er : reasons) {
					if(er == EscapeReason.escape_argument) {

					} else if(er == EscapeReason.escape_global) {
						Map<Integer, String> result; 
						result = checkforEscapingPlaces(lo, bu, er);
						for(Integer i : result.keySet()){
							System.out.println("For BCI: "+ i + " If: "+ result.get(i) + " it Escapes.");
						}
					} else if(er == EscapeReason.escape_parameter) {

					} else if(er == EscapeReason.escape_return) {

					} 
				}
			}
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

	public static BranchUnits getBranchUnits(SootMethod curr_met, PatchingChain<Unit> units) {
		Unit target = null;
		BranchUnits bu = new BranchUnits();
		bu.method = curr_met;
		int count = 0;
		int bci = 0;
		BranchType currentBranch = null;
	
		for (Unit unit : units) {
			if (unit instanceof JIfStmt) {
				UnitBox targetBox = ((JIfStmt) unit).getTargetBox();
				Unit targetUnit = targetBox.getUnit();
				
				if (targetUnit.toString().contains("return")) {
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
	
			System.out.println("Unit: " + unit.toString() + " Target: " + target);
			
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
	
	// /**
	//  * Determines the next branch type based on the current unit and control flow.
	//  */
	// private static BranchType getNextBranchType(Unit unit, PatchingChain<Unit> units) {
	// 	Unit nextUnit = units.getSuccOf(unit);
	// 	if (nextUnit instanceof JIfStmt) {
	// 		return BranchType.ELSEIF;
	// 	} else {
	// 		Unit prevUnit = units.getPredOf(unit);
	// 		if (prevUnit instanceof JGotoStmt) {
	// 			return BranchType.ELSE;
	// 		}
	// 	}
	// 	return BranchType.ELSE;
	// }
	
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
		if(er == EscapeReason.escape_global) {
			// Check Inside IF Part
			for(Unit u: bu.ifpart.IfUnits) {
				if(u instanceof JAssignStmt) {
					Value rhs = ((JAssignStmt)u).getRightOp();
					if(rhs.toString().contains(lo.toString()) ) {
						result.put(bu.ifpart.BCI, "Taken");
					}
				}
			}
			// Check Inside ElseIf Part
			for(Integer bci: bu.elseifpart.keySet()) {
				for(Unit u: bu.elseifpart.get(bci).ElseIfUnits) {
					if(u instanceof JAssignStmt) {
						Value rhs = ((JAssignStmt)u).getRightOp();
						if(rhs.toString().contains(lo.toString()) ) {
							result.put(bci, "Taken");
						}
					}
				}
			}
			// Check Inside Else Part
			for(Unit u: bu.elsepart.ElseUnits) {
				if(u instanceof JAssignStmt) {
					Value rhs = ((JAssignStmt)u).getRightOp();
					if(rhs.toString().contains(lo.toString()) ) {
						result.put(bu.elsepart.BCI, "Taken");
					}
				}
			}
		}
		return result;
	}
}
