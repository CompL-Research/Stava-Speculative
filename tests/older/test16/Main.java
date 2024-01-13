// Testcase: Check 1 level of the polymorphic callsite
// Currently if it escapes in either of them it escapes in the caller.


class Node {
	static Node a;
	public static Node global;
	public Node() {
		a = this;
	}
	void foo(Node p1) {
		Node o1 = new ChildNode();
		Node o2 = new Node();
		if (p1 instanceof Node) {
			o1.bar(o2);
		} else {
			o1.bar(o2);
		}
	}
	void bar(Node p2){
		global = p2;
//		Node o1 = new Node();
//		o1.fb2();
	}
	void fb2() { System.out.println(this); }
}
class ChildNode extends Node {
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
		o2.bar(o1);
	}
}

