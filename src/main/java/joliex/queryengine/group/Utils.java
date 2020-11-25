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

package joliex.queryengine.group;

import jolie.js.JsUtils;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.util.Pair;
import joliex.queryengine.common.Path;
import joliex.queryengine.match.*;
import joliex.queryengine.project.ProjectExpressionChain;
import joliex.queryengine.project.ValueToPathProjectExpression;
import joliex.queryengine.project.valuedefinition.ConstantValueDefinition;

import javax.management.RuntimeErrorException;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
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

	public GroupPair getGroupPair( Path sourcePath, Path destinationPath ) {
		return new GroupPair( sourcePath, destinationPath );
	}

	public static void main( String[] args ) throws IOException {
//		String jsonString = "{ \"data\": [\n" +
//			"  { \"a\": [ { \"d\" : [ \"1.a.d.1\" , \"1.a.d.2\" , \"1.a.d.3\" ] }, { \"e\": [ \"1.a.e.1\", \"1.a.e.2\" ] } ], \"b\": [ \"1.b.1\", \"1.b.2\" ], \"c\": { \"f\": [ \"1.c.f.1\", \"1.c.f.2\" ] } },\n" +
//			"  { \"a\": { \"e\": [ \"2.a.e.1\", \"2.a.e.2\" ] }, \"b\": [ \"2.b.1\" ], \"c\": { \"f\": [ \"2.c.f.1\" ] } }, \n" +
//			"  { \"a\": { \"d\": [ \"3.a.d.1\", \"3.a.d.2\", \"3.a.d.3\", \"3.a.d.4\", \"3.a.d.5\", \"3.a.d.6\" ] }, \"c\": { \"f\": [  \"3.c.f.1\" ] } }\n" +
//			"]}";
		String jsonString = "{ \"data\": [\n" +
				"  { \"a\": [ { \"d\" : [ 5, 3, 1 ] }, { \"e\": [ 2, 4 ] } ], \"b\": [ 1, 3 ], \"c\": { \"f\": [ 1, 6 ] } },\n" +
				"  { \"a\": { \"e\": [ 2, 5 ] }, \"b\": [ 9 ], \"c\": { \"f\": [ 6 ] } }, \n" +
				"  { \"a\": { \"d\": [ 1, 8, 7 ] }, \"c\": { \"f\": [  1 ] } }\n" +
				"]}";
		Value v = Value.create();
		JsUtils.parseJsonIntoValue( new StringReader( jsonString ), v, false );

		// grouping request parsing, i.e., s_1 > r_1, ..., s_n > r_n
		List< GroupPair > groupingList = new ArrayList<>();
		Utils u = new Utils();
		groupingList.add( u.getGroupPair( Path.parsePath( "a.d" ), Path.parsePath( "nodeA" ) ) );
		groupingList.add( u.getGroupPair( Path.parsePath( "b" ), Path.parsePath( "nodeB" ) ) );
		groupingList.add( u.getGroupPair( Path.parsePath( "c" ), Path.parsePath( "nodeC" ) ) );

		List< GroupPair > aggregationList = new ArrayList<>();
		aggregationList.add( u.getGroupPair( Path.parsePath( "a.e" ), Path.parsePath( "nodeE" ) ) );

		ValueVector dataElements = v.children().get( "data" );
		ValueVector resultElements = ValueVector.create();

		// we get all the combinations h \in H of the grouping set
		// (e.g., (\neg \exists s1, ..., \neg \exists sn), (\exists s1, ..., \neg \exists sn), etc
		List< List< Boolean > > combinations = getCombinations( groupingList.size() );
		List< MatchExpression > existenceGroups =
				combinations.stream().map( c -> getMatchExistenceExpression( c, groupingList ) ).collect( Collectors.toList() );
		List< ValueVector > concatenationList =
			existenceGroups.stream().map( matchExpression -> {
				boolean[] bitMask = matchExpression.applyOn( dataElements );
				List< Value > dataList = IntStream.range( 0, bitMask.length )
						.filter( i -> bitMask[ i ] )
						.mapToObj( dataElements::get )
						.collect( Collectors.toList() );
				return group( dataList, bitMask, groupingList, aggregationList );
			}).collect( Collectors.toList() );
	}

	private static ValueVector group( List< Value > dataList, boolean[] combination, List< GroupPair > groupingList, List< GroupPair > aggregationList ){
		System.out.println( Arrays.toString( combination ) );
		return null;
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
		MatchExpression currentExpression = new ExistsExpression( groupPairList.get( 0 ).s );
		currentExpression = combination.get( 0 ) ? currentExpression : new NotExpression( currentExpression );
		if( combination.size() > 1 ){
			return BinaryExpression.AndExpression(
					currentExpression,
					getMatchExistenceExpression( combination.subList( 1, combination.size() - 1 ), groupPairList.subList( 1, combination.size() - 1 ) )
			);
		}
		if( combination.size() > 0 ){
			return currentExpression;
		} else {
			throw new RuntimeException( "getMatchExistenceExpression received a combination of size 0" ); //todo: refine this error into a Fault
		}
	}

}
