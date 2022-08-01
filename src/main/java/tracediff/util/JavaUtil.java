package tracediff.util;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

@SuppressWarnings("restriction")
public class JavaUtil {
	public static HashMap<String, CompilationUnit> sourceFile2CUMap = new HashMap<>();
	
	public static CompilationUnit findCompilationUnitBySourcePath(String javaFilePath,
			String declaringCompilationUnitName) {
		
		CompilationUnit parsedCU = sourceFile2CUMap.get(javaFilePath);
		if(parsedCU != null) {
			return parsedCU;
		}
		
		File javaFile = new File(javaFilePath);
		
		if(javaFile.exists()){
			
			String contents;
			try {
				contents = new String(Files.readAllBytes(Paths.get(javaFilePath)));
				
				final ASTParser parser = ASTParser.newParser(AST.JLS8);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				parser.setSource(contents.toCharArray());
				parser.setResolveBindings(true);
				
				CompilationUnit cu = (CompilationUnit)parser.createAST(null);
				sourceFile2CUMap.put(javaFilePath, cu);
				
				return cu;
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else{
			System.err.print("cannot find " + declaringCompilationUnitName + " under " + javaFilePath);			
		}
		
		return null;
	}

	public static String getFullNameOfCompilationUnit(CompilationUnit cu){

		String packageName = "";
		if(cu.getPackage() != null){
			packageName = cu.getPackage().getName().toString();
		}
		AbstractTypeDeclaration typeDeclaration = (AbstractTypeDeclaration) cu.types().get(0);
		String typeName = typeDeclaration.getName().getIdentifier();

		if(packageName.length() == 0){
			return typeName;
		}
		else{
			return packageName + "." + typeName;
		}

	}
}
