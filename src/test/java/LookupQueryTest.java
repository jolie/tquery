import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.common.Utils;
import joliex.queryengine.lookup.LookupQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.io.IOException;

import static jolie.js.JsUtils.parseJsonIntoValue;

public class LookupQueryTest {
    static final String PATH_TO_RESOURCES = "src/test/resources";
    static final String LEFT_DATA_SET_NAME = "quality.json";
    static final String RIGHT_DATA_SET_NAME = "temperature.json";

    private static final String LEFT_DATA = "leftData";
    private static final String RIGHT_DATA = "rightData";
    private static final String LEFT_PATH = "leftPath";
    private static final String RIGHT_PATH = "rightPath";
    private static final String DST_PATH = "dstPath";

    private static ValueVector rightDataArray;
    private static ValueVector leftDataArray;

    @BeforeAll
    public static void setUp() throws IOException {
        Value rightData = Value.create();
        Value leftData = Value.create();

        parseJsonIntoValue(new FileReader(PATH_TO_RESOURCES + "/" + RIGHT_DATA_SET_NAME), rightData, false);
        parseJsonIntoValue(new FileReader(PATH_TO_RESOURCES + "/" + LEFT_DATA_SET_NAME), leftData, false);

        leftDataArray = leftData.getChildren("_");
        rightDataArray = rightData.getChildren("_");
    }

    @Test
    public void lookupTest() throws FaultException, IOException {
        final String TEST_RESULT_NAME = "lookup_test_result.json";

        Value lookupRequest = Value.create();

        lookupRequest.children().put(LEFT_DATA, leftDataArray);
        lookupRequest.children().put(RIGHT_DATA, rightDataArray);

        lookupRequest.setFirstChild(LEFT_PATH, "patient_id");
        lookupRequest.setFirstChild(RIGHT_PATH, "patient_id");
        lookupRequest.setFirstChild(DST_PATH, "temps");

        Value lookup = LookupQuery.lookup(lookupRequest);
        Value testResultData = Value.create();
        parseJsonIntoValue(new FileReader(PATH_TO_RESOURCES + "/" + TEST_RESULT_NAME), testResultData, false);
        ValueVector children = testResultData.getChildren("_");

        assert(Utils.checkVectorEquality( lookup.getChildren( "result" ), children));
    }

    @Test
    public void lookupTestPaths() throws FaultException, IOException {
        final String TEST_RESULT_NAME = "lookup_test_paths_result.json";

        Value lookupRequest = Value.create();

        lookupRequest.children().put(LEFT_DATA, leftDataArray);
        lookupRequest.children().put(RIGHT_DATA, rightDataArray);

        lookupRequest.setFirstChild(LEFT_PATH, "patient_id");
        lookupRequest.setFirstChild(RIGHT_PATH, "patient.patient_id");
        lookupRequest.setFirstChild(DST_PATH, "temps");

        Value lookup = LookupQuery.lookup(lookupRequest);

        Value testResultData = Value.create();
        parseJsonIntoValue(new FileReader(PATH_TO_RESOURCES + "/" + TEST_RESULT_NAME), testResultData, false);
        ValueVector children = testResultData.getChildren("_");

        assert(Utils.checkVectorEquality(lookup.getChildren( "result" ), children));
    }

    @Test
    public void lookupTestEmptyDst() throws FaultException {
        Value lookupRequest = Value.create();

        lookupRequest.children().put(LEFT_DATA, leftDataArray);
        lookupRequest.children().put(RIGHT_DATA, rightDataArray);

        lookupRequest.setFirstChild(LEFT_PATH, "quality");
        lookupRequest.setFirstChild(RIGHT_PATH, "quality");
        lookupRequest.setFirstChild(DST_PATH, "temp");

        Value lookup = LookupQuery.lookup(lookupRequest);

        assert (leftDataArray.first().equals(lookup.getChildren( "result" ).first()));
    }
}
