package utils;

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.internal.*;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.Tag;

public class getBCI {
	public static int get(Unit u) throws IllegalBCIException{
		int _ret = -1;
		boolean invokeflag = false;

		/*
			We are splitting the unit into "boxes" and for semi-boxes we check if any one of 
			them is JNewArray ot JNewMultiArray, then return BCI of the expression.

			I don't know if this will always work. It is based on the data I got by printing
			a lot of stuff. 

			Do We need to include JNew in this?
		*/
		System.out.println("[GETBCI]: for "+u);
//		System.out.println("Boxes: "+u.getUseBoxes());
		for(ValueBox ub: u.getUseBoxes() ) {
			/*
				This loop is to get the BCI specifically in the case of JNewArrayExpr and JNewMultiArrayExpr.
				Each Unit in soot is constructed of multiple sub-Units. So, we reverse engineer the problematic
				statements. We were getting problems only in AssignStmts and we need to get BCI of statement
				on the right side of "=". This below line filters only the right side of an Assignment Statement.
			*/
//			if ( !ub.getClass().toString().equals("class soot.jimple.internal.JAssignStmt$LinkedRValueBox") )
//				continue;
	
			/*
				Next get the value contained in this box. If this value is a New Array/MultiArray Expr only
				then we need to move further.
			 */
            Value v = ub.getValue();
//			System.out.println(v.getClass().getName());
			if (v == null)
				continue;
//			System.out.println(v.getClass().getName());
			if(v instanceof  JAssignStmt) {
				Value lhs = ((JAssignStmt) v).getLeftOp();
				Value rhs = ((JAssignStmt) v).getRightOp();
				if(rhs instanceof VirtualInvokeExpr) {
					invokeflag = true;
					continue;
				}
			}

			if(v instanceof JVirtualInvokeExpr) {
//				System.out.println("UseBox : "+ v.getUseBoxes().toString());
//				System.out.println("Base Box: "+ ((JVirtualInvokeExpr) v).getBaseBox());
				if(((VirtualInvokeExpr) v).getBaseBox().toString().contains("$")) {
//					System.out.println("True");
					invokeflag = true;
					continue;
				}
			}


			// System.err.println(v.getClass().getName());
            if ( ! (v instanceof JNewArrayExpr || v instanceof JNewMultiArrayExpr) )
                continue;
//			 System.out.println("This is a new/JVirtualInvoke Expr, tags: "+ub.getTags());

			 /*
				Because multiple statements in the Java classfile can be combined together to form 
				one JimpleStatment, BCI is linked to each box rather than the Unit. BCI of a Unit is 
				the BCI of last box processed in the Unit. Now, we get the BCI of this
				required box and return it. If this BCI is null, we can fall back to the BCI associated with 
				the Unit.
			*/
			BytecodeOffsetTag tg = (BytecodeOffsetTag) ub.getTag("BytecodeOffsetTag");
			if (tg != null)
				return tg.getBytecodeOffset();
		}

		Tag t = u.getTag("BytecodeOffsetTag");
		if (t == null) {
			return -1;
//			String error = u.toString() + " doesn't have bytecodeoffset!";
//			System.out.println("[utils.getBCI] [Warn] " + error);
//			throw new IllegalBCIException(error);
		}
		try {
			_ret = ((BytecodeOffsetTag) u.getTag("BytecodeOffsetTag")).getBytecodeOffset();
		} catch (Exception e) {
			System.out.println(u);
			System.out.println("GET BCI: "+ e);
			throw e;

		}
		if(invokeflag) {
			_ret = _ret - 3;
		}
		System.out.println("The returned BCI is: "+ _ret);
		return _ret;
	}
}
