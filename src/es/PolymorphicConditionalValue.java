package es;

import ptg.ObjectNode;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class PolymorphicConditionalValue extends EscapeState {

    public final int BCI;
    public List<SootClass> types;
    public List<Integer> object;



    public PolymorphicConditionalValue(int bci, List<SootClass> types, List<Integer> object) {
            this.BCI = bci;
            this.types = types != null ? types : new ArrayList<>();
            this.object = object != null ? object : new ArrayList<>();
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(BCI);
        sb.append(" ");
        sb.append("{");
        int i = 0;
        for(SootClass s: types) {
            sb.append(s);
            i++;
            if (i < (types.size())) sb.append(",");
        }
        sb.append("}");
        i = 0;
        sb.append(" ");
        sb.append("[");
        for(Integer o: object) {
            sb.append(o);
            i++;
            if (i < (object.size())) sb.append(",");
        }
        sb.append("]");
        //sb.append("]");
        return sb.toString();
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolymorphicConditionalValue that = (PolymorphicConditionalValue) o;
        return BCI == that.BCI &&
                types.equals(that.types) &&
                object.equals(that.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(BCI, types, object);
    }
}
