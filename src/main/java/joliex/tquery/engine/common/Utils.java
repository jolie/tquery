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

package joliex.tquery.engine.common;

import jolie.runtime.*;
import jolie.runtime.expression.CompareCondition;
import jolie.runtime.typing.TypeCastingException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Utils {

	public static boolean strictEquals( Value v1, Value v2 ) {
		boolean r = false;
		try {
			if ( v1.isDefined() && v2.isDefined() ) {
				if ( v1.isByteArray() && v2.isByteArray() ) {
					r = v1.byteArrayValueStrict().equals( v2.byteArrayValueStrict() );
				} else if ( v1.isString() && v2.isString() ) {
					r = v1.strValueStrict().equals( v2.strValueStrict() );
				} else if ( v1.isInt() && v2.isInt() ) {
					r = v1.intValueStrict() == v2.intValueStrict();
				} else if ( v1.isDouble() && v2.isDouble() ) {
					r = v1.doubleValueStrict() == v2.doubleValueStrict();
				} else if ( v1.isBool() && v2.isBool() ) {
					r = v1.boolValueStrict() == v1.boolValueStrict();
				} else if ( v1.isLong() && v2.isLong() ) {
					r = v1.longValueStrict() == v2.longValueStrict();
				} else if ( v1.valueObject() != null && v2.valueObject() != null ) {
					r = v1.valueObject().equals( v2.valueObject() );
				}
			} else {
				// undefined == undefined
				r = !( v1.isDefined() && v2.isDefined() );
			}
		} catch ( TypeCastingException ignored ) {}
		return r;
	}

	public static boolean checkTreeEquality( Value v1, Value v2 ) {
//		if ( v1.equals( v2 ) ){ // if the root value matches
		if ( strictEquals( v1, v2 ) ) {
			if ( v1.children().keySet().equals( v2.children().keySet() ) ) {
				for ( String node : v1.children().keySet() ) {
					if ( !checkVectorEquality( v1.getChildren( node ), v2.getChildren( node ) ) ) {
						return false;
					}
				}
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public static boolean checkVectorEquality( ValueVector v1, ValueVector v2 ) {
		if ( v1.size() == v2.size() ) {
			for ( int i = 0; i < v1.size(); i++ ) {
				if ( !checkTreeEquality( v1.get( i ), v2.get( i ) ) ) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public static Value merge( Value v1, Value v2 ) throws FaultException {
		if ( CompareOperators.EQUAL.test( v1, v2 ) ) {
			Value returnValue = v1.clone();
			// REMOVE THIS AND JUST USE CONDITIONALS
			Set< String > keySetIntersection = new HashSet<>( v1.children().keySet() );
			keySetIntersection.retainAll( v2.children().keySet() );
			// we just need the unique set of keys for v2 since we already cloned the keys and children of V1
			Set< String > uniqueKeySetV2 = new HashSet<>( v2.children().keySet() );
			uniqueKeySetV2.removeAll( keySetIntersection );
			for ( String key : keySetIntersection ) {
				returnValue.children().put( key, merge( v1.getChildren( key ), v2.getChildren( key ) ) );
			}
			uniqueKeySetV2.forEach( ( key ) -> {
				returnValue.children().put( key, v2.getChildren( key ) );
			} );
			return returnValue;
		} else {
			throw new FaultException(
							"MergeValueException",
							"Values: \n" + valueToPrettyString( v1 ) + "\n and \n" + valueToPrettyString( v2 ) + "\n cannot be merged"
			);
		}
	}

	public static ValueVector merge( ValueVector v1, ValueVector v2 ) throws FaultException {
		if ( v1.size() >= v2.size() ) {
			ValueVector returnVector = ValueVector.create();
			for ( int i = 0; i < v1.size(); i++ ) {
				if ( v2.size() > i ) {
					returnVector.add( merge( v1.get( i ), v2.get( i ) ) );
				} else {
					returnVector.add( v1.get( i ) );
				}
			}
			return returnVector;
		} else {
			return merge( v2, v1 );
		}
	}

	public static String valueToPrettyString( Value v ) {
		Writer writer = new StringWriter();
		ValuePrettyPrinter printer = new ValuePrettyPrinter( v, writer, "Value" );
		try {
			printer.run();
		} catch ( IOException e ) {
		} // Should never happen
		return writer.toString();
	}

	public static ValueVector listToValueVector( List< Value > values ) {
		ValueVector returnVector = ValueVector.create();
		values.stream().forEach( returnVector::add );
		return returnVector;
	}


	public static < T > T applyOrFault( Supplier< T > supplier ) throws FaultException {
		try {
			return supplier.get();
		} catch ( Exception e ){
			throw new FaultException( e.getMessage() );
		}
	}

}
