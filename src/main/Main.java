package main;

import analyser.PostResolutionAnalyser;
import analyser.StaticAnalyser;
import config.StoreEscape;
import counters.PolymorphicInvokeCounter;
import es.*;
import ppg.spec.Spec;
import ptg.ObjectNode;
import ptg.ObjectType;
import ptg.PointsToGraph;
import resolver.SpeculativeResolver;
import soot.*;
import soot.options.Options;
import utils.GetListOfNoEscapeObjects;
import utils.Stats;
import Inlining.PrintInlineInfo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.io.*;
import java.lang.*;

import static java.lang.System.exit;
import static utils.KillCallerOnly.kill;
import branch.Pair;

public class Main {

	public static int i = 0;
	public static Set<String> ListofMethods = new HashSet<>();
	public static HashMap<SootMethod, List<ObjectNode>> finalObjects = new HashMap<>();
//	static void setStoreEscapeOptions(String[] args) {
//		if (args.length >=6 ) {
//            StoreEscape.ReduceParamDependence = args[5].equals("true");
//		}
//		if (args.length >= 7) {
//            StoreEscape.MarkParamReturnEscaping = args[6].equals("true");
//		}
//	}
	public static void main(String[] args) {

		GetSootArgs g = new GetSootArgs();
		String[] sootArgs = g.get(args);

		//setStoreEscapeOptions(args);

		if (sootArgs == null) {
			System.out.println("Unable to generate args for soot!");
			return;
		}
		// New DaCapo Includes.

		//kafka
		Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcJJ$sp",SootClass.HIERARCHY);
		Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcZJ$sp",SootClass.HIERARCHY);
		Scene.v().addBasicClass("scala.runtime.java8.JFunction0$mcD$sp",SootClass.HIERARCHY);
		Scene.v().addBasicClass("scala.runtime.java8.JFunction2$mcZII$sp",SootClass.HIERARCHY);
		Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcVD$sp",SootClass.HIERARCHY);
		
		// h2o
		Scene.v().addBasicClass("water.codegen.CodeGenerator",SootClass.HIERARCHY);



		PolymorphicInvokeCounter pic = new PolymorphicInvokeCounter();
		PackManager.v().getPack("jtp").add(new Transform("jtp.pic", pic));

		System.out.println("\n 1. Generating CV's and PTG(s): ");
		StaticAnalyser staticAnalyser = new StaticAnalyser();
		CHATransform prepass = new CHATransform();
		PackManager.v().getPack("wjap").add(new Transform("wjap.pre", prepass));
		PackManager.v().getPack("jtp").add(new Transform("jtp.sample", staticAnalyser));
		// -- 1.
		long analysis_start = System.currentTimeMillis();
		Options.v().parse(sootArgs);
		Scene.v().loadNecessaryClasses();
		Scene.v().loadDynamicClasses();
		PackManager.v().runPacks();
		// soot.Main.main(sootArgs);
		long analysis_end = System.currentTimeMillis();
		// -- 1.
		System.out.println(" :> CV and PTG(s) Generated!");
		System.out.println(" :> Time Taken in phase 1: [" + (analysis_end - analysis_start) / 1000F + "]seconds");
		System.out.println("**********************************************************");

		printAllInfo(StaticAnalyser.ptgs, StaticAnalyser.summaries, args[4]);
		// -- 2.
		long res_start = System.currentTimeMillis();
		System.out.println(" 2. Resolving the Dependencies: ");
		SpeculativeResolver sr = new SpeculativeResolver(StaticAnalyser.summaries,
				StaticAnalyser.ptgs,
				StaticAnalyser.noBCIMethods);
		long res_end = System.currentTimeMillis();
		// -- 2.
		System.out.println(" :> Resolution is done!");
		System.out.println(" :> Time Taken in phase 2: [" + (res_end - res_start) / 1000F + "]seconds");
		System.out.println("**********************************************************");
		// -- 3.
		long postresolution_start = System.currentTimeMillis();
		System.out.println(" 3. Starting Post Resolution Phase: ");
		PostResolutionAnalyser ps = new PostResolutionAnalyser(StaticAnalyser.ptgs);
		PackManager.v().getPack("jtp").add(new Transform("jtp.postres", ps));
		Scene.v().loadNecessaryClasses();
		Scene.v().loadDynamicClasses();
		PackManager.v().runPacks();
		long postresolution_end = System.currentTimeMillis();
		// -- 3.
		System.out.println(" :> Post Resolution Phase is done!");
		System.out.println(" :> Time Taken in phase 3: [" + (postresolution_end - postresolution_start) / 1000F + "]seconds");
		System.out.println("**********************************************************");

		System.out.println(" :> Overall Time Taken: [" + ((analysis_end - analysis_start) / 1000F + (res_end - res_start) / 1000F) + (postresolution_end - postresolution_start) / 1000F + "]seconds");
		System.out.println("**********************************************************");
		
//		for(SootMethod sm : PostResolutionAnalyser.BranchResult.keySet()) {
//			if(!PostResolutionAnalyser.BranchResult.get(sm).isEmpty()) {
//				System.out.println("Method is : "+ sm.toString());
//				System.out.println(PostResolutionAnalyser.BranchResult.get(sm).toString());
//			}
//		}
//		for (Map.Entry<SootMethod, List<Pair<List<Integer>, Pair<String, List<Integer>>>>> entry : PostResolutionAnalyser.FinalBranchResult.entrySet()) {
//			SootMethod method = entry.getKey();
//			System.out.println("Method: " + method);
//
//			for (Pair<List<Integer>, Pair<String, List<Integer>>> pair : entry.getValue()) {
//				System.out.println("  [" + pair.getKey() + ", \"" + pair.getValue().getKey() + "\", " + pair.getValue().getValue() + "]");
//			}
//		}
		// PostResolutionAnalyser.printFinalBranchResult();

		HashMap<SootMethod, HashMap<ObjectNode, EscapeStatus>> resolved = (HashMap) kill(SpeculativeResolver.MergedSummaries);

		HashMap<SootMethod, HashMap<ObjectNode, List<ContextualEscapeStatus>>> cresolved = (HashMap) (SpeculativeResolver.PassedCallsiteValues);

		printAllInfo(StaticAnalyser.ptgs, resolved, args[4]);

		/*
		 * Getting the final list of objects that directly needs the new CV to be added.
		 * CVFinalES: contains the final escape status of each CV for each object.
		 * InterestingObjects: contains the list of objects that have CV with polymorphic call site for each method (Caller Method, Object, Callee method).
		 * FinalObjects: contains the list of objects that have CV with polymorphic call site and the object is escaping only due to that CV.
		 */
		for (SootMethod sm : SpeculativeResolver.CVfinalES.keySet()) {
			if (SpeculativeResolver.InterstingObjects.containsKey(sm)) {
				for (ObjectNode obj : SpeculativeResolver.InterstingObjects.get(sm).keySet()) {
					if (obj.type == ObjectType.internal) {
//						System.out.println("Method is "+ sm + " and Object: " + obj.toString());
						for (EscapeState es : SpeculativeResolver.CVfinalES.get(sm).get(obj).keySet()) {
							if (es instanceof ConditionalValue) {
								if(SpeculativeResolver.InterstingObjects.get(sm).containsKey(obj)) {
									if(((ConditionalValue) es).object.type != ObjectType.argument) {
//										System.out.println("The current es is : "+ es.toString());
										if (((ConditionalValue) es).method.getName().equals(SpeculativeResolver.InterstingObjects.get(sm).get(obj).getName())) {
											boolean Suitable = Is_Suitable_Object(sm, obj);
											//System.out.println("CV is : "+ es + " and Suitable : "+Suitable);
											if (Suitable) {
												if (SpeculativeResolver.CVfinalES.get(sm).get(obj).get(es).doesEscape()) {
													if (finalObjects.containsKey(sm)) {
														finalObjects.get(sm).add(obj);
													} else {
														List<ObjectNode> l = new ArrayList<>();
														l.add(obj);
														finalObjects.put(sm, l);
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		// List of objects that depends on these direct objects.

//		System.out.println();
		//System.out.println("Final Objects: " + finalObjects.toSjava.awt.GraphicsEnvironment$$Lambda$1tring());

		for (SootMethod sm : SpeculativeResolver.CVfinalES.keySet()) {
			if (SpeculativeResolver.InterstingObjects.containsKey(sm)) {
				for (ObjectNode obj : SpeculativeResolver.InterstingObjects.get(sm).keySet()) {
					if (obj.type == ObjectType.internal && finalObjects.containsKey(sm) && finalObjects.get(sm).contains(obj)) {
//						System.out.println("******* In Method: "+ sm + " Object: " + obj.toString() + " *******");
						SpeculativeResolver.count++;
						for (EscapeState es : SpeculativeResolver.CVfinalES.get(sm).get(obj).keySet()) {
							//System.out.println("CV is : " + es + " and its Status is : " + SpeculativeResolver.CVfinalES.get(sm).get(obj).get(es));
						}
					}
				}
			}
		}

		System.out.println();
		int number_obj = 0;
		// Add new Dependency to these polymorphic call-site objects.
		// First find the receiver object
		Map<SootMethod, List<PolymorphicConditionalValue>> SPEC_OPT = new HashMap<>();
		for(SootMethod sm : finalObjects.keySet()) {
			for(ObjectNode obj : finalObjects.get(sm)) {
				int bci;
				boolean flag = false;
				SootMethod caller_method;
				SootClass type;
				ObjectNode ob = null;

				for (EscapeState es : SpeculativeResolver.CVfinalES.get(sm).get(obj).keySet()) {
					if (es instanceof ConditionalValue) {
						if (((ConditionalValue) es).method.getName().toString().equals(SpeculativeResolver.InterstingObjects.get(sm).get(obj).getName().toString())) {
							if (!SpeculativeResolver.CVfinalES.get(sm).get(obj).get(es).doesEscape()) {
								bci = ((ConditionalValue) es).BCI;
								caller_method = sm;
								type = ((ConditionalValue) es).method.getDeclaringClass();
								//System.out.println("BCI: "+ bci + " caller_method: " + caller_method + " type: "+ type );
								for(ObjectNode o : StaticAnalyser.summaries.get(sm).keySet()) {
									for(EscapeState e : StaticAnalyser.summaries.get(sm).get(o).status) {
										if(e instanceof ConditionalValue) {
											if(((ConditionalValue) e).method != null) {
												if (((ConditionalValue) e).method.getName().toString().equals(SpeculativeResolver.InterstingObjects.get(sm).get(obj).getName().toString()) &&
														((ConditionalValue) e).object.type == ObjectType.parameter &&
														((ConditionalValue) e).BCI == bci) {
													ob = o;
												}
											}
										}
									}
								}
								if(ob != null && ob.type != ObjectType.internal) {

								}
								boolean tmpflag = false;
								if(!SPEC_OPT.containsKey(sm)) {
									List<SootClass> type_tmp = new ArrayList<>();
									type_tmp.add(type);
									List<Integer> obj_tmp = new ArrayList<>();
									obj_tmp.add(obj.ref);
									List<PolymorphicConditionalValue> tmp_pcv = new ArrayList<>();
									PolymorphicConditionalValue pcv = new PolymorphicConditionalValue(bci, type_tmp, obj_tmp);
									//System.out.println("First time: Method: "+sm.toString() + "BCI: "+ bci + "type: "+ type_tmp.toString() + "obj: "+ obj.toString());
									tmp_pcv.add(pcv);
									SPEC_OPT.put(sm,tmp_pcv);
								} else {
									List<PolymorphicConditionalValue> plist = SPEC_OPT.get(sm);
									PolymorphicConditionalValue existing = null;

									for (PolymorphicConditionalValue p : plist) {
										if (p.BCI == bci) {
											existing = p;
											break;
										}
									}

									if (existing != null) {
										if (!existing.types.contains(type)) {
											existing.types.add(type);
											tmpflag = true;
										}
										if (!existing.object.contains(obj.ref)) {
											existing.object.add(obj.ref);
											tmpflag = true;
										}
									} else {
										List<Integer> obj_tmp = new ArrayList<>();
										obj_tmp.add(obj.ref);
										List<SootClass> type_tmp = new ArrayList<>();
										type_tmp.add(type);
										PolymorphicConditionalValue pcv = new PolymorphicConditionalValue(bci, type_tmp, obj_tmp);
										//System.out.println("BCI was not there!!  : Method: " + sm.toString() + " BCI: " + bci + " type: " + type_tmp.toString() + " obj: " + obj.toString());
										plist.add(pcv);
									}
								}
								if(!flag) {
									number_obj++;
									flag = true;
								}
								//SpeculativeResolver.MergedSummaries.get(sm).get(obj).status.add(pcv);
							}
						}
					}
				}

			}
		}
//		System.out.println();
//		for(SootMethod sm : SPEC_OPT.keySet()) {
//			System.out.println("Method is : "+ sm.toString());
//			for(PolymorphicConditionalValue p : SPEC_OPT.get(sm)) {
//				System.out.println(p.toString());
//			}
//		}

		// System.out.println( " INLINE RESULTS " );
		// for(CallSite cs :  SpeculativeResolver.inlineSummaries.keySet()) {
		// 	for(SootMethod sm : SpeculativeResolver.inlineSummaries.get(cs).keySet()) {
		// 		if(SpeculativeResolver.inlineSummaries.get(cs).get(sm).isEmpty()) {
		// 			continue;
		// 		}
		// 	}			
		// 	System.out.println("CallSite: "+ cs.toString());
		// 	for(SootMethod sm : SpeculativeResolver.inlineSummaries.get(cs).keySet()) {
		// 		System.out.println("SootMethod: "+ sm.toString() + "List of BCIs: "+ SpeculativeResolver.inlineSummaries.get(cs).get(sm).toString());
		// 	}
		// }

		//System.out.println("Additional stack allocatable sites: "+ number_obj);
		saveConStats(SpeculativeResolver.existingSummaries, resolved, SpeculativeResolver.inlineSummaries, args[4], StaticAnalyser.ptgs);
		if (args[5] != null && args[5].equals("inline")) {
			printContReswitinlineForJVM(SpeculativeResolver.MergedSummaries, SpeculativeResolver.inlineSummaries, args[2], args[4]);
		} else if (args[5] != null && args[5].equals("specopt")) {
			printContReswithSPECTForJVM(SpeculativeResolver.MergedSummaries, args[2], args[4],SPEC_OPT);
		} else if (args[5] != null && args[5].equals("specoptini")) {
			printContReswithSPECTAndInlineForJVM(SpeculativeResolver.MergedSummaries,  SpeculativeResolver.inlineSummaries, args[2], args[4],SPEC_OPT);
		} else if (args[5] != null && args[5].equals("specoptinibranch")) {
			printContReswithSPECTAndInlineAndBranchForJVM(SpeculativeResolver.MergedSummaries,  SpeculativeResolver.inlineSummaries, args[2], args[4],SPEC_OPT, PostResolutionAnalyser.FinalBranchResult);
		} else if(args[5] != null && args[5].equals("printpldi")) {
			printPLDI(SpeculativeResolver.MergedSummaries, args[2], args[4]);
		} else {
			// printContResForJVM(SpeculativeResolver.MergedSummaries, args[2], args[4]);
		}

		// if (args[5] != null && args[5].equals("specopt")) {
		// 	printjustSPECTForJVM(SpeculativeResolver.MergedSummaries, args[2], args[4],SPEC_OPT);
		// } else if (args[5] != null && args[5].equals("specoptini")) {
		// 	printjusInlineForJVM(SpeculativeResolver.MergedSummaries,  SpeculativeResolver.inlineSummaries, args[2], args[4],SPEC_OPT);
		// }  else if (args[5] != null && args[5].equals("specoptinibranch")) {
		// 	printjustBranchForJVM(SpeculativeResolver.MergedSummaries,  SpeculativeResolver.inlineSummaries, args[2], args[4],SPEC_OPT, PostResolutionAnalyser.FinalBranchResult);
		// } else if(args[5] != null && args[5].equals("printpldi")) {
		// 	printPLDI(SpeculativeResolver.MergedSummaries, args[2], args[4]);
		// } else {
		// 	printContResForJVM(SpeculativeResolver.MergedSummaries, args[2], args[4]);
		// }

	}


	/*
	 * -- Function: Is_Suitable_Object --
	 * Check if the object is suitable for the new CV to be added.
	 * It just checks if the object is escaping only due to the polymorphic CV.
 	 */
	private static boolean Is_Suitable_Object(SootMethod sm, ObjectNode obj) {
			if (SpeculativeResolver.InterstingObjects.containsKey(sm)) {
				for (EscapeState es : SpeculativeResolver.CVfinalES.get(sm).get(obj).keySet()) {
					if(SpeculativeResolver.CVfinalES.containsKey(sm)) {
						if (SpeculativeResolver.CVfinalES.get(sm).containsKey(obj)) {
							if (SpeculativeResolver.CVfinalES.get(sm).get(obj).containsKey(es) && SpeculativeResolver.CVfinalES.get(sm).get(obj).get(es).doesEscape()) {


							if (es instanceof ConditionalValue && SpeculativeResolver.InterstingObjects.get(sm).containsKey(obj) && ((ConditionalValue) es).object.type != ObjectType.argument) {
								if (((ConditionalValue) es).method.getName().toString().equals(SpeculativeResolver.InterstingObjects.get(sm).get(obj).getName().toString())) {
//							System.out.println("Fine in Suitable Object: "+obj);
									continue;
								} else {
//							System.out.println("Not Suitable Object: "+obj);
									return false;
								}
							} else {
								return false;
							}
						}
					}
					}


				}
			}
		return true;
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
			for(ObjectNode sm : summaries.get(method).keySet()) {
				output.append("Object: "+ sm + " "+ summaries.get(method).get(sm).toString() + "\n");
			}
//			output.append(summaries.get(method).toString() + "\n");
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
	static void printContReswithSPECTForJVM(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries,
								   String ipDir, String opDir, Map<SootMethod, List<PolymorphicConditionalValue>> SPEC_OPT ) {
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
			if(method != null) {
				HashMap<ObjectNode, EscapeStatus> summary = entry.getValue();
				String sbtemp = GetListOfNoEscapeObjects.get(summary);
				if(sbtemp != null) {
					//System.out.println("Value of sbtemp : "+ sbtemp.toString());
					sb.append(transformFuncSignature(method.getBytecodeSignature()));
					sb.append(" ");
					sb.append(GetListOfNoEscapeObjects.get(summary));
					sb.append(" ");
					if(SPEC_OPT.containsKey(method)) {
						for(SootMethod sm : SPEC_OPT.keySet()) {
							int count = 0;
							if(method.equals(sm)) {
								sb.append("[");
								for(PolymorphicConditionalValue p : SPEC_OPT.get(sm)) {
									count ++;
									if(count > 1) {
										sb.append(" | ");
									}
									sb.append(p.toString());
								}
								sb.append("]");
							}
						}
					} else {
						sb.append("[]");
					}
					sb.append(" ! ");
					sb.append("[]");
					sb.append("\n");
				} else if(sbtemp == null && SPEC_OPT.containsKey(method)) {
					sb.append(transformFuncSignature(method.getBytecodeSignature()));
					sb.append(" ");
					sb.append("[]");
					sb.append(" ");
					for(SootMethod sm : SPEC_OPT.keySet()) {
						int count = 0;
						if(method.equals(sm)) {
							sb.append("[");
							for(PolymorphicConditionalValue p : SPEC_OPT.get(sm)) {
								count++;
								if(count > 1) {
									sb.append(" | ");
								}
								sb.append(p.toString());
							}
							sb.append("]");
						}
					}
					sb.append(" ! ");
					sb.append("[]");
					sb.append("\n");
				}
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


	// 1st Contribution


	static void printjustSPECTForJVM(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries,
								   String ipDir, String opDir, Map<SootMethod, List<PolymorphicConditionalValue>> SPEC_OPT ) {
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
			if(method != null) {
				HashMap<ObjectNode, EscapeStatus> summary = entry.getValue();
				String sbtemp = GetListOfNoEscapeObjects.get(summary);
				if(sbtemp != null) {
					//System.out.println("Value of sbtemp : "+ sbtemp.toString());
					sb.append(transformFuncSignature(method.getBytecodeSignature()));
					sb.append(" ");
					sb.append(GetListOfNoEscapeObjects.get(summary));
					sb.append(" ");
					if(SPEC_OPT.containsKey(method)) {
						for(SootMethod sm : SPEC_OPT.keySet()) {
							int count = 0;
							if(method.equals(sm)) {
								sb.append("[");
								for(PolymorphicConditionalValue p : SPEC_OPT.get(sm)) {
									count ++;
									if(count > 1) {
										sb.append(" | ");
									}
									sb.append(p.toString());
								}
								sb.append("]");
							}
						}
					} else {
						sb.append("[]");
					}
					sb.append(" ! ");
					sb.append("[]");
					sb.append("\n");
				} else if(sbtemp == null && SPEC_OPT.containsKey(method)) {
					sb.append(transformFuncSignature(method.getBytecodeSignature()));
					sb.append(" ");
					sb.append("[]");
					sb.append(" ");
					for(SootMethod sm : SPEC_OPT.keySet()) {
						int count = 0;
						if(method.equals(sm)) {
							sb.append("[");
							for(PolymorphicConditionalValue p : SPEC_OPT.get(sm)) {
								count++;
								if(count > 1) {
									sb.append(" | ");
								}
								sb.append(p.toString());
							}
							sb.append("]");
						}
					}
					sb.append(" ! ");
					sb.append("{}");
					sb.append(" ~ ");
					sb.append("[]");
					sb.append("\n");
				}
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

	static void printContReswithSPECTAndInlineForJVM(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries,
													 Map<CallSite, HashMap<SootMethod, HashSet<Integer>>> inlinesummaries,
											String ipDir, String opDir, Map<SootMethod, List<PolymorphicConditionalValue>> SPEC_OPT ) {
		// Open File
		Path p_ipDir = Paths.get(ipDir);
		Path p_opDir = Paths.get(opDir);
//		System.out.println("Coming here");
//		System.out.println("The inline Summary: "+ inlinesummaries.toString());
		Path p_opFile = Paths.get(p_opDir.toString() + "/" + p_ipDir.getFileName() + ".res");
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<SootMethod, HashMap<ObjectNode, EscapeStatus>> entry : summaries.entrySet()) {
			SootMethod method = entry.getKey();
			if(method.toString().contains("methodType")) {
				continue;
			}
			//if(!method.isJavaLibraryMethod()) {
			if(method != null) {
				HashMap<ObjectNode, EscapeStatus> summary = entry.getValue();
				String sbtemp = GetListOfNoEscapeObjects.get(summary);
				if(sbtemp != null) {
					//System.out.println("Value of sbtemp : "+ sbtemp.toString());
					sb.append(transformFuncSignature(method.getBytecodeSignature()));
					sb.append(" ");
					sb.append(GetListOfNoEscapeObjects.get(summary));
					sb.append(" ");
					if(SPEC_OPT.containsKey(method)) {
						for(SootMethod sm : SPEC_OPT.keySet()) {
							int count = 0;
							if(method.equals(sm)) {
								sb.append("[");
								for(PolymorphicConditionalValue p : SPEC_OPT.get(sm)) {
									count ++;
									if(count > 1) {
										sb.append(" | ");
									}
									sb.append(p.toString());
								}
								sb.append("]");
							}
						}
					} else {
						sb.append("[]");
					}

					sb.append(" ! ");
					sb.append("{");
					i = 0;
					List<CallSite> c = PrintInlineInfo.getSortedCallSites(method, inlinesummaries);
					for(CallSite cs : c) {
//						System.out.println("CS is : "+ cs.toString());
						sb.append(PrintInlineInfo.get(cs, inlinesummaries.get(cs)));
					}
					sb.append("}");
					sb.append("\n");
				} else if(sbtemp == null && SPEC_OPT.containsKey(method)) {
					sb.append(transformFuncSignature(method.getBytecodeSignature()));
					sb.append(" ");
					sb.append("[]");
					sb.append(" ");
					for(SootMethod sm : SPEC_OPT.keySet()) {
						int count = 0;
						if(method.equals(sm)) {
							sb.append("[");
							for(PolymorphicConditionalValue p : SPEC_OPT.get(sm)) {
								count++;
								if(count > 1) {
									sb.append(" | ");
								}
								sb.append(p.toString());
							}
							sb.append("]");
						}
					}
					sb.append(" ! ");
					sb.append("{");
					i = 0;
					List<CallSite> c = PrintInlineInfo.getSortedCallSites(method, inlinesummaries);
					for(CallSite cs : c) {
						// System.out.println("CS is : "+ cs.toString());
						sb.append(PrintInlineInfo.get(cs, inlinesummaries.get(cs)));
					}
					sb.append("}");
					sb.append("\n");
				}
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

	// 2nd Contribution
	static void printjusInlineForJVM(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries,
													 Map<CallSite, HashMap<SootMethod, HashSet<Integer>>> inlinesummaries,
											String ipDir, String opDir, Map<SootMethod, List<PolymorphicConditionalValue>> SPEC_OPT ) {
		// Open File
		Path p_ipDir = Paths.get(ipDir);
		Path p_opDir = Paths.get(opDir);
//		System.out.println("Coming here");
//		System.out.println("The inline Summary: "+ inlinesummaries.toString());
		Path p_opFile = Paths.get(p_opDir.toString() + "/" + p_ipDir.getFileName() + ".res");
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<SootMethod, HashMap<ObjectNode, EscapeStatus>> entry : summaries.entrySet()) {
			SootMethod method = entry.getKey();
			if(method.toString().contains("methodType")) {
				continue;
			}
			//if(!method.isJavaLibraryMethod()) {
			if(method != null) {
				HashMap<ObjectNode, EscapeStatus> summary = entry.getValue();
				String sbtemp = GetListOfNoEscapeObjects.get(summary);
				if(sbtemp != null) {
					//System.out.println("Value of sbtemp : "+ sbtemp.toString());
					sb.append(transformFuncSignature(method.getBytecodeSignature()));
					sb.append(" ");
					sb.append(GetListOfNoEscapeObjects.get(summary));
					sb.append(" ");
					sb.append("[]");
					sb.append(" ! ");
					sb.append("{");
					i = 0;
					List<CallSite> c = PrintInlineInfo.getSortedCallSites(method, inlinesummaries);
					for(CallSite cs : c) {
//						System.out.println("CS is : "+ cs.toString());
						sb.append(PrintInlineInfo.get(cs, inlinesummaries.get(cs)));
					}
					sb.append("}");
					sb.append(" ~ ");
					sb.append("[]");
					sb.append("\n");
				} else if(sbtemp == null && SPEC_OPT.containsKey(method)) {
					sb.append(transformFuncSignature(method.getBytecodeSignature()));
					sb.append(" ");
					sb.append("[]");
					sb.append(" ");
					sb.append("[]");
					sb.append(" ! ");
					sb.append("{");
					i = 0;
					List<CallSite> c = PrintInlineInfo.getSortedCallSites(method, inlinesummaries);
					for(CallSite cs : c) {
						// System.out.println("CS is : "+ cs.toString());
						sb.append(PrintInlineInfo.get(cs, inlinesummaries.get(cs)));
					}
					sb.append("}");
					sb.append(" ~ ");
					sb.append("[]");
					sb.append("\n");
				}
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


	static void printContReswithSPECTAndInlineAndBranchForJVM(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries,
													 Map<CallSite, HashMap<SootMethod, HashSet<Integer>>> inlinesummaries,
													 String ipDir, String opDir, Map<SootMethod, List<PolymorphicConditionalValue>> SPEC_OPT,
													  Map<SootMethod, List<Pair<List<Integer>, Pair<String, List<Integer>>>>> FinalBranchResult ) {
		// Open File
		Path p_ipDir = Paths.get(ipDir);
		Path p_opDir = Paths.get(opDir);
//		System.out.println("Coming here");
//		System.out.println("The inline Summary: "+ inlinesummaries.toString());
		Path p_opFile = Paths.get(p_opDir.toString() + "/" + p_ipDir.getFileName() + ".res");
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<SootMethod, HashMap<ObjectNode, EscapeStatus>> entry : summaries.entrySet()) {
			SootMethod method = entry.getKey();
			if(method.toString().contains("methodType")) {
				continue;
			}
			//if(!method.isJavaLibraryMethod()) {
			if(method != null) {
				HashMap<ObjectNode, EscapeStatus> summary = entry.getValue();
				String sbtemp = GetListOfNoEscapeObjects.get(summary);
				if(sbtemp != null) {
					//System.out.println("Value of sbtemp : "+ sbtemp.toString());
					sb.append(transformFuncSignature(method.getBytecodeSignature()));
					sb.append(" ");
					sb.append(GetListOfNoEscapeObjects.get(summary));
					sb.append(" ");
					if(SPEC_OPT.containsKey(method)) {
						for(SootMethod sm : SPEC_OPT.keySet()) {
							int count = 0;
							if(method.equals(sm)) {
								sb.append("[");
								for(PolymorphicConditionalValue p : SPEC_OPT.get(sm)) {
									count ++;
									if(count > 1) {
										sb.append(" | ");
									}
									sb.append(p.toString());
								}
								sb.append("]");
							}
						}
					} else {
						sb.append("[]");
					}

					sb.append(" ! ");
					sb.append("{");
					i = 0;
					List<CallSite> c = PrintInlineInfo.getSortedCallSites(method, inlinesummaries);
					for(CallSite cs : c) {
//						System.out.println("CS is : "+ cs.toString());
						sb.append(PrintInlineInfo.get(cs, inlinesummaries.get(cs)));
					}
					sb.append("}");
					sb.append(" ~ ");
					List<Pair<List<Integer>, Pair<String, List<Integer>>>> entry2 = FinalBranchResult.get(method);
					sb.append("[");
					if(entry2 != null) {
						int size = entry2.size();
						for (int i = 0; i < size; i++) {
							Pair<List<Integer>, Pair<String, List<Integer>>> pair = entry2.get(i);
							sb.append(pair.getKey()).append(" ").append(pair.getValue().getKey()).append(" ").append(pair.getValue().getValue());

							// Append " | " only if this is not the last entry
							if (i < size - 1) {
								sb.append(" | ");
							}
						}
					}
					sb.append("]");

					sb.append("\n");
				} else if(sbtemp == null && SPEC_OPT.containsKey(method)) {
					sb.append(transformFuncSignature(method.getBytecodeSignature()));
					sb.append(" ");
					sb.append("[]");
					sb.append(" ");
					for(SootMethod sm : SPEC_OPT.keySet()) {
						int count = 0;
						if(method.equals(sm)) {
							sb.append("[");
							for(PolymorphicConditionalValue p : SPEC_OPT.get(sm)) {
								count++;
								if(count > 1) {
									sb.append(" | ");
								}
								sb.append(p.toString());
							}
							sb.append("]");
						}
					}
					sb.append(" ! ");
					sb.append("{");
					i = 0;
					List<CallSite> c = PrintInlineInfo.getSortedCallSites(method, inlinesummaries);
					for(CallSite cs : c) {
						// System.out.println("CS is : "+ cs.toString());
						sb.append(PrintInlineInfo.get(cs, inlinesummaries.get(cs)));
					}
					sb.append("}");
					sb.append(" ~ ");
					List<Pair<List<Integer>, Pair<String, List<Integer>>>> entry2 = FinalBranchResult.get(method);
					sb.append("[");
					if(entry2 != null) {
						int size = entry2.size();
						for (int i = 0; i < size; i++) {
							Pair<List<Integer>, Pair<String, List<Integer>>> pair = entry2.get(i);
							sb.append(pair.getKey()).append(" ").append(pair.getValue().getKey()).append(" ").append(pair.getValue().getValue());

							// Append " | " only if this is not the last entry
							if (i < size - 1) {
								sb.append(" | ");
							}
						}
					}
					sb.append("]");
					sb.append("\n");
				}
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


	// 3rd Contribution 
	static void printjustBranchForJVM(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries,
													 Map<CallSite, HashMap<SootMethod, HashSet<Integer>>> inlinesummaries,
													 String ipDir, String opDir, Map<SootMethod, List<PolymorphicConditionalValue>> SPEC_OPT,
													  Map<SootMethod, List<Pair<List<Integer>, Pair<String, List<Integer>>>>> FinalBranchResult ) {
		// Open File
		Path p_ipDir = Paths.get(ipDir);
		Path p_opDir = Paths.get(opDir);
//		System.out.println("Coming here");
//		System.out.println("The inline Summary: "+ inlinesummaries.toString());
		Path p_opFile = Paths.get(p_opDir.toString() + "/" + p_ipDir.getFileName() + ".res");
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<SootMethod, HashMap<ObjectNode, EscapeStatus>> entry : summaries.entrySet()) {
			SootMethod method = entry.getKey();
			if(method.toString().contains("methodType")) {
				continue;
			}
			//if(!method.isJavaLibraryMethod()) {
			if(method != null) {
				HashMap<ObjectNode, EscapeStatus> summary = entry.getValue();
				String sbtemp = GetListOfNoEscapeObjects.get(summary);
				if(sbtemp != null) {
					//System.out.println("Value of sbtemp : "+ sbtemp.toString());
					sb.append(transformFuncSignature(method.getBytecodeSignature()));
					sb.append(" ");
					sb.append(GetListOfNoEscapeObjects.get(summary));
					sb.append(" ");
					sb.append("[]");
					sb.append(" ! ");
					sb.append("{");
					sb.append("}");
					sb.append(" ~ ");
					List<Pair<List<Integer>, Pair<String, List<Integer>>>> entry2 = FinalBranchResult.get(method);
					sb.append("[");
					if(entry2 != null) {
						int size = entry2.size();
						for (int i = 0; i < size; i++) {
							Pair<List<Integer>, Pair<String, List<Integer>>> pair = entry2.get(i);
							sb.append(pair.getKey()).append(" ").append(pair.getValue().getKey()).append(" ").append(pair.getValue().getValue());

							// Append " | " only if this is not the last entry
							if (i < size - 1) {
								sb.append(" | ");
							}
						}
					}
					sb.append("]");

					sb.append("\n");
				} else if(sbtemp == null && SPEC_OPT.containsKey(method)) {
					sb.append(transformFuncSignature(method.getBytecodeSignature()));
					sb.append(" ");
					sb.append("[]");
					sb.append(" ");
					sb.append("[]");
					sb.append(" ! ");
					sb.append("{");
					sb.append("}");
					sb.append(" ~ ");
					List<Pair<List<Integer>, Pair<String, List<Integer>>>> entry2 = FinalBranchResult.get(method);
					sb.append("[");
					if(entry2 != null) {
						int size = entry2.size();
						for (int i = 0; i < size; i++) {
							Pair<List<Integer>, Pair<String, List<Integer>>> pair = entry2.get(i);
							sb.append(pair.getKey()).append(" ").append(pair.getValue().getKey()).append(" ").append(pair.getValue().getValue());

							// Append " | " only if this is not the last entry
							if (i < size - 1) {
								sb.append(" | ");
							}
						}
					}
					sb.append("]");
					sb.append("\n");
				}
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


	static void printContResForJVM(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries,
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


	static void printPLDI(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries,
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
				sb.append("[]");
				sb.append(" ! ");
				sb.append("{");
				sb.append("}");
				sb.append(" ~ ");
				sb.append("[]");
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
					System.out.println("CS is : "+ cs.toString());
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
		System.out.println("calculating stats for MergedSummaries");
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
		System.out.println("calculating stats for MergedSummaries");
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
