package hw09;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) 
    	throws IOException {
	    	BufferedReader account_reader = new BufferedReader(new FileReader(args[0]));
	    	String line;
	    	List<Account> accounts = new ArrayList<Account>();
	    	while ((line = account_reader.readLine()) != null){
	    		String[] tokenized = line.split(" ");
	    		accounts.add(new Account( Integer.parseInt(tokenized[1]) ));
	    	}

	    	MultithreadedServer a = new MultithreadedServer();
	    	a.runServer("hw09/data/rotate", accounts.toArray(new Account[accounts.size()]));

    }

}