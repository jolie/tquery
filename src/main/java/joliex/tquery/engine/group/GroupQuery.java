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
		}
	}

	public static Value group( Value groupRequest ) throws FaultException {
		ValueVector dataElements = groupRequest.getChildren( RequestType.DATA );
		Value query = groupRequest.getFirstChild( RequestType.QUERY );
		ValueVector group = query.getChildren( RequestType.GROUPBY );
		ValueVector aggregate = query.getChildren( RequestType.AGGREGATE );

		// grouping request parsing, i.e., s_1 > r_1, ..., s_n > r_n
		List< GroupPair > groupingList = new ArrayList<>();
		group.forEach( gRequest -> groupingList.add( getGroupPair(
				Path.parsePath( gRequest.getFirstChild( RequestType.GroupDefinition.SRC_PATH ).strValue() ),
				Path.parsePath( gRequest.getFirstChild( RequestType.GroupDefinition.DST_PATH ).strValue() )
				) ) );

		// aggregation request parsing, i.e., q_1 > p_1, ..., q_n > p_n
		List< GroupPair > aggregationList = new ArrayList<>();
		aggregate.forEach( gRequest -> aggregationList.add( getGroupPair(
				Path.parsePath( gRequest.getFirstChild( RequestType.GroupDefinition.SRC_PATH ).strValue() ),
				Path.parsePath( gRequest.getFirstChild( RequestType.GroupDefinition.DST_PATH ).strValue() )
		) ) );

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
				} ).collect( Collectors.toList() );
		concatenationList.forEach( l -> l.forEach( resultElements::add ) );
		Value response = Value.create();
		response.children().put( TQueryExpression.ResponseType.RESULT, resultElements );
		return response;
	}

	private static List< Value > group( List< Value > dataList, List< Boolean > combination, List< GroupPair > groupingList, List< GroupPair > aggregationList ) {
		List< Value > returnVector = new ArrayList<>();
		if( dataList.size() > 0 ){
			if( combination.stream().anyMatch( i -> i ) ){
				List< Path > paths = IntStream.range( 0, combination.size() ).filter( combination::get ).mapToObj( i -> groupingList.get( i ).srcPath() ).collect( Collectors.toList() );
				boolean[] bitMask = getMatchGroupingExpression( paths, dataList.get( 0 ) )
						.applyOn( Utils.listToValueVector( dataList ) );
				List< Value > current = new ArrayList<>();
				List< Value > residuals = new ArrayList<>();
				IntStream.range( 0, bitMask.length ).forEach( i -> {
					if( bitMask[ i ] ){
						current.add( dataList.get( i ) );
					} else{
						residuals.add( dataList.get( i ) );
					}
				} );
				returnVector.add( aggregate( current, groupingList, aggregationList ) );
				returnVector.addAll( group( residuals, combination, groupingList, aggregationList ) );
			} else{
				returnVector.add( aggregate( dataList, groupingList, aggregationList ) );
			}
		}
		return returnVector;
	}

	private static MatchExpression getMatchGroupingExpression( List< Path > paths, Value v ) {
		if( paths.size() < 0 ){
			throw new RuntimeException( "getMatchGroupingExpression received a combination of size 0" ); //todo: refine this error into a Fault
		} else{
			MatchExpression currentExpression = new EqualExpression( paths.get( 0 ), paths.get( 0 ).apply( v ).get() ); // we can always get here, because we passed the existence tests in the group method
			if( paths.size() > 1 ){
				return BinaryExpression.AndExpression(
						currentExpression,
						getMatchGroupingExpression( paths.subList( 1, paths.size() ), v )
				);
			} else{
				return currentExpression;
			}
		}
	}

	private static Value aggregate( List< Value > dataList, List< GroupPair > groupingList, List< GroupPair > aggregationList ) {
		Value returnValue = Value.create();
		ProjectExpressionChain returnProjectionChain = new ProjectExpressionChain();
		groupingList.forEach( groupElement ->
		{
			try{
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
			try{
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
		} else{
			return Collections.emptyList();
		}
	}

	private static MatchExpression getMatchExistenceExpression( List< Boolean > combination, List< GroupPair > groupPairList ) {
		if( combination.size() < 0 || groupPairList.size() < 0 ){
			throw new RuntimeException( "getMatchExistenceExpression received a combination of size 0" ); //todo: refine this error into a Fault
		} else{
			MatchExpression currentExpression = new ExistsExpression( groupPairList.get( 0 ).srcPath() );
			currentExpression = combination.get( 0 ) ? currentExpression : new NotExpression( currentExpression );
			if( combination.size() > 1 ){
				return BinaryExpression.AndExpression(
						currentExpression,
						getMatchExistenceExpression( combination.subList( 1, combination.size() ), groupPairList.subList( 1, combination.size() ) )
				);
			} else{
				return currentExpression;
			}
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

	private static class DistinctFilter {
		Set< ValueVector > seen;

		DistinctFilter() {
			seen = new HashSet<>();
		}

		public boolean isDistinct( ValueVector element ) {
			if( seen.stream().anyMatch( v -> Utils.checkVectorEquality( v, element ) ) ){
				return false;
			} else{
				seen.add( element );
				return true;
			}
		}
	}


}
