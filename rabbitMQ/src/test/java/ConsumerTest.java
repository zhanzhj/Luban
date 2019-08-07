import com.alistar.consumer.Consumer;

public class ConsumerTest {
	public static void main(String[] args) {
		Consumer consumer = new Consumer();
		try {
			consumer.getMessage();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
