class BranchSpecOpt3 {
    BranchSpecOpt3 f1;
    static BranchSpecOpt3 global2;
    public static void main(String args[]) {
        for(int i = 0; i <1000; i++) {
            BranchSpecOpt3 o2 = new BranchSpecOpt3();
            BranchSpecOpt3 o3 = o2.foo(new BranchSpecOpt3()); // Callsite 1 with only type as BranchSpecOpt3.
        }
    }

    BranchSpecOpt3 foo(BranchSpecOpt3 p1) {
        BranchSpecOpt3 o1 = new BranchSpecOpt3();
        BranchSpecOpt3 o2 = new BranchSpecOpt3();
        BranchSpecOpt3 o3 = new BranchSpecOpt3();
        for(int i = 0; i<100000; i++) {
            if(i%2==0) {
                o2 = new BranchSpecOpt3();
//                o3 = o1;
//                return o1;
            } else if(i%3==0) {
                o2 = new Child2();
                o2.bar(o1);
            } else {
                o2 = new Child();
                global2 = o3;
            }
            o2.bar(new BranchSpecOpt3()); // Polymorphic Callsite: 3 types
        }
        o1.f1 = new BranchSpecOpt3();
        return new BranchSpecOpt3();
    }

    void bar(BranchSpecOpt3 p1) {
        BranchSpecOpt3 o4 = new BranchSpecOpt3();
        p1.f1 = o4;
    }
}

class Child extends BranchSpecOpt3 {
    void bar(BranchSpecOpt3 p3) {
        BranchSpecOpt3 o4 = new BranchSpecOpt3();
        p3.f1 = o4;
    }
}

class Child2 extends BranchSpecOpt3 {
    public static BranchSpecOpt3 global;
    void bar(BranchSpecOpt3 p3) {
        BranchSpecOpt3 o4 = new BranchSpecOpt3();
        global = p3;
        p3.f1 = new BranchSpecOpt3();
    }
}
