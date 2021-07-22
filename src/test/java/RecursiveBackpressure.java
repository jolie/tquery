import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class RecursiveBackpressure {


	private static List< ? > listOfList( Integer max ) {
		List< ? >[] l = new List<?>[ max ];
		IntStream.range( 0, max )
//						.parallel()
						.forEach( i -> l[ i ] = listOfList( max - 1 ) );
		return Arrays.asList( l );
	}


	public static void main( String[] args ) {
		long s = System.currentTimeMillis();
		List< ? > l = listOfList( 11 );
		long e = System.currentTimeMillis();
//		System.out.println( l );
		System.out.println( "done: " + ( e - s ) );
	}

}
