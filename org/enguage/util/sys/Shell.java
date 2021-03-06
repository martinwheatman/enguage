package org.enguage.util.sys;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import org.enguage.interp.repertoire.Repertoire;
import org.enguage.util.Audit;
import org.enguage.util.Strings;
import org.enguage.vehicle.Utterance;
import org.enguage.vehicle.reply.Reply;

public class Shell {

	static  Audit audit = new Audit( "Shell" );
	
	public static final String SUCCESS = "TRUE";	
	public static final String FAIL    = "FALSE";
	public static final String IGNORE  = "";
		
	public static final Strings Success = new Strings( SUCCESS );	
	public static final Strings Fail    = new Strings( FAIL );
	public static final Strings Ignore  = new Strings( IGNORE );
		
	//public Strings interpret( Strings argv ) ;
	
	static public Strings terminators = new Strings( ". ? !" );
	static public void    terminators( Strings a ){ terminators = a; }
	static public Strings terminators() { return terminators; }
	
	static public boolean  isTerminator( String s ) { return terminators().contains( s ); }
	static public String   terminatorIs( Strings a ){ return (null != a) && a.size()>0 ? a.get( a.size() -1) : ""; }
	static private boolean isTerminated( Strings a ) {
		boolean rc = false;
		if (null != a) {
			int last = a.size() - 1;
			if (last > -1) rc = isTerminator( a.get( last ));
		}
		return rc; 
	}
	static public Strings stripTerminator( Strings a ) {
		if (isTerminated( a ))
			a.remove( a.size() - 1 );
		return a;
	}
	static public String stripTerminator( String s ) {
		Strings a = new Strings( s );
		if (isTerminated( a ))
			a.remove( a.size() - 1 );
		return a.toString();
	}
	static public Strings addTerminator( Strings a, String terminator ) {
		a.add( terminators.contains( terminator ) ? terminator : terminators.get( 0 ));
		return a;
	}
	static public Strings addTerminator( Strings a ) { return addTerminator( a, terminators.get( 0 ));}
	
	private String  prompt;
	public  String  prompt() { return prompt; }
	public  Shell   prompt( String p ) { prompt = p; return this; }
	
	private boolean aloud = true;
	public  boolean isAloud() { return aloud; }
	public  Shell   aloudIs( boolean is ) { aloud = is; return this; }
	
	private String prog;
	public  String name() { return prog; }
	public  Shell  name( String nm ) { prog = nm; return this; }
	
	private String who, dates, copyright;
	public  String copyright() { return prog +" (c) "+ (copyright!=null? copyright : who +", "+ dates); }
	public  Shell  copyright( String wh, String dts ) { who = wh; dates = dts; return this; }
	public  Shell  copyright( String statement ) { copyright = statement; return this; }

	static private long then = new GregorianCalendar().getTimeInMillis();
	static public  long interval() {
		long now = new GregorianCalendar().getTimeInMillis();
		long rc = now - then;
		then = now;
		return rc;
	}
	
	public Shell( String name, String copyright ) {
		name( name ).prompt( "> " ).copyright( copyright );
	}
	public Shell( String name ) {
		name( name ).prompt( "> " ).copyright( "Martin Wheatman", "2001-4, 2011-20" );
	}
	public Shell( String name, Strings args ) { this( name ); }
	public void interpret( InputStream fp, String from, String to ) { // reads file stream and "interpret()"s it
		if (fp==System.in) System.err.print( prompt() );
		BufferedReader br = null;
		try {
			String line;
			Strings stream = new Strings();
			br = new BufferedReader( new InputStreamReader( fp ));
			boolean was = aloud; // so we can reset volume between utterances.
			while ((line = br.readLine()) != null) {

				//remove Byte order mark...
				if (line.startsWith("\uFEFF")) { line = line.substring(1); }

				if (!line.equals("\n")) {
					// truncate comment -- only in real files
					int i = line.indexOf( '#' );
					if (-1 != i) line = line.substring( 0, i );
					
					// if we're converting on the fly, e.g. want -> need
					if (from != null) line = line.replace( from, to );
					
					// will return "cd .." as ["cd", ".", "."], not ["cd" ".."] -- "cd.." is meaningless!
					// need new stage of non-sentence sign processing
					stream.addAll( new Strings( line ));
					ArrayList<Strings> sentences = stream.divide( terminators );
					if ( sentences.size() > 1 ) {
						Strings sentence = sentences.remove( 0 );
						stream = Strings.combine( sentences );
						if (sentence.size() > 0) {
							// strip sentence of its terminator, if "."
							if (sentence.get( sentence.size()-1 ).equals("."))
								sentence.remove( sentence.size()-1 );
							// Expand sentence here...
							for (Strings s : expandSemicolonList( sentence )) {
								//Audit.LOG( "one" );
								Reply r = Repertoire.mediate( new Utterance( new Strings( s )));
								if (aloud) System.out.println( r.toString());
								//Audit.LOG( "two" );
							}
							// ...expand sentence here.
					}	}
					if (fp==System.in) System.err.print( prompt() );
				}
				aloud = was;	
			}
		} catch (java.io.IOException e ) {
			audit.ERROR( "IO error in Shell::interpret(stdin);" );
		} finally {
			try {
				br.close();
			} catch (java.io.IOException e ) { //ignore?
	}	}	}
	public void run() { interpret( System.in, null, null ); } // we're not converting on-the-fly!
	
	public static ArrayList<Strings> expandSemicolonList( Strings sentence ) {
		/*  "on one: do two; do three; and, do four." =>
		 *  [ "on one, do two.", "and on one, do three.", "and on one, do four." ]
		 *  "and" many be replace by, for example "or", "then", "also"
		 */
		ArrayList<Strings> rc = new ArrayList<Strings>();
		
		// create a primary and a list of secondaries
		boolean isPrimary = true;
		Strings primary = new Strings(),
				secondary = new Strings ();
		for (String s : sentence) {
			if (s.equals(":")) {
				isPrimary = false;
			} else if (s.equals(";")) {
				rc.add( secondary );
				secondary = new Strings();
			} else if (isPrimary) {
				primary.add( s );
			} else {
				secondary.add( s );
		}	}
		if (secondary.size() > 0) rc.add( secondary );
		
		// if we've found no semi-colon separated list...
		if (rc.size()==0) {
			// ...just pass back the original list
			rc.add( sentence );
		} else {
			// remove connector from last in list
			int rcSz = rc.size();
			String connector = "then"; // default: this is the only one used!
			Strings lastList = rc.get( rcSz-1 );
			if ( lastList.size() > 2 && lastList.get( 1 ).equals( ",") ) {
				connector=lastList.remove( 0 ); // remove connector
				lastList.remove( 0 );           // remove ","
				rc.set( rcSz-1, lastList );     // replace this last item
			}
			// re-jig list, adding-in primary and secondary with connector on subsequent lists
			for (int i=0; i<rcSz; i++) {
				Strings tmp = new Strings();
				if (i>0 && !connector.equals(""))
					tmp.add( 0, connector );
				tmp.addAll( primary );
				tmp.add( "," );
				tmp.addAll( rc.get( i ));
				rc.set( i, tmp );
		}	}
		return rc;
	}
	private static void test( String string ) {
		Strings list = new Strings( string );
		ArrayList<Strings> listOfLists = expandSemicolonList( list );
		Audit.log("expanded list is:");
		for (Strings s : listOfLists ) {
			Audit.log( ">>>"+ s.toString( Strings.SPACED ) );
	}	}
	public static void main( String args[]) {
		test( "I need coffee" );
		test( "on this: do here; there; and, everywhere" );
		test( "On \"X needs PHRASE-Y\":"
				+"	if Y exists in X needs list, reply \"I know\";"
				+"	if not, add Y to X needs list;"
				+"	then, reply \"ok, X needs Y\"." );
		test( "On \"X needs PHRASE-Y\":"
				+"	if Y exists in X needs list, reply \"I know\";"
				+"	if not, add Y to X needs list;"
				+"	reply \"ok, X needs Y\"." );
}	}
