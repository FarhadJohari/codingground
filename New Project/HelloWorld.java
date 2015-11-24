public class HelloWorld{

     public static void main(String []args){
        String test = "Hel__lo_World";
        System.out.println(test);
        System.out.println(encodingType(test));
     }
     
     private String encodingType(String path){
        String[] str = test.split("_");
        return str[str.length-1];
     }
}
