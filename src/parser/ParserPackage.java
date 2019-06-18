package parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ParserPackage extends Parser{

	public Set<String> inside_classes;

	public ParserPackage(String path,DependencyExtractor xml){
		super(path,xml);
		inside_classes = new HashSet<>();
		updateStructures();
		
		updateHierarchyLevels();
	}

	private void updateHierarchyLevels() {
		for(String c : clases){
			String [] sp = c.split("\\.");
			hierarchy_levels.put(c,sp.length);
		}
	}

	private void updateStructures() {

		Map<String,Map<String,Float>> outAux = new HashMap<>();
		Map<String,Map<String,Float>> inAux = new HashMap<>();

		for(String n:namespaces.keySet()){
			outAux.put(n,new HashMap<String,Float>());
			inAux.put(n,new HashMap<String,Float>());
		}

		for(String n:namespaces.keySet()){
			Set<String> clasesIn = namespaces.get(n);
			for(String c:clasesIn){ //por todas las clases acá!

				Map<String,Float> cOut = efferents.get(c);
				if(cOut == null)
					continue;
				Map<String,Float> nOut = outAux.get(n);
				for(String co:cOut.keySet()){
					String nameSpaceCO = classNamespace.get(co);

					if(nameSpaceCO == null)
						continue;

					if(n.equals(nameSpaceCO))
						continue;

					Float f = nOut.get(nameSpaceCO);
					if(f==null)
						f = 0f;
					f += cOut.get(co);
					nOut.put(nameSpaceCO, f);
				}

				//Ahora los in!
				Map<String,Float> cIn = afferents.get(c);
				Map<String,Float> nIn = inAux.get(n);

				if(cIn==null)
					continue;

				for(String ci:cIn.keySet()){
					String nameSpaceCI = classNamespace.get(ci);

					if(n.equals(nameSpaceCI))
						continue;

					Float f = nIn.get(nameSpaceCI);
					if(f == null)
						f = 0f;
					f += cIn.get(ci);
					nIn.put(nameSpaceCI, f);
				}
			}
		}

		efferents = outAux;
		afferents = inAux;

		inside_classes.addAll(clases);

		clases.clear();
		clases.addAll(namespaces.keySet());

	}

	public String toString(){
		return "package";
	}

}
