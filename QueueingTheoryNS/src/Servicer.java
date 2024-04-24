import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class Servicer extends Thread {
    boolean mRunning;
    int server;
    public PriorityBlockingQueue priorityQueue;
    public LinkedBlockingQueue nonPriorityQueue;
    Random rand = new Random();
    public double mu;
    boolean fullBatch;
    public int batchSize;
    public ArrayList<Customer> customersProcessing = new ArrayList<>();
    private final ReentrantLock mutex = new ReentrantLock(); // protect customers array
    private final ArrayList<Customer> servicedCustomers = new ArrayList<>();
    public double ms;
    public int numberInService = 0;
    public boolean priorities;
    public double effectiveMu = 0.0;
    boolean sleep;

    public Servicer(int server, double mu, LinkedBlockingQueue queue, int batchSize, boolean partial, int ms) {
        this.mRunning = true;
        this.server = server;
        this.priorities = false;
        this.nonPriorityQueue = queue;
        this.mu = mu;
        this.batchSize = batchSize;
        this.fullBatch = !partial;
        this.ms = ms;
        this.sleep = this.mu < 10;
    }

    public Servicer(int server, double mu, PriorityBlockingQueue queue, int batchSize, boolean partial, int ms) {
        this.mRunning = true;
        this.server = server;
        this.priorities = true;
        this.priorityQueue = queue;
        this.mu = mu;
        this.batchSize = batchSize;
        this.fullBatch = !partial;
        this.ms = ms;
        this.sleep = this.mu < 10;
    }

    public void stopRun()
    {
        mRunning = false;
    }

    public ArrayList<Customer> getServicedCustomers() {
        // Use this just for testing. If we lock the thread, then we are going to get bad results for service times
        return new ArrayList<>(servicedCustomers);
    }

    public double poissonRandomServiceTime(double rate) {
        return (Math.log(1.0-rand.nextDouble())/(-rate));
    }

    public void run() {
        double totalServiceTimes = 0;
        while (mRunning) {
            try {
                while (customersProcessing.size() < batchSize) {
                    // Keep grabbing customers until we reach our batch size
                    // If sim ends while waiting to fill up a batch, break out to avoid infinite loop
                    if (!mRunning) break;
                    if (priorities) {
                        Customer c = (Customer) priorityQueue.poll();
                        if (c != null) customersProcessing.add(c);
                        // if we allow partial batches and there are no more customers, break out early
                        if (!fullBatch && priorityQueue.size() == 0 && !customersProcessing.isEmpty()) break;
                    }
                    else {
                        Customer c = (Customer) nonPriorityQueue.poll();
                        if (c != null) customersProcessing.add(c);
                        // if we allow partial batches and there are no more customers, break out early
                        if (!fullBatch && nonPriorityQueue.size() == 0 && !customersProcessing.isEmpty()) break;
                    }
                }
                numberInService = customersProcessing.size();
                if (numberInService == 0) break;

                // At this point we wait to simulate the service time
                double sleepTime = poissonRandomServiceTime(mu/(ms*1000000));
                totalServiceTimes += sleepTime;
                long timeNow = System.nanoTime();
                long sleepUntil = timeNow + (long)sleepTime;
                for (Customer x : customersProcessing) x.setServiceStartTime(timeNow);
                double fraction = sleepTime/1000000 - (long) (sleepTime/1000000);

                if (this.sleep) sleep((long) sleepTime/1000000, (int) fraction*1000000);
                else while (sleepUntil - System.nanoTime() > 0);
                for (Customer x : customersProcessing)
                {
                    x.setServiceCompletionTime(sleepUntil);
                    servicedCustomers.add(x);
                }
                customersProcessing.clear();
                numberInService = 0;
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        effectiveMu = (double) 1000000 * ms / (totalServiceTimes / servicedCustomers.size());
    }
}
