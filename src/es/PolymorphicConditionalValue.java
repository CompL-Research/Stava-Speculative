package es;

import ptg.ObjectNode;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

import java.util.Iterator;

public class PolymorphicConditionalValue extends EscapeState {
    public final int source_object;
    public final SootMethod caller_method;
    public final int object;
    public final int BCI;
    public final SootClass type;


    public PolymorphicConditionalValue(int sobj, SootMethod m, int obj, int bci, SootClass type) {
        source_object = sobj;
        caller_method = m;
        object = obj;
        BCI = bci;
        this.type = type;

    }

    public String toString() {
        StringBuilder sb = new StringBuilder("<<");
        sb.append(source_object + ",");
        sb.append(caller_method.toString() + ",");
        sb.append(object);
        sb.append(">");
        sb.append(","+ BCI);
        sb.append(","+ type);
        sb.append(">");
        return sb.toString();
    }

}
