class BranchSpecOpt {
    BranchSpecOpt f1;
    static BranchSpecOpt global;
    public static void main(String args[]) {
        for(int i = 0; i <100000; i++) {
            BranchSpecOpt o1;
            if (i < 10000) {
                o1 = new BranchSpecOpt();
            } else if(i>= 10000 && i < 50000) {
                o1 = new Child();
            } else {
                o1 = new Child2();
            }
            BranchSpecOpt o2 = new BranchSpecOpt();
            o2.foo(o1); // Callsite 1 with only type as BranchSpecOpt.
        }
    }

    void foo(BranchSpecOpt p1) {
        BranchSpecOpt o2 = new BranchSpecOpt();
        for(int j = 0; j<1000; j++) {
            if(p1 instanceof Child) {
                global = o2;
                o2.f1 = new BranchSpecOpt();
            } else if (p1 instanceof Child2){
                o2.f1 = new BranchSpecOpt();;
            } else {
                o2.f1 = new BranchSpecOpt();
            }
            o2.f1.bar(new BranchSpecOpt()); // Polymorphic Callsite: 3 types
            new BranchSpecOpt().bar(new BranchSpecOpt());
        }
    }

    void bar(BranchSpecOpt p1) {
        BranchSpecOpt o4 = new BranchSpecOpt();
        p1.f1 = o4;
    }
}

class Child extends BranchSpecOpt {
    void bar(BranchSpecOpt p3) {
        BranchSpecOpt o4 = new BranchSpecOpt();
        p3.f1 = o4;
    }
}

class Child2 extends BranchSpecOpt {
    public static BranchSpecOpt global;
    void bar(BranchSpecOpt p3) {
        BranchSpecOpt o4 = new BranchSpecOpt();
        global = p3;
        p3.f1 = new BranchSpecOpt();
    }
}