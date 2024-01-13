/* Testcase 2: Check mutilevel of the polymorphic call-sites.
 * Currently if escapes at any level it makes all its dependencies and dependees escape.
 *
 * Objective: Statically the objective over here wil be at line 38 if speculate that at runtime
 * the object pointer to by o2 at line 38 will be of type ChildNode, in that case these list
 * of objects will not escape.
 */
class Node {
	public static Node global;
	Node field1;
	void foo(Node p1) {
		//Node o1 = new Node();
		if (p1 instanceof Node) {
			new ChildNode().bar(p1);
		} else {
			new Node().bar(p1);
		}
	}
	void bar(Node p2){
		global = p2;
	}
}
class ChildNode extends Node {
	void foo(Node p1) {
		//Node o1 = new Node();
		if (p1 instanceof Node) {
			new Node().bar(p1);
		} else {
			new ChildNode().bar(p1);
		}
	}
	void bar(Node p1) {
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
	}
}

