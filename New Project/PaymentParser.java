import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.NumberFormatException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal; 

public class PaymentParser{
    InputStream is = null;
    InputStreamReader isr = null;
    BufferedReader br = null;
    String fileType = "";
    String encoding = "";
    final String UTF8 = "UTF-8";
    final String ISO88591 = "ISO-8859-1";
    final String paymentservice = "betalningsservice.txt";
    final String depositservice = "inbetalningstjansten.txt";
    

     public static void main(String []args){
        if(args.length == 0)
        {
            throwMessageAndExit("Missing argument. For proper usage include a input file.");
        }
        try{
            identifyFileType(args[0]);
            is = new FileInputStream(args[0]);
            isr = new InputStreamReader(is, Charset.forName(encoding));
            br = new BufferedReader(isr);
            switch(fileType){
                case paymentservice:
                    parsePaymentService(br);
                    break;
                case depositservice:
                    parseDepositService(br);
                    break;
                default:
                    System.err.println("Unknown posttype. No posts handled.");
                    break;
            }
        }
        catch(IOException e){
            System.err.println("File opening failed.");
        }
        finally{
            closeStreams();
        }
     }
     
     /**
      * A method for parsing payment services, which envokes the payment receiver.
      * @param the BufferedReader is used to get the lines from the file.
      */
     private void parsePaymentService(BufferedReader br){
        String line = br.readLine();
        if(!line.substring(0,1).equals("O")){
            throwMessageAndExit("Missing opening post. Exiting program.");
        }
        String accNum = line.substring(1,16);
        accNum = accNum.replaceAll("\\s+","");
        String strsum = line.substring(16,30).trim();
        int numPosts, year, month, date;
        BigDecimal accumsum;
        try{
            accumsum = convertStringToBigDecimal(strsum);
            
            numPosts = Integer.parseInt(line.substring(30,40));
            year = Integer.parseInt(line.substring(40,44));
            month = Integer.parseInt(line.substring(44,46));
            date = Integer.parseInt(line.substring(46,48));
        }catch(NumberFormatException e){
                throwMessageAndExit("Unable to convert information in the ending post. Exiting program.");
        }
        Date d = new Date(year,month,date);
        String currency = line.substring(48,51);
        line = br.readLine();
        String ref;
        String[] paymentString;

        BigDecimal depValue, calcSum = new BigDecimal("0");
        ArrayList<String> references = new ArrayList<String>();
        ArrayList<BigDecimal> paymentlist = new ArrayList<BigDecimal>();
        while(line.substring(0,1).equals("B")){
            ref = line.substring(15,50).trim();
            references.add(ref);
            
            paymentString = line.substring(1,15).trim();
            try{
                depValue = convertStringToBigDecimal(paymentString);
            }
            catch(NumberFormatException e){
                throwMessageAndExit("Unable to convert information in the ending post. Exiting program.");
            }
            calcSum = calcSum.add(depValue);
            paymentlist.add(depValue);
        }
        if(calcSum.compareTo(accumsum) != 0 || references.size()!=numPosts){
            throwMessageAndExit("Exiting program due to inconsistencies in the ending post.");
        }
        envokePayment(accNum,d,currency,references,paymentlist);
     }
     
     /**
      * A method for parsing deposites and envoking the payment receiver.
      * @param the BufferedReader is used to get the lines from the file.
      */
     private void parseDepositService(BufferedReader br){
        String line = br.readLine();
        if(!line.substring(0,2).equals("00")){
            throwMessageAndExit("Missing opening post. Exiting program.");
        }
        String accNum;
        accNum = line.substring(10,24);
        line = br.readLine();
        ArrayList<BigDecimal> deposits = new ArrayList<BigDecimal>();
        ArrayList<String> references = new ArrayList<String>();
        String ref, depString;
        BigDecimal depDecimal, accumsum = new BigDecimal();
        BigDecimal depValue;
        BigDecimal divider = new BigDecimal("100");
        while(line.substring(0,2).equals("30")){
            ref = line.substring(40,65).trim();
            references.add(ref);
            
            depString = line.substring(2,22);
            try{
                depDecimal = convertStringToBigDecimal(depString).divide(divider);
            }
            catch(NumberFormatException e){
                throwMessageAndExit("Unable to convert payment. Exiting program.");
            }
            accumsum = accumsum.add(depDecimal);
            deposits.add(depDecimal);
            line = br.readLine();
        }
        if(!line.substring(0,2).equals("99")){
            throwMessageAndExit("Missing ending post. Exiting program.");
        }
        try{
            BigDecimal postAccumsum = new BigDecimal(line.substring(2,22));
            postAccumsum = postAccumsum.divide(divider);
            int numPosts = Integer.parseInt(line.substring(30,38));
        }catch(NumberFormatException e){
                throwMessageAndExit("Unable to convert information in the ending post. Exiting program.");
        }
        if(numPosts!=deposits.size() || postAccumsum.compareTo(accumsum)!=0){
            throwMessageAndExit("Exiting program due to inconsistencies in the ending post.");
        }
        envokePayment(accNum, new Date(), "SEK", references, deposits);
     }
     
     /**
      * This method is used to envoke all the payments through the payment receiver.
      * @param account is the accountnumber related to the payments.
      * @param theDate is the date of the transactions.
      * @param currency holds the currency of the transactions.
      * @param references consists of a list containing all the references related to the transactions.
      * @param valueList consists of a list containing all the values of the transactions.
      */
     private void envokePayment(String account, Date theDate, String currency, ArrayList references, ArrayList valueList){
        PaymentReceiver pr = new PaymentReceiver;
        pr.startPaymentBundle(account,theDate,currency);
        for(int i=0;i<valueList.size();i++){
            pr.payment(valueList.get(i),references.get(i));
        }
        pr.endPaymentBundle();
     }
     
     /**
      * The method both identifies the file type as well as the encoding
      * used for the specific file type. If the file type is unknown
      * the method throwMessageAndExit is called.
      * @param filepath is used to identify the type of the file.
      */
     private void identifyFileType(String filePath){
        String[] str = filePath.split("_");
        String filenameEnding = str[str.length-1];
        switch(filenameEnding){
            case depositservice:
                fileType = depositservice;
                encoding = ISO88591;
                break;
            case paymentservice:
                fileType = paymentservice;
                encoding = ISO88591;
                break;
            default:
                throwMessageAndExit("Exiting due to unknown filetype.");
                break;
        }
     }
     
     /**
      * Checks if any streams are open and if any stream is open they're
      * closed.
      */
     private void closeStreams(){
        if(is!=null){
            is.close();
        }
        if(isr!=null){
            isr.close();
        }
        if(br!=null){
            br.close();
        }
     }
     
     /**
      * Called when an error occurs. A error message is printed before
      * the streams and the program is shut down.
      * @param exitMessage Error message which is printed.
      */
     private void throwMessageAndExit(String exitMessage){
        System.err.println(exitMessage);
        closeStreams();
        System.exit(1);
     }
     
     /**
      * 
      * @param amount converted from a string to a BigDecimal
      */
     private BigDecimal convertStringToBigDecimal(String amount){
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator(',');

        DecimalFormat decimalFormat = new DecimalFormat("#0,0#", symbols);
        decimalFormat.setParseBigDecimal(true);
            
        // parse the string
        BigDecimal bd = (BigDecimal) decimalFormat.parse(amount);
        return bd;
     }
}
