import jolie.runtime.FaultException;
import jolie.runtime.Value;
import joliex.tquery.engine.common.Utils;
import joliex.tquery.engine.group.GroupQuery;

import java.io.IOException;
import java.io.StringReader;

import static jolie.js.JsUtils.parseJsonIntoValue;

public class GroupQueryTest {

	public static void main( String[] args ) {
		Value v = Value.create();
		try {
			parseJsonIntoValue(
							new StringReader(
										"{\"data\":[" +
												"{\"year\":[2020],\"month\":[11],\"day\":[27],\"quality\":[\"poor\"]}," +
												"{\"year\":[2020],\"month\":[11],\"day\":[29],\"quality\":[\"good\"]}," +
												"{\"year\":[2020],\"month\":[11],\"day\":[29],\"quality\":[\"good\"]}" +
											"]," +
											"\"query\":{" +
															"\"groupBy\":[" +
																"{\"dstPath\":\"day\",\"srcPath\":\"day\",}" +
															"]," +
															"\"aggregate\":[" +
																"{\"dstPath\":\"quality\"," +
																"\"srcPath\":\"quality\"," +
																"\"distinct\":false" +
															"}]" +
											"}" +
										"}"
							),
							v,
							false
			);
			System.out.println( Utils.valueToPrettyString( GroupQuery.group( v ) ) );
		} catch ( IOException | FaultException e ) {
			e.printStackTrace();
		}
	}

}
