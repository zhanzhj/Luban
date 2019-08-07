import com.alistar.producer.Producer;

public class Test {
	public static void main(String[] args) {
		Producer producer = new Producer();
		try {
			producer.sendByExchange("hello RabbitMQ");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
