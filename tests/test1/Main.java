/* Testcase 1: Check 1-level of the polymorphic call-sites.
 * Currently if it escapes in either of them it escapes in the caller.
 *  			...
 *            a.foo()
 *				|
 * 		  --------------
 *
 *		foo1()		foo2()
 * Objective: Statically the objective over here will be at line 38 if speculate that at runtime
 * the object pointer to by o2 at line 38 will be of type ChildNode, in that case these list
 * of objects will not escape.
 */

class Node {
	public static Node global;
	Node field1;
	void foo(Node p1){
		// Escapes Here
		global = p1;
		// No Escape Case
		// Node o1 = new Node();
		// o1.fb2();
	}
	void fb2() { System.out.println(this); }
}
class ChildNode extends Node {
	void foo(Node p1) {
		ChildNode o1 = new ChildNode();
		o1.fb();
	}
	void fb() {
		System.out.println(this);
	}
}
public class Main {
	public static void main(String[] args) {
		Node o1 = new Node();
		Node o2;
		if(args.length > 1) {
			o2 = new Node();
		} else {
			o2 = new ChildNode();
		}
		o2.foo(o1);
		o1.field1 = new Node();
	}
}

