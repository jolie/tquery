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

package joliex.tquery.engine.group;

import jolie.js.JsUtils;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.tquery.engine.common.Path;
import joliex.tquery.engine.match.*;
import joliex.tquery.engine.project.ProjectExpressionChain;
import joliex.tquery.engine.project.ValueToPathProjectExpression;
import joliex.tquery.engine.project.valuedefinition.ConstantValueDefinition;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Utils {

	public static class GroupPair {
		private final Path s, d;

		public GroupPair( Path sourcePath, Path destinationPath ) {
			this.s = sourcePath;
			this.d = destinationPath;
		}

		public Path dstPath() {
			return d;
		}

		public Path srcPath() {
			return s;
		}

	}

	public static GroupPair getGroupPair( Path sourcePath, Path destinationPath ) {
		return new GroupPair( sourcePath, destinationPath );
	}

	private static class DistinctFilter {
		Set< ValueVector > seen;
		DistinctFilter(){ seen = new HashSet<>(); }
		public boolean isDistinct( ValueVector element ) {
			if ( seen.stream().anyMatch( v -> joliex.tquery.engine.common.Utils.checkVectorEquality( v, element ) ) ){
				return false;
			} else {
				seen.add( element );
				return true;
			}
		}
	}

	public static void main( String[] args ) throws IOException {
//		String jsonString = "{ \"data\": [\n" +
//			"  { \"a\": [ { \"d\" : [ \"1.a.d.1\" , \"1.a.d.2\" , \"1.a.d.3\" ] }, { \"e\": [ \"1.a.e.1\", \"1.a.e.2\" ] } ], \"b\": [ \"1.b.1\", \"1.b.2\" ], \"c\": { \"f\": [ \"1.c.f.1\", \"1.c.f.2\" ] } },\n" +
//			"  { \"a\": { \"e\": [ \"2.a.e.1\", \"2.a.e.2\" ] }, \"b\": [ \"2.b.1\" ], \"c\": { \"f\": [ \"2.c.f.1\" ] } }, \n" +
//			"  { \"a\": { \"d\": [ \"3.a.d.1\", \"3.a.d.2\", \"3.a.d.3\", \"3.a.d.4\", \"3.a.d.5\", \"3.a.d.6\" ] }, \"c\": { \"f\": [  \"3.c.f.1\" ] } }\n" +
//			"]}";
		String jsonString = "{ \"data\": [\n" +
				"  { \"a\": [ { \"d\" : [ 5, 3, 1 ] }, { \"e\": [ 2, 4 ] } ], \"b\": [ 1, 3 ], \"c\": { \"f\": [ 1, 6 ] } },\n" +
				"  { \"a\": [ { \"d\" : [ 5, 3, 1 ] }, { \"e\": [ 6, 7 ] } ], \"b\": [ 1, 3 ], \"c\": { \"f\": [ 1, 6 ] } },\n" +
				"  { \"a\": { \"e\": [ 2, 5 ] }, \"b\": [ 9 ], \"c\": { \"f\": [ 6 ] } }, \n" +
				"  { \"a\": { \"d\": [ 1, 8, 7 ] }, \"c\": { \"f\": [  1 ] } }\n" +
				"]}";
		Value v = Value.create();
		JsUtils.parseJsonIntoValue( new StringReader( jsonString ), v, false );

		// grouping request parsing, i.e., s_1 > r_1, ..., s_n > r_n
		List< GroupPair > groupingList = new ArrayList<>();
		groupingList.add( getGroupPair( Path.parsePath( "a.d" ), Path.parsePath( "nodeA" ) ) );
		groupingList.add( getGroupPair( Path.parsePath( "b" ), Path.parsePath( "nodeB" ) ) );
		groupingList.add( getGroupPair( Path.parsePath( "c" ), Path.parsePath( "nodeC" ) ) );

		List< GroupPair > aggregationList = new ArrayList<>();
		aggregationList.add( getGroupPair( Path.parsePath( "a.e" ), Path.parsePath( "nodeE" ) ) );

		ValueVector dataElements = v.children().get( "data" );
		ValueVector resultElements = ValueVector.create();

		List< List< Boolean > > combinations = getCombinations( groupingList.size() );
		List< MatchExpression > existenceGroups =
				combinations.stream().map( c -> getMatchExistenceExpression( c, groupingList ) ).collect( Collectors.toList() );
		List< List< Value > > concatenationList =
			existenceGroups.stream().map( matchExpression -> {
				boolean[] bitMask = matchExpression.applyOn( dataElements );
				List< Value > dataList = IntStream.range( 0, bitMask.length )
						.filter( i -> bitMask[ i ] )
						.mapToObj( dataElements::get )
						.collect( Collectors.toList() );
				return group( dataList, combinations.get( existenceGroups.indexOf( matchExpression ) ), groupingList, aggregationList );
			}).collect( Collectors.toList() );
		concatenationList.forEach( l -> l.forEach( e -> System.out.println( joliex.tquery.engine.common.Utils.valueToPrettyString( e ) ) ) );
	}

	private static List< Value > group( List< Value > dataList, List< Boolean > combination, List< GroupPair > groupingList, List< GroupPair > aggregationList ){
		List< Value > returnVector = new ArrayList<>();
		if( dataList.size() > 0 ){
			if( combination.stream().anyMatch( i -> i ) ){
				List< Path > paths = IntStream.range( 0, combination.size() ).filter( combination::get ).mapToObj( i -> groupingList.get( i ).srcPath() ).collect( Collectors.toList() );
				boolean[] bitMask = getMatchGroupingExpression( paths, dataList.get( 0 ) )
						.applyOn( joliex.tquery.engine.common.Utils.listToValueVector( dataList ) );
				List< Value > current = new ArrayList<>();
				List< Value > residuals = new ArrayList<>();
				IntStream.range( 0, bitMask.length ).forEach( i -> {
					if( bitMask[ i ] ){
						current.add( dataList.get( i ) );
					} else {
						residuals.add( dataList.get( i ) );
					}
				} );
				returnVector.add( aggregate( current, groupingList, aggregationList ) );
				returnVector.addAll( group( residuals, combination, groupingList, aggregationList ) );
			} else {
				returnVector.add( aggregate( dataList, groupingList, aggregationList ) );
			}
		}
		return returnVector;
	}

	private static MatchExpression getMatchGroupingExpression( List< Path > paths, Value v  ){
		if( paths.size() < 0 ){
			throw new RuntimeException( "getMatchGroupingExpression received a combination of size 0" ); //todo: refine this error into a Fault
		} else {
			MatchExpression currentExpression = new EqualExpression( paths.get( 0 ), paths.get( 0 ).apply( v ).get() ); // we can always get here, because we passed the existence tests in the group method
			if( paths.size() > 1 ){
				return BinaryExpression.AndExpression(
						currentExpression,
						getMatchGroupingExpression( paths.subList( 1, paths.size() ), v )
				);
			} else {
				return currentExpression;
			}
		}
	}

	private static Value aggregate( List< Value > dataList, List< GroupPair > groupingList, List< GroupPair > aggregationList ){
		Value returnValue = Value.create();
		ProjectExpressionChain returnProjectionChain = new ProjectExpressionChain();
		groupingList.forEach( groupElement ->
		{
			try {
				Optional< ValueVector > maybeValueVector = groupElement.srcPath().apply( dataList.get( 0 ) );
				if( maybeValueVector.isPresent() ){
					returnProjectionChain.addExpression(
							new ValueToPathProjectExpression(
									groupElement.dstPath(),
									new ConstantValueDefinition( maybeValueVector.get() )
							) );
				}
			} catch( FaultException e ){
				e.printStackTrace();
			}
		} );
		aggregationList.forEach( aggregationElement ->
		{
			DistinctFilter d = new DistinctFilter();
			List< ValueVector > distinctValueVectors = dataList.stream()
					.map( v -> aggregationElement.srcPath().apply( v ) )
					.filter( Optional::isPresent )
					.map( Optional::get )
					.filter( d::isDistinct )
					.collect( Collectors.toList() );
			ValueVector mergedValueVector = ValueVector.create();
			distinctValueVectors.stream().flatMap( ValueVector::stream ).forEach( mergedValueVector::add );
			try {
				returnProjectionChain.addExpression(
						new ValueToPathProjectExpression(
								aggregationElement.dstPath(),
								new ConstantValueDefinition( mergedValueVector )
						) );
			} catch( FaultException e ){
				e.printStackTrace();
			}
		} );
		try{
			returnValue = returnProjectionChain.applyOn( returnValue );
		} catch( FaultException e ){
			e.printStackTrace();
		}
		return returnValue;
	}

	private static List< List< Boolean > > getCombinations( int size ) {
		if( size > 1 ){
			List< List< Boolean > > subCombinations = getCombinations( size - 1 );
			return subCombinations.stream()
					.flatMap( e -> Stream.of(
								Stream.concat( Stream.of( true ), e.stream() ).collect( Collectors.toList() ),
								Stream.concat( Stream.of( false ), e.stream() ).collect( Collectors.toList() )
							) )
					.collect( Collectors.toList() );
		}
		if( size > 0 ){
			return List.of( Collections.singletonList( true ), Collections.singletonList( false ) );
		} else {
			return Collections.emptyList();
		}
	}

	private static MatchExpression getMatchExistenceExpression( List< Boolean > combination, List< GroupPair > groupPairList ){
		if( combination.size() < 0 || groupPairList.size() < 0 ){
			throw new RuntimeException( "getMatchExistenceExpression received a combination of size 0" ); //todo: refine this error into a Fault
		} else {
			MatchExpression currentExpression = new ExistsExpression( groupPairList.get( 0 ).srcPath() );
			currentExpression = combination.get( 0 ) ? currentExpression : new NotExpression( currentExpression );
			if( combination.size() > 1 ){
				return BinaryExpression.AndExpression(
						currentExpression,
						getMatchExistenceExpression( combination.subList( 1, combination.size() ), groupPairList.subList( 1, combination.size() ) )
				);
			} else {
				return currentExpression;
			}
		}
	}

}
