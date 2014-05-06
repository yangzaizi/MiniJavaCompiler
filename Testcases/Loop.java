
class B{

        int b;

}

class A{

        public static int fibonacci(int n){
                int result=1;
                if(n>=3)
                        result=result+fibonacci(n-1);
                return result;

        }


        public static void main(String[]args){

                int i=1;
                while(i<=6){

                        System.out.println(fibonacci(i));
                        i=i+1;
                }
        }
}