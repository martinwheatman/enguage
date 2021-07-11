package org.enguage;

import java.io.File;

import org.enguage.interp.intention.Redo;
import org.enguage.interp.repertoire.Autoload;
import org.enguage.interp.repertoire.Concepts;
import org.enguage.interp.repertoire.Repertoire;
import org.enguage.objects.space.Overlay;
import org.enguage.util.Audit;
import org.enguage.util.Strings;
import org.enguage.util.sys.Fs;
import org.enguage.util.sys.Server;
import org.enguage.util.sys.Shell;
import org.enguage.vehicle.Utterance;
import org.enguage.vehicle.pronoun.Pronoun;
import org.enguage.vehicle.reply.Reply;
import org.enguage.vehicle.where.Where;

import com.yagadi.Assets;

public class Enguage {
	
	private static String copyright = "Martin Wheatman, 2001-4, 2011-21";

	public  static final String      RO_SPACE = Assets.LOCATION;
	public  static final String      RW_SPACE = "var"+ File.separator;
	
	public  static final String           DNU = "DNU";
	private static final boolean startupDebug = false;
	private static      int            level = 0; // TODO: 0 = every level, -n = ignore level n
	
	private static Audit     audit = new Audit( "Enguage" );
	public  static Overlay       o = Overlay.Get();

	private static Shell   shell   = new Shell( "Enguage", copyright );
	public  static Shell   shell() {return shell;}
	
	public  static boolean verbose = false;
	
	public  static void init() {init( RW_SPACE );}
	public  static void init( String root ) {
		Fs.root( root );
		Concepts.addConcepts( Assets.listConcepts() );
		Config.load( "config.xml" );
	}
	
	public static Strings mediate( Strings said ) { return mediate( "uid", said );}
	public static Strings mediate( String uid, Strings utterance ) {
				
		Strings reply;
		audit.in( "mediate", utterance.toString() );
		
		Overlay.attach( uid );
			
		if (Server.serverOn()) Audit.log( "Server  given: " + utterance.toString() );
		
		// locations contextual per utterance
		Where.clearLocation();
		
		if (Reply.isUnderstood()) // from previous interpretation!
			Overlay.startTxn( Redo.undoIsEnabled() ); // all work in this new overlay
		
		Reply r = Repertoire.mediate( new Utterance( utterance ));

		// once processed, keep a copy
		Utterance.previous( utterance );

		if (Reply.isUnderstood()) {
			Overlay.finishTxn( Redo.undoIsEnabled() );
			Redo.disambOff();
		} else {
			// really lost track?
			audit.debug( "Enguage:interpret(): not understood, forgetting to ignore: "
			             +Repertoire.signs.ignore().toString() );
			Repertoire.signs.ignoreNone();
			shell.aloudIs( true ); // sets aloud for whole session if reading from fp
		}

		// auto-unload here - autoloading() in Repertoire.interpret() 
		// asymmetry: load as we go; tidy-up once finished
		Autoload.unload();

		reply = Reply.say().appendAll( r.toStrings());
		Reply.say( null );
		
		if (Server.serverOn()) Audit.log( "Server replied: "+ reply );
			
		Overlay.detach();
		
		return audit.out( reply );
	}
	
	// ==== test code =====
	private static boolean   serverTest = false;

	private static int       portNumber = 8080;
	private static void      portNumber( String pn ) { portNumber = Integer.parseInt( pn );}

	// Call this direct, so it's not counted!
	private static final String ihe =  "I have everything";
	private static void clearTheNeedsList() { clearTheNeedsList( ihe );}
	private static void clearTheNeedsList( String s ) { Enguage.mediate( new Strings( s ));	}
	private static void tidyUpViolenceTest( String fname ) {
		Enguage.mediate( new Strings( "delete "+ fname +" advocate list" ));
		Enguage.mediate( new Strings( "delete "+ fname +" fear     list" ));
		Enguage.mediate( new Strings( "delete _user causal list" ));
		Enguage.mediate( new Strings( "unset the value of they" ));
	}
	private static void tidyUpViolenceTest() { tidyUpViolenceTest( "violence" ); }
	
	private static int testGrp = 0;
	private static String testName = null;
	private static boolean runTheseTests() {return runTheseTests( null );}
	private static boolean runTheseTests( String title ) {
		++testGrp;
		boolean runTheseTests = testName != null ?
				title != null && title.contains( testName )
				: level == 0 || level == testGrp || (level < 0 && -level != testGrp);
		if (runTheseTests) audit.title( "Test "+ testGrp + (title != null ? ": "+ title : ""));
		return runTheseTests;
	}
	
	private static String testPrompt = "";
	private static String testPrompt() { return testPrompt;}
	private static void   testPrompt( String prompt) { testPrompt = prompt;}
	
	private static String replyPrompt = "";
	private static String replyPrompt() { return replyPrompt;}
	private static void   replyPrompt( String prompt) { replyPrompt = prompt;}
	
	public  static void test( Strings cmd ) { test( cmd.toString() );}
	private static void test( String  cmd ) { test( cmd, null );}
	private static void test( String  cmd, String expected ) { test( cmd, expected, null );}
	private static void test( String  cmd, String expected, String unexpected ) {
		// expected == null => silent!
		if (expected != null)
			Audit.log( testPrompt()+ cmd +".");
		
		Strings reply = serverTest ?
				new Strings( Server.client( "localhost", portNumber, cmd ))
				: Enguage.mediate( new Strings( cmd ));

		if (expected == null      ||
			expected.equals( "" ) ||
			reply.equalsIgnoreCase( new Strings( expected )))
		
			audit.passed( replyPrompt()+ reply +"." );      // 1st success
			
		else if (unexpected == null)                        // no second chance
			//Repertoire.signs.show();
			audit.FATAL(
				"reply: '"+    reply    +"',\n             "+
				"expected: '"+ expected +"' "
			);
		
		else if (unexpected.equals( "" ) ||
				 reply.equalsIgnoreCase( new Strings( unexpected )))
		
			audit.passed( replyPrompt()+ reply +".\n" );
		
		else                                           // second chance failed too!
			//Repertoire.signs.show();
			audit.FATAL(
				"reply: '"      + reply      +"'\n             "+
				"expected: '"   + expected   +"'\n          "+
				"alternately: '"+ unexpected +"'\n          "
			);
	}
	
	public static void selfTest() {
		// ...useful ephemera...
		//interpret( "detail on" );
		//interpret( "tracing on" );
		//Audit.allOn();
		//Repertoire.signs.show( "OTF" );
		
		Audit.interval(); // reset timer
		testPrompt(  "\nuser> "    );
		replyPrompt( "enguage> " );

		Pronoun.interpret( new Strings( "add masculine martin" ));
		Pronoun.interpret( new Strings( "add masculine james" ));
		Pronoun.interpret( new Strings( "add feminine  ruth" ));

//		if (runThisTest( "title" )) {
//			test( "", "" );
//		}
		if (runTheseTests( "can - capabilities" )) {
			test( "flowers can be yellow", "ok , flowers can be yellow" );
		}
		if (runTheseTests( "Megan's Thales Experiment" )) {
			/*
//			test( "you can say variable colour is a colour", "go on" );
//			test( "this implies that you add variable colour to your colour list", "go on" );
//			test( "and that is it",                             "ok" );
//			
//			test( "red is a colour", "ok" );
//			test( "blue is a colour", "ok" );
//			test( "green is a colour", "ok" );
//			test( "yellow is a colour", "ok" );
			
			test( "set colour to yellow", "ok, colour is set to yellow" );
			test( "set colour to red",    "ok, colour is set to red" );
			test( "set colour to fred",   "sorry, fred is not a colour i know" );
			
			//
			// before logging in, test that we can't just affect things...
			test( "switch to submarines screen", "sorry, you need to be logged in" );
			test( "clear the filter",            "sorry, you need to be logged in" );
			
			test( "login as megan",              "ok, you're logged in as megan" );
			test( "switch to submarines screen", "ok, switched to submarines screen" );
			test( "what is the value of screen", "submarines, the value of screen is submarines" );
			test( "filter by submarines only",   "ok, you're filtering by submarines only" );
			
			test( "logout",                      "ok, you have logged out" );
			*/
			test( "martin is a user",   "ok");
			test( "set user as martin", "ok, martin is logged on");
		}
		if (runTheseTests( "BCS HCI Workshop" )) { // code generation features
			
			test( "to the phrase hello reply hello to you too", "" );
			test( "hello",                 "hello to you too" );
			test( "to the phrase my name is variable name reply hello variable name", "" );
			test( "please my name is martin", "hello martin" );
			
			clearTheNeedsList();
			test( "what do i need",        "you don't need anything" );
			test( "i need a coffee",       "ok, you need a coffee" );
			test( "what do i need",        "you need a coffee" );
			
			test( "what do i think",       "i'm sorry, I don't understand the question" );
			
			test( "what do i want",        "i'm sorry, i don't understand the question" );
			test( "want is like need",     "ok, want is like need" );
			test( "what do i want",        "you don't want anything" );
			
			test( "interpret something can be variable quality thus", "" );
			test( "first add variable quality to my quality list",    "" );
			test( "and then reply ok variable quality is a quality",  "" );
			test( "that concludes interpretation",                    "ok" );
			
			test( "something can be cool", "ok cool is a quality" );
			
			test( "interpret variable things are variable quality thus", "" );
			test( "first add variable things to my variable quality list", "" );
			test( "and then reply ok variable things are variable quality", "" );
			test( "that concludes interpretation", "ok" );
		
			test( "ferraris are cool",     "ok ferraris are cool" );
			test( "want is like need",     "" );
			test( "i want a ferrari because ferraris are cool", "ok, you want a ferrari because ferraris are cool" );
			test( "why do I want a ferrari", "because ferraris are cool" );
			
			// Tidy up...
			test( "I don't want anything", "ok, you don't want anything" );
			test( "want is not like need", "ok, want is not like need" );
			test( "what do i want",        "i'm sorry, i don't understand the question" );
		}
		
		if (runTheseTests( "Simple Food Diary" )) {
			test( "i just ate breakfast",             "ok, you have eaten breakfast today" );
			test( "today i have eaten a mars bar",    "ok, you have eaten a mars bar today" );
			test( "i have eaten 2 packets of crisps", "ok, you have eaten 2 packets of crisps today" );
			
			test( "what have i eaten today",
			      "you have eaten breakfast today , a mars bar today , and 2 packets of crisps today" );
		}
				
		if (runTheseTests( "WSC - advocacy and fear" )) {
			// TODO: WSC - alternative states tests
			// mut ex: dead is the opposite of alive, no?
			//         dead and alive are mutually exclusive
			// mut ex: i am martin            - ok
			//         i am martin wheatman   - ok
			//         i am martin            - yes, i know
			//         i am harvey wallbanger - no you're martin
			//         i've changed my name to harvey wallbanger - ok
			// fat and thin and athletic are mutually exclusive.
			//         I am fat.     Am I thin. No
			//         I am not fat. Am i thin. I don't know
			audit.subtl( "Contradiction test... can't swap between states directly");
			test( "demonstrators fear violence",        "ok, demonstrators fear violence" );
			test( "demonstrators advocate violence",    "no, demonstrators fear violence" );
			test( "demonstrators do not fear violence", "ok, demonstrators don't fear violence" );
			test( "demonstrators advocate violence",    "ok, demonstrators advocate violence" );
			test( "demonstrators fear violence",        "no, demonstrators advocate violence" );
			test( "demonstrators don't advocate violence", 
					 "ok, demonstrators don't advocate violence" );
			// tidy up
			tidyUpViolenceTest();
			
			// ----------------------------------------------------------------
			audit.subtl( "Openly stated: opposing views" );
			test( "the councillors   fear     violence", "ok, the councillors       fear violence" );
			test( "the demonstrators advocate violence", "ok, the demonstrators advocate violence" );
			// test 1
			test( "the councillors refused the demonstrators a permit because they fear violence",
					 "ok, the councillors refused the demonstrators a permit because they fear violence" );
			test( "who are they", "they are the councillors" );
			// test 2
			test( "the councillors refused the demonstrators a permit because they advocate violence",
					 "ok, the councillors refused the demonstrators a permit because they advocate violence" );
			test( "who are they", "they are the demonstrators" );
			// tidy up
			tidyUpViolenceTest();
			
			// ----------------------------------------------------------------
			audit.subtl( "Openly stated: aligned views - advocate" );
			test( "the councillors   advocate violence", "ok, the councillors   advocate violence" );
			test( "the demonstrators advocate violence", "ok, the demonstrators advocate violence" );
			
			// test  1
			test( "the councillors refused the demonstrators a permit because they fear violence",
					 "i'm sorry, I don't think they fear violence" );
			test( "who are they", "I don't know" );
			
			// test 2
			test( "the councillors refused the demonstrators a permit because they advocate violence",
					 "ok, the councillors refused the demonstrators a permit because they advocate violence" );
			test( "who are they", "they are the councillors , and the demonstrators" );
			
			// tidy up
			tidyUpViolenceTest();
			
			// ----------------------------------------------------------------
			audit.subtl( "Openly stated: aligned views - fear" );
			test( "the councillors fear violence because the voters fear violence",
					 "ok, the councillors fear violence because the voters fear violence" );
			test( "the demonstrators fear violence", "ok, the demonstrators fear violence" );
			
			// test 1
			test( "the councillors refused the demonstrators a permit because they fear violence",
					 "ok, the councillors refused the demonstrators a permit because they fear violence" );
			test( "who are they", "they are the councillors , the voters , and the demonstrators" );
			
			// test 2
			test( "the councillors refused the demonstrators a permit because they advocate violence",
					 "i'm sorry, I don't think they advocate violence" );
			test( "who are they", "i don't know" );
			
			// tidy up
			tidyUpViolenceTest();
			
			// ----------------------------------------------------------------
			audit.subtl( "the common sense view" );
			// This should be configured within the repertoire...
			test( "common sense would suggest the councillors fear violence",
					 "ok, common sense would suggest the councillors fear violence" );
			
			test( "the councillors refused the demonstrators a permit because they fear violence",
					 "ok, the councillors refused the demonstrators a permit because they fear violence" );
			test( "who are they", "they are the councillors" );
			
			// tidy up - N.B. these will affect previous tests on re-runs
			tidyUpViolenceTest( "csviolence" );
		}
		if (runTheseTests( "Saving spoken concepts" )) {
			// First, check we're clean!  -- may not be the case with BCS HCI code
			test( "hello",                          "hello to you too", "I don't understand" );
			// Build a repertoire...
			test( "to the phrase hello reply hello to you too", "ok" );
			test( "ok",                             "ok" );
			test( "hello",                          "hello to you too" );
			test( "to the phrase my name is variable name reply hello variable name", "ok" );
			test( "ok", "ok" );
			// OK, now save this...
			test( "save  spoken concepts as hello", "ok" );
			
			// then remove the cached concept...
			Autoload.unload( "hello" );
			
			// So, this will reload from the saved repertoire
			test( "hello",                          "hello to you too" );
			
			// OK, now delete the repertoire (and the sign)
			test( "delete spoken concept hello",    "ok" );
			// ...and its gone. We're clean again!
			test( "hello",                          "i don't understand" );
		}
		if (runTheseTests( "Why/because, IJCSSA article example" )) {
			audit.subtl( "Simple action demo" );
			test( "i am baking a cake",     "yes, i know", "ok, you're     baking a cake" );
			test( "am i baking a cake",     "yes,               you're     baking a cake" );
			test( "i am not baking a cake",                "ok, you're not baking a cake" );
			
			audit.title( "Why/because" );
			test( "i am baking a cake so i need 3 eggs",
					   "ok, you need 3 eggs because you're baking a cake" );
			
			test( "am i baking a cake",      "yes, you're baking a cake" );
			test( "how many eggs do i need", "3, you need 3 eggs" );
			
			test( "so why do i need 3 eggs", "because you're baking a cake" );
			test( "do I need 3 eggs because I am baking a cake",
				       "yes, you need 3 eggs because you're baking a cake" );
			// simple check for infinite loops
			test( "i am baking a cake because i need 3 eggs",
					   "i'm sorry, you need 3 eggs because you're baking a cake" );
			
			audit.subtl( "Distinguishing negative responses" );
			// I do understand, "sophie needs dr martens", but
			// I don't understand, "sophie is very fashionable"
			test( "sophie needs dr martens because sophie is very fashionable",
                       "I don't understand" );
			test( "sophie is very fashionable because sophie needs dr martens",
                       "I don't understand" );
			test( "do i need 250 grams of flour because i am baking a cake",
                       "i'm sorry, it is not the case that you need 250 grams of flour" );
			test( "why am i heating the oven",
					   "i'm sorry, i didn't know you're heating the oven" );
			
			audit.subtl( "Transitivity" );
			test( "i need to go to the shops because i need 3 eggs",
					   "ok, you need to go to the shops because you need 3 eggs" );
			test( "do i need to go to the shops because i need 3 eggs",
					   "yes, you need to go to the shops because you need 3 eggs" );
			// this test steps over one reason...
			test( "do i need to go to the shops because i am baking a cake",
					   "yes, you need to go to the shops because you're baking a cake" );
			
			audit.subtl( "Why might.../abduction" );
			test( "i am not baking a cake",  "ok, you're not baking a cake" );
			test( "am i baking a cake",      "no, you're not baking a cake" );
			test( "i do not need any eggs",  "ok, you don't need any eggs" );
			test( "why do i need 3 eggs",    "no, you don't need 3 eggs" );
			test( "why might i need 3 eggs", "because you're baking a cake" );
			
		}
		if (runTheseTests( "The Non-Computable concept of Need" )) { // 
			
			// regression test: "do i need" != >do i need OBJECT<
			// blank var at end of utterance
			test( "do i need",                  "i'm sorry, i don't understand the question" );
			
			clearTheNeedsList();
			test( "what do i need",	            "you don't need anything" );
			test( "i need 2 cups of coffee and a biscuit",
					                            "ok, you need 2 cups of coffee and a biscuit");
			test( "what do i need",             "you need 2 cups of coffee, and a biscuit");
			test( "how many coffees do i need", "2, you need 2 coffees" );
			test( "i need 2 coffees",           "yes, i know" );
			test( "i don't need any coffee",    "ok, you don't need any coffee" );
			test( "what do i need",             "you need a biscuit" );

			audit.title( "Semantic Thrust" );
			test( "i need to go to town",       "ok, you need to go to town" );
			test( "what do i need",             "you need a biscuit, and to go to town" );
			test( "i have the biscuit",         "ok, you don't need any biscuit" );
			test( "i have to go to town",       "yes, i know" );
			test( "i don't need to go to town", "ok, you don't need to go to town" );
			test( "what do i need",             "you don't need anything" );
			
			audit.title( "Numerical Context" );
			clearTheNeedsList();
			test( "i need a coffee",     "ok, you need a coffee" );
			test( "and another",         "ok, you need another coffee" );
			test( "how many coffees do i need", "2, you need 2 coffees" );
			test( "i need a cup of tea", "ok, you need a cup of tea" );
			test( "and another coffee",  "ok, you need another coffee" );
			test( "what do i need",      "You need 3 coffees , and a cup of tea" );
			
			audit.title( "Correction" );
			test( "i need another coffee", "ok, you need another coffee" );
			test( "no i need another 3",   "ok, you need another 3 coffees" );
			//Audit.allOn();
			test( "what do i need",        "you need 6 coffees, and a cup of tea" );
			//System.exit( 0 );
			test( "prime the answer yes",  "ok, the next answer will be yes" );
			test( "i don't need anything", "ok, you don't need anything" );
			
			audit.title( "Group-as-entity" );		
			clearTheNeedsList( "MartinAndRuth does not need anything" );
			
			test( "martin and ruth need a coffee and a tea",
			         "ok, martin and ruth need a coffee and a tea" );
			
			test( "what do martin and ruth need",
			         "martin and ruth need a coffee , and a tea" );
			
			test( "martin and ruth do not need a tea", 
			         "ok, martin and ruth don't need a tea" );
			
			test( "what do martin and ruth need",
			         "martin and ruth need a coffee" );
			
			test( "martin and ruth need some biscuits",
			         "ok, martin and ruth need some biscuits" );
			
			test( "what do martin and ruth need",
			         "martin and ruth need a coffee, and some biscuits" );
			// Tidy up
			test( "martin and ruth do not need anything", "ok , martin and ruth don't need anything" );

			audit.title( "Combos, multiple singular entities");
			test( "james and martin and ruth all need a chocolate biscuit",
			         "ok, james and martin and ruth all need a chocolate biscuit" );
			
			test( "martin and ruth both need a cocoa and a chocolate biscuit",
			         "ok, martin and ruth both need a cocoa and a chocolate biscuit" );
			
			test( "what does martin need",
					 "martin needs a chocolate biscuit, and a cocoa" );
			clearTheNeedsList( "james  doesn't need anything" );
			clearTheNeedsList( "martin doesn't need anything" );
			clearTheNeedsList( "ruth   doesn't need anything" );
			
			audit.title( "Pronouns - see need+needs.txt" );
			clearTheNeedsList();
			
			test( "i need biscuits and coffee", "ok, you need biscuits and coffee" );
			test( "they are from Sainsbury's",  "ok, they are from sainsbury's" );
			test( "i need a pint of milk",      "ok, you need a pint of milk" );
			test( "it is from the dairy aisle", "ok, it is from the dairy aisle" );
			test( "i need cheese and eggs from the dairy aisle",
					                               "ok, you need cheese and eggs" );
			//mediate( "group by",                   "i'm sorry, i need to know what to group by" );
			test( "group by location",          "ok" );
			
			test( "what do i need from sainsbury's",
					   "you need biscuits, and coffee from sainsbury's" );
			
			test( "what do i need from the dairy aisle",
					   "you need a pint of milk, cheese, and eggs from the dairy aisle" );
			
			test( "i don't need anything from the dairy aisle",
					   "ok, you don't need anything from the dairy aisle" );
			
			audit.title( "Late Binding Floating Qualifiers" );
			clearTheNeedsList();
			test( "i need biscuits",       "ok, you need biscuits" );
			test( "i need milk from the dairy aisle", "ok, you need milk from the dairy aisle" );
			test( "i from the dairy aisle need milk", "yes, i know" );
			test( "from the dairy aisle i need milk", "yes, i know" );
			test( "what do i need",        "you need biscuits; and, milk from the dairy aisle" );
			test( "from the dairy aisle what do i need",  "you need milk from the dairy aisle" );
			test( "what from the dairy aisle do i need",  "you need milk from the dairy aisle" );
			test( "what do i need from the dairy aisle",  "you need milk from the dairy aisle" );
			
			audit.title( "Numbers ERROR!" );
			clearTheNeedsList();
			test( "i need an apple", "" );
			test( "how many apples do i need",  "1, you need 1 apples" ); // <<<<<<<<< see this!
		}
//		if (runThisTest( "james's experimental example" )) { // variables, arithmetic and lambda tests
//			//interpret( "england is a country",  "ok, england is a country" );
//			test( "preston is in england", "ok, preston is in england" );
//			test( "i am in preston",       "ok, you're in england" );
//		}
		if (runTheseTests( "Simple Variables" )) { // 
			test( "the value of name is fred",       "ok, name is set to fred" );
			test( "get the value of name",           "fred" );
			test( "set the value of name to fred bloggs", "ok, name is set to fred bloggs" );
			test( "what is the value of name",       "fred bloggs, the value of name is fred bloggs" );
			
			audit.subtl( "Simple Numerics" );
			test( "set the weight of martin to 104", "ok" );
			test( "get the weight of martin",        "Ok, the weight of martin is 104" );
			
			// non-numerical values
			audit.title( "Simply ent/attr model" );
			test( "the height of martin is 194",  "Ok,  the height of martin is 194" );
			test( "what is the height of martin", "194, the height of martin is 194" );

			audit.title( "Apostrophe's ;-)" );
			test( "what is martin's height", "194, martin's height is 194" );
			test( "martin's height is 195",  "Ok,  martin's height is 195" );
			test( "what is the height of martin", "195, the height of martin is 195" );
		}
		if (runTheseTests( "Annotation" )) {
			test( "delete martin was       list", "ok" );
			test( "delete martin wasNot    list", "ok" );
			test( "delete i      am        list", "ok" );
			test( "delete i      amNot     list", "ok" );
			test( "delete martin is        list", "ok" );
			test( "delete martin isNot     list", "ok" );
			test( "delete i      willBe    list", "ok" );
			test( "delete i      willNotBe list", "ok" );
			test( "delete martin willBe    list", "ok" );
			test( "delete martin willNotBe list", "ok" );
			
			/*
			 * Test 5.1 - IS
			 */
			// e.g. i am alive - 5.1
			test( "interpret i am variable state thus",         "go on" );
			test( "first add    variable state to   i am list", "go on" );
			test( "then  remove variable state from i amNot list", "go on" );
			test( "then whatever reply ok",                     "ok" );
			
			// e.g. i am not alive - 5.1
			test( "interpret i am not variable state thus",        "go on" );
			test( "first add    variable state to   i amNot list", "go on" );
			test( "then  remove variable state from i am    list", "go on" );
			test( "then whatever reply ok",                        "ok" );
			
			// e.g. am i alive? - 5.1
			test( "interpret am i variable state thus",                "go on" );
			test( "first variable state exists in i am list",          "go on" );
			test( "then reply yes i am variable state",                "go on" );
			test( "then if not variable state exists in i amNot list", "go on" );
			test( "then if not reply i do not know",                   "go on" );
			test( "then reply no i am not variable state",             "go on" );
			test( "ok", "ok" );
			
			//  e.g. martin is alive - 5.1
			test( "interpret variable entity is variable state thus",            "go on" );
			test( "first add    variable state to   variable entity is    list", "go on" );
			test( "then  remove variable state from variable entity isNot list", "go on" );
			test( "then whatever reply ok",                                      "ok" );
			
			// e.g. martin is not alive - 5.1
			test( "interpret variable entity is not variable state thus",       "go on" );
			test( "first add   variable state to   variable entity isNot list", "go on" );
			test( "then remove variable state from variable entity is    list", "go on" );
			test( "then whatever reply ok",                                     "ok" );
			
			// e.g. is martin alive - 5.1
			test( "interpret is variable entity variable state thus",        "go on" );
			test( "first variable state  exists in variable entity is list", "go on" );
			test( "then reply yes variable entity is variable state",        "go on" );
			test( "then if not variable state exists in variable entity isNot list", "go on" );
			test( "then reply no variable entity is not variable state",     "go on" );
			test( "then if not reply i do not know",                         "go on" );
			test( "ok", "ok" );

			// e.g. is martin not alive - 5.1
			test( "interpret is variable entity not variable state thus",       "go on" );
			test( "first variable state  exists in variable entity isNot list", "go on" );
			test( "then reply yes variable entity is not variable state",        "go on" );
			test( "then if not variable state exists in variable entity is list", "go on" );
			test( "then reply no variable entity is variable state",             "go on" );
			test( "then if not reply i do not know",                            "go on" );
			test( "ok", "ok" );

			// test 5.1
			test( "am i alive",     "i don't know" );
			test( "i am alive",     "ok" );
			test( "am i alive",     "yes i'm alive" );
			test( "i am not alive", "ok" );
			test( "am i alive",     "no i'm not alive" );
			
			// test 5.1
			test( "is martin alive", "i don't know" );
			test( "martin is alive", "ok" );
			test( "is martin alive", "yes martin is alive" );
			test( "martin is not alive", "ok" );
			test( "is martin alive",     "no martin is not alive" );
			test( "is martin not alive", "yes martin is not alive" );
			
			/*
			 *  Test 5.2 was/was not
			 */
			//  e.g. martin was alive - 5.2
			test( "interpret variable entity was variable state thus",            "go on" );
			test( "first add    variable state to   variable entity was    list", "go on" );
			test( "then  remove variable state from variable entity wasNot list", "go on" );
			test( "then whatever reply ok",                                       "ok" );
			
			// e.g. martin was not alive - 5.2
			test( "interpret variable entity was not variable state thus",       "go on" );
			test( "first add   variable state to   variable entity wasNot list", "go on" );
			test( "then remove variable state from variable entity was    list", "go on" );
			test( "then whatever reply ok",                                      "ok" );
			
			// e.g. was martin alive - 5.2
			test( "interpret was variable entity variable state thus",        "go on" );
			test( "first variable state  exists in variable entity was list", "go on" );
			test( "then reply yes variable entity was variable state",        "go on" );
			test( "then if not variable state exists in variable entity wasNot list", "go on" );
			test( "then reply no variable entity was not variable state",     "go on" );
			test( "then if not reply i do not know",                          "go on" );
			test( "ok", "ok" );

			// e.g. was martin not alive - 5.2
			test( "interpret was variable entity not variable state thus",       "go on" );
			test( "first variable state  exists in variable entity wasNot list", "go on" );
			test( "then reply yes variable entity was not variable state",       "go on" );
			test( "then if not variable state exists in variable entity was list", "go on" );
			test( "then reply no variable entity was variable state",            "go on" );
			test( "then if not reply i do not know",                             "go on" );
			test( "ok", "ok" );

			// test 5.2
			test( "was martin alive",     "i don't know" );
			test( "martin was alive",     "ok" );
			test( "was martin alive",     "yes martin was alive" );
			test( "martin was not alive", "ok" );
			test( "was martin alive",     "no martin was not alive" );
			test( "was martin not alive", "yes martin was not alive" );
			
			/*
			 *  Test 5.3 will be/will not be
			 */
			//  e.g. martin will be alive - 5.3
			test( "interpret variable entity will be variable state thus",           "go on" );
			test( "first add    variable state to   variable entity willBe    list", "go on" );
			test( "then  remove variable state from variable entity willNotBe list", "go on" );
			test( "then whatever reply ok",                                          "ok" );
			
			// e.g. martin will not be alive - 5.3
			test( "interpret variable entity will not be variable state thus",      "go on" );
			test( "first add   variable state to   variable entity willNotBe list", "go on" );
			test( "then remove variable state from variable entity willBe    list", "go on" );
			test( "then whatever reply ok",                                         "ok" );
			
			// e.g. will martin be alive - 5.3
			test( "interpret will variable entity be variable state thus",      "go on" );
			test( "first variable state exists in variable entity willBe list", "go on" );
			test( "then reply yes variable entity will be variable state",      "go on" );
			test( "then if not variable state exists in variable entity willNotBe list", "go on" );
			test( "then reply no variable entity will not be variable state",   "go on" );
			test( "then if not reply i do not know",                            "go on" );
			test( "ok", "ok" );

			// e.g. will martin not be alive - 5.3
			test( "interpret will variable entity not be variable state thus",      "go on" );
			test( "first variable state  exists in variable entity willNotBe list", "go on" );
			test( "then reply yes variable entity will not be variable state",      "go on" );
			test( "then if not variable state exists in variable entity willBe list", "go on" );
			test( "then reply no variable entity will be variable state",           "go on" );
			test( "then if not reply i do not know",                                "go on" );
			test( "ok", "ok" );

			// test 5.3
			test( "will i be alive",     "i don't know" );
			test( "i will be alive",     "ok" );
			test( "will i be alive",     "yes you'll be alive" );
			test( "i will not be alive", "ok" );
			test( "will i be alive",     "no you'll not be alive" );
			test( "will i not be alive", "yes you'll not be alive" );

			test( "will martin be alive",     "i don't know" );
			test( "martin will be alive",     "ok" );
			test( "will martin be alive",     "yes martin will be alive" );
			test( "martin will not be alive", "ok" );
			test( "will martin be alive",     "no martin will not be alive" );
			test( "will martin not be alive", "yes martin will not be alive" );

			// Test
			// Event: to move is to was (traverse time quanta)
			// interpret( "interpret when i am dead then move what i am to what i was thus", "go on" );
		}
		if (runTheseTests( "Verbal Arithmetic" )) {
			test( "what's 1 + 2",                     "1 plus 2 is 3" );
			test( "times 2 all squared",              "times 2 all squared makes 36" );
			test( "what is 36 + 4     divided by 2",  "36 plus 4     divided by 2 is 38" );
			test( "what is 36 + 4 all divided by 2",  "36 plus 4 all divided by 2 is 20" );
			
			audit.title( "Simple Functions" );
			test( "the sum of x and y is x plus y",  "ok, the sum of x and y is x plus y" );
			test( "what is the sum of 3 and 2",      "the sum of 3 and 2 is 5 " );
			test( "set x to 3",                      "ok, x is set to 3" );
			test( "set y to 4",                      "ok, y is set to 4" );
			test( "what is the value of x",          "3, the value of x is 3" );
			test( "what is the sum of x and y",      "the sum of x and y is 7" );
			
			audit.title( "Factorial Description" );
			//Audit.allOn();
			//mediate( "what is the factorial of 4",       "I don't know" );
			/* Ideally, we want:
			 * - the factorial of 1 is 1;
			 * - the factorial of n is n times the factorial of n - 1;
			 * - what is the factorial of 3.
			 */
			test( "the factorial of 1 is 1",          "ok, the factorial of 1 is 1" );
			
			// in longhand this is...
			test( "to the phrase what is the factorial of 0 reply 1", "ok" );
			test( "what is the factorial of 0",  "1" );
			
			test( "interpret multiply numeric variable a by numeric variable b thus", "go on" );
			test( "first evaluate variable a times variable b",                       "go on" );
			test( "ok", "ok" );
			
			test( "the product of x and y is x times y", "" );
			test( "what is the product of 3 and 4",  "the product of 3 and 4 is 12" );
			//TODO:
			//interpret( "what is the product of x and y",  "the product of x and y is x times y" );
			test( "the square of x is x times x",    "Ok, the square of x is x times x" );
			test( "what is 2 times the square of 2", "2 times the square of 2 is 8" );
			
			// again, in longhand this is...
			test( "interpret subtract numeric variable c from numeric variable d thus", "go on" );
			test( "first evaluate variable d minus variable c",                         "go on" );
			test( "ok", "ok" );
			
			test( "subtract 2 from 3", "1" );
			
			// interpret( "the factorial of n is n times the factorial of n - 1", "ok" );
			// interpret( "what is the factorial of n",   "n is n times the factorial of n minus 1" );
//			mediate( "interpret what is the factorial of numeric variable n thus",  "go on" );
//			mediate( "first subtract 1 from variable n",                            "go on" );
//			mediate( "then what is the factorial of whatever",                      "go on" );
//			mediate( "then multiply whatever by variable n",  "go on" );
//			mediate( "then reply whatever the factorial of variable n is whatever", "go on" );
//			mediate( "ok", "ok" );
			
			test( "the factorial of n is n times the factorial of n minus 1",
					"ok, the factorial of n is n times the factorial of n minus 1" );
			test( "what is the factorial of 4", "the factorial of 4 is 24" );
		}
		if (runTheseTests( "Temporal interpret" )) {
			test( "what day is christmas day", "" );
			//testInterpret( "what day is it today" );
			// my date of birth is
			// how old am i.
			// age is the given date minus the date of inception
			// if no date given, use the current date.
			// persons age given in years
			// what is my age [in <epoch default="years"/>]
		}
		if (runTheseTests( "Temporospatial concept MEETING" )) {
			/* TODO: interpret think of a variable entity thus.  // see sofa for particular details!
			 * first create a class variable entity.             // mkdir pub; touch pub/isa 
			 * then  create an anonymous entity variable entity. // mkdir pub/a
			 * then  set the context of the variable entity to a variable entity // ln -s pub/the pub/a
			 * ok.
			 */
			test( "I'm not meeting anybody",
					   "Ok , you're not meeting anybody" );
			test( "At 7 I'm meeting my brother at the pub",
					   "Ok , you're meeting your brother at 7 at the pub" );
			test( "When  am I meeting my brother",
					   "You're meeting your brother at 7" );
			test( "Where am I meeting my brother",
					   "You're meeting your brother at the pub" );
			test( "Am I meeting my brother",
					   "Yes , you're meeting your brother" );
			
			test( "I'm meeting my sister at the pub", "" );
			test( "When am I meeting my sister",
					   "I don't know when you're meeting your sister" );
			
			test( "When am I meeting my dad",
					   "i don't know if you're meeting your dad" );
			test( "Where am I meeting my dad" ,
					   "i don't know if you're meeting your dad" );
		}
		if (runTheseTests( "Generic Pronouns" )) { // Language features
			clearTheNeedsList( "martin doesn't need anything" );
			test( "martin needs a coffee", "ok, martin needs a coffee" );
			test( "what does he need",     "martin needs a coffee" );
			clearTheNeedsList( "martin doesn't need anything" );
			
			test( "ruth needs a tea",      "ok, ruth needs a tea" );
			test( "what does she need",    "ruth needs a tea" );
			clearTheNeedsList( "ruth   doesn't need anything" );
			
			test( "laurel and hardy need a coffee and a tea",
			         "ok, laurel and hardy need a coffee and a tea" );
			
			test( "what do they need",     "laurel and hardy need a coffee , and a tea" );
			clearTheNeedsList( "MartinAndRuth does not need anything" );
			
			test( "james needs 3 eggs because he is baking a cake",
					 "ok, james needs 3 eggs because he is baking a cake" );
		}
		if (runTheseTests()) { // 
			/* TODO:
			 *  create a queen called elizabeth the first  (eliz = woman's name, a queen is a monarch => person)
			 *  she died in 1603
			 *  she reigned for 45 years (so she ascended/came to the throne in 1548!)
			 */
			test( "a queen is a monarch", "ok, a queen is a monarch" );
		}
		if (runTheseTests( "Disambiguation" )) {
			test( "the eagle has landed",    ""   /* "Are you an ornithologist" */);
			test( "no the eagle has landed", "" /* "So , you're talking about the novel" */ );
			test( "no the eagle has landed", "" /*"So you're talking about Apollo 11" */	);
			test( "no the eagle has landed", "" /* "I don't understand" */ );
			// Issue here: on DNU, we need to advance this on "the eagle has landed"
			// i.e. w/o "no ..."
		}
		if (runTheseTests( "TCP/IP test" )) {
			// bug here??? config.xml has to be 8080 (matching this) so does  // <<<< see this!
			// config port get chosen over this one???
			test( "tcpip localhost "+ Server.TestPort +" \"a test port address\"", "ok" );
			test( "tcpip localhost 5678 \"this is a test, which will fail\"",  "i'm sorry" );
			test( "simon says put your hands on your head", "" ); //, "ok, success" );
		}
		if (runTheseTests( "Polymorphism - setup new idea and save" )) { // code generation features
			
			clearTheNeedsList( "i don't want anything" );
			test( "want is unlike need", "ok, want is unlike need", "yes, i know" );
			test( "what do i want",      "i'm sorry, i don't understand the question" );
			
			test( "want is like need",   "ok, want is like need" );
			test( "what do i want",      "you want another pony", "you don't want anything" );
			test( "i want another pony", "yes, i know", "ok, you want another pony" );
			test( "what do i want",      "you want another pony" );
		}
		if (runTheseTests( "On-the-fly Language Learning" )) { // 
			/* TODO: create filename from pattern:
			 *    "i need phrase variable objects" => i_need-.txt (append? create overlay)
			 *    "this is part of the need concept" => need.txt (append)
			 *    Enguage.interpret() => overlay
			 *    Conceept.load() => can this outlive Enguage overlay???
			 */

			// First, what we can't say yet...
			test( "my name is martin",                 "I don't understand" );
			test( "if not  reply i already know this", "I don't understand" );
			test( "unset the value of name",           "ok" );

			// build-a-program...
			test( "interpret my name is phrase variable name thus", "go on" );
			test( "first set name to variable name",                "go on" );
			test( "then get the value of name",                     "go on" ); // not strictly necessary!
			test( "then reply hello whatever",                      "go on" );
			test( "ok",                                             "ok"    );

			test( "my name is ruth",   "hello   ruth" );
			test( "my name is martin", "hello martin" );


			//...or to put it another way
			test( "interpret i am called phrase variable name like this", "go on" );
			test( "first reply hi whatever",                             "go on" );
			test( "this implies that you set name to variable name",    "go on" );
			test( "this implies that name is not set to variable name", "go on" );
			test( "then if not reply i already know this",              "go on" );
			test( "ok",                                                    "ok" );

			test( "i am called martin", "i already know this" );

			// ...means.../...the means to...
			// 1. from the-means-to repertoire
			test( "to the phrase phrase variable x the means to phrase variable y reply i really do not understand", "ok" );
			test( "ok", "ok" );

			test( "do we have the means to become rich", "I really don't understand" );

			// 2. could this be built thus?
			test( "to phrase variable this means phrase variable that reply ok", "ok" );
			test( "this implies that you set transformation to false",        "go on" );
			test( "this implies that you perform sign think variable that",   "go on" );
			test( "this implies that you perform sign create variable this",  "go on" );
			test( "this implies that you set transformation to true",         "go on" );
			test( "ok", "ok" );

			test( "just call me phrase variable name means i am called variable name", "ok" );
			test( "just call me martin", "i already know this" );
		}
		if (runTheseTests( "Example: 9-line input" )) {
			test( "havoc 1 this is a Type II control",  "ok, go ahead" );
			test( "lines 1 through 3 are not applicable",
					 "ok, lines 1 through 3 are not applicable" );
			test( "target elevation is 142 feet",      "ok, target elevation is 142 feet" );
			test( "target description is vehicle in the open",
					 "ok, target description is vehicle in the open" );
			test( "target location is three zero uniform, whiskey Foxtrot, 15933 13674",
					 "ok, target location is three zero uniform, whiskey Foxtrot, 15933 13674" );
			test( "none",                              "ok, mark type is none" );
			test( "friendlies are 30 clicks east of target",
					 "ok, friendlies are present" );
			test( "egress back into the wheel",        "ok, egress back into the wheel" );
			
			test( "read back",
					"line 1 is not applicable . "+
					"line 2 is not applicable . "+
					"line 3 is not applicable . "+
					"target elevation is 142 feet . "+
					"target description is vehicle in the open . "+
					"target location is three zero uniform , whiskey foxtrot , 15933 13674 . "+
					"none . "+
					"friendlies are 30 clicks east of target . "+
					"egress is back into the wheel . "+
					"ok" );
			
//			test( "no friendlies",                "ok, no friendlies are present" );
//			test( "where are friendlies",         "no friendlies are present" );
			test( "what is the target elevation", "142 feet, target elevation is 142 feet"     );
			test( "where are friendlies",         "30 clicks east of target, friendlies are 30 clicks east of target" );
		}
		if (runTheseTests( "Light bins" )) {
			test( "there are 6 light bins",        "ok, there are 6 light bins" );
			test( "how many light bins are there", "6,  there are 6 light bins" );
			test( "show me light bin 6",           "ok, light bin 6 is flashing", "i'm sorry" );
		}
		if (runTheseTests( "Checking spoken concepts - have we remembered Hello" )) {
			// see if we've remembered hello... shouldn't have
			test( "hello", "i don't understand" );
		}
		if (runTheseTests( "Ask: Confirmation" )) {
//
			test( "the colour of the sky is blue", "ok, the colour of the sky is blue" );
			test( "what is the colour of the sky", "blue , the colour of the sky is blue" );
			
			test( "to the phrase ask me phrase variable question reply variable question", "ok" );

			test( "prime the answer blue", "ok, the next answer will be blue" );
			test( "ask me what is the colour of the sky", "what is the colour of the sky" );
			test( "blue", "i don't understand" );
//
//			//interpret( "prime the answer i do not understand", "ok, the next answer will be i don't understand" );
//			//interpret( "i have everything", "Ok , let us leave things as they are" );
//
//			/* TODO:
//			 * To the phrase: i am p v name       => set user name NAME
//			 *                my name is p v name => set user name NAME
//			 *                p v name            => set user name NAME
//			 * Ask: what is your name?
//			 */
		}
		
		Audit.log( testGrp +" test group(s) found" );
		audit.PASSED();
	}
	private static void usage() {
		Audit.LOG(
			 "Usage: java [-jar enguage.jar|org.enguage.Enguage]\n"  // program
			+"            --verbose --data <path> --server <port>\n" // switches
			+"            [-h | --port <port> | --httpd <port> | [<utterance>] | --test ]" //optis
		);
		Audit.LOG( "Switches are:" );
		Audit.LOG( "       -v, --verbose\n" );
		Audit.LOG( "       -d, --data <path> specifies the data volume to use\n" );
		Audit.LOG( "       -s, --server <port>" );
		Audit.LOG( "          switch to send test commands to a server." );
		Audit.LOG( "          This is only a test, and is on localhost." );
		Audit.LOG( "          (Needs to be initialised with -p nnnn);\n" );
		Audit.LOG( "Options are:" );
		Audit.LOG( "       -h, --help" );
		Audit.LOG( "          displays this message\n" );
		Audit.LOG( "       -p, --port <port>" );
		Audit.LOG( "          listens on local TCP/IP port number\n" );
		Audit.LOG( "       -H, --httpd [<port>]" );
		Audit.LOG( "          webserver on port number, default to 8080\n" );
		Audit.LOG( "       -t, --test <n>, -T <name>" );
		Audit.LOG( "          runs a self test, where" );
		Audit.LOG( "           n is the test number, or" );
		Audit.LOG( "          -n excludes a test, or" );
		Audit.LOG( "          -T <name> is part of the test name.\n" );
		Audit.LOG( "       [<utterance>]" );
		Audit.LOG( "          with an utterance it runs one-shot;" );
		Audit.LOG( "          with no utterance it runs as a shell," );
		Audit.LOG( "             requiring full stops (periods) to" );
		Audit.LOG( "             terminate utterances." );
	}
	private static void selfTest( String cmd, Strings cmds ) {
		// If we're sanity testing, remove yet preserve persistent data...
		String fsys = "./selftest";
		Fs.root( fsys );
		
		if (!Fs.destroy( fsys ))
			audit.FATAL( "failed to remove old database - "+ fsys );
		else
			try {
				if (cmd.equals( "-T" ))
					testName = cmds.size()==0 ? testName : cmds.remove( 0 );
				else
					level = cmds.size()==0 ? level : Integer.valueOf( cmds.remove( 0 ));
				selfTest();
			} catch (NumberFormatException nfe) {
				Audit.LOG( "Insanity: "+ nfe.toString() );
	}		}
	public static void main( String args[] ) {
		
		Audit.startupDebug = startupDebug;
		Strings    cmds = new Strings( args );
		String     cmd,
		           fsys = RW_SPACE;
		int i = 0;
		while (i < cmds.size()) {
			cmd = cmds.get( i );
			if (cmd.equals( "-v" ) || cmd.equals( "--verbose" )) {
				verbose = true;
				cmds.remove( i );
			} else if (cmd.equals( "-s" ) || cmd.equals( "--server" )) {
				serverTest = true;
				cmds.remove( i );
				cmd = cmds.size()==0 ? "8080":cmds.remove( i );
				portNumber( cmd );
			} else if (cmd.equals( "-d" ) || cmd.equals( "--data" )) {
				cmds.remove( i );
				fsys = cmds.size()==0 ? fsys : cmds.remove( i );
			} else
				i++;
		}

		init( fsys );
				
		cmd = cmds.size()==0 ? "":cmds.remove( 0 );
		if (cmd.equals( "-p" ) || cmd.equals( "--port" ))
			Server.server( cmds.size() == 0 ? "8080" : cmds.remove( 0 ));
		
		else if (cmd.equals( "-H" ) || cmd.equals( "--httpd" ))
			Server.httpd( cmds.size() == 0 ? "8080" : cmds.remove( 0 ));
		
		else if (cmd.equals( "-t" )
			  || cmd.equals( "--test" )
			  || cmd.equals( "-T" ))
			selfTest( cmd, cmds );
		
		else if (cmd.equals( "-h" ) || cmd.equals( "--help" ))
			usage();
		
		else if (cmd.equals( "" )) {
			Overlay.attach( "uid" );
			shell.aloudIs( true ).run();
		
		} else {
			// Command line parameters exists...
			// reconstruct original commands and interpret...

			// - remove full stop, if one given -
			cmds = new Strings( cmds.toString() );
			if (cmds.get( cmds.size()-1 ).equals( "." )) cmds.remove( cmds.size()-1 );

			// ...reconstruct original commands and interpret
			test( cmds.prepend( cmd ));
}	}	}
