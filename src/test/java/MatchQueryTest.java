import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.tquery.engine.common.Utils;
import joliex.tquery.engine.match.MatchQuery;
import org.junit.jupiter.api.Test;

import java.io.FileReader;

import static jolie.js.JsUtils.parseJsonIntoValue;

public class MatchQueryTest {
    static final String PATH_TO_RESOURCES = "src/test/resources/match";
    static final String QUERY = "query";
    static final String RESULT = "result";

    @Test
    public void testEmptyDestPath() throws Exception {
        final String fileName = "lookup_empty_dest_path.json";
        test(fileName);
    }

    @Test
    public void testEmptyLeftPath() throws Exception {
        final String fileName = "lookup_empty_left_path.json";
        test(fileName);
    }

    @Test
    public void testEmptyRightPath() throws Exception {
        final String fileName = "lookup_empty_right_path.json";
        test(fileName);
    }

    @Test
    public void testEmptyLeftData() throws Exception {
        final String fileName = "lookup_empty_left_array.json";
        test(fileName);
    }

    @Test
    public void testEmptyRightData() throws Exception {
        final String fileName = "lookup_empty_right_array.json";
        test(fileName);
    }

    @Test
    public void testSimplePath() throws Exception {
        final String fileName = "lookup_simple_path.json";
        test(fileName);
    }

    @Test
    public void testCompoundPath() throws Exception {
        final String fileName = "lookup_compound_path.json";
        test(fileName);
    }


    private void test(String fileName) throws Exception {
        Value value = Value.create();
        parseJsonIntoValue(new FileReader(String.format("%s/%s", PATH_TO_RESOURCES, fileName)), value, false);


        Value matchRequest = value.getChildren(QUERY).first();

        ValueVector actualResult = MatchQuery.match(matchRequest).getChildren(RESULT);
        ValueVector expectedResult = value.getChildren(RESULT);

        String assertionMessage = String.format("Error in test %s: " +
                        "Actual result does not match the expected result.\n" +
                        "Expected: %s  \n" +
                        "Actual: %s  \n",
                fileName, Utils.valueToPrettyString(expectedResult.first()), Utils.valueToPrettyString(actualResult.first()));

        assert (Utils.checkVectorEquality(actualResult, expectedResult)) : assertionMessage;
    }
}
