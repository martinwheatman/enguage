package com.yagadi.enguage.obj.tier2;

import java.util.ListIterator;

import com.yagadi.enguage.intp.Tag;
import com.yagadi.enguage.intp.Tags;
import com.yagadi.enguage.obj.Attribute;
import com.yagadi.enguage.obj.Attributes;
import com.yagadi.enguage.util.Audit;
import com.yagadi.enguage.util.Number;
import com.yagadi.enguage.util.Shell;
import com.yagadi.enguage.util.Strings;
import com.yagadi.enguage.veh.Plural;
import com.yagadi.enguage.veh.when.Moment;
import com.yagadi.enguage.veh.when.When;

public class Item {
	/* 
	 * Item.class replaces Tuple.class
	 */
	static Audit audit = new Audit("Item", true);
	static public final String NAME = "item";
	//static private boolean debug = true;
	
	static private Strings format = new Strings(); // e.g. "cake slice", "2 cake slices" or "2 slices of cake"
	static public  void    format( String csv ) { format = new Strings( csv, ',' ); }
	static public  Strings format() { return format; }

	public Item() { tag.name( "item" ); }
	public Item( Tag t ) { this(); tag( t ); }
	public Item( Strings ss ) { // [ "black", "coffee", "quantity='1'", "unit='cup'" ]
		this();
		Attributes a = new Attributes();
		Strings content = new Strings();
		
		for (String s : ss)
			if (Attribute.isAttribute( s ))
				a.add( new Attribute( s ));
			else if (!s.equals("-"))
				content.add( s );

		tag.content( new Tag().prefix( content.toString( Strings.SPACED )))
		   .attributes( a );
	}
	public Item( Strings ss, Attributes as ) { // [ "black", "coffee", "quantity='1'"], [unit='cup']
		this( ss );
		tag.attributes().addAll( as );
	}
	public Item( String s ) { this( new Strings( s ).contract( "=" )); } // "black coffee quantity='1' unit='cup'
	public Item( Item item ) { // copy c'tor
		this( item.content().size()>0 ? item.content().get( 0 ).prefix() : "" );
		tag.attributes( new Attributes( item.attributes()) );
	}
	
	// contains one tag -- can't extend as it is recursively defined.
	private Tag        tag = new Tag();
	public  void       tag( Tag t ) { tag = t; }
	public  Tag        tag() {        return tag; }
	public  Tags       content()    { return tag.content(); }
	public  Attributes attributes() { return tag.attributes(); }
	
	public String counted( Float num, String val ) {
		// N.B. val may be "wrong", e.g. num=1 and val="coffees"
		if (val.equals("1")) return "a";
		return Plural.ise( num, val );
	}
	public String toXml() { return tag.toString(); }
	public String toString() {
		Strings rc = new Strings();
		Strings formatting = format();
		if (formatting == null || formatting.size() == 0) {
			if (tag.content().size()>0) rc.add( tag.content().get(0).prefix());
		} else {
			Float prevNum = Float.NaN;    // pluralise to the last number... NaN means no number found yet
			/* Read through the format string:
			 *    add attributes (uppercase loaded),
			 * OR plain text (if lower case),
			 * OR the content if blank string.
			 */
			for (String format : formatting)
				if (format.equals("")) { // main item: "black coffee"
					if (tag.content().size()>0)
						rc.add( Plural.ise( prevNum, tag.content().get(0).prefix() ));
				} else { // formatted attributes: "UNIT of" + unit='cup' => "cups of"
					Strings subrc = new Strings();
					boolean found = true;
					for (String component : new Strings( format ))
						if ( Strings.isUpperCase( component )) { // UNIT
							if (tag.attributes().hasIgnoreCase( component )) {
								String val = tag.attributes().getIgnoreCase( component );
								if (component.equals("WHEN"))
									subrc.add( new When( new Moment( Long.valueOf( val ))).toString() );
								else {
									subrc.add( counted( prevNum, val ) );  // UNIT='cup(S)'
									ListIterator<String> si = new Strings( val ).listIterator();
									prevNum = Number.getNumber( si ).magnitude(); //Integer.valueOf( val );
									if (prevNum.isNaN()) prevNum = 1.0f; 
								}
							} else
								found = false;
						} else
							subrc.add( component ); // ...of...
					
					if (found) rc.addAll( subrc );
				}
		}
		return rc.toString( Strings.SPACED );
	}
	static public String interpret( Strings cmd ) {
		String rc = Shell.FAIL;
		if (cmd.size() == 3
				&& cmd.get( 0 ).equals( "set" )
				&& cmd.get( 1 ).equals( "format" ))
		{
			Item.format( Strings.stripQuotes( cmd.get( 2 )));
			rc = Shell.SUCCESS;
		}
		return rc;
	}
	private static void test( String s ) {
		audit.debug( ">>>>>>>"+ s +"<<<<" );
		Item t1 = new Item( new Strings( s ).contract( "=" ));
		//audit.debug( ">>t1 is "+ t1.toXml() );
		audit.debug( "is ===> "+ t1.toString() );
	}
	public static void main( String args[] ) {
		Audit.allOn();
		Audit.traceAll( true );
		Item.format( "QUANTITY,UNIT of,,from FROM,WHEN,LOCATOR,LOCATION" );
		audit.debug("Item.toString(): using format:"+ format() );
		test( "black coffees quantity=1 unit='cup' from='Tesco' locator='in' location='London'" );
		test( "black coffees quantity='2' unit='cup'" );
		test( "black coffees quantity='1'" );
		test( "black coffee quantity='2'" );
		test( "black coffees quantity='5 more' when='20151125074225'" );
}	}