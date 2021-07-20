import jolie.js.JsUtils;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.tquery.engine.common.TQueryExpression;
import joliex.tquery.engine.common.Utils;
import joliex.tquery.engine.pipeline.PipelineQuery;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class PipelineQueryTest {

	public static void main( String[] args ) throws IOException {
		sleeplogQuery();
	}

	public static ValueVector biometricQuery() throws IOException {
		String jsonString = Files.readString( Path.of( "release_test/data/biometric-0.json" ) );
		StringReader reader = new StringReader( jsonString );
		Value data = Value.create();
		JsUtils.parseJsonIntoValue( reader, data, false );

		jsonString = Files.readString( Path.of( "release_test/data/biometric-query.json" ) );
		jsonString = jsonString.replaceAll( "//.+", "" );
		reader = new StringReader( jsonString );
		Value pipeline = Value.create();
		JsUtils.parseJsonIntoValue( reader, pipeline, false );

		Value request = Value.create();
		request.children().put( "data", data.getChildren( "_" ) );
		request.children().put( "pipeline", pipeline.getChildren( "_" ) );

//		System.out.println( Utils.valueToPrettyString( request ) );

		try {
			Value response = PipelineQuery.pipeline( request );
//			System.out.println( Utils.valueToPrettyString( response ) );
			return response.getChildren( TQueryExpression.ResponseType.RESULT );
		} catch ( FaultException e ) {
			e.printStackTrace();
		}
		return null;
	}

	public static void sleeplogQuery() throws IOException {

		String jsonString = Files.readString( Path.of( "release_test/data/sleeplog-0.json" ) );
		StringReader reader = new StringReader( jsonString );
		Value data = Value.create();
		JsUtils.parseJsonIntoValue( reader, data, false );

		jsonString = Files.readString( Path.of( "release_test/data/sleeplog-query.json" ) );
		jsonString = jsonString.replaceAll( "//.+", "" );
		reader = new StringReader( jsonString );
		Value pipeline = Value.create();
		JsUtils.parseJsonIntoValue( reader, pipeline, false );

		Value request = Value.create();
		request.children().put( "data", data.getChildren( "_" ) );
		request.children().put( "pipeline", pipeline.getChildren( "_" ) );

		request.getChildren( "pipeline" ).stream()
						.filter( value -> value.hasChildren( "lookupQuery" ) )
						.findAny().ifPresent( value -> {
			try {
				value.getFirstChild( "lookupQuery" ).children().put( "rightData", biometricQuery() );
			} catch ( IOException e ) {
				e.printStackTrace();
			}
		} );

//		System.out.println( Utils.valueToPrettyString( request ) );

		try {
			Value response = PipelineQuery.pipeline( request );
			System.out.println( Utils.valueToPrettyString( response ) );
		} catch ( FaultException e ) {
			e.printStackTrace();
		}


	}

}
