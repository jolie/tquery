package joliex.tquery.engine.lookup;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.tquery.engine.common.Path;
import joliex.tquery.engine.common.TQueryExpression;
import joliex.tquery.engine.common.Utils;
import joliex.tquery.engine.match.EqualDataExpression;
import joliex.tquery.engine.project.ProjectExpression;
import joliex.tquery.engine.project.ValueToPathProjectExpression;

import java.util.Optional;

public final class LookupQuery {
	private static class RequestType {

		private static final String LEFT_DATA = "leftData";
		private static final String RIGHT_DATA = "rightData";
		private static final String LEFT_PATH = "leftPath";
		private static final String RIGHT_PATH = "rightPath";
		private static final String DST_PATH = "dstPath";

	}

	public static Value lookup( Value lookupRequest ) throws FaultException {
		ValueVector leftData = lookupRequest.getChildren( RequestType.LEFT_DATA );
		ValueVector rightData = lookupRequest.getChildren( RequestType.RIGHT_DATA );

		Value leftPathValue = lookupRequest.getFirstChild( RequestType.LEFT_PATH );
		Value rightPathValue = lookupRequest.getFirstChild( RequestType.RIGHT_PATH );
		Value dstPath = lookupRequest.getFirstChild( RequestType.DST_PATH );

		ValueVector result = ValueVector.create();
		Path leftPath;
		Path rightPath;

		try {
			leftPath = Path.parsePath( leftPathValue.strValue() );
		} catch ( IllegalArgumentException e ) {
			throw new FaultException( "LookupQuerySyntaxException", "Could not parse left path value" + Utils.valueToPrettyString( leftPathValue ) );
		}

		try {
			rightPath = Path.parsePath( rightPathValue.strValue() );
		} catch ( IllegalArgumentException e ) {
			throw new FaultException( "LookupQuerySyntaxException", "Could not parse right path value" + Utils.valueToPrettyString( rightPathValue ) );
		}

		try {
			Path.parsePath( dstPath.strValue() );
		} catch ( IllegalArgumentException e ) {
			throw new FaultException( "LookupQuerySyntaxException", "Could not parse destination path " + Utils.valueToPrettyString( dstPath ) );
		}

		try {
			Flowable.range( 0, leftData.size() )
							.subscribeOn( Schedulers.computation() )
							.blockingSubscribe( index -> {
								try {
									ValueVector responseVector = ValueVector.create();
									Value leftValue = leftData.get( index );
									Optional< ValueVector > optionalValues = leftPath.apply( leftValue );

									if ( optionalValues.isPresent() ) {
										ValueVector values = optionalValues.get();

										//check if the rightData array contains any tree under the rightPath with the content equals to the content of values
										EqualDataExpression v = new EqualDataExpression( rightPath, values );
										boolean[] mask = v.applyOn( rightData );

										for ( int i = 0; i < mask.length; i++ ) {
											if ( mask[ i ] ) {
												responseVector.add( rightData.get( i ) );
											}
										}

										ProjectExpression beta = new ValueToPathProjectExpression( dstPath.strValue(), responseVector );
										Value value = beta.applyOn( leftValue );
										result.set( index, Utils.merge( leftValue, value ) );
									}
								} catch ( FaultException e ) {
									throw new RuntimeException( e.getMessage() );
								}
							}
			);
		} catch ( Exception e ){
			throw new FaultException( e.getMessage() );
		}

		Value response = Value.create();
		response.children().put( TQueryExpression.ResponseType.RESULT, result );
		return response;
	}

}
