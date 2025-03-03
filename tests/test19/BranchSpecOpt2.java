class BranchSpecOpt2 {
    BranchSpecOpt2 f1;
    static BranchSpecOpt2 global;
    public static void main(String args[]) {
        for(int i = 0; i <1000; i++) {
            BranchSpecOpt2 o2 = new BranchSpecOpt2();
            o2.foo(); // Callsite 1 with only type as BranchSpecOpt2.
        }
    }

    void foo() {
        BranchSpecOpt2 o2;
        for(int i = 0; i<100000; i++) {
            if(i%2==0) {
                o2 = new BranchSpecOpt2();
            } else {
                o2 = new Child2();
                global = o2;
            }
            o2.bar(new BranchSpecOpt2()); // Polymorphic Callsite: 3 types
        }
    }

    void bar(BranchSpecOpt2 p1) {
        BranchSpecOpt2 o4 = new BranchSpecOpt2();
        p1.f1 = o4;
    }
}

class Child extends BranchSpecOpt2 {
    void bar(BranchSpecOpt2 p3) {
        BranchSpecOpt2 o4 = new BranchSpecOpt2();
        p3.f1 = o4;
    }
}

class Child2 extends BranchSpecOpt2 {
    public static BranchSpecOpt2 global;
    void bar(BranchSpecOpt2 p3) {
        BranchSpecOpt2 o4 = new BranchSpecOpt2();
//        global = p3;
        p3.f1 = new BranchSpecOpt2();
    }
}

