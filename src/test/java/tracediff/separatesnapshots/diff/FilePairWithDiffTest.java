package tracediff.separatesnapshots.diff;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FilePairWithDiffTest {
    @Test
    public void getDeclaringCompilationUnit_validPathAndSourceFolderName_returnsCorrectClassName() {
        String path = "C:/Users/username/projRoot/src/main/java/Main.java";
        String srcFolderName = "src/main/java";
        FilePairWithDiff filePairWithDiff = new FilePairWithDiff("", "","");
        String actualResult = filePairWithDiff.getDeclaringCompilationUnit(path, srcFolderName);
        String expectedResult = "Main";
        assertEquals(expectedResult, actualResult);
    }
}
