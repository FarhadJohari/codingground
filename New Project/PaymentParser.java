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
      * 
      * @param the BufferedReader is used to get the lines from the file.
      */
     private void parsePaymentService(BufferedReader br){
        String line = br.readLine();
        if(!line.substring(0,1).equals("O")){
            throwMessageAndExit("Missing opening post. Exiting program.");
        }
        String accNum = line.substring(1,16);
        accNum = accNum.replaceAll("\\s+","");
        String[] strsum = (line.substring(16,30).trim()).split(",");
        int numPosts, year, month, date;
        Double accumsum;
        try{
            accumsum = Double.parseDouble(strsum[0]) + Double.parseDouble(strsum[1])*Math.pow(10,(-1*strsum[1].length()));
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
        Double calcTemp, calcSum;
        calcSum = 0;
        BigDecimal depValue;
        ArrayList<String> references = new ArrayList<String>();
        ArrayList<BigDecimal> paymentlist = new ArrayList<BigDecimal>();
        while(line.substring(0,1).equals("B")){
            ref = line.substring(15,50).trim();
            references.add(ref);
            
            paymentString = (line.substring(1,15).trim()).split(",");
            try{
                calcTemp = Double.parseDouble(paymentString[0]) + Double.parseDouble(paymentString[1])*Math.pow(10,(-1*paymentString[1].length()));
            }
            catch(NumberFormatException e){
                throwMessageAndExit("Unable to convert information in the ending post. Exiting program.");
            }
            calcSum = calcSum + calcTemp;
            depValue = new BigDecimal(calcTemp.toString());
            paymentlist.add(depValue);
        }
        if(calcSum!=accumsum || references.size()!=numPosts){
            throwMessageAndExit("Exiting program due to inconsistencies in the ending post.");
        }
        PaymentReceiver pr = new PaymentReceiver;
        pr.startPaymentBundle(accNum,d,currency);
        for(int i=0;i<paymentlist.size();i++){
            pr.payment(paymentlist.get(i),references.get(i));
        }
     }
     
     /**
      * 
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
        Double depDouble, accumsum;
        BigDecimal depValue;
        
        while(line.substring(0,2).equals("30")){
            ref = line.substring(40,65).trim();
            references.add(ref);
            
            depString = line.substring(2,22);
            try{
                depDouble = Double.parseDouble(depString)*0.01;
            }
            catch(NumberFormatException e){
                throwMessageAndExit("Unable to convert payment. Exiting program.");
            }
            accumsum = accumsum + depDouble;
            depValue = new BigDecimal(depDouble.toString());
            deposits.add(depValue);
            line = br.readLine();
        }
        if(!line.substring(0,2).equals("99")){
            throwMessageAndExit("Missing ending post. Exiting program.");
        }
        try{
            Double postAccumsum = Double.parseDouble(line.substring(2,22));
            int numPosts = Integer.parseInt(line.substring(30,38));
        }catch(NumberFormatException e){
                throwMessageAndExit("Unable to convert information in the ending post. Exiting program.");
        }
        if(numPosts!=deposits.size() || postAccumsum!=accumsum){
            throwMessageAndExit("Exiting program due to inconsistencies in the ending post.");
        }
        PaymentReceiver pr = new PaymentReceiver;
        pr.startPaymentBundle(accNum,new Date(),"SEK");
        for(int i=0;i<deposits.size();i++){
            pr.payment(deposits.get(i),references.get(i));
        }
        pr.endPaymentBundle();
     }
     
     /**
      * The method both identifies the file type as well as the encoding
      * used for the specific file type. If the file type is unknown
      * the method throwMessageAndExit is called.
      * @param the filepath is used to identify the type of the file.
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
      * @param Error message which is printed.
      */
     private void throwMessageAndExit(String exitMessage){
        System.err.println(exitMessage);
        closeStreams();
        System.exit(1);
     }
}
