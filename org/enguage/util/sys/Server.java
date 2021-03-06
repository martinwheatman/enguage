package org.enguage.util.sys;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Random;

import org.enguage.Enguage;
import org.enguage.interp.Context;
import org.enguage.interp.repertoire.Repertoire;
import org.enguage.util.Audit;
import org.enguage.util.Strings;
import org.enguage.vehicle.Utterance;
import org.enguage.vehicle.reply.Reply;

public class Server {
	
	static final public int TestPort = 0;

	static private Audit         audit = new Audit( "net" );
	static private boolean httpRequest = false;
	static private Random       random = new Random();
	
	static private boolean serverOn = false;
	static public  boolean serverOn() { return serverOn; }
	
	static public void httpd( String port ) {
		httpRequest = true;
		server( port, "" );
	}
	static public void server( String port ) { server( port, "" );}
	static public void server( String port, String prefix ) {
		
		serverOn = true;
		try (ServerSocket server = new ServerSocket( Integer.valueOf( port ));)
		{
			Audit.LOG( "Server listening on port: "+ port );
			while (true) {
				try (	Socket    connection = server.accept();
						BufferedReader   in  = new BufferedReader( new InputStreamReader( connection.getInputStream()));
						DataOutputStream out = new DataOutputStream( connection.getOutputStream());
				) {
					String reply;
					if (httpRequest) {
						// parse request
						String request = in.readLine();   // "GET /i need a coffee HTTP/2.0"
						String[] reqs = request.split("/"); // ["GET ", ...
						reqs = reqs[ 1 ].split(" ");          // ["i", "need", "coffee", "HTTP"]
						Strings utterances = new Strings( reqs ); // ("i", "need", "coffee", "HTTP")
						utterances.remove( utterances.size() - 1 );   // remove "HTTP"
						utterances = new Strings( URLDecoder.decode( utterances.toString(), "UTF-8" ));
						
						// parse header for cookies...
						boolean found = false;
						String enguid = "";
						String setCookie = "";
						String cookiesString = in.readLine();
						while (!(found = cookiesString.startsWith( "Cookie:" ))) {
							cookiesString = in.readLine();
							if (cookiesString.equals( "" ))
								break;
						}
						if (found) {
							// parse cookies for enguid
							found = false;
							String[] cookies = cookiesString.split(";");
							for (String cookie : cookies)
								if (cookie.startsWith( "enguid=" )) {
									enguid = cookie.split( "=" )[ 1 ];
									found = true;
									break;
						}		}
						if (!found) {
							enguid    = "" + random.nextInt();
							setCookie = "Set-cookie: enguid=\""+ enguid +"\"\n";
						}
						prefix = "HTTP/2.0 200 OK\n"
								 + "Content-type: text/html\n"
								 + setCookie +"\n";
						reply = Enguage.mediate( enguid,  utterances ).toString();
					} else
						reply = Repertoire.mediate( new Utterance( new Strings( in.readLine() ))).toString();
					
					Audit.LOG( "Relying with: "+ reply );
					out.writeBytes( prefix + reply + "\n" );
					
				} catch (Exception e) {
					audit.ERROR( "Error in child socket");
			}	}
		} catch (IOException e) {
			audit.ERROR( "Engauge.main():IO error in TCP socket operation" );
		}
		serverOn = false;
	}
	static public String client( String addr, int port, String data ) {
		audit.in( "tcpip", "addr="+ addr +", port="+ port +", value='"+ data +"', ["+ Context.valueOf() +"]" );
		
		// don't know why, but data needs to be stripped of single quotes???
		data=Strings.trim( data, '\'' );
		data=Utterance.externalise( new Strings( data ), false ).toString();
		audit.debug( "data is: "+ data);
		
		String rc = Reply.failureStr();
		
		if (port == TestPort) { // test value
			rc = Reply.successStr(); // assume we've stuffed the server intentionally
			
		} else if (port > 1024 && port < 65536) {
			addr = addr==null || addr.equals( "" ) ? "localhost" : addr;
		
			try (	Socket connection = new Socket( addr, port );
					DataOutputStream out = new DataOutputStream( connection.getOutputStream());
					BufferedReader in = new BufferedReader( new InputStreamReader( connection.getInputStream()));
			) {
				audit.debug( "  writing: "+ data );
				out.writeBytes( data );
				out.flush();
				
				audit.debug( "  reading..." );
				rc = in.readLine();
				
			} catch (IOException e) {
				audit.ERROR( "error: "+ e.toString());
		}	}
		return audit.out( rc );
	}
	public static void main( String args[]) {
		Audit.allOn();
		if (args.length == 1)
			server( args[ 0 ]);
		else
			Audit.log( "output: "+ client( "localhost", 8080, "'put your hands on your head'" ));
}	}
