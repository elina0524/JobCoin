import java.util.Scanner;
import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class JobCoinMixer{
  public static final String url = "http://jobcoin.gemini.com/negative/api/" ;
  public static final String house_account = "House";
  public static final double FEE = 0.015;
  private static String deposit_address = "";
  private static List<String> addresses;
  private static String balance = "";

  public static void main (String args []) throws Exception{
    //Step 1: System will ask user for 3 unused addresses
    userAddresses();

    //Step 2: System chooses a deposit address
    deposit_address = chooseDepositAddress();
    System.out.println("Please deposit your jobcoins to the following address: " + deposit_address );
    System.out.println("A 1.5% fee will be taken out");

    //Step 3: System waits 5 seconds and then checks to see if deposit has occured
    TimeUnit.SECONDS.sleep(5);
    getDepositInfo();
    System.out.println("Your deposit was a success!");

    //Step 4: deposit is moved to house_account after 3-10 seconds
    Random rand = new Random();
    int num = 3 + rand.nextInt((10 - 3) + 1);
    TimeUnit.SECONDS.sleep(num);
    postValue(deposit_address, house_account, balance);

    //Step 5: Different incrememnts of the balance is moved from the house account to three different adddresses provided by the user
    distributeFunds();


  }

//userAddresses() scans in 3 addresses provided by the user and stores them in an ArrayList
  private static void userAddresses(){
    addresses = new ArrayList<>(3);
    System.out.println("Please provide 3 unused addresses : ");
    Scanner scanner = new Scanner (System.in);
    int address_counter = 0;

    while(address_counter< 3){
      String temp_address = scanner.next();
      addresses.add(temp_address);
      address_counter++;
    }

    System.out.println ("Your JobCoins will be dispersed to following addresses: " +
    addresses.get(0) + " , " + addresses.get(1) + " , " + addresses.get(2));

  }

//chooseDepositAddress() chooses a random address from a list of possible deposit addresses
  private static String chooseDepositAddress(){
    List<String> deposit_options = new ArrayList<>(Arrays.asList("deposit1", "deposit2", "deposit3", "deposit4", "deposit5"));

    Random rand = new Random();
    int randomNum = 0 + rand.nextInt(((deposit_options.size()-1) - 0) + 1);
    return deposit_options.get(randomNum);
  }


//getDepositInfo() makes a get request and checks the deposit account every 8 seconds to see if deposit has been made
  public static void getDepositInfo() throws Exception {
    String string_balance = "0";
    Boolean first_time = true;

    while(string_balance.charAt(0) == '0'){

    //if it is not the first time in the while loop, send a reminder to the customer
    if(!first_time){
    System.out.println("REMINDER: Please deposit your bitcoins to the following address: " + deposit_address);
    TimeUnit.SECONDS.sleep(8);
    }

    String urlToRead  = url + "addresses/";
    StringBuilder stringBuilder = new StringBuilder(urlToRead);
    stringBuilder.append(URLEncoder.encode(deposit_address, "UTF-8"));
    URL obj = new URL(stringBuilder.toString());

		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Accept-Charset", "UTF-8");

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String line;
		StringBuffer response = new StringBuffer();

		while ((line = in.readLine()) != null) {
			response.append(line);
		}
		in.close();

    String address_info = response.toString();
    string_balance = address_info.substring(12, address_info.indexOf(",") -1);
    first_time = false;
    }

    balance = string_balance;
  }


//distributeFunds() takes fees and breaks the deposit into 3 different deposits and sends the parameters to postValue()
  public static void distributeFunds() throws Exception{

    double currBalance = (Integer.parseInt(balance)) * 1.0;
    currBalance = currBalance - (currBalance * FEE);

    double balance1= currBalance/(5.0);
    currBalance -= balance1;
    TimeUnit.SECONDS.sleep(5);
    postValue(house_account, addresses.get(0) , String.valueOf(balance1));

    double balance2 = currBalance/(3.0);
    currBalance -= balance2;
    TimeUnit.SECONDS.sleep(7);
    postValue(house_account, addresses.get(1) , String.valueOf(balance2));

    TimeUnit.SECONDS.sleep(4);
    postValue(house_account, addresses.get(2) , String.valueOf(currBalance));

  }


//postValue takes in the 3 values needed for a transaction, and completes the transaction
  public static void postValue (String from, String to, String coins) throws Exception{
    String urlToRead = url + "transactions";

    URL post_url = new URL(urlToRead);
    URLConnection con = post_url.openConnection();
    HttpURLConnection http = (HttpURLConnection)con;
    http.setRequestMethod("POST"); // PUT is another valid option
    http.setDoOutput(true);

    Map<String,String> arguments = new HashMap<>();
    arguments.put("fromAddress", from);
    arguments.put("toAddress", to);
    arguments.put("amount", coins);

    StringJoiner sj = new StringJoiner("&");
    for(Map.Entry<String,String> entry : arguments.entrySet())
        sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "="
             + URLEncoder.encode(entry.getValue(), "UTF-8"));
    byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
    int length = out.length;

    http.setFixedLengthStreamingMode(length);
    http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    http.connect();

    try(OutputStream os = http.getOutputStream()) {
        os.write(out);
    }
  }

}
