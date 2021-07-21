import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DependableTasks {

	private static List< Task< List< ? > > > _listOfList( Integer max, ExecutorService executorService ) {
		return IntStream.range( 0, max ).mapToObj( i -> {
							List< Task< List < ? > > > tasks = _listOfList( max - 1, executorService );
							Task< List < ? > > task = new Task< List< ? > >( t -> {
								List< List< ? > > list = new LinkedList<>();
								IntStream.range( 0, tasks.size() ).forEach( j ->
												list.add( j, t.get( List.class, "sublist-" + j ) ) );
								return list;
							} );
							IntStream.range( 0, tasks.size() ).forEach( j -> task.addDependency( "sublist-" + j, tasks.get( j ).future() ) );
							task.run( executorService );
							return task;
		} ).collect( Collectors.toList() );
	}

	private static List< ? > listOfList( Integer max, ExecutorService executorService ) {
		return _listOfList( max, executorService ).stream().map( t -> {
			try {
				return t.future().get();
			} catch ( InterruptedException | ExecutionException e ) {
				e.printStackTrace();
				return Collections.emptyList();
			}
		} ).collect( Collectors.toList() );
	}

	public static void simpleExample() throws ExecutionException, InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool( 5 );

		Task< Integer > ta = new Task< Integer >( t -> {
			try {
				Thread.sleep( 1000 );
			} catch ( InterruptedException e ) {
				e.printStackTrace();
			}
			System.out.println( "A executed" );
			return 5;
		} ).run( executorService );

		Task< Integer > tb = new Task< Integer >( t -> 5 + t.get( Integer.class, "A" ) )
						.addDependency( "A", ta.future() )
						.run( executorService );

		System.out.println( tb.future().get() );
		executorService.shutdown();
	}


	public static void main( String[] args ) {
		ExecutorService executorService = Executors.newWorkStealingPool( 4 );
		long s = System.currentTimeMillis();
		List< ? > l = listOfList( 10, executorService );
		long e = System.currentTimeMillis();
		System.out.println( l );
		System.out.println( "done: " + ( e - s ) );
		executorService.shutdown();
	}

}

class Task< T > {

	private final Map< String, Object > data;
	private final Map< String, CompletableFuture< Object > > dependencies;
	private final Runnable r;
	private final CompletableFuture< T > f;
	private final String name;

	public Task( Function< Task< T >, T > c ) {
		this.data = new HashMap<>();
		this.dependencies = new HashMap<>();
		this.f = new CompletableFuture<>();
		this.r = () -> f.complete( c.apply( this ) );
		this.name = UUID.randomUUID().toString();
	}

	public Task( Function< Task< T >, T > c, String name ) {
		this.data = new HashMap<>();
		this.dependencies = new HashMap<>();
		this.f = new CompletableFuture<>();
		this.r = () -> f.complete( c.apply( this ) );
		this.name = name;
	}

	public Task< T > addDependency( String name, CompletableFuture< ? > f ) {
		this.dependencies.put( name, ( CompletableFuture< Object > ) f );
		return this;
	}

	public Task< T > run( ExecutorService e ) {
		e.submit( new Runnable() {
			@Override
			public void run() {
				if ( dependencies.keySet().size() > 0 ) {
					List< String > keys = new ArrayList<>( dependencies.keySet() );
					keys.forEach( key -> {
						if ( dependencies.get( key ).isDone() ) {
							try {
								data.put( key, dependencies.get( key ).get() );
								dependencies.remove( key );
							} catch ( Exception ex ) {
								ex.printStackTrace();
								e.shutdown();
							}
						}
					} );
					e.submit( this );
				} else {
					e.submit( r );
				}
			}
		} );
		return this;
	}

	public < R > R get( Class< R > c, String name ) {
		return ( R ) data.get( name );
	}

	public CompletableFuture< T > future() {
		return f;
	}
}