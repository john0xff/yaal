package on2015_09.on2015_09_05_Booking_com_Hackathon.Nearby_Attractions;

import net.egork.chelper.tester.NewTester;

import org.junit.Assert;
import org.junit.Test;

public class Main {
	@Test
	public void test() throws Exception {
		if (!NewTester.test("lib/test/on2015_09/on2015_09_05_Booking_com_Hackathon/Nearby_Attractions/Nearby Attractions.task"))
			Assert.fail();
	}
}