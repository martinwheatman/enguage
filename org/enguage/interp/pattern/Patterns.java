package org.enguage.interp.pattern;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Random;

import org.enguage.interp.sign.Sign;
import org.enguage.util.Audit;
import org.enguage.util.Indent;
import org.enguage.util.Strings;
import org.enguage.util.algorithm.Expression;
import org.enguage.util.attr.Attribute;
import org.enguage.util.attr.Attributes;
import org.enguage.vehicle.Language;
import org.enguage.vehicle.Plural;
import org.enguage.vehicle.Utterance;
import org.enguage.vehicle.number.Number;
import org.enguage.vehicle.pronoun.Gendered;
import org.enguage.vehicle.pronoun.Pronoun;
import org.enguage.vehicle.reply.Reply;
import org.enguage.vehicle.where.Where;

public class Patterns extends ArrayList<Pattern> {
	static final         long serialVersionUID = 0;
	static private       Audit           audit = new Audit( "Pattern" );
	
	static private final Locale  locale        = Locale.getDefault();
	static private final String  variable      = "variable";
	static public  final String  quoted        = "quoted";
	static public  final String  list          = "list";
	static public  final String  quotedPrefix  = quoted.toUpperCase( locale ) + "-";
	static public  final String  grouped       = "grouped";
	static public  final String  groupedPrefix = grouped.toUpperCase( locale ) + "-";
	static public  final String  ungrouped     = "ungrouped";
	static public  final String  ungrpedPrefix = ungrouped.toUpperCase( locale ) + "-";
	static public  final String  phrase        = "phrase";
	static public  final String  phrasePrefix  = phrase.toUpperCase( locale ) + "-";
	static public  final String  numeric       = "numeric";
	static public  final String  numericPrefix = numeric.toUpperCase( locale ) + "-";
	static public  final String  expression    = "expression";
	static public  final String  expr          = "expr";
	static public  final String  exprPrefix    = expr.toUpperCase( locale ) + "-";
	static public  final String  plural        = Plural.NAME; // "plural";
	static public  final String  pluralPrefix  = plural.toUpperCase( locale ) + "-";
	static public  final String  sinsign       = "said";
	static public  final String  sinsignPrefix = sinsign.toUpperCase( locale ) + "-";
	static public  final String  external      = "ext";
	static public  final String  externPrefix  = external.toUpperCase( locale ) + "-";
	
	public Patterns() { super(); }
	public Patterns( Strings words ) {
		
		// "if X do Y" -> [ <x prefix=["if"]/>, <y prefix=["do"] postfix="."/> ]
		Pattern t = new Pattern();
		for ( String word : words ) {
			
			if (word.equals( "an" )) word = "a";
			
			if (Strings.isUCwHyphUs( word )) {
				Strings arr = new Strings( word.toLowerCase( locale ), '-' );
				ListIterator<String> wi = arr.listIterator();
				String sw = wi.next();
				if (sw.equals( phrase )) {
					t.phrasedIs();
					if (wi.hasNext()) sw = wi.next();
					else audit.ERROR( "ctor: PHRASE variable, missing name." );
				} else if (sw.equals( plural )) {
					t.pluralIs();
					if (wi.hasNext()) sw = wi.next();
					else audit.ERROR( "ctor: PLURAL variable, missing name." );
				} else if (sw.equals( quoted )) {
					t.quotedIs();
					if (wi.hasNext()) sw = wi.next();
					else audit.ERROR( "ctor: QUOTED variable, missing name." );
				} else if (sw.equals( numeric )) {
					t.numericIs();
					if (wi.hasNext()) sw = wi.next();
					else audit.ERROR( "ctor: NUMERIC variable, missing name." );
				} else if (sw.equals( expr )) {
					t.exprIs();
					if (wi.hasNext()) sw = wi.next();
					else audit.ERROR( "ctor: EXPR variable, missing name." );
				} else if (sw.equals( sinsign )) {
					t.signIs();
					if (wi.hasNext()) sw = wi.next();
					else audit.ERROR( "ctor: SAID, missing name." );
				} else if (sw.equals( Reply.andConjunction() )) {
					//audit.LOG( "found: "+ Reply.andConjunction() );
					if (wi.hasNext()) {
						sw = wi.next();
						if (sw.equals( list )) {
							if (wi.hasNext()) {
								//audit.LOG( "bingo: found isList" );
								sw = wi.next();
								t.listIs();
							} else
								audit.ERROR( "ctor: AND-LIST variable, missing name." );
						} else
							audit.ERROR( "ctor: AND-LIST? 'LIST' missing" );
					} else
						audit.ERROR( "ctor: AND terminates variable" );
				}
				
				while ((sw.equals( phrase  ) ||
				        sw.equals( plural  ) ||
				        sw.equals( quoted  ) ||
				        sw.equals( expr    ) ||
				        sw.equals( sinsign ) ||
				        sw.equals( numeric )   )
					 && wi.hasNext())
				{
					audit.ERROR( "ctor: mutually exclusive modifiers" );
					sw = wi.next();
				}
				
				Strings apostrophes = new Strings( sw, Language.APOSTROPHE.charAt( 0 ));
				if (apostrophes.size() == 2 && apostrophes.get( 1 ).equals( "s" )) {
					sw = apostrophes.get( 0 );
					t.apostrophedIs( apostrophes.get( 1 ));
				}
				
				add( t.name( sw ));
				t = new Pattern();
				
			} else
				t.prefixAppend( word );
		}
		if (!t.isEmpty()) add( t );
	}
	
	// Manual Autopoiesis... needs to deal with:
	// if variable x do phrase variable y => if X do PHRASE-Y
	// i need numeric variable quantity variable units of phrase variable needs.
	// => i need NUMERIC-QUANTITY UNIT of PHRASE-NEEDS
	public Patterns( String str ) { this( toPattern( new Strings( str ))); }
	
	// TODO: not quite right, what about "l'eau" - the water
	// want to move to u.c. but preserve l.c. apostrophe...
	static private String toUpperCase( String word ) {
		// "martin's" => "MARTIN's"
		Strings uppers = new Strings( word, Language.APOSTROPHE.charAt( 0 ));
		uppers.set( 0, uppers.get( 0 ).toUpperCase( locale ));
		return uppers.toString( Strings.CONCAT );
	}
	static public Strings toPattern( Strings in ) {
		// my name is variable name => my name is NAME
		Strings out = new Strings();
		Iterator<String> wi = in.iterator();
		while ( wi.hasNext() ) {
			String word = wi.next();
			
			if (word.equals( "an" )) word = "a"; // English-ism!
			
			if (word.equals( variable ))
				if (wi.hasNext() && null != (word = wi.next()) && !word.equals( variable ))
					out.append( toUpperCase( word ));
				else // variable. OR variable variable
					out.append( variable );
				
			else if (word.equals( numeric ))
				if (wi.hasNext() && null != (word = wi.next()))
					if (word.equals( variable ))
						if (wi.hasNext() && (null != (word = wi.next() )) && !word.equals( variable ))
							out.append( numericPrefix + word.toUpperCase( locale ));
						else // numeric variable. or numeric variable variable
							out.append( numeric ).append( variable );
					else // numeric blah
						out.append( numeric ).append( word );		
				else // numeric.
					out.append( numeric );
				
			else if (word.equals( expression ))
				if (wi.hasNext() && null != (word = wi.next()))
					if (word.equals( variable ))
						if (wi.hasNext() && (null != (word = wi.next() )) && !word.equals( variable ))
							out.append( exprPrefix + word.toUpperCase( locale ));
						else // numeric variable. or numeric variable variable
							out.append( expression ).append( variable );
					else // numeric blah
						out.append( expression ).append( word );		
				else // numeric.
					out.append( expression );
				
			else if (word.equals( phrase ))
				if (wi.hasNext() && null != (word = wi.next()))
					if (word.equals( variable ))
						if (wi.hasNext() && null != (word = wi.next()) && !word.equals( variable ))
							out.append( phrasePrefix + word.toUpperCase( locale ));
						else // phrase variable. OR phrase variable variable
							out.append( phrase ).append( word );
					else // phrase blah
						out.append( phrase ).append( word );
				else // phrase.
					out.append( phrase );
			
			else if (word.equals( "and" ))
				if (wi.hasNext() && null != (word = wi.next()) && word.equals( "list" ))
					if (wi.hasNext() && null != (word = wi.next()) && word.equals( "variable" ))
						if (wi.hasNext() && null != (word = wi.next()) && !word.equals( "variable" ))
							out.append( word.toUpperCase( locale )+"-AND-LIST" );
						else // and list variable variable
							out.append( "and" ).append( "list" ).append( "variable" );
					else // and list blah
						out.append( "and" ).append( "list" ).append( word );						
				else // so we can't have just VARIABLE, ok...
					out.append( "and" ).append( word );						

			// ... "why sentence because reason sentence" ???
// SAID IS NOT YET articulated! TODO:
//			else if (word.equals( "sign" ))
//				if (wi.hasNext() && null != (word = wi.next()) && word.equals( "list" ))
//					if (wi.hasNext() && null != (word = wi.next()) && word.equals( "variable" ))
//						if (wi.hasNext() && null != (word = wi.next()) && !word.equals( "variable" ))
//							out.append( word.toUpperCase( locale )+"-AND-LIST" );
//						else // and list variable variable
//							out.append( "and" ).append( "list" ).append( "variable" );
//					else // and list blah
//						out.append( "and" ).append( "list" ).append( word );						
//				else // so we can't have just VARIABLE, ok...
//					out.append( "and" ).append( word );						

			else // blah
				out.append( word );
		}
		return out;
	}
	
	/*  The complexity of a pattern, used to rank signs in a repertoire.
	 *  "the eagle has landed" comes before "the X has landed", BUT
	 *  "the    X    Y-PHRASE" comes before "the Y-PHRASE" so it is not
	 *  a simple count of tags, phrased hot-spots "hoover-up" tags!
	 *  Phrased hot-spot has a large complexity, and any normal tags
	 *  will bring this complexity down!
	 *  
	 *  Three planes of complexity: bplate hotspots phrased-hotspots
	 *  ==========================
	 *  complexity increases with   1xm bp 1->100.
	 *  complexity increases with 100xn tags 100->10000
	 *  if phrase exists, complexity counts down from 1000000:
	 *  10000 x m bp,   range = 10000 -> 100000
	 *    100 x n tags, range =   100 -> 10000, as before
	 *
	 *  Boilerplate complexity:
	 *  Has been fine tuned to reduce number of clashes when inserting into TreeMap
	 *  Using a random number (less processing?/more random?)  Make it least
	 *  
	 *  Finite:
	 *  |-----------+-----------+------------------|
	 *  | Tags 0-99 |Words 0-99 | Random num 0-99  |
	 *  |-----------+-----------+------------------|
	 *  
	 *  Infinite:
	 *  |----------|   |------------+-----------|------------------+
	 *  | 1000000  | - | Words 0-99 | Tags 0-99 | Random num 0-99  |
	 *  |----------|   |------------+-----------|------------------+
	 *  Range at 1000, random component is 1-100 * 10 - 100 -1000 with 1-99 being
	 *  the count element 
	 */
	private static Random rn = new Random();
	private static final int RANGE = 1000; // means full range will be up to 1 billion
	private static final int INFTY = RANGE*RANGE*RANGE;
	private static final int LARGE = RANGE*RANGE;
	private static final int SMALL = RANGE;
	
	public int cplex() {
		boolean infinite = false;
		int cons = 0,
		    vars = 0,
		    rand = rn.nextInt( RANGE );
		
		for (Pattern t : this) {
			cons += t.nconsts();
			if (t.isPhrased())
				infinite = true;
			else if (!t.name().equals( "" ))
				vars++; // count non-phrase named tags as words
		}
		return rand + (infinite ? INFTY - LARGE*cons - SMALL*vars
				                : LARGE*vars - SMALL*cons);
	}
	
	// initialise with values from Pronoun, provide functions to update from pronoun...
	static private String subjGroup = Pronoun.pronoun(Pronoun.SUBJECTIVE, Pronoun.PLURAL, Gendered.PERSONAL); // i.e. local copy of "they"
	static public  void   subjGroup( String pl ) { subjGroup = pl;}
	static private String objGroup = Pronoun.pronoun(Pronoun.OBJECTIVE, Pronoun.PLURAL, Gendered.PERSONAL); // i.e. local copy of "they"
	static public  void   objGroup( String pl ) { objGroup = pl;}
	static private String possGroup = Pronoun.pronoun(Pronoun.SUBJECTIVE, Pronoun.PLURAL, Gendered.PERSONAL); // i.e. local copy of "they"
	static public  void   possGroup( String pl) { possGroup = pl;}
	static private String subjOther = Pronoun.pronoun(Pronoun.SUBJECTIVE, Pronoun.PLURAL, Gendered.NEUTRAL); // i.e. local copy of "they"
	static public  void   subjOther( String pl ) { subjOther = pl;}
	static private String objOther = Pronoun.pronoun(Pronoun.OBJECTIVE, Pronoun.PLURAL, Gendered.NEUTRAL); // i.e. local copy of "they"
	static public  void   objOther( String pl ) { objOther = pl;}
	static private String possOther = Pronoun.pronoun(Pronoun.SUBJECTIVE, Pronoun.PLURAL, Gendered.NEUTRAL); // i.e. local copy of "they"
	static public  void   possOther( String pl) { possOther = pl;}
	
	static private       boolean debug = false;
	static public        boolean debug() { return debug; }
	static public        void    debug( boolean b ) { debug = b; }
	
	/* *************************************************************************
	 * matchValues() coming soon...
	 */
	private Attributes matched = null; // lazy creation
	private void matched( Attribute a ) {
		if (null == matched) matched = new Attributes();
		matched.add( a ); // remember what it was matched with!
	}
	private void matched( Where w ) {
		if (null != w) {
			matched( new Attribute( Where.LOCTR, w.locatorAsString(  0 )));
			matched( new Attribute( Where.LOCTN, w.locationAsString( 0 )));
	}	}
	
	static private int notMatched = 0;
	static String term = "", word = "";
	static public String notMatched() {
		return  notMatched ==  0 ? "matched" :
				notMatched ==  1 ? "precheck 1" :
				notMatched ==  2 ? "precheck 2" :
				notMatched == 11 ? term +" != "+ word:
				notMatched == 12 ? "... "+term +" != "+ word +" ...":
				notMatched == 13 ? "invalid expr" :
				notMatched == 14 ? "and-list runs into hotspot" :
				notMatched == 15 ? "not numeric" :
				notMatched == 16 ? "invalid flags" :
				notMatched == 17 ? "unterminated and-list" :
				notMatched == 18 ? "... "+term +" != "+ word +"..":
				notMatched == 19 ? "... "+term +" != "+ word +"." :
				notMatched == 20 ? "trailing hotspot value missing" :
				notMatched == 21 ? "more pattern" :
				notMatched == 22 ? "more utterance" :
				notMatched == 23 ? "missing apostrophe" : ("unknown:"+ notMatched);
	}
	
	private String doNumeric( ListIterator<String> ui ) {
		String toString = new Number( ui ).toString();
		return toString.equals( Number.NOT_A_NUMBER ) ? null : toString;
	}
	private String doExpr( ListIterator<String> ui ) {
		Strings rep = Expression.getExpr( ui, new Strings() );
		return rep == null ? "" : rep.toString();

	}
	private String doList( ListIterator<Pattern> patti,
	                       ListIterator<String>      utti  ) 
	{
		String  word = utti.next();
		Strings words = new Strings(),
		        vals  = new Strings();
		if (patti.hasNext()) {
			
			// peek at terminator
			String terminator = patti.next().prefix().get( 0 );
			patti.previous();
			//Audit.log( "Terminator is "+ terminator );
			
			words.add( word );  // add at least one val!
			if (utti.hasNext()) word = utti.next();
			
			while ( !word.equals( terminator )) {
				
				if ( word.equals( "and" )) {
					vals.add( words.toString() );
					words = new Strings();
				} else
					words.add( word );
				
				if (utti.hasNext())
					word = utti.next();
				else
					return null;
			}
			utti.previous(); // replace terminator!
		} else { // read to end {
			words.add( word ); // at least one!
			while (utti.hasNext()) {
				word = utti.next();
				if ( word.equals( "and" )) {
					vals.add( words.toString() );
					words = new Strings();
				} else
					words.add( word );
		}	}
		if (words.size() > 0) vals.add( words.toString());
		int sz = vals.size();
		if (sz == 0) return null;
		// deal with "they", "our", "we" etc as a list
		if (sz == 1) {
			String val = vals.get( 0 );
			if (!val.equals( subjGroup )
				&& !val.equals( objGroup  )
				&& !val.equals( possGroup )
				&& !val.equals( subjOther )
				&& !val.equals( objOther  )
				&& !val.equals( possOther )) return null;
		}
		return vals.toString("", " and ", "");
	}
	
	private boolean matchBoilerplate(
			Strings bp,
			ListIterator<String> said,
			boolean spatial )
	{
		Iterator<String> bpi = bp.iterator();
		while (bpi.hasNext() && said.hasNext()) {
			term = bpi.next();
			if (spatial)
				matched( Where.getWhere( said, term ) );
			
			if (!term.equalsIgnoreCase( word = said.next() )) {
				said.previous();
				notMatched = 11;
				return false; // string mismatch
		}	}
		
		notMatched = 12;
		return !bpi.hasNext();
	}
	private Strings getNextBoilerplate( Pattern t, ListIterator<Pattern> ti ) {
		Strings term = null;
		if (t.postfix().size() != 0)
			term = t.postfix();
		else if (ti.hasNext()) {
			term = ti.next().prefix();
			ti.previous();
		}
		return term;
	}
	private String getVariable(
			Pattern t,
			ListIterator<Pattern> ti,
			ListIterator<String> said,
			boolean spatial )
	{
		String u = "";
		if (said.hasNext()) u = said.next();
		Strings vals = new Strings( u );
		if (t.isPhrased() || t.isSign() || (said.hasNext() && Reply.andConjunction().equals( u ))) {
			Where where = null;
			Strings terms = getNextBoilerplate( t, ti ); // null if this is last tag
			String term = terms==null ? null : terms.get( 0 );
			while (said.hasNext()) {
				// term==null? => read to end
				if (spatial && null != (where = Where.getWhere( said, term ))) {
					matched( where );
					break; // finding a where is the end of a variable...
				} else {
					u = said.next();
					if (term != null && term.equals( u )) {
						said.previous();
						break;
					} else
						vals.add( u );
		}	}	}
		String val = vals.toString();
		// TODO: ...again "l'eau"
		if (t.isApostrophed())
			val = val.endsWith( Language.Apostrophed() ) ? val.substring( 0, val.length()-2 ) : null;
		
		return val;
	}
	private String getValue(
			Pattern t,
			ListIterator<Pattern> patti,
			ListIterator<String> utti,
			boolean spatial)
	{
		String val = null;
		if (t.isNumeric()) {
			
			if (null == (val = doNumeric( utti )))
				notMatched = 15;
			
		} else if (t.isList()) {
			
			if (null == (val = doList( patti, utti )))
				notMatched = 17;
			
		} else if (t.isExpr()) {
			
			if (null == (val = doExpr( utti )))
				notMatched = 13;
			
		} else if (t.invalid( utti )) {
			notMatched = 16;
			
		} else if (null == (val = getVariable( t, patti, utti, spatial ))) {
			notMatched = 23;
		}
		return val;
	}
	
	/* TODO: Proposal: that a singular tag (i.e. non-PHRASE) can match with a known string
	 * i.e. an object id: e.g. theOldMan, thePub, fishAndChips.
	 * Matched first so as to avoid "sergeant at arms" as spatial
	 */
	public  Attributes matchValues( Strings utterance, boolean spatial ) {
		
		notMatched = 0;
		matched = null;
		
		// First, a sanity check
		if (size() == 0) {
			notMatched = 1;
			return null; // manual/vocal Tags creation can produce null Tags objects
                         // see "first reply well fancy that" in Enguage sanity test.
		}
		
		/* We need to be able to extract:
		 * NAME="value"				... <NAME/>
		 * NAME="some value"		... <NAME phrased="phrased"/>
		 * NAME="68"                ... <NAME numeric='numeric'/>
		 * ???NAME="an/array/or/list"	... <NAME array="array"/>
		 * ???NAME="value one/value two/value three" <NAME phrased="phrased" array="array"/>
		 */
		ListIterator<Pattern> patti = listIterator();           // [ 'this    is    a   <test/>' ]
		ListIterator<String>       utti = utterance.listIterator(); // [ "this", "is", "a", "test"   ]
		
		Pattern next = null;
		while (patti.hasNext() && utti.hasNext()) {
			
			Pattern t = (next != null) ? next : patti.next();
			next = null;
			
			if (!matchBoilerplate( t.prefix(), utti, spatial )) { // ...match prefix
				//notMatched set within matchBoilerplate()
				return null;
				
			} else if (!t.named()) { // last tag - no postfix?
				
				if (utti.hasNext()) { // end of array on null (end?) tag...
					
					if (spatial)
						matched( Where.getWhere( utti, null ));
					
				} else { // check 4 trailing where
					if (patti.hasNext()) next = patti.next();
				}
				
			} else if (!utti.hasNext() && t.named()) { // "do i need" == "do i need OBJECT"
				
				notMatched = 20;
				return null;
				
			} else { // do these loaded match?
				
				String val = getValue( t, patti, utti, spatial );
				if (val == null) return null;
				
				matched( t.matchedAttr( val ));
			}
			
			if (!matchBoilerplate( t.postfix(), utti, spatial )) {
				notMatched += 7; // 18 or 19!
				return null;
		}	}
		
		if (patti.hasNext()) {
			notMatched = 21;
			return null;
		}
		if (utti.hasNext()) {
			notMatched = 22;
			return null;
		}
		return null == matched ? new Attributes() : matched;
	}
	// with postfix boilerplate:
	// typically { [ ">>>", "name1" ], [ "/", "name2" ], [ "/", "name3" ], [ "<<<", "" ] }.
	// could be  { [ ">>>", "name1", "" ], [ "/", "name2", "" ], [ "/", "name3", "<<<" ] }.
	public String toXml() { return toXml( new Indent( "   " )); }
	public String toXml( Indent indent ) {
		String oldName = "";
		String str  = "\n"+indent.toString();
		Iterator<Pattern> ti = iterator();
		while (ti.hasNext()) {
			Pattern t = ti.next();
			str += (t.name().equals( oldName ) ? "\n"+indent.toString() : "") + t.toXml( indent );
			oldName = t.name();
		}
		return str;
	}
	public String toString() {
		String tmp, str="";
		Iterator<Pattern> ti = iterator();
		while (ti.hasNext())
			if (!(tmp = ti.next().toString()).equals(""))
				str += tmp +(ti.hasNext() ? " " : "");
		return str;
	}
	public String toFilename() {
		String tmp, str="";
		Iterator<Pattern> ti = iterator();
		while (ti.hasNext())
			if (!(tmp = ti.next().toPattern()).equals(""))
				str += tmp +(ti.hasNext() ? "_" : "");
		return str;
	}
	public String toText() {
		String str="";
		Iterator<Pattern> ti = iterator();
		while (ti.hasNext()) {
			str += ti.next().toText();
			if (ti.hasNext()) str += " ";
		}
		return str;
	}
	public String toLine() {
		String str="";
		Iterator<Pattern> ti = iterator();
		while (ti.hasNext()) {
			Pattern t = ti.next();
			str += ( " "+t.prefix().toString()+" <"+t.name() +" "
			//+ t.attributes().toString()
					+"/> "+t.postfix().toString());
		}
		return str;
	}
	
	// --- test code...
	static public void printTagsAndValues( Patterns interpretant, String phrase, Attributes expected ) {
		audit.in( "printTagsAndValues", "ta="+ interpretant.toString() +", phr="+ phrase +", expected="+ 
				(expected == null ? "":expected.toString()) );
		Attributes values = interpretant.matchValues( new Strings( phrase ), true );
		
		if (values == null)
			Audit.log( "no match" );
		else {
			// de-reference values...
			String vals = values.toString();
			if (null == expected)
				Audit.log( "values => ["+ vals +"]" );
			else if (values != null && values.matches( expected ))
				Audit.log( "PASSED => ["+ vals +"]" );
			else {
				Audit.log( "FAILED: expecting: "+ expected +", got: "+ vals );
				Audit.log( "      :       got: "+ vals );
		}	}
		audit.out();
	}
//	private static void toPatternTest( String utt  ) {
//		toPatternTest( utt, utt ); // check it against itself!
//	}
//	private static void toPatternTest( String utt, String answer ) {
//		String patt = toPattern( utt ).toString();
//		if (answer != null && !answer.equals( patt ))
//			audit.FATAL( "answer '"+ patt +"' doesn't equal expected: '" + answer +"'" );
//		Audit.log( ">"+ utt +"< to pattern is >"+ patt +"<" );
//	}
	static private void matchTest( String pref, String var, String concept, String utterance ) {
		audit.in( "matchTest", utterance );
		Attributes as;
		Utterance u = new Utterance( new Strings( utterance ));
		Audit.log( "Utterance: "+ utterance );
		
		Sign s = new Sign( new Pattern( pref, var ));
		s.concept( concept );
		Audit.log( "     Sign: "+ s.toXml(0, -1) );
		
		if (null != (as = u.match( s )))
			audit.passed( "  matches: "+ as.toString());
		else
			Audit.log( "notMatched ("+ notMatched() +")" );
		audit.out();
	}
	static private void complexityTest( String str ) {
		Patterns patt = new Patterns( toPattern( new Strings( str )));
		Audit.LOG( "pattern: "+ patt );
		//audit.LOG( "    Xml: "+ patt.toXml() );
		Audit.LOG( " cmplxy: "+ patt.cplex() );

	}
	static public void main(String args[]) {
//		Audit.allOn();
//		audit.tracing = true;
//		debug( true );
		
//		Pattern t = new Pattern();
//		t.add( new Patternette( "what is ", "X" ).numericIs() );
//		printTagsAndValues( t, "what is 1 + 2", new Attributes().add( "X", "1 + 2" ));
//
//		printTagsAndValues( new Pattern( "i need phrase variable need" ),
//				"I need coffee", 
//				new Attributes()
//					.add( "need",     "coffee" )
//		);
//		printTagsAndValues( new Pattern( "i need numeric variable quantity variable unit of phrase variable need" ),
//				"I need a cup of coffee", 
//				new Attributes()
//					.add( "quantity", "1" )
//					.add( "unit",     "cup" )
//					.add( "need",     "coffee" )
//		);
		
//		//toPattern() tests...
//		toPatternTest( "the factorial of n" );
//		toPatternTest( "the factorial of n blah" );
//		toPatternTest( "the factorial of variable variable", "the factorial of variable" );
//		toPatternTest( "the factorial of variable n", "the factorial of N" );
//		toPatternTest( "the factorial of variable n blah", "the factorial of N blah" );
//
//		toPatternTest( "the factorial of phrase" );
//		toPatternTest( "the factorial of phrase n" );
//		toPatternTest( "the factorial of phrase variable variable","the factorial of phrase variable" );
//		toPatternTest( "the factorial of phrase variable n", "the factorial of PHRASE-N" );
//		toPatternTest( "the factorial of phrase variable n blah", "the factorial of PHRASE-N blah" );
//
//		toPatternTest( "the factorial of numeric" );
//		toPatternTest( "the factorial of numeric n" );
//		toPatternTest( "the factorial of numeric variable variable", "the factorial of numeric variable" );
//		toPatternTest( "the factorial of numeric variable n", "the factorial of NUMERIC-N" );
//		toPatternTest( "the factorial of numeric variable n blah", "the factorial of NUMERIC-N blah" );
//
//		toPatternTest( "the sum of and list variable params is blah", "the sum of PARAMS-AND-LIST is blah" );

//		Audit.log( "First: martin is alive" );
//		Audit.incr();
//		printTagsAndValues( new Pattern(
//				"first phrase variable x" ),
//				"first variable state exists in variable entity is list", 
//				new Attributes()
//					.add( "x",  "variable state exists in variable entity is list" )
//		);
//		printTagsAndValues( new Pattern(
//				"phrase variable object exists in variable subject variable list list" ),
//				"variable state exists in variable entity is list", 
//				new Attributes()
//					.add( "object",  "variable state" )
//					.add( "subject", "variable entity" )
//					.add( "list",    "is" )
//		);
//		Audit.decr();
		
//		Audit.log( "Second: i am alive" );
//		Audit.incr();
//		printTagsAndValues( new Pattern(
//				"phrase variable object exists in variable subject variable list list" ),
//				"first variable state exists in i am list", 
//				new Attributes()
//				.add( "object",  "variable state" )
//				.add( "subject", "i" )
//				.add( "list",    "am" )
//				);
//		printTagsAndValues( new Pattern(
//				"first phrase variable x" ),
//				"first variable state exists in i am list", 
//				new Attributes()
//					.add( "object",  "variable state" )
//					.add( "subject", "i" )
//					.add( "list",    "am" )
//		);
//		Audit.decr();
		
//		Audit.allOn();
//		audit.tracing = false;
//		debug( debug );
//		Audit.allOff();
//		Audit.runtimeDebug = false;
//		Audit.traceAll( false );		
		
//		Audit.allOn();
//		//Audit.traceAll( true );		
//		
//		p = new Pattern( "help" );
//		Audit.log( "sign is now: "+ p.toXml());
//		p.newTest( "at the pub i am meeting my brother" );
//		
//		p.newTest( "doesnt match at all" );
//
//		// -- expr
//		p = new Pattern( "the FUNCITON of LIST-FNAME is EXPR-VAL" );
//		Audit.log( "sign is: "+ p.toXml());
//		
//		p.newTest( "the sum of x is x plus y" );

		// pronouns test...
//		p = new Pattern( "they are" );
//		Audit.log( "sign is: "+ p.toXml());
//		p.newTest( "they are from sainsburys" );

		complexityTest(	"i am legend" );
		complexityTest(	"variable nm needs numeric variable quantity units of phrase variable object" );
		complexityTest(	"spatially something can be phrase variable locator" );
		complexityTest(	"is variable var not set to phrase variable value" );
		complexityTest(	"i am not variable action phrase variable value" );
		complexityTest(	"i am     variable action phrase variable value" );
		complexityTest(	"set value of variable x to phrase variable y" );
		complexityTest(	"set variable attribute of variable x to phrase variable y" );
		
		Where.doLocators("at/from/in");
		Where.addConcept( "need+needs" );
		Patterns p = new Patterns( "i need PHRASE-OBJECTS" );
		Audit.log( "pattern is: "+ p.toXml());
		
		Audit.allOn();
		matchTest(
				"i need",
				"PHRASE-OBJECTS",
				"need+needs",
				"i need milk" );
		matchTest(
				"i need",
				"PHRASE-OBJECTS",
				"need+needs",
				"i need milk from the dairy aisle" );
		matchTest(
				"i need",
				"PHRASE-OBJECTS",
				"need+needs",
				"i from the dairy aisle need milk" );
		matchTest(
				"i need",
				"PHRASE-OBJECTS",
				"need+needs",
				"from the dairy aisle i need milk" );
		Audit.allOff();
//		newTest( "i need sliced bread from the bakery" );
		
		audit.PASSED();
}	}
