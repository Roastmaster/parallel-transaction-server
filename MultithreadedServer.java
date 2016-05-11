package hw09;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;


class Task implements Callable<Integer>{
    private static final int A = constants.A;
    private static final int Z = constants.Z;
    private static final int numLetters = constants.numLetters;

    private Account[] accounts;
    private String transaction;
    private List<AccountCache> openCached = new ArrayList<AccountCache>();

    // TO DO: The sequential version of Task peeks at accounts
    // whenever it needs to get a value, and opens, updates, and closes
    // an account whenever it needs to set a value.  This won't work in
    // the parallel version.  Instead, you'll need to cache values
    // you've read and written, and then, after figuring out everything
    // you want to do, (1) open all accounts you need, for reading,
    // writing, or both, (2) verify all previously peeked-at values,
    // (3) perform all updates, and (4) close all opened accounts.

    public Task(Account[] allAccounts, String trans) {
        accounts = allAccounts;
        transaction = trans; 
    }

    private class AccountCache extends Account {
        public int accountNum;
        public int cachedValue;
        public Account acc;

        public AccountCache(int initialValue, int accountNum_, Account acc_){
            super(initialValue);
            cachedValue = initialValue;
            accountNum = accountNum_;
            acc = acc_;
        }

        public Account getAcc(){
            return acc;
        }

        public void superverify() throws TransactionAbortException{
            try{
                acc.verify(cachedValue);
                System.out.println("made it past that!");

            }
            catch (TransactionAbortException e){
                System.out.println(Integer.toString(accountNum) + "Shit's in use" );
                throw new TransactionAbortException();
            
            }
            catch (TransactionUsageError e){
                System.out.println("You fucked up trying to open, it's not a reader dawg "+accountNum);
            }
        }

        public int getAccountNum(){
            return accountNum;
        }

        public int getCachedValue(){
            return cachedValue;
        }
    }
    
    // TO DO: parseAccount currently returns a reference to an account.
    // You probably want to change it to return a reference to an
    // account *cache* instead.
    //
    private AccountCache parseAccount(String name) {
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        if (accountNum < A || accountNum > Z)
            throw new InvalidTransactionError();
        Account a = accounts[accountNum];
        AccountCache ac = new AccountCache(a.getValue(), accountNum, a);
        for (int i = 1; i < name.length(); i++) {
            if (name.charAt(i) != '*')
                throw new InvalidTransactionError();
            accountNum = (accounts[accountNum].peek() % numLetters);
            a = accounts[accountNum];
            ac = new AccountCache(a.getValue(), accountNum, a);
        }
        return ac;
    }

    // oh shit this literally returns values 
    private int parseAccountOrNum(String name) {
        int rtn;
        AccountCache ac;
        if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
            rtn = new Integer(name).intValue();
        } else {
            ac = parseAccount(name);
            rtn = ac.acc.peek();
            openCached.add(ac);
        }
        return rtn;
    }

    private void openAndVerifyRhs(int lhs) throws TransactionAbortException{
        for (AccountCache ac : openCached){
            try{
                if (ac.getAccountNum() != lhs){
                    System.out.println("Opening (rhs) " + ac.getAccountNum());
                    ac.acc.open(false);
                    System.out.println("Verifying "+ac.getAccountNum());
                    ac.superverify();
                }
            }
            catch (TransactionAbortException e) {
                System.out.println("Shit's open");
                throw new TransactionAbortException();
            }
        }
    }



    public void run() {
        // tokenize transaction
        // TO DO a bunch of shit in here 
        String[] commands = transaction.split(";");

        for (int i = 0; i < commands.length; i++) {
            String[] words = commands[i].trim().split("\\s");
            if (words.length < 3)
                throw new InvalidTransactionError();
            AccountCache lhs = parseAccount(words[0]);
            if (!words[1].equals("="))
                throw new InvalidTransactionError();
            int rhs = parseAccountOrNum(words[2]);
            for (int j = 3; j < words.length; j+=2) {
                if (words[j].equals("+"))
                    rhs += parseAccountOrNum(words[j+1]);
                else if (words[j].equals("-"))
                    rhs -= parseAccountOrNum(words[j+1]);
                else
                    throw new InvalidTransactionError();
            }
            try {
                System.out.println("Opening "+lhs.getAccountNum());
                lhs.open(true);
            } catch (TransactionAbortException e) {
                System.out.println(words[0] + "(LHS) Can't, shit's in use");
                // won't happen in sequential version
                run();
            }

            try {
                openAndVerifyRhs(lhs.getAccountNum());
                System.out.println(words[0]+" old value "+Integer.toString((lhs.getValue())));
                lhs.update(rhs);
                System.out.println(words[0]+" new value "+Integer.toString((lhs.getValue())));
                lhs.close();
            }
            catch (TransactionAbortException e){
                System.out.println("Shit's already open");
                run();
            }
        }
        System.out.println("commit: " + transaction);
    }

    @Override
    public Integer call() throws Exception{
        run();
        return 0;
    }
}

public class MultithreadedServer {

	// requires: accounts != null && accounts[i] != null (i.e., accounts are properly initialized)
	// modifies: accounts
	// effects: accounts change according to transactions in inputFile
    public static void runServer(String inputFile, Account accounts[])
        throws IOException {

        // read transactions from input file
        String line;
        BufferedReader input =
            new BufferedReader(new FileReader(inputFile));

        ExecutorService pool;
        List<Task> Tasks = new ArrayList<Task>(); 
        while ((line = input.readLine()) != null) {
            Tasks.add( new Task(accounts, line) );
        }

        pool = Executors.newFixedThreadPool(Tasks.size());

        for (Task t : Tasks){
            pool.submit(t);    
        }


        input.close();
        pool.shutdown();
    }
}
