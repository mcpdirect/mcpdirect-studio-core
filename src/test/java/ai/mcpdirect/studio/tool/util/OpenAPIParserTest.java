package ai.mcpdirect.studio.tool.util;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class OpenAPIParserTest {

    @Test
    public void parser() throws IOException {
        InputStream resourceAsStream = OpenAPIParserTest.class.getResourceAsStream("/openapi.yaml");
        OpenAPIParser.parser(new String(resourceAsStream.readAllBytes()));
    }
}