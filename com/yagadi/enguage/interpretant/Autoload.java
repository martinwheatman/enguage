package com.yagadi.enguage.interpretant;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.yagadi.enguage.object.Overlay;
import com.yagadi.enguage.util.Audit;
import com.yagadi.enguage.util.Fs;
import com.yagadi.enguage.util.Strings;
import com.yagadi.enguage.vehicle.Language;

public class Autoload {
	/* Implements Dynamic Repertoires:
	 * attempts to load all words in an utterance, singularly, as a repertoire.
	 */
	private static Audit audit = new Audit( "Autoload" );
	
	private static int ttl = 5;
	public  static void ttl( String age ) { try { ttl = Integer.valueOf( age ); } catch( Exception e ) {}}
	
	public static int autoloading = 0;
	public static void    ing( boolean al ) { if (al || autoloading>0) autoloading += al ? 1 : -1; }
	public static boolean ing() { return autoloading>0; }

	static private TreeMap<String,Integer> autoloaded = new TreeMap<String,Integer>();

	private static Strings matches( Strings utterance, TreeSet<String> candidates ) {
		audit.in( "matches", utterance.toString() );
		// matches: utt=[martin is a wally], candiates=[ "is_a+has_a" ] => add( is_a+has_a )
		Strings matches = new Strings();
		for (String candidate : candidates ) { // e.g. "is_a+has_a" OR need
			Strings cand = new Strings( candidate, '+' );
			// matching: "martin is a wally" + is_a
			for (String c : cand) { // e.g. "is_a"
				Strings d = new Strings( c, '_' );
				if (utterance.contains( d ))
					matches.add( candidate );
		}	}
		return audit.out( matches );
	}
	static public void load( Strings utterance ) {
		audit.in( "load", utterance.toString());
		// should not be called if initialising or if autoloading
		if (ing()) {
			audit.ERROR("Autoload.load() called while already autoloading" );
		} else {
			Autoload.ing( true );
			Allopoiesis.undoEnabledIs( false ); // disable undo while loading repertoires
			
			Strings tmp = new Strings();
			for (String candidate : matches( new Strings( utterance ), Concepts.names() )) {
				if (!Language.isQuoted( candidate )// don't try to load: anything that is quoted, ...
					&&	!candidate.equals(",")             // ...punctuation, ...
					&&	!Strings.isUpperCase( candidate )) // ...hotspots, ...
				{
					audit.debug( "candidate is "+ candidate );
					// let's just singularise it: needs -> need
					//if (Plural.isPlural( candidate )) candidate = Plural.singular( candidate );
					
					if (Concepts.loaded().contains( candidate )) {// don't load...
						audit.debug( "already loaded on init: "+ candidate );
					} else if (null==autoloaded.get( candidate )) { //...stuff already loaded.
						if (Concept.load( candidate )) {
							audit.debug( "autoloaded: "+ candidate );
							autoloaded.put( candidate, 0 ); // just loaded so set new entry to age=0
							tmp.add( candidate );
						} else // ignore, if no repertoire!
							audit.ERROR( "not loaded" );
					} else { // already exists, so reset age to 0
						audit.debug("resetting age: " + candidate);
						autoloaded.put(candidate, 0);
			}	}	}
			
			audit.debug( "Autoload.load(): "+ utterance +" => ["+ tmp.toString( Strings.CSV ) +"]");
			Allopoiesis.undoEnabledIs( true );
			Autoload.ing( false );
			audit.out();
	}	}
	static public void unload() {
		if (!ing()) {
			Strings repsToRemove = new Strings();
			Set<Map.Entry<String,Integer>> set = autoloaded.entrySet();
			Iterator<Map.Entry<String,Integer>> i = set.iterator();
			while(i.hasNext()) {
				Map.Entry<String,Integer> me = (Map.Entry<String,Integer>)i.next();
				String repertoire = me.getKey();
				Integer nextVal = me.getValue() + 1;
				if (nextVal > ttl) {
					repsToRemove.add( repertoire );
				} else {
					audit.debug( "ageing "+ repertoire +" (now="+ nextVal +"): " );
					autoloaded.put( repertoire, nextVal );
			}	}
			
			// now do the removals...
			Iterator<String> ri = repsToRemove.iterator();
			while (ri.hasNext()) {
				String repertoire = ri.next();
				if (Audit.detailedDebug) audit.debug( "unloaded: "+ repertoire );
				Repertoire.signs.remove( repertoire );
				autoloaded.remove( repertoire );
			}
			audit.debug( "unloanding => ["+ repsToRemove.toString( Strings.CSV ) +"]" );
	}	}
	public static void main( String args[] ) {
		Audit.allOn();
		Audit.allTracing = true;
		if (!Fs.location( "./src/assets" ))
			audit.FATAL( "./src/assets: not found" );
		else if (!Overlay.autoAttach()) {
			audit.ERROR( " can't auto attach" );
		} else {
			Concepts.names( "./src/assets" );
			load( new Strings( "i need a coffee" ));
			load( new Strings( "martin needs a coffee" ));
}	}	}