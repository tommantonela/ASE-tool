package parser.factory;

import parser.CDAExtractor;
import parser.Parser;
import parser.ParserPackage;

public class FactoryParser {

	public static Parser getParser(String path, String level){
		
		if(level.equalsIgnoreCase("class"))
			return new Parser(path,new CDAExtractor());
		else
			return new ParserPackage(path,new CDAExtractor());
	};
	
}