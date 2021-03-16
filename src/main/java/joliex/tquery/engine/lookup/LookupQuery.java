package joliex.queryengine.lookup;

import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.common.Path;
import joliex.queryengine.common.TQueryExpression;
import joliex.queryengine.common.Utils;
import joliex.queryengine.match.EqualExpression;
import joliex.queryengine.project.ValueToPathProjectExpression;

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
    	ValueVector leftData = lookupRequest.getChildren(RequestType.LEFT_DATA);
        ValueVector rightData = lookupRequest.getChildren(RequestType.RIGHT_DATA);

        Value leftPathValue = lookupRequest.getFirstChild(RequestType.LEFT_PATH);
        Value rightPathValue = lookupRequest.getFirstChild(RequestType.RIGHT_PATH);
        Value dstPath = lookupRequest.getFirstChild(RequestType.DST_PATH);

        ValueVector result = ValueVector.create();
		Path leftPath;
		Path rightPath;

        try {
			leftPath = Path.parsePath(leftPathValue.strValue());
		} catch (IllegalArgumentException e){
			throw new FaultException( "LookupQuerySyntaxException", "Could not parse left path value" + Utils.valueToPrettyString( leftPathValue ));
		}

		try {
			rightPath = Path.parsePath(rightPathValue.strValue());
		} catch (IllegalArgumentException e){
			throw new FaultException( "LookupQuerySyntaxException", "Could not parse right path value" + Utils.valueToPrettyString( rightPathValue ));
		}

		try {
			Path.parsePath(dstPath.strValue());
		} catch (IllegalArgumentException e){
			throw new FaultException( "LookupQuerySyntaxException", "Could not parse destination path " + Utils.valueToPrettyString( dstPath ));
		}

        for (Value leftValue : leftData){
			ValueVector responseVector = ValueVector.create();

			//result of the path application to the first tree (leftValue) of leftData array
			Optional< ValueVector > optionalValues = leftPath.apply( leftValue );

			if( optionalValues.isPresent() ){
				ValueVector values = optionalValues.get();

				//check if the rightData array contains any tree under the rightPath with the content equals to the content of values
				EqualExpression v = new EqualExpression( rightPath, values );
				boolean[] mask = v.applyOn( rightData );

				for( int i = 0; i < mask.length; i++ ){
					if( mask[ i ] ){
						responseVector.add( rightData.get( i ) );
					}
				}

				TQueryExpression beta = new ValueToPathProjectExpression( dstPath.strValue(), responseVector );
				Value value = beta.applyOn( leftValue );

				result.add( Utils.merge( leftValue, value ) );
			}

		}
		Value response = Value.create();
		response.children().put( TQueryExpression.ResponseType.RESULT, result );
		return response;
    }

}
