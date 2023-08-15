//class P1 {
//	public static void main(String[] args) {
//		try {
//			 A x;
//			 P1 z;
//			 
//			 x = new A();
//			 z = new P1(); 
//			 x.start();
//			 x.f = z;
//			 x.join();
//			 
//			}catch (Exception e) {
//					
//			} 
//	}
//}
//	 
//class A extends Thread{
//		P1 f;
//		
//		public void run() {
//			try {
//				A a;
//				P1 b;
//				a = this;
//				b = new P1();
//				a.f = b;
//			}catch(Exception e) {
//				
//			}
//		}
//	}
//


class P1{
	
}
public class T2 {
	
}
public class T1 {

	public static void main(String[] args) {
		try {
			 A x;
			 A y;
			 A z;
			 
			 x = new A();
			 y = new A(); 
			 z = new A();
			 
			 boolean flag =true;
			 if(flag){
				 x = y;
			 }else {
				 x = z;
			 }
			 
			 x.start();
			 T1 aa = x.f1;
			 z.start();
			 y.start();
			 y.join();
			 x.join();
			 z.join();
			 
			}catch (Exception e) {
					
			} 

	}

}


class A extends Thread{
	T1 f1;
	T1 f2;
	
	
	public void run() {
		try {
			A a;
			T1 b;
			a = this;
			b = new T1();
			a.f1 = b;
		}catch(Exception e) {
			
		}
	}
}