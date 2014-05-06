/* miniJava test program
 *
 * bubblesort
 */
class BubbleSort{
    public static void main(String[] args){
        BBS b = new BBS();
	b.start(10);
    }
}


// This class contains the array of integers and
// methods to initialize, print and sort the array
// using Bublesort
class BBS{
    
    int[] numberList ;
    int size ;

    // Invoke the Initialization, Sort and Printing
    // Methods
    public void start(int sz){
	init(sz);
	print();
	System.out.println(99999);
	sort();
	print();
	System.out.println(99999);
    }

 
    // Sort array of integers using Bublesort method
    private void sort(){
	int n = size - 1 ;
	int i = 0;
	while (i < n) {
	    if (numberList[i] <= numberList[i + 1])
		i = i + 1;
	    else {
                swap(numberList,i,i+1);
		if (i > 0 )
		    i = i - 1;
	    }
	}
    }

    // swap v[i] with v[j]
    private void swap(int [] v, int i, int j) {
        int t = v[i] ;
        v[i] = v[j] ;
        v[j] = t;
    }


    // Printing method
    private void print(){
	int j = 0;
	while (j < size) {
	    System.out.println(numberList[j]);
	    j = j + 1 ;
	}
    }
    
    // Initialize array of integers
    private void init(int sz){
	numberList = new int[sz] ;
        size = numberList.length;

        int incr = 17; // gcd(size,incr) == 1
        int i = 1;

        while (i <= size) {
            numberList[mod(i*incr,size)] = i;
            i = i + 1;
        }
    }

    // a mod b
    private int mod(int a, int b) {
        return a - (a/b)*b;
    }
}