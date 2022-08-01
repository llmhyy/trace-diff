package tracediff.separatesnapshots;

import microbat.model.BreakPoint;
import microbat.model.ClassLocation;
import tracediff.util.JavaUtil;
import tracediff.util.MinimumASTNodeFinder;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import tracediff.StepChangeType;
import tracediff.separatesnapshots.diff.DiffChunk;
import tracediff.separatesnapshots.diff.DiffParser;
import tracediff.separatesnapshots.diff.FilePairWithDiff;
import tracediff.separatesnapshots.diff.LineChange;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DiffMatcher {
	
	private String sourceFolderName;
	private String testFolderName;
	
	private String buggyPath;
	private String fixPath;
	
	protected List<FilePairWithDiff> fileDiffList;
	
	public DiffMatcher(String sourceFolderName, String testFolderName, String buggyPath, String fixPath) {
		super();
		this.sourceFolderName = sourceFolderName;
		this.testFolderName = testFolderName;
		this.buggyPath = buggyPath;
		this.fixPath = fixPath;
	}

	protected static List<String> getRawDiffContent(String buggySourcePath, String fixSourcePath) {
		return getRawDiffContent(buggySourcePath, fixSourcePath, false);
	}

	protected static List<String> getRawDiffContent(String buggySourcePath, String fixSourcePath, boolean ignoreSpace) {
		List<String> cmdList = new ArrayList<>();

		cmdList.add("git");
		cmdList.add("diff");
		cmdList.add("--no-index");
		if (ignoreSpace) {
			cmdList.add("--ignore-space-change");
		}
		cmdList.add(buggySourcePath);
		cmdList.add(fixSourcePath);

		String[] cmds = cmdList.toArray(new String[0]);
		try {
			ProcessBuilder pb = new ProcessBuilder(cmds);
			pb.redirectErrorStream(true); // merge stdout and stderr
			Process proc = pb.start();

//			Process proc = Runtime.getRuntime().exec(cmds, new String[]{});

			InputStream stdin = proc.getInputStream();
			InputStreamReader isr = new InputStreamReader(stdin);
			BufferedReader br = new BufferedReader(isr);

			List<String> diffContent = new ArrayList<>();
			String line = null;
			while ( (line = br.readLine()) != null)
				diffContent.add(line);

			stdin.close();

			return diffContent;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public boolean isMatch(BreakPoint srcPoint, BreakPoint targetPoint){
		if(!srcPoint.isSourceVersion()){
			BreakPoint tmp = srcPoint;
			srcPoint = targetPoint;
			targetPoint = tmp;
		}
		System.currentTimeMillis();
		FilePairWithDiff fileDiff = findDiffBySourceFile(srcPoint);
		if(fileDiff==null){
			boolean isSameFile = srcPoint.getDeclaringCompilationUnitName().equals(targetPoint.getDeclaringCompilationUnitName());
			boolean isSameLocation = srcPoint.getLineNumber()==targetPoint.getLineNumber();
			
			return isSameFile && isSameLocation;
		}
		else{
			List<Integer> targetLines = fileDiff.getSourceToTargetMap().get(srcPoint.getLineNumber());
			if(targetLines==null){
				return false;
			}
			
			if(fileDiff.getTargetDeclaringCompilationUnit().equals(targetPoint.getDeclaringCompilationUnitName())){
				if(targetLines.contains(targetPoint.getLineNumber())){
					return true;
				}
			}
		}
		
		return false;
	}

	public FilePairWithDiff findDiffBySourceFile(BreakPoint srcPoint) {
		for(FilePairWithDiff diff: this.fileDiffList){
			if(diff.getSourceDeclaringCompilationUnit().equals(srcPoint.getDeclaringCompilationUnitName())){
				return diff;
			}
		}
		
		return null;
	}

	public void matchCode(){
		List<String> diffContent = getRawDiffContent(sourceFolderName);
		diffContent.add("diff end");
		List<FilePairWithDiff> fileDiffs = new DiffParser().parseDiff(diffContent, sourceFolderName);

		List<String> testDiffContent = getRawDiffContent(testFolderName);
		testDiffContent.add("diff end");
		List<FilePairWithDiff> testFileDiffs = new DiffParser().parseDiff(testDiffContent, testFolderName);

		fileDiffs.addAll(testFileDiffs);
		Iterator<FilePairWithDiff> iter = fileDiffs.iterator();
		while(iter.hasNext()) {
			FilePairWithDiff diff = iter.next();
			if(diff.getSourceFile() == null)
				iter.remove();
		}

		for(FilePairWithDiff fileDiff: fileDiffs){
			HashMap<Integer, List<Integer>> sourceToTargetMap = new HashMap<>();
			HashMap<Integer, List<Integer>> targetToSourceMap = new HashMap<>();

			constructMapping(fileDiff, sourceToTargetMap, targetToSourceMap);
			System.currentTimeMillis();

			fileDiff.setSourceToTargetMap(sourceToTargetMap);
			fileDiff.setTargetToSourceMap(targetToSourceMap);
		}

		this.fileDiffList = fileDiffs;
	}

	protected List<String> getRawDiffContent(String folderName) {
		String buggySourcePath = buggyPath + File.separator + folderName;
		String fixSourcePath = fixPath + File.separator + folderName;
		return getRawDiffContent(buggySourcePath, fixSourcePath);
	}

	private int countLineNumber(String fileName){
		LineNumberReader lnr;
		try {
			File file = new File(fileName);
			lnr = new LineNumberReader(new FileReader(file));
			lnr.skip(Long.MAX_VALUE);
			int count = lnr.getLineNumber() + 1; //Add 1 because line index starts at 0
			// Finally, the LineNumberReader object should be closed to prevent resource leak
			lnr.close();

			return count;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return -1;
	}

	private void constructMapping(FilePairWithDiff fileDiff, HashMap<Integer, List<Integer>> sourceToTargetMap,
			HashMap<Integer, List<Integer>> targetToSourceMap) {

		if(fileDiff.getSourceFile() == null) return;

		int sourceLineCursor = 1;
		int targetLineCursor = 1;

		for(DiffChunk chunk: fileDiff.getChunks()){
			int startLineInSource = chunk.getStartLineInSource();
			int startLineInTarget = chunk.getStartLineInTarget();

			while(sourceLineCursor<startLineInSource && targetLineCursor<startLineInTarget){
				mapLine(sourceToTargetMap, targetToSourceMap, sourceLineCursor, targetLineCursor);
				sourceLineCursor++;
				targetLineCursor++;
			}

			for(int index=0; index<chunk.getChangeList().size(); ){
				LineChange line = chunk.getChangeList().get(index);
				if(line.getType()==LineChange.UNCHANGE){
					mapLine(sourceToTargetMap, targetToSourceMap, sourceLineCursor, targetLineCursor);
					sourceLineCursor++;
					targetLineCursor++;
					index++;
				}
				else if(line.getType()==LineChange.REMOVE){
					int successiveRemoveLines = findSuccessiveRemoveLines(chunk, line.getIndex());
					System.currentTimeMillis();
					boolean followByAdd = checkFollowByAdd(chunk, line.getIndex(), successiveRemoveLines);

					if(followByAdd){
						int successiveAddLines = findSuccessiveAddLines(chunk, line.getIndex()+successiveRemoveLines);
						for(int i=sourceLineCursor; i<sourceLineCursor+successiveRemoveLines; i++){
							for(int j=targetLineCursor; j<targetLineCursor+successiveAddLines; j++){
								mapAdditionalLine(sourceToTargetMap, i, j);
								mapAdditionalLine(targetToSourceMap, j, i);
							}
						}

						sourceLineCursor += successiveRemoveLines;
						targetLineCursor += successiveAddLines;

						index += successiveAddLines + successiveRemoveLines;
					}
					else{
						sourceLineCursor += successiveRemoveLines;
						index += successiveRemoveLines;
					}
				}
				else{
					targetLineCursor++;
					index++;
				}
			}

		}

		int totalSoureLineNumber = countLineNumber(fileDiff.getSourceFile());
		int totalTargetLineNumber = countLineNumber(fileDiff.getTargetFile());
		while(sourceLineCursor<totalSoureLineNumber && targetLineCursor<totalTargetLineNumber){
			mapLine(sourceToTargetMap, targetToSourceMap, sourceLineCursor, targetLineCursor);
			sourceLineCursor++;
			targetLineCursor++;
		}
	}

	private int findSuccessiveAddLines(DiffChunk chunk, int startIndex) {
		int count = 0;
		for(int i=startIndex; i<chunk.getChangeList().size(); i++){
			LineChange line = chunk.getChangeList().get(i);
			if(line.getType()==LineChange.ADD){
				count++;
			}
			else{
				break;
			}
		}

		return count;
	}

	private boolean checkFollowByAdd(DiffChunk chunk, int startIndex, int successiveRemoveLines) {
		int index = startIndex+successiveRemoveLines;
		if(index <= chunk.getChangeList().size()){
			return chunk.getChangeList().get(index).getType()==LineChange.ADD;
		}

		return false;
	}

	private int findSuccessiveRemoveLines(DiffChunk chunk, int startIndex) {
		int count = 0;
		for(int i=startIndex; i<chunk.getChangeList().size(); i++){
			LineChange line = chunk.getChangeList().get(i);
			if(line.getType()==LineChange.REMOVE){
				count++;
			}
			else{
				break;
			}
		}

		return count;
	}

	private void mapAdditionalLine(HashMap<Integer, List<Integer>> sourceToTargetMap, int sourceLineCursor,
			int targetLineCursor) {
		List<Integer> targetLines = sourceToTargetMap.get(sourceLineCursor);
		if(targetLines == null){
			targetLines = new ArrayList<>();
		}

		if(!targetLines.contains(targetLineCursor)){
			targetLines.add(targetLineCursor);
		}

		sourceToTargetMap.put(sourceLineCursor, targetLines);
	}

	private void mapLine(HashMap<Integer, List<Integer>> sourceToTargetMap,
			HashMap<Integer, List<Integer>> targetToSourceMap, int sourceLineCursor, int targetLineCursor){
		List<Integer> targetLines = new ArrayList<>();
		targetLines.add(targetLineCursor);
		List<Integer> sourceLines = new ArrayList<>();
		sourceLines.add(sourceLineCursor);

		sourceToTargetMap.put(sourceLineCursor, targetLines);
		targetToSourceMap.put(targetLineCursor, sourceLines);
	}
}
