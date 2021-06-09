import jolie.js.JsUtils;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValuePrettyPrinter;
import jolie.runtime.ValueVector;
import joliex.tquery.engine.common.Utils;
import joliex.tquery.engine.unwind.UnwindQuery;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class UnwindQueryTest {

	public static void main( String[] args ) throws IOException {

		String jsonString = Files.readString( Path.of( "tests/data/sleeplog-1.json" ) );
		StringReader reader = new StringReader( jsonString );
		Value data = Value.create();
		JsUtils.parseJsonIntoValue( reader, data, false );
		ValueVector query = ValueVector.create();
		query.get( 0 ).setValue( "M.D.L" );
		Value request = Value.create();
		request.children().put( "data", data.getChildren( "_" ) );
		request.children().put( "query", query );

//		System.out.println( Utils.valueToPrettyString( request ) );

		Long start = System.currentTimeMillis();

		try {
			Value response = UnwindQuery.unwind( request );
			System.out.println( "Finished after: " + ( System.currentTimeMillis() - start ) + "ms" );
//			System.out.println( Utils.valueToPrettyString( response ) );
		} catch ( FaultException e ) {
			e.printStackTrace();
		}

	}

}
