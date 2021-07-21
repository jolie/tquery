import io.reactivex.rxjava3.core.Flowable;

import java.util.LinkedList;
import java.util.List;

public class RecursiveBackpressure {



	private static List< ? > listOfList( Integer max ) {
//		System.out.println( "Current thread: " + Thread.currentThread().getName() );
//		ExecutorService e = Executors.newSingleThreadExecutor();
		List< List< ? > > l = new LinkedList<>();
		Flowable.range( 0, max )
//						.parallel()
						.map( i -> { l.add( i, listOfList( max - 1 ) ); return 0; } )
//						.sequential()
						.blockingSubscribe();
//		e.shutdown();
		return l;
	}


	public static void main( String[] args ) {
		long s = System.currentTimeMillis();
		List< ? > l = listOfList( 10 );
		long e = System.currentTimeMillis();
//		System.out.println( l );
		System.out.println( "done: " + ( e - s ) );
	}

}
