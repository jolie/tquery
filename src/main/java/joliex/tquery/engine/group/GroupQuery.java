package joliex.tquery.engine.group;

import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.tquery.engine.common.Path;
import joliex.tquery.engine.common.TQueryExpression;
import joliex.tquery.engine.match.*;
import joliex.tquery.engine.common.Utils;
import joliex.tquery.engine.project.ProjectExpressionChain;
import joliex.tquery.engine.project.ValueToPathProjectExpression;
import joliex.tquery.engine.project.valuedefinition.ConstantValueDefinition;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public final class GroupQuery {
	private static class RequestType {
		private static final String DATA = "data";
		private static final String QUERY = "query";
		private static final String AGGREGATE = "aggregate";
		private static final String GROUPBY = "groupBy";

		private static class GroupDefinition {
			private static final String DST_PATH = "dstPath";
			private static final String SRC_PATH = "srcPath";
			private static final String DISTINCT = "distinct";
		}
	}

	public static Value group( Value groupRequest ) throws FaultException {
		ValueVector dataElements = groupRequest.getChildren( RequestType.DATA );
		Value query = groupRequest.getFirstChild( RequestType.QUERY );
		ValueVector group = query.getChildren( RequestType.GROUPBY );
		ValueVector aggregate = query.getChildren( RequestType.AGGREGATE );

		// grouping request parsing, i.e., s_1 > r_1, ..., s_n > r_n
		List< GroupPair > groupingList = new LinkedList<>();
		for ( Value gRequest : group ) {
			groupingList.add( getGroupPair(
							Path.parsePath( gRequest.getFirstChild( RequestType.GroupDefinition.SRC_PATH ).strValue() ),
							Path.parsePath( gRequest.getFirstChild( RequestType.GroupDefinition.DST_PATH ).strValue() ) )
			);
		}

		// aggregation request parsing, i.e., q_1 > p_1, ..., q_n > p_n
		List< AggregatePair > aggregationList = new LinkedList<>();
		for ( Value gRequest : aggregate ) {
			aggregationList.add( getAggregatePair(
							Path.parsePath( gRequest.getFirstChild( RequestType.GroupDefinition.SRC_PATH ).strValue() ),
							Path.parsePath( gRequest.getFirstChild( RequestType.GroupDefinition.DST_PATH ).strValue() ),
							gRequest.hasChildren( RequestType.GroupDefinition.DISTINCT )
											&& gRequest.getFirstChild( RequestType.GroupDefinition.DISTINCT ).boolValue()
			) );
		}

		ValueVector resultElements = ValueVector.create();


		if ( groupingList.size() > 0 ) {
			List< List< Boolean > > combinations = getCombinations( groupingList.size() );
			List< MatchExpression > existenceGroups =
							Utils.applyOrFault( () -> combinations.stream()
											.map( c -> {
												try {
													return getMatchExistenceExpression( c, groupingList );
												} catch ( FaultException e ) {
													throw new RuntimeException( e.getMessage() );
												}
											} )
											.collect( Collectors.toList() ) );
			List< List< Value > > concatenationList =
							Utils.applyOrFault( () -> existenceGroups.stream().map( matchExpression -> {
												boolean[] bitMask = matchExpression.applyOn( dataElements );
												List< Value > dataList = IntStream.range( 0, bitMask.length )
																.filter( i -> bitMask[ i ] )
																.mapToObj( dataElements::get )
																.collect( Collectors.toList() );
												try {
													return group( dataList, combinations.get( existenceGroups.indexOf( matchExpression ) ), groupingList, aggregationList );
												} catch ( FaultException e ) {
													throw new RuntimeException( e.getMessage() );
												}
											}
							).collect( Collectors.toList() ) );
			concatenationList.forEach( l -> l.forEach( resultElements::add ) );
		} else {
			List< Value > dataList = dataElements.stream().collect( Collectors.toList() );
			resultElements.add( aggregate( dataList, groupingList, aggregationList ) );
		}
		Value response = Value.create();
		response.children().put( TQueryExpression.ResponseType.RESULT, resultElements );
		return response;
	}

	private static List< Value > group(
					List< Value > dataList,
					List< Boolean > combination,
					List< GroupPair > groupingList,
					List< AggregatePair > aggregationList ) throws FaultException {
		List< Value > returnVector = new LinkedList<>();
		if ( dataList.size() > 0 ) {
			if ( combination.stream().anyMatch( i -> i ) ) {
				List< Path > paths = IntStream.range( 0, combination.size() )
								.filter( combination::get )
								.mapToObj( i -> groupingList.get( i ).srcPath() )
								.collect( Collectors.toList() );
				boolean[] bitMask = getMatchGroupingExpression( paths, dataList.get( 0 ) )
								.applyOn( Utils.listToValueVector( dataList ) );
				List< Value > current = new LinkedList<>();
				List< Value > residuals = new LinkedList<>();
				IntStream.range( 0, bitMask.length ).forEach( i -> {
					if ( bitMask[ i ] ) {
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

	private static MatchExpression getMatchGroupingExpression( List< Path > paths, Value v ) throws FaultException {
		if ( paths.size() < 0 ) {
			throw new FaultException( "getMatchGroupingExpression received a combination of size 0" ); //todo: refine this error into a Fault
		} else {
			MatchExpression currentExpression = new EqualDataExpression( paths.get( 0 ), paths.get( 0 ).apply( v ).get() ); // we can always get here, because we passed the existence tests in the group method
			if ( paths.size() > 1 ) {
				return BinaryExpression.AndExpression(
								currentExpression,
								getMatchGroupingExpression( paths.subList( 1, paths.size() ), v )
				);
			} else {
				return currentExpression;
			}
		}
	}

	private static Value aggregate( List< Value > dataList, List< GroupPair > groupingList, List< AggregatePair > aggregationList ) {
		Value returnValue = Value.create();
		ProjectExpressionChain returnProjectionChain = new ProjectExpressionChain();
		groupingList.forEach( groupElement ->
		{
			try {
				Optional< ValueVector > maybeValueVector = groupElement.srcPath().apply( dataList.get( 0 ) );
				if ( maybeValueVector.isPresent() ) {
					returnProjectionChain.addExpression(
									new ValueToPathProjectExpression(
													groupElement.dstPath(),
													new ConstantValueDefinition( maybeValueVector.get() )
									) );
				}
			} catch ( FaultException e ) {
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
							.filter( e -> ( !aggregationElement.isDistinct() ) || d.isDistinct( e ) )
							.collect( Collectors.toList() );
			ValueVector mergedValueVector = ValueVector.create();
			distinctValueVectors.stream().flatMap( ValueVector::stream ).forEach( mergedValueVector::add );
			try {
				returnProjectionChain.addExpression(
								new ValueToPathProjectExpression(
												aggregationElement.dstPath(),
												new ConstantValueDefinition( mergedValueVector )
								) );
			} catch ( FaultException e ) {
				e.printStackTrace();
			}
		} );
		try {
			returnValue = returnProjectionChain.applyOn( returnValue );
		} catch ( FaultException e ) {
			e.printStackTrace();
		}
		return returnValue;
	}

	// 0 -> emptyList
	// 1 -> [ [ true ], [ false ] ]
	// 2 -> [ [ true, false ], [ true, true ], [ false, true ], [ true, false ]
	// ...
	private static List< List< Boolean > > getCombinations( int size ) {
		if ( size > 1 ) {
			List< List< Boolean > > subCombinations = getCombinations( size - 1 );
			return subCombinations.stream()
							.flatMap( e -> Stream.of(
											Stream.concat( Stream.of( true ), e.stream() ).collect( Collectors.toList() ),
											Stream.concat( Stream.of( false ), e.stream() ).collect( Collectors.toList() )
							) )
							.collect( Collectors.toList() );
		}
		if ( size > 0 ) {
			return List.of( Collections.singletonList( true ), Collections.singletonList( false ) );
		} else {
			return Collections.emptyList();
		}
	}

	private static MatchExpression getMatchExistenceExpression( List< Boolean > combination, List< GroupPair > groupPairList ) throws FaultException {
		if ( combination.size() < 0 || groupPairList.size() < 0 ) {
			throw new FaultException( "getMatchExistenceExpression received a combination of size 0" ); //todo: refine this error into a Fault
		} else {
			MatchExpression currentExpression = new ExistsExpression( groupPairList.get( 0 ).srcPath() );
			currentExpression = combination.get( 0 ) ? currentExpression : new NotExpression( currentExpression );
			if ( combination.size() > 1 ) {
				return BinaryExpression.AndExpression(
								currentExpression,
								getMatchExistenceExpression( combination.subList( 1, combination.size() ), groupPairList.subList( 1, combination.size() ) )
				);
			} else {
				return currentExpression;
			}
		}
	}

	public static class AggregatePair extends GroupPair {

		private final Boolean isDistinct;

		public AggregatePair( Path sourcePath, Path destinationPath, Boolean isDistinct ) {
			super( sourcePath, destinationPath );
			this.isDistinct = isDistinct;
		}

		public Boolean isDistinct() {
			return isDistinct;
		}
	}

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

	public static AggregatePair getAggregatePair( Path sourcePath, Path destinationPath, Boolean isDistinc ) {
		return new AggregatePair( sourcePath, destinationPath, isDistinc );
	}

	private static class DistinctFilter {
		Set< ValueVector > seen;

		DistinctFilter() {
			seen = new HashSet<>();
		}

		public boolean isDistinct( ValueVector element ) {
			if ( seen.stream().anyMatch( v -> Utils.checkVectorEquality( v, element ) ) ) {
				return false;
			} else {
				seen.add( element );
				return true;
			}
		}
	}


}
