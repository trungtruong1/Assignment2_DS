/**
 * This class implements a simple Lamport clock.
 */
public class LamportClock {
    private int value = 0;

    /**
     * Increments the Lamport clock value by 1.
     * This is typically done when an internal event occurs within a process.
     */
    public synchronized void increment() {
        value++;
    }

    /**
     * Updates the Lamport clock value based on a received value from another process.
     * The clock takes the maximum of its own value and the received value, and then increments it by 1.
     * This is used to synchronize the clock when receiving a message from another process.
     *
     * @param receivedValue The value of the Lamport clock from the message received from another process.
     */
    public synchronized void update(int receivedValue) {
        value = Math.max(value, receivedValue) + 1;
    }

    /**
     * Gets the current value of the Lamport clock.
     *
     * @return The current value of the Lamport clock.
     */
    public synchronized int getValue() {
        return value;
    }
}