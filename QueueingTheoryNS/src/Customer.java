public class Customer implements Comparable<Customer> {
    // All times are in nanoseconds
    private final int customerNumber;
    private final int priority;
    private long arrivalTime;
    private long serviceStartTime;
    private long serviceCompletionTime;

    public Customer(int priority, int sequenceNum) {
        this.customerNumber = sequenceNum;
        this.priority = priority;
    }
    public void setArrivalTime(long arrivalTime) {
        this.arrivalTime = arrivalTime;
    }
    public long getArrivalTime() {
        return arrivalTime;
    }

    public long getServiceStartTime() {
        return serviceStartTime;
    }

    public void setServiceStartTime(long serviceStartTime) {
        this.serviceStartTime = serviceStartTime;
    }

    public long getServiceCompletionTime() {
        return serviceCompletionTime;
    }

    public void setServiceCompletionTime(long serviceCompletionTime) {
        this.serviceCompletionTime = serviceCompletionTime;
    }

    @Override
    public int compareTo(Customer o) {
        // If customers are the same priority, the first customer in should remain at the front of the queue
        if (o.priority - this.priority == 0)
        {
            return o.customerNumber - this.customerNumber;
        }
        return o.priority - this.priority;
    }
}
