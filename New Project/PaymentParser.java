import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PaymentParser{
    InputStream is = null;
    InputStreamReader isr = null;
    BufferedReader br = null;
    String fileType = "";
    String encoding = "";
    final String UTF8 = "UTF-8";
    final String ISO88591 = "ISO-8859-1";
    final String betalningsservice = "betalningsservice.txt";
    final String inbetalningstjansten = "inbetalningstjansten.txt";
    

     public static void main(String []args){
        if(args.length == 0)
        {
            System.err.println("Missing argument. For proper usage include a input file.");
            System.exit(0);
        }
        try{
            fileType = identifyFileType(args[0]);
            is = new FileInputStream(args[0]);
            isr = new InputStreamReader(is, Charset.forName(encoding));
            br = new BufferedReader(isr);
            switch(fileType){
                case betalningsservice:
                    break;
                case inbetalningstjansten:
                    break;
                default:
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
     
     private String identifyFileType(String filePath){
        String[] str = filePath.split("_");
        String filenameEnding = str[str.length-1];
        switch(filenameEnding){
            case inbetalningstjansten:
                fileType = inbetalningstjansten;
                encoding = ISO88591;
                break;
            case betalningsservice:
                fileType = betalningsservice;
                encoding = ISO88591;
                break;
            default:
                fileType = "unidentified";
                encoding = UTF8;
                System.err.println("Unidentified filetype, encoding set to default type (UTF-8).");
                break;
        }
        return fileType;
     }
     
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
}
