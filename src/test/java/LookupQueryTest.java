import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.common.Utils;
import joliex.queryengine.lookup.LookupQuery;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

import static jolie.js.JsUtils.parseJsonIntoValue;

public class LookupQueryTest {
    static final String PATH_TO_RESOURCES = "src/test/resources";

    @Test
    public void lookupTest() throws IOException, FaultException {
        Value lookupRequest = Value.create();
        Value rightData = Value.create();
        Value leftData = Value.create();

        parseJsonIntoValue(new FileReader(PATH_TO_RESOURCES + "/temperature.json"), rightData, false);
        parseJsonIntoValue( new FileReader(PATH_TO_RESOURCES + "/quality.json"), leftData, false );

        lookupRequest.children().put( "leftData", leftData.getChildren( "_" ) );
        lookupRequest.children().put( "rightData", rightData.getChildren( "_" ) );

        lookupRequest.setFirstChild( "leftPath", "patient_id" );
        lookupRequest.setFirstChild( "rightPath", "patient_id" );
        lookupRequest.setFirstChild( "dstPath", "temps" );
		
		System.out.println( Utils.valueToPrettyString( lookupRequest ) );

        ValueVector lookup = LookupQuery.lookup( lookupRequest);
        System.out.println("Result: ");
        lookup.forEach(it -> System.out.println( Utils.valueToPrettyString( it ) ) );
    }
}