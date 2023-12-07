package es;

import ptg.ObjectNode;
import soot.SootField;
import soot.SootMethod;

import java.util.List;

public class GECondtionalvalue extends ConditionalValue{

    public GECondtionalvalue(SootMethod m, ObjectNode obj, List<SootField> fl, Boolean isReal, int bci) {
        super(m, obj, fl, isReal, bci);
    }
}
