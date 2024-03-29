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

package joliex.tquery.engine.unwind;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.tquery.engine.common.Path;

import java.util.Arrays;
import java.util.stream.IntStream;

class UnwindExpression {

	private final Path path;

	UnwindExpression( Path path ) {
		this.path = path;
	}

	public ValueVector applyOn( ValueVector elements ) {
		ValueVector resultElements = ValueVector.create();

		ValueVector[] batches = new ValueVector[ elements.size() ];

		IntStream.range( 0, elements.size() )
						.parallel()
						.forEach( index -> {
							try {
								String node = path.getCurrentNode();
								Value element = elements.get( index );
								ValueVector elementsContinuation = Path.parsePath( node )
												.apply( element )
												.orElse( ValueVector.create() );
								if ( path.getContinuation().isPresent() ) {
									elementsContinuation =
													new UnwindExpression( path.getContinuation().get() )
																	.applyOn( Path.parsePath( node ).apply( element ).orElse( ValueVector.create() ) );
								}
								batches[ index ] = expand( element, elementsContinuation, node );
							} catch ( Exception e ) {
								e.printStackTrace(); // we should never reach this due to a Path.parse exception since the path is checked at the beginnign
							}
						} );
		Arrays.stream( batches ).flatMap( ValueVector::stream ).forEach( resultElements::add );
		return resultElements;

	}


	private ValueVector expand( Value element, ValueVector elements, String node ) {
		ValueVector returnVector = ValueVector.create();

		IntStream.range( 0, elements.size() )
						.parallel()
						.forEach( index -> {
							Value thisElement = Value.create();
							returnVector.set( index, thisElement );
							element.children().keySet().stream()
											.filter( key -> !key.equals( node ) )
											.parallel()
											.forEach( key ->
															thisElement.children().put( key, ValueVector.createClone( element.getChildren( key ) ) )
											);
							thisElement.children().put( node, getFreshValueVector( elements.get( index ) ) );
						} );

		return returnVector;
	}

	private ValueVector getFreshValueVector( Value element ) {
		ValueVector result = ValueVector.create();
		result.add( element );
		return result;
	}

}
