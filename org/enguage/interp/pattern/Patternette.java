package org.enguage.interp.pattern;

import java.util.ListIterator;
import java.util.Locale;

import org.enguage.util.Audit;
import org.enguage.util.Indent;
import org.enguage.util.Strings;
import org.enguage.util.attr.Attribute;
import org.enguage.vehicle.Language;
import org.enguage.vehicle.Plural;

public class Patternette {
	private static Audit audit = new Audit( "Patternette" );
	
	
	// -- constructors...
	public Patternette() {}
	public Patternette( Strings pre, String nm ) {
		this();
		prefix( pre ).name( nm );
	}
	public Patternette( Strings pre, String nm, Strings post ) {
		this( pre, nm );
		postfix( new Strings( post ));
	}
	//just a helper ctor for hardcoded Patternettes
	public Patternette( String pre ) { this( new Strings( pre ), "" ); }
	public Patternette( String pre, String nm ) { this( new Strings( pre ), nm );}
	public Patternette( String pre, String nm, String pst ) { this( new Strings( pre ), nm, new Strings( pst ) );}

	
	private Strings     prefix = new Strings();
	public  Strings     prefix() { return prefix; }
	public  Patternette prefix( Strings s ) { prefix = s; return this; }
	public  Patternette prefix( String str ) {
		for (String s : new Strings( str ))
			prefix.append( s );
		return this;
	}

	private Strings     postfix = new Strings();
	public  Strings     postfix() { return postfix; }
	public  Patternette postfix( Strings ss ) { postfix = ss; return this; }
	public  Patternette postfix( String str ) {
		for (String s : new Strings( str ))
			postfix.append( s );
		return this;
	}

	private boolean     named = false;
	private String      name = "";
	public  String      name() { return name; }
	public  Patternette name( String nm ) { if (null != nm) {name = nm; named = !nm.equals("");} return this; }
	public  boolean     named() { return named;}

	// -- mutually exclusive attributes --:
	private boolean     isNumeric = false;
	public  boolean     isNumeric() { return isNumeric; }
	public  Patternette numericIs( boolean nm ) { isNumeric = nm; return this; }
	public  Patternette numericIs() { isNumeric = true; return this; }

	private boolean     isExpr = false;
	public  boolean     isExpr() { return isExpr; }
	public  Patternette exprIs( boolean ex ) { isExpr = ex; return this; }
	public  Patternette exprIs() { isExpr = true; return this; }

	private boolean     isQuoted = false;
	public  boolean     quoted() { return isQuoted;}
	public  Patternette quotedIs( boolean b ) { isQuoted = b; return this; }
	public  Patternette quotedIs() { isQuoted = true; return this; }
	
	private boolean     isPlural = false;
	public  boolean     isPlural() { return isPlural; }
	public  Patternette pluralIs( boolean b ) { isPlural = b; return this; }
	public  Patternette pluralIs() { isPlural = true; return this; }
	
	private boolean     isPhrased = false;
	public  boolean     isPhrased() { return isPhrased; }
	public  Patternette phrasedIs() { isPhrased = true; return this; }
	
	private boolean     isSign = false;
	public  boolean     isSign() { return isSign; }
	public  Patternette signIs() { isSign = true; return this; }
	
   // TODO: Apretrophed
	private String      isApostrophed = null;
	public  boolean     isApostrophed() { return isApostrophed != null; }
	public  Patternette apostrophedIs( String s ) { isApostrophed = s; return this; }
	
	private boolean     isList = false;
	public  boolean     isList() { return isList; }
	public  Patternette listIs() { isList = true; return this; }
	// --
	
	private String      conjunction = "";
	public  String      conjunction() { return conjunction; }
	public  Patternette conjunction( String c ) { conjunction = c; return this; }
	
	public Attribute matchedAttr( String val ) {
		return new Attribute(
				name,
				Attribute.expandValues( // prevents X="x='val'"
					name.equals("unit") ? Plural.singular( val ) : val
				).toString());
	}
	
	public boolean isEmpty() { return name.equals("") && prefix().size() == 0; }

	public boolean invalid( ListIterator<String> ui  ) {
		boolean rc = false;
		if (ui.hasNext()) {
			String candidate = ui.next();
			rc = (  quoted() && !Language.isQuoted( candidate ))
			  || (isPlural() && !Plural.isPlural(   candidate ));
			if (ui.hasPrevious()) ui.previous();
		}
		return rc;
	}
	
	public String toXml( Indent indent ) {
		indent.incr();
		String s = prefix().toString( Strings.OUTERSP )
				+ (name.equals( "" ) ? "" :
					("<"+ name
							+ (isPhrased() ? " phrased='true'":"")
							+ (isNumeric() ? " numeric='true'":"")
							+ "/>"
				  ) )
				+ postfix().toString();
		indent.decr();
		return s;
	}
	public String toString() {
		return prefix().toString() 
				+" "+ 
				(name.equals( "" ) ? "" :
					(isNumeric()? Pattern.numericPrefix : "")
					+ (isPhrased()? Pattern.phrasePrefix : "")
					+ name.toUpperCase( Locale.getDefault()) +" ")
				+ postfix().toString();
	}
	public String toText() {
		return prefix().toString()
			+ (prefix().toString()==null||prefix().toString().equals("") ? "":" ")
			+ (name.equals( "" ) ? "" :
				( name.toUpperCase( Locale.getDefault() ) +" "))
			+ postfix().toString();
	}
	public String toLine() { return prefix().toString() +" "+ name +" "+ postfix().toString(); }
	
	// -- test code
	public static void main( String argv[]) {
		Audit.allOn();
		audit.tracing = true;
}	}