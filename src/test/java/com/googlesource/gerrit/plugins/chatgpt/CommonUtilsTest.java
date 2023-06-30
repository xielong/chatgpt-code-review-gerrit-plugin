package com.googlesource.gerrit.plugins.chatgpt;

import com.google.common.io.Files;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

public class CommonUtilsTest {

    @Test
    public void reducePatchSet() throws IOException {
        String diffContent = readFile("reducePatchSet/patchSetInput.diff");
        String expectedReducedDiff = readFile("reducePatchSet/patchSetOutput.diff");
        String actualReducedDiff = PatchSetReviewer.reducePatchSet(diffContent);
        assertEquals(expectedReducedDiff, actualReducedDiff);
    }

    private String readFile(String filePath) throws IOException {
        File file = new File(getClass().getClassLoader().getResource(filePath).getFile());
        return Files.asCharSource(file, Charset.defaultCharset()).read();
    }

}
