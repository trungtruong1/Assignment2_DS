import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LamportClockTest {

    private LamportClock lamportClock;

    @BeforeEach
    void setUp() {
        lamportClock = new LamportClock();
    }

    @Test
    void testInitialValue() {
        assertEquals(0, lamportClock.getValue(), "Initial value should be 0");
    }

    @Test
    void testIncrement() {
        lamportClock.increment();
        assertEquals(1, lamportClock.getValue(), "Value should be 1 after one increment");

        lamportClock.increment();
        assertEquals(2, lamportClock.getValue(), "Value should be 2 after two increments");
    }

    @Test
    void testUpdateWithLargerReceivedValue() {
        lamportClock.update(5);
        assertEquals(6, lamportClock.getValue(), "Clock should be updated to 6 after receiving value 5");

        lamportClock.increment();
        assertEquals(7, lamportClock.getValue(), "Value should be 7 after incrementing");
    }

    @Test
    void testUpdateWithSmallerReceivedValue() {
        lamportClock.increment();
        lamportClock.increment();
        lamportClock.update(1);
        assertEquals(3, lamportClock.getValue(), "Clock should remain at 3 after receiving value 1 (since 2 > 1)");
    }

    @Test
    void testUpdateWithEqualReceivedValue() {
        lamportClock.increment(); // 1
        lamportClock.increment(); // 2
        lamportClock.update(2);
        assertEquals(3, lamportClock.getValue(), "Clock should be updated to 3 after receiving an equal value of 2");
        lamportClock.increment();
        assertEquals(4, lamportClock.getValue(), "Clock value should be 4 after incrementing");
    }
}
