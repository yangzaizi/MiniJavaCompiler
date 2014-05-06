class A{

	public static void main(String[]args){
		print();

	}

	public static void print(){
		B[]array=new B[9];
		int i=0;
		while(i<array.length){
			array[i]=new B();
			array[i].x=i+1;
			System.out.println(array[i].x);
			i=i+1;

		}
		
	
	}


}


class B{

	int x;

	public int fibonacci(int n){
		int result=1;
		if(n>=3)
			result=result+fibonacci(n-1);
		return result;
	
	}

}