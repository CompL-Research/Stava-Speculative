import java.*;

class Node {
	public Node n;
}

class newchild extends Node {
	public Node m;
}

public class Main {
	public static Node A;
	public static Node global;
	public static void main(String[] args) {
		for(int i= 0; i< 1000000; i++) {
			Node B = new Node(); // <internal,0>
			Node C = new Node(); // <internal,8>
			Node D = new newchild(); // <internal,16>
			Node R = foo(C, D);
			System.err.println(i +" "+ R);
		}
	}
	public static Node foo(Node p1, Node p2) {
		Node E = new Node(); // <internal,0>
		if (p2 instanceof newchild) {
			new child().bar(p1, E);
		} else {
			new Main().bar(p1, E);
		}
		return new Node();
	}
	public  void bar(Node p3, Node p4) {
		if(p3 instanceof Node) {
			new Main().foobar(p3, p4);
		} else {
			new child().foobar(p3, p4);
		}
	}
	public  void foobar(Node p5, Node p6) {
		p5.n = p6;
	}

}
class child extends Main {
	public  void bar(Node p3, Node p4) {
		if(p3 instanceof Node) {
			new Main().foobar(p3, p4);
		} else {
			new child().foobar(p3, p4);
		}
	}
	public  void foobar(Node p5, Node p6) {
		A = p6;
		global = p5;
		p5.n = p6;
	}

}


//Main.foo(LNode;LNode;)V [0, 8]
//Main.main([Ljava/lang/String;)V [0, 8, 16]