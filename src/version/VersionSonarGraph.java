package version;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.hello2morrow.sonargraph.integration.access.controller.ControllerAccess;
import com.hello2morrow.sonargraph.integration.access.controller.ISonargraphSystemController;
import com.hello2morrow.sonargraph.integration.access.controller.ISystemInfoProcessor;
import com.hello2morrow.sonargraph.integration.access.foundation.Result;
import com.hello2morrow.sonargraph.integration.access.model.ICycleGroupIssue;
import com.hello2morrow.sonargraph.integration.access.model.INamedElement;

public class VersionSonarGraph extends Version{

	public VersionSonarGraph(String path, String versionName) {
		super(path, versionName);
	}
	
	@Override
	protected void processVersion(String xmlPath) {
		
		ISonargraphSystemController controller = ControllerAccess.createController();
//		Result result = controller.loadSystemReport(new File(xmlPath));
		controller.loadSystemReport(new File(xmlPath));

		ISystemInfoProcessor info = controller.createSystemInfoProcessor();

		List<ICycleGroupIssue> packageCycles = getCycleGroup(info, "NamespaceCycleGroup");
		packageCycles.addAll(getCycleGroup(info, "CriticalNamespaceCycleGroup"));
		
		smellTypes.add("cd");
		
		features.put("componentdependenciestoremove",features.size());
		features.put("parserdependenciestoremove",features.size());
		features.put("structuraldebtindex",features.size());
		
		sortedFeatures.add("componentdependenciestoremove");
		sortedFeatures.add("parserdependenciestoremove");
		sortedFeatures.add("structuraldebtindex");
		
		List<String> cycleComponents = new ArrayList<>();
		for(ICycleGroupIssue cc : packageCycles){
			
			cycleComponents.clear();
			
			List<INamedElement> elements = cc.getAffectedNamedElements();
			for(INamedElement ee : elements)
				cycleComponents.add(ee.getPresentationName().replace("_","*"));
						
			double [] fes = new double[features.size()];
			
			fes[0] = cc.getComponentDependenciesToRemove();
			fes[1] = cc.getParserDependenciesToRemove();
			fes[2] = cc.getStructuralDebtIndex();
			
			Collections.sort(cycleComponents);
			String cycle = cycleComponents.toString();
			cycle = "cd_"+cycle.substring(1, cycle.length()-1).replace(", ",";");
						
			scores.put(cycle, fes);
			
		}
		
	}

	@Override
	public String findSmell(String smell) {
		if(scores.containsKey(smell)) //as packages in a smell are sorted, we only need to compare by equals, no need to do anything extra
			return smell;
		
		return null;
	}

	@Override
	public Set<String> findPotentiallyMatchingSmells(String smell) {
		Set<String> matchings = new HashSet<>();
		
		if(scores.containsKey(smell)){
			matchings.add(smell);
			return matchings;
		}
		
		Set<String> cycles = smells.get("cd");

		List<String> smell_cycle = Arrays.asList(smell.replace("cd_", "").split(";"));

		List<String> cycle_aux = null;		

		for(String cycle : cycles){

			cycle_aux = Arrays.asList(cycle.replace("cd_", "").split(";"));

			if(cycle_aux.size() < smell_cycle.size()) //at this point, we are not interested in exact matchings
				if(isAMatch(cycle_aux, smell_cycle))
					matchings.add(cycle);
			else
				if(cycle_aux.size() > smell_cycle.size())
					if(isAMatch(smell_cycle, cycle_aux))
						matchings.add(cycle);
		}
		
		return matchings;
	}
	

	private static List<ICycleGroupIssue> getCycleGroup(final ISystemInfoProcessor infoProcessor, final String issueTypeName){
		return infoProcessor.getIssues(i -> i.getIssueType().getName().equals(issueTypeName)).stream().map(i -> (ICycleGroupIssue) i)
				.collect(Collectors.toList());
	}

	public static void main(String[] args) {

		String xmlPath = "E:/test_arcanSA/aa_2019-06-12_00-52-28.xml";
		xmlPath = "C:/Users/Anto/Desktop/esem/hibernate";
		
		File f = new File(xmlPath);
		for(File ff : f.listFiles()){
			
			if(!ff.isFile() || !ff.getName().endsWith(".xml"))
				continue;
			
			ISonargraphSystemController controller = ControllerAccess.createController();
			Result result = controller.loadSystemReport(new File(ff.getAbsolutePath()));

			ISystemInfoProcessor info = controller.createSystemInfoProcessor();

			List<ICycleGroupIssue> packageCycles = getCycleGroup(info, "NamespaceCycleGroup");
			packageCycles.addAll(getCycleGroup(info, "CriticalNamespaceCycleGroup"));

			System.out.println(ff.getName()+" "+packageCycles.size());

		}
		
		System.exit(0);
		
		ISonargraphSystemController controller = ControllerAccess.createController();
		Result result = controller.loadSystemReport(new File(xmlPath));

		System.out.println(result);

		ISystemInfoProcessor info = controller.createSystemInfoProcessor();

		List<ICycleGroupIssue> packageCycles = getCycleGroup(info, "NamespaceCycleGroup");
		packageCycles.addAll(getCycleGroup(info, "CriticalNamespaceCycleGroup"));

		System.out.println(packageCycles.size());

		System.out.println("----------------------");

		for(ICycleGroupIssue cc : packageCycles){
			System.out.println("----------------------");
			System.out.println(cc.getIssueType());
			//			System.out.println("    -- "+cc.getAffectedNamedElements());
			List<INamedElement> elements = cc.getAffectedNamedElements();
			for(INamedElement ee : elements){
				System.out.println("  "+ee.getPresentationName());
			}

			System.out.println("ComponentDependenciesToRemove: "+cc.getComponentDependenciesToRemove());
			System.out.println("ParserDependenciesToRemove: "+cc.getParserDependenciesToRemove());
			System.out.println("StructuralDebtIndex: "+cc.getStructuralDebtIndex());
		}
	}
}
