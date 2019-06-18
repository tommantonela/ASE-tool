package parser;

import java.util.Map;
import java.util.Set;

public abstract class DependencyExtractor {

	Set<String> clases;
	Map<String, Map<String, Float>> efferents;
	Map<String, Map<String, Float>> afferents;
	Map<String, String> classNamespace;
	Map<String, Set<String>> namespaces;
	
	public abstract void parseArchive(String path);
	
	public abstract void set(Object...objects);

}

