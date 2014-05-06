class A{
		int x;
        public static void main(String[]args){
                int[]a=B.array();
                int i=0;
                while(i<a.length){
                     a[i]=i+1;
                     i=i+1;
                }
                System.out.println(a[1]);
				A aa= B.a();
				aa.x=4;
				System.out.println(aa.x);
				K k = new K();
				k.method();
        }
}

class K{

	A a;
	
	public void method(){
		
		A[]j=get();
		int i=0;
	
		while(i<j.length){
			j[i]=new A();
			j[i].x=i+1;
			i=i+1;
		}
		a=j[2];
		System.out.println(a.x);
	}
	
	public A[] get(){
		return new A[3];
	}
}

class B{
		public static A a(){
			return new A();
		}


        public static int[] array(){

                return new int[3];
        }

}