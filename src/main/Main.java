package main;

import analyser.StaticAnalyser;
import config.StoreEscape;
import counters.PolymorphicInvokeCounter;
import es.*;
import ptg.ObjectNode;
import ptg.ObjectType;
import ptg.PointsToGraph;
import resolver.SpeculativeResolver;
import soot.PackManager;
import soot.Scene;
import soot.options.Options;
import soot.SootMethod;
import soot.Transform;
import utils.GetListOfNoEscapeObjects;
import utils.Stats;
import Inlining.PrintInlineInfo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.*;
import java.io.*;
import java.lang.*;
import static utils.KillCallerOnly.kill;

public class Main {

	public static int i = 0;
	public static Set<String> ListofMethods = new HashSet<>();
	static void setStoreEscapeOptions(String[] args) {
		if (args.length >=6 ) {
            StoreEscape.ReduceParamDependence = args[5].equals("true");
		}
		if (args.length >= 7) {
            StoreEscape.MarkParamReturnEscaping = args[6].equals("true");
		}
	}
	public static void main(String[] args) {

		GetSootArgs g = new GetSootArgs();
		String[] sootArgs = g.get(args);
		setStoreEscapeOptions(args);
		if (sootArgs == null) {
			System.out.println("Unable to generate args for soot!");
			return;
		}
		// File path is passed as parameter

////
//		try {
//			File file = new File("/home/aditya/Documents/Research-Workspace/Stava/stava-contextual/fft-jitc-methodlist.txt");
//			BufferedReader br = null;
//			br = new BufferedReader(new FileReader(file));
//			String st;
//			while ((st = br.readLine()) != null) {
//				ListofMethods.add(st);
//			}
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//		try {
//			File file = new File("/home/aditya/Documents/Research-Workspace/Stava/stava-contextual/fft-jitc-methodlist.txt");
//			BufferedReader br = null;
//			br = new BufferedReader(new FileReader(file));
//			String st;
//			while ((st = br.readLine()) != null) {
//				ListofMethods.add(st);
//			}
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//		System.out.println("Read from file: ");
//		int j =0;
//		for(String s : ListofMethods) {
//			System.out.println(j++ + ". Method : "+ s);
//		}
//		System.out.println();
		/*
		 * 0. Compute the list of Polymporphic Invokes and affected objects
		 */
		PolymorphicInvokeCounter pic = new PolymorphicInvokeCounter();

		System.out.println("\n 1. Static Analysis starts: ");
		StaticAnalyser staticAnalyser = new StaticAnalyser();
		CHATransform prepass = new CHATransform();
		PackManager.v().getPack("wjap").add(new Transform("wjap.pre", prepass));
		PackManager.v().getPack("jtp").add(new Transform("jtp.sample", staticAnalyser));
		long analysis_start = System.currentTimeMillis();
		Options.v().parse(sootArgs);
		Scene.v().loadNecessaryClasses();
		Scene.v().loadDynamicClasses();
		//List<SootMethod> entryPoints = Scene.v().getEntryPoints();
		// SootClass sc = Scene.v().loadClassAndSupport("java.lang.CharacterData");
		// System.out.println(sc.getMethods());
		// Scene.v().forceResolve(sc.getName(), SootClass.BODIES);
		// SootMethod tobeAdded = sc.getMethodByName("toUpperCaseEx");
		// System.out.println("Method: "+tobeAdded);
		// // SootMethod tobeAdded = Scene.v().getMethod("<java.lang.CharacterData: toUpperCaseEx(I)I>");
		// entryPoints.add(tobeAdded);

//		Chain<SootClass> appClasses = Scene.v().getClasses();
//		Iterator<SootClass> appClassItertator = appClasses.iterator();
//		SootClass objclass = Scene.v().getSootClass("java.lang.Object");
//		while(appClassItertator.hasNext()) {
//			SootClass aclass = appClassItertator.next();
//			if (aclass.getName().contains("spec.")) {
//				aclass.setApplicationClass();
//				if (aclass.hasSuperclass() == false) {
//					if (aclass == objclass) {
//						continue;
//					}
//					else aclass.setSuperclass(objclass);
//				}
//				else {
//					// System.out.println("SuperClass: "+aclass.getSuperclass());
//				}
//			}
		// 	aclass = Scene.v().loadClassAndSupport(aclass.getName());
		// 	aclass = Scene.v().forceResolve(aclass.getName(), SootClass.BODIES);
		// 	// if (aclass.getName().contains("spec.validity.Digests")) {
		// 	// 	System.out.println("Aclass spec: "+aclass.getName()+" : "+aclass.getMethodByName("crunch_jars"));
		// 	// }
		// 	System.out.println("Aclass: "+aclass.getName()+ " phantom: "+aclass.isPhantomClass()+" app: "+aclass.isApplicationClass()+" Concrete: "+
		// 		aclass.isConcrete()+" : " + aclass.getMethods());
		// 	// System.out.println(aclass.getMethods());
			// entryPoints.addAll(aclass.getMethods());
		//}
		// System.out.println(entryPoints);
		// if (true) 
		// 	return;
		//Scene.v().setEntryPoints(entryPoints);
		PackManager.v().runPacks();
		// soot.Main.main(sootArgs);
		long analysis_end = System.currentTimeMillis();
		System.out.println("Static Analysis is done!");
		System.out.println("Time Taken:"+(analysis_end-analysis_start)/1000F);
		System.out.println("**********************************************************");
		long res_start = System.currentTimeMillis();

//		for(SootMethod m : StaticAnalyser.summaries.keySet()) {
//			if(!m.isJavaLibraryMethod()) {
//				System.out.println("Method : " + m);
//				for (ObjectNode o : StaticAnalyser.summaries.get(m).keySet()) {
//					System.out.println(" For object : " + o);
//					System.out.println(" Summaries : ");
//					System.out.println(StaticAnalyser.summaries.get(m).get(o).status);
//				}
//				System.out.println("----------");
//			}
//		}


		// printSummary(staticAnalyser.summaries);
		// System.err.println(staticAnalyser.ptgs);
		printAllInfo(StaticAnalyser.ptgs, StaticAnalyser.summaries, args[4]);
		// if (true)
		// 	return;
		// printCFG();
		System.out.println("2. Contextual Resolution Starts : ");
        SpeculativeResolver sr = new SpeculativeResolver(StaticAnalyser.summaries,
															StaticAnalyser.ptgs,
															StaticAnalyser.noBCIMethods);
        long res_end = System.currentTimeMillis();
        System.out.println("Resolution is done");
        System.out.println("Time Taken in phase 1:"+(analysis_end-analysis_start)/1000F);
        System.out.println("Time Taken in phase 2:"+(res_end-res_start)/1000F);

        // System.out.println(staticAnalyser.summaries.size()+ " "+staticAnalyser.ptgs.size());


        HashMap<SootMethod, HashMap<ObjectNode, EscapeStatus>> resolved = (HashMap) kill(SpeculativeResolver.solvedSummaries);

        HashMap<SootMethod, HashMap<ObjectNode, List<ContextualEscapeStatus>>> cresolved = (HashMap) (SpeculativeResolver.solvedContextualSummaries);

        printAllInfo(StaticAnalyser.ptgs, resolved, args[4]);

        //printContextualInfo(StaticAnalyser.ptgs, cresolved, args[4]);

        //printCombinedInfo(StaticAnalyser.ptgs, resolved, cresolved, args[4]);

        //saveStats(cr.existingSummaries, resolved, args[4], staticAnalyser.ptgs);

        saveConStats(SpeculativeResolver.existingSummaries, resolved, SpeculativeResolver.inlineSummaries, args[4], StaticAnalyser.ptgs);
        if(args[5] != null && args[5].equals("inline")) {
            printContReswitinlineForJVM(SpeculativeResolver.solvedSummaries, SpeculativeResolver.inlineSummaries, args[2], args[4]);
        } else {
            printContResForJVM(SpeculativeResolver.solvedSummaries, SpeculativeResolver.inlineSummaries, args[2], args[4]);
        }
        //printCVresValues(ContextualResolver.ResolvedCVValue, args[4]);
        for(SootMethod m : SpeculativeResolver.solvedSummaries.keySet()) {
            if(Main.ListofMethods.toString().contains(m.getBytecodeSignature().toString())) {
                System.err.println("Method : " + m);
                for (ObjectNode o : SpeculativeResolver.solvedSummaries.get(m).keySet()) {
                    if(o.type == ObjectType.internal) {
                        System.err.print(" For object : " + o);
                        System.err.println(SpeculativeResolver.solvedSummaries.get(m).get(o).status);
                    }
                }
                System.err.println("----------");
            }
        }
    }



	static void printCFG() {
		try {
			FileWriter f = new FileWriter("cfg1.txt");
			f.write(Scene.v().getCallGraph().toString());
			f.write(CHATransform.getCHA().toString());
			f.close();
		}
		catch( Exception e) {
			System.err.println("WHILE PRINTING CFG: Exception occured " + e);
		}
	}

	static void printSummary(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> existingSummaries) {
		try {
            FileWriter f = new FileWriter("sum1.txt");
			// f.write(existingSummaries.toString());
			for (SootMethod sm: existingSummaries.keySet()) {
				HashMap<ObjectNode, EscapeStatus> hm = existingSummaries.get(sm);
				int hash = 0;
				List<ObjectNode> lobj = new ArrayList<>(hm.keySet());
				Collections.sort(lobj, new Comparator<ObjectNode>(){
					public int compare(ObjectNode a, ObjectNode b)
						{
							return a.toString().compareTo(b.toString());
						}
				});
				f.write(sm.toString()+": ");
				for (ObjectNode obj: lobj)
				{
					EscapeStatus es = hm.get(obj);
					List<EscapeState> les = new ArrayList<>(es.status);
					Collections.sort(les,  new Comparator<EscapeState>(){
						public int compare(EscapeState a, EscapeState b)
							{
								return a.toString().compareTo(b.toString());
							}
					});
					f.write(les+" ");
					// hash ^= es.status.size();
					// if (es instanceof ConditionalValue)
				}
				f.write("\n");
				
			}
            f.close();
        }
        catch(Exception e) {
            System.err.println("WHILE PRINTING SUMMARY EXCEPTION HAPPENED: "+ e);
        }
    }

	private static void printAllInfo(Map<SootMethod, PointsToGraph> ptgs,
									 Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries, String opDir) {

		Path p_opDir = Paths.get(opDir);
		for (Map.Entry<SootMethod, PointsToGraph> entry : ptgs.entrySet()) {
			SootMethod method = entry.getKey();
			PointsToGraph ptg = entry.getValue();
			Path p_opFile = Paths.get(p_opDir.toString() + "/" + method.getDeclaringClass().toString() + ".info");
//			System.out.println("Method "+method.toString()+" appends to "+p_opFile);
			StringBuilder output = new StringBuilder();
			output.append(method.toString() + "\n");
			output.append("PTG:\n");
			output.append(ptg.toString());
			output.append("\nSummary\n");
			output.append(summaries.get(method).toString() + "\n");
			output.append("**************************************** \n");
			try {
				Files.write(p_opFile, output.toString().getBytes(StandardCharsets.UTF_8),
						Files.exists(p_opFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
			} catch (Exception e) {
				// TODO: handle exception
				System.out.println("Unable to write info of " + method.toString() + " to file " + p_opFile.toString());
				e.printStackTrace();
			}
		}
	}
	private static void printCombinedInfo(Map<SootMethod, PointsToGraph> ptgs,
										  Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries,
										  HashMap<SootMethod, HashMap<ObjectNode, List<ContextualEscapeStatus>>> csummaries, String opDir) {

		Path p_opDir = Paths.get(opDir);
		for (Map.Entry<SootMethod, PointsToGraph> entry : ptgs.entrySet()) {
			SootMethod method = entry.getKey();
			PointsToGraph ptg = entry.getValue();
			Path p_opFile = Paths.get(p_opDir.toString() + "/" + method.getDeclaringClass().toString() + "new.info");
//			System.out.println("Method "+method.toString()+" appends to "+p_opFile);
			StringBuilder output = new StringBuilder();
			output.append(method.toString() + "\n");
			output.append("PTG:\n");
			output.append(ptg.toString());
			output.append("\nSummary\n");
			for(ObjectNode o : summaries.get(method).keySet()){
				if(csummaries.get(method).containsKey(o) && !csummaries.get(method).get(o).isEmpty()) {
					output.append(o + "=" + csummaries.get(method).get(o).toString() +" ");
				} else {
					output.append(o + "=" + summaries.get(method).get(o).toString() + " ");
				}
			}
			output.append("\n");
			//output.append(summaries.get(method).toString() + "\n");
			output.append("**************************************** \n");
			try {
				Files.write(p_opFile, output.toString().getBytes(StandardCharsets.UTF_8),
						Files.exists(p_opFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
			} catch (Exception e) {
				// TODO: handle exception
				System.out.println("Unable to write info of " + method.toString() + " to file " + p_opFile.toString());
				e.printStackTrace();
			}
		}
	}


	private static void printContextualInfo(Map<SootMethod, PointsToGraph> ptgs,
											HashMap<SootMethod, HashMap<ObjectNode, List<ContextualEscapeStatus>>> summaries, String opDir) {

		Path p_opDir = Paths.get(opDir);
		for (Map.Entry<SootMethod, PointsToGraph> entry : ptgs.entrySet()) {
			SootMethod method = entry.getKey();
			PointsToGraph ptg = entry.getValue();
			Path p_opFile = Paths.get(p_opDir.toString() + "/" + method.getDeclaringClass().toString() + ".info");
//			System.out.println("Method "+method.toString()+" appends to "+p_opFile);
			StringBuilder output = new StringBuilder();
			output.append(method.toString() + "\n");
			output.append("PTG:\n");
			output.append(ptg.toString());
			output.append("\nSummary\n");
			output.append(summaries.get(method).toString() + "\n");
			output.append("**************************************** \n");
			try {
				Files.write(p_opFile, output.toString().getBytes(StandardCharsets.UTF_8),
						Files.exists(p_opFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
			} catch (Exception e) {
				// TODO: handle exception
				System.out.println("Unable to write info of " + method.toString() + " to file " + p_opFile.toString());
				e.printStackTrace();
			}
		}
	}
	static String transformFuncSignature(String inputString) {
		StringBuilder finalString = new StringBuilder();
		for(int i=1;i<inputString.length()-1;i++) {
			if(inputString.charAt(i) == '.')
				finalString.append('/');
			else if(inputString.charAt(i) == ':')
				finalString.append('.');
			else if(inputString.charAt(i) == ' ')
				continue;
			else finalString.append(inputString.charAt(i));
		}
		return finalString.toString();
	}
	static void printResForJVM(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries, String ipDir, String opDir) {
		// Open File
		Path p_ipDir = Paths.get(ipDir);
		Path p_opDir = Paths.get(opDir);

		Path p_opFile = Paths.get(p_opDir.toString() + "/" + p_ipDir.getFileName() + ".res");

		StringBuilder sb = new StringBuilder();
		for (Map.Entry<SootMethod, HashMap<ObjectNode, EscapeStatus>> entry : summaries.entrySet()) {
			SootMethod method = entry.getKey();
			//if(!method.isJavaLibraryMethod()) {
				HashMap<ObjectNode, EscapeStatus> summary = entry.getValue();
				sb.append(transformFuncSignature(method.getBytecodeSignature()));
				sb.append(" ");
				sb.append(GetListOfNoEscapeObjects.get(summary));
				sb.append("\n");
			//}

		}
		try {
			System.out.println("Trying to write to:" + p_opFile);
			Files.write(p_opFile, sb.toString().getBytes(StandardCharsets.UTF_8),
					Files.exists(p_opFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
			System.out.println("Results have been written.");
		} catch (IOException e) {
			System.out.println("There is an exception"+e);
			e.printStackTrace();
		}
	}
	static void printContResForJVM(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries,
							   Map<CallSite, HashMap<SootMethod, HashSet<Integer>>> inlinesummaries,
							   String ipDir, String opDir) {
		// Open File
		Path p_ipDir = Paths.get(ipDir);
		Path p_opDir = Paths.get(opDir);

		Path p_opFile = Paths.get(p_opDir.toString() + "/" + p_ipDir.getFileName() + ".res");

		StringBuilder sb = new StringBuilder();
		for (Map.Entry<SootMethod, HashMap<ObjectNode, EscapeStatus>> entry : summaries.entrySet()) {
			SootMethod method = entry.getKey();
			if(method.toString().contains("methodType")) {
				continue;
			}
			//if(!method.isJavaLibraryMethod()) {
				HashMap<ObjectNode, EscapeStatus> summary = entry.getValue();
				String sbtemp = GetListOfNoEscapeObjects.get(summary);
				if(sbtemp != null) {
					//System.out.println("Value of sbtemp : "+ sbtemp.toString());
					sb.append(transformFuncSignature(method.getBytecodeSignature()));
					sb.append(" ");
					sb.append(GetListOfNoEscapeObjects.get(summary));
					sb.append("\n");
//					sb.append(" ");
//					sb.append("[");
//					i = 0;
//					List<CallSite> c = PrintInlineInfo.getSortedCallSites(method, inlinesummaries);
//					for(CallSite cs : c) {
//						sb.append(PrintInlineInfo.get(cs, inlinesummaries.get(cs)));
//					}
//					sb.append("]");
//					sb.append("\n");
				}

//				sb.append(" ");
//				sb.append("[");
//				i = 0;
//				List<CallSite> c = PrintInlineInfo.getSortedCallSites(method, inlinesummaries);
//				for(CallSite cs : c) {
//					sb.append(PrintInlineInfo.get(cs, inlinesummaries.get(cs)));
//				}
//				sb.append("]");
//				sb.append("\n");
			//}

		}
		try {
			System.out.println("Trying to write to:" + p_opFile);
			Files.write(p_opFile, sb.toString().getBytes(StandardCharsets.UTF_8),
					Files.exists(p_opFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
			System.out.println("Results have been written.");
		} catch (IOException e) {
			System.out.println("There is an exception"+e);
			e.printStackTrace();
		}
	}

	static void printContReswitinlineForJVM(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries,
								   Map<CallSite, HashMap<SootMethod, HashSet<Integer>>> inlinesummaries,
								   String ipDir, String opDir) {
		// Open File
		Path p_ipDir = Paths.get(ipDir);
		Path p_opDir = Paths.get(opDir);

		Path p_opFile = Paths.get(p_opDir.toString() + "/" + p_ipDir.getFileName() + ".res");

		StringBuilder sb = new StringBuilder();
		for (Map.Entry<SootMethod, HashMap<ObjectNode, EscapeStatus>> entry : summaries.entrySet()) {
			SootMethod method = entry.getKey();
			if(method.toString().contains("methodType")) {
				continue;
			}
			//if(!method.isJavaLibraryMethod()) {
			HashMap<ObjectNode, EscapeStatus> summary = entry.getValue();
			String sbtemp = GetListOfNoEscapeObjects.get(summary);
			if(sbtemp != null) {
				//System.out.println("Value of sbtemp : "+ sbtemp.toString());
				sb.append(transformFuncSignature(method.getBytecodeSignature()));
				sb.append(" ");
				sb.append(GetListOfNoEscapeObjects.get(summary));
				sb.append(" ");
				sb.append("[");
				i = 0;
				List<CallSite> c = PrintInlineInfo.getSortedCallSites(method, inlinesummaries);
				for(CallSite cs : c) {
					sb.append(PrintInlineInfo.get(cs, inlinesummaries.get(cs)));
				}
				sb.append("]");
				sb.append("\n");
			}


		}
		try {
			System.out.println("Trying to write to:" + p_opFile);
			Files.write(p_opFile, sb.toString().getBytes(StandardCharsets.UTF_8),
					Files.exists(p_opFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
			System.out.println("Results have been written.");
		} catch (IOException e) {
			System.out.println("There is an exception"+e);
			e.printStackTrace();
		}
	}



	static void saveStats(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> unresolved,
						  Map<SootMethod, HashMap<ObjectNode, EscapeStatus>>resolved,
						  String opDir,
						  Map<SootMethod, PointsToGraph> ptg) {
		Stats beforeResolution = new Stats(unresolved, ptg);
		System.out.println("calculating stats for solvedsummaries");
		Stats afterResolution = new Stats(resolved, null);
		Path p_opFile = Paths.get(opDir + "/stats.txt");
		StringBuilder sb = new StringBuilder();
		sb.append("Before resolution:\n"+beforeResolution);
		sb.append("\nAfter resolution:\n"+afterResolution);
		sb.append("\n");
		try {
			System.out.println("Trying to write to:" + p_opFile);
			Files.write(p_opFile, sb.toString().getBytes(StandardCharsets.UTF_8),
					Files.exists(p_opFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
			System.out.println("Stats have been written.");
		} catch (IOException e) {
			System.out.println("There is an exception"+e);
			e.printStackTrace();
		}

	}

	static void printCVresValues(Map<SootMethod, HashMap<ObjectNode, HashMap<EscapeState, EscapeStatus>>> resolvedStateValues, String opDir) {
		Path p_opDir = Paths.get(opDir);
		for (Map.Entry<SootMethod, HashMap<ObjectNode, HashMap<EscapeState, EscapeStatus>>> entry : resolvedStateValues.entrySet()) {
			SootMethod method = entry.getKey();
			Path p_opFile = Paths.get(p_opDir.toString() + "/" + method.getDeclaringClass().toString() + "CVRES.txt");
			StringBuilder output = new StringBuilder();
			output.append(method.toString() + "\n");
			output.append("\nResolved Value\n");
			output.append(resolvedStateValues.get(method).toString() + "\n");
			output.append("**************************************** \n");
			try {
				Files.write(p_opFile, output.toString().getBytes(StandardCharsets.UTF_8),
						Files.exists(p_opFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
			} catch (Exception e) {
				// TODO: handle exception
				System.out.println("Unable to write info of " + method.toString() + " to file " + p_opFile.toString());
				e.printStackTrace();
			}
		}
	}

	static void saveConStats(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> unresolved,
						  Map<SootMethod, HashMap<ObjectNode, EscapeStatus>>resolved,
						  Map<CallSite, HashMap<SootMethod, HashSet<Integer>>> inlinesummaries,
						  String opDir,
						  Map<SootMethod, PointsToGraph> ptg) {
		Stats beforeResolution = new Stats(unresolved, ptg);
		System.out.println("calculating stats for solvedsummaries");
		Stats afterResolution = new Stats(resolved, null);
		System.out.println("calculating stats for inline summaries");
		Stats afterInline= new Stats(inlinesummaries);
		Path p_opFile = Paths.get(opDir + "/stats.txt");
		StringBuilder sb = new StringBuilder();
		sb.append("Before resolution:\n"+beforeResolution);
		sb.append("\nAfter resolution:\n"+afterResolution);
		sb.append("\nAfter inline:\n"+afterInline);
		sb.append("\n");
		try {
			System.out.println("Trying to write to:" + p_opFile);
			Files.write(p_opFile, sb.toString().getBytes(StandardCharsets.UTF_8),
					Files.exists(p_opFile) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
			System.out.println("Stats have been written.");
		} catch (IOException e) {
			System.out.println("There is an exception"+e);
			e.printStackTrace();
		}

	}

}

/*
-Xjit:count = 0

JIT: 12-12.5K

without optimization: 26K
with redued dependence: 27.5k
with reduce dependence and param non escaping: 27.5K

*/
