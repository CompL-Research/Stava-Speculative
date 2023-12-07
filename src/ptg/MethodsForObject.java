package ptg;

import es.EscapeStatus;
import soot.SootMethod;
import utils.getBCI;
import java.util.HashMap;
import java.util.Map;

public class MethodsForObject {

    public SootMethod calleeMethod;
    public getBCI bci;

    public int hashCode() {
        return calleeMethod.hashCode()%13;
    }
}

