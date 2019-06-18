package parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pfsw.odem.DependencySet;
import org.pfsw.odem.IDependency;
import org.pfsw.odem.IDependencyFilter;
import org.pfsw.tools.cda.base.model.ClassInformation;
import org.pfsw.tools.cda.base.model.Workset;
import org.pfsw.tools.cda.base.model.workset.ClasspathPartDefinition;
import org.pfsw.tools.cda.core.init.WorksetInitializer;

public class CDAExtractor extends DependencyExtractor{
	
	@Override
	public void parseArchive(String path){
		
		Workset workset = new Workset("Sample1");
			
		ClasspathPartDefinition partDefinition = new ClasspathPartDefinition(path);
		workset.addClasspathPartDefinition(partDefinition);

		WorksetInitializer wsInitializer = new WorksetInitializer(workset);
		wsInitializer.initializeWorksetAndWait(null);
		
		ClassInformation [] ci = workset.getAllContainedClasses();
		
		for(ClassInformation c : ci){
						
			String c_name = c.getClassName().replace("_", "*");
			
			if(c_name.contains("$"))
				c_name = c_name.substring(0,c_name.indexOf("$"));
			
			String c_package = c.getPackageName().replace("_", "*");
			
			clases.add(c_name);
			classNamespace.put(c_name, c_package);
			
			Set<String> c_classNamespace = namespaces.get(c_package);
			if(c_classNamespace == null){
				c_classNamespace = new HashSet<String>();
				namespaces.put(c_package,c_classNamespace);
			}
			c_classNamespace.add(c_name);
			
			Map<String,Float> c_outs = efferents.get(c_name);
			if(c_outs == null){
				c_outs = new HashMap<String,Float>();
				efferents.put(c_name,c_outs);
			}
			
			@SuppressWarnings("unchecked")
			DependencySet<ClassInformation, ClassInformation> ds  = c.getDependencies(); //las clases aparecen tantas veces como se dependa de ellas
			List<IDependency<ClassInformation, ClassInformation>> lis = ds.collect(new IDependencyFilter<IDependency<ClassInformation,ClassInformation>>() {
				
				@Override
				public boolean matches(IDependency<ClassInformation, ClassInformation> arg0) {
				return true;
				}
			});
			
			for(IDependency<ClassInformation, ClassInformation> l : lis){
				
				String c_target = l.getTargetElement().getClassName();
				
				if(c_target.contains("$"))
					c_target = c_target.substring(0,c_target.indexOf("$"));
				
				Float apps = c_outs.get(c_target);
				if(apps == null)
					apps = 1f;
				apps++;
				c_outs.put(c_target, apps);
				
				Map<String,Float> ins = afferents.get(c_target);
				if(ins == null){
					ins = new HashMap<String,Float>();
					afferents.put(c_target, ins);
				}
				apps = ins.get(c_name);
				if(apps == null)
					apps = 1f;
				apps++;
				ins.put(c_name, apps);
				
//				outRelations.put(c_name,c_outs);
			}
				
		}
		workset.release();
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void set(Object... objects) {
		clases = (Set<String>)objects[0];
		efferents = (Map<String, Map<String, Float>>)objects[1];
		afferents = (Map<String, Map<String, Float>>)objects[2];
		classNamespace = (Map<String, String>)objects[3];
		namespaces = (Map<String, Set<String>>)objects[4];
	}

}

