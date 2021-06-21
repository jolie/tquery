/*******************************************************************************
 *   Copyright (C) 2018 by Larisa Safina <safina@imada.sdu.dk>                 *
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *   Copyright (C) 2018 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                             *
 *   This program is free software; you can redistribute it and/or modify      *
 *   it under the terms of the GNU Library General Public License as           *
 *   published by the Free Software Foundation; either version 2 of the        *
 *   License, or (at your option) any later version.                           *
 *                                                                             *
 *   This program is distributed in the hope that it will be useful,           *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *   GNU General Public License for more details.                              *
 *                                                                             *
 *   You should have received a copy of the GNU Library General Public         *
 *   License along with this program; if not, write to the                     *
 *   Free Software Foundation, Inc.,                                           *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 *
 *                                                                             *
 *   For details about the authors of this software, see the AUTHORS file.     *
 *******************************************************************************/

package joliex.tquery.engine.project;

import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.tquery.engine.common.TQueryExpression;

import java.util.stream.IntStream;

public class ProjectQuery {

	private static class RequestType {

		private static final String DATA = "data";
		private static final String QUERY = "query";

		private static class ValueToPathExpression {
			private static final String DESTINATION_PATH = "dstPath";
			private static final String VALUE = "value";
		}
	}

	public static Value project( Value projectRequest ) throws FaultException {
		ValueVector query = projectRequest.getChildren( RequestType.QUERY );
		ValueVector dataElements = projectRequest.getChildren( RequestType.DATA );
		TQueryExpression projectExpression = parseProjectionChain( query );
		Value response = Value.create();
		ValueVector responseVector = ValueVector.create();
		response.children().put( TQueryExpression.ResponseType.RESULT, responseVector );

		try {
			IntStream.range( 0, dataElements.size() ).parallel()
							.forEach( i -> {
								try {
									responseVector.set( i, projectExpression.applyOn( dataElements.get( i ) ) );
								} catch ( FaultException e ) {
									throw new RuntimeException( e.getMessage() );
								}
							} );
		} catch ( Exception e ) {
			throw new FaultException( e.getMessage() );
		}
//		for ( Value dataElement : dataElements ) {
//			responseVector.add( projectExpression.applyOn( dataElement ) );
//		}
		return response;
	}

	private static ProjectExpressionChain parseProjectionChain( ValueVector queries ) throws FaultException {
		ProjectExpressionChain returnExpressionChain = new ProjectExpressionChain();
		for ( Value query : queries ) {
			returnExpressionChain.addExpression( parseProjectExpression( query ) );
		}
		return returnExpressionChain;
	}

	private static TQueryExpression parseProjectExpression( Value query ) throws FaultException {
		if ( query.isString() ) {
			return new PathProjectExpression( query.strValue() );
		} else {
			return new ValueToPathProjectExpression(
							query.getFirstChild( RequestType.ValueToPathExpression.DESTINATION_PATH ).strValue(),
							query.getChildren( RequestType.ValueToPathExpression.VALUE )
			);
		}
	}
}
