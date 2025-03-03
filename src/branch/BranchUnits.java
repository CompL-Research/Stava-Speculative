package branch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.*;

public class BranchUnits {
    public SootMethod method;
    public Ifpart ifpart;
    public Map<Integer, ElseIfpart> elseifpart;
    public Elsepart elsepart;

    public BranchUnits() {
        this.ifpart = new Ifpart();
        this.elseifpart = new HashMap<>();
        this.elsepart = new Elsepart();
    }

    public String getBranchUnitsInfo() {
        return this.ifpart.toString() + this.elseifpart.toString() + this.elsepart.toString();
    }

    public class Ifpart {
        public int BCI;
        public Value condition;
        public List<Unit> IfUnits;
        public Ifpart() {
            this.IfUnits = new ArrayList<>();
        }
        public  String toString() {
            if(!this.IfUnits.isEmpty()) {
                System.out.println(" ================================================");
                return "IF PART--> BCI: " + this.BCI + " CONDITION: " + this.condition.toString() + "\n Units: " + this.IfUnits.toString();
            }
            return null;
        }
    }

    public class ElseIfpart {
        public int BCI;
        public Value condition;
        public List<Unit> ElseIfUnits;
        public ElseIfpart() {
            this.ElseIfUnits = new ArrayList<>();
        }
        public  String toString() {
            if(!this.ElseIfUnits.isEmpty()) {
                System.out.println(" ================================================");
                return "ELSE IF PART--> BCI: " + this.BCI + " CONDITION: " + this.condition.toString() + "\n Units: " + this.ElseIfUnits.toString();
            }
            return null;
        }
    }

    public class Elsepart {
        public int BCI;
        public List<Unit> ElseUnits;
        public Elsepart() {
            this.BCI = -5;
            this.ElseUnits = new ArrayList<>();
        }
        public  String toString() {
            if(!this.ElseUnits.isEmpty()) {
                System.out.println(" ================================================");
                return "ELSE PART--> BCI: " + this.BCI + "\n Units: " + this.ElseUnits.toString();
            }
            return null;
        }
    }
}
