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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

public class PipelineQueryTestBenchmark {

	public static void main( String[] args ) throws IOException {

		Value biometricData = getValueFromJson( "tests/data/biometric-5.json" );
		Value biometricQuery = getValueFromJson( "tests/data/biometric-query.json" );

		Value sleeplogData = getValueFromJson( "tests/data/sleeplog-5.json" );
		Value sleeplogQuery = getValueFromJson( "tests/data/sleeplog-query.json" );

		IntStream.range( 0, 3 ).mapToDouble( i ->
			IntStream.range( 0, 20 ).parallel().map( _i -> {
				LinkedList< ValueVector > lookupData = new LinkedList<>();

				long bio_start = System.currentTimeMillis();
				lookupData.add( performQuery( biometricData, biometricQuery ) );
				long bioTime = System.currentTimeMillis() - bio_start;

				long sleep_start = System.currentTimeMillis();
				performQuery( sleeplogData, sleeplogQuery, lookupData );
				long sleepTime = System.currentTimeMillis() - sleep_start;

				long queryTime = bioTime + sleepTime;
				System.out.println( "Benchmark time: " + queryTime + " milliseconds." );
				return (int) queryTime;
			} ).average().getAsDouble()
		).average().ifPresent( avg -> System.out.println( "Benchmarks average: " + avg + " milliseconds" ) );


	}

	public static Value getValueFromJson( String filename ) throws IOException {
		String jsonString = Files.readString( Path.of( filename ) );
		StringReader reader = new StringReader( jsonString );
		Value v = Value.create();
		JsUtils.parseJsonIntoValue( reader, v, false );
		return v;
	}

	public static ValueVector performQuery( Value data, Value pipeline ) {
		return performQuery( data, pipeline, Collections.emptyList() );
	}

	public static ValueVector performQuery( Value data, Value pipeline, List< ValueVector > lookupData ) {
		Value request = Value.create();
		request.children().put( "data", data.getChildren( "_" ) );
		request.children().put( "pipeline", pipeline.getChildren( "_" ) );
//		System.out.println( Utils.valueToPrettyString( request ) );
		request.getChildren( "pipeline" ).stream()
						.filter( value -> value.hasChildren( "lookupQuery" ) )
						.forEach( value ->
										value.getFirstChild( "lookupQuery" ).children().put( "rightData", lookupData.remove( 0 ) )
						);
		try {
			Value response = PipelineQuery.pipeline( request );
			if ( data.getChildren( "_" ).get( 0 ).hasChildren("y") ) {
				System.out.println(
								"#q:" + response.getFirstChild( "result" ).getChildren( "quality" ).size()
												+ "; #t:" + response.getFirstChild( "result" ).getChildren( "temperatures" ).size() );
			}
//			System.out.println( Utils.valueToPrettyString( response ) );
			return response.getChildren( TQueryExpression.ResponseType.RESULT );
		} catch ( FaultException e ) {
			e.printStackTrace();
		}
		return null;
	}

}
