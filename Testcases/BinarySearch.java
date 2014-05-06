class BinarySearch{


	public static void main(String[]args){


		int[]array=new int[5];
		int i=0;
		while(i<array.length){

			array[i]=i+4;
			i=i+1;

		}
		int k=binarySearch(array,0,array.length,4);
		System.out.println(k);
	}

	public static int binarySearch(int[]array, int start, int end, int target){

		int result=-1;
		if(start<end){


			int mid=(start+end)/2;
			if(array[mid]==target)
				result=mid;
			else{

			    if(target<array[mid])
				result=binarySearch(array,start,mid-1,target);
			    else
				result=binarySearch(array,mid+1, end, target);
			}
		}
		return result;

	}

}