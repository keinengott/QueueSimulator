import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class ArrivalProcess extends Thread {
    boolean mRunning;
    int maxSystemSize;
    Random rand = new Random();
    public double mLambda;
    public PriorityBlockingQueue priorityQueue;
    public LinkedBlockingQueue nonPriorityQueue;

    int batchSize;
    double ms;
    boolean assignPriorities;
    // priority 1 2 or 3 will be assigned from a uniform random distribution
    // servicing happens without preemption
    public double effectiveLambda = 0.0;
    public int totalArrivals = 0;
    private boolean sleep;

    public ArrivalProcess(int maxSystemSize, double lambda, PriorityBlockingQueue queue, int batchSize, boolean priority, int ms) {
        this.maxSystemSize = maxSystemSize;
        this.mLambda = lambda;
        this.priorityQueue = queue;
        this.mRunning = true;
        this.batchSize = batchSize;
        this.assignPriorities = priority;
        this.ms = ms;
        this.sleep = this.mLambda < 10;
    }

    public ArrivalProcess(int maxSystemSize, double lambda, LinkedBlockingQueue queue, int batchSize, boolean priority, int ms) {
        this.maxSystemSize = maxSystemSize;
        this.mLambda = lambda;
        this.nonPriorityQueue = queue;
        this.mRunning = true;
        this.batchSize = batchSize;
        this.assignPriorities = priority;
        this.ms = ms;
        this.sleep = this.mLambda < 10;
    }

    public void stopRun()
    {
        mRunning = false;
    }

    public double poissonRandomInterarrivalDelay(double rate) {
        return (Math.log(1.0-rand.nextDouble())/(-rate));
    }

    public void run() {
        double totalInterarrivalTimes = 0;
        while (mRunning)
        {
            double sleepTime = poissonRandomInterarrivalDelay(mLambda/(ms*1000000));
            double fraction = sleepTime /1000000 - (long) (sleepTime/1000000);
            long timeNow = System.nanoTime();
            long sleepUntil = timeNow + (long)sleepTime;
            try {
                if (this.sleep) {
                    sleep((long) sleepTime / 1000000, (int) fraction * 1000000);
                }
                else {
                    while (sleepUntil - System.nanoTime() > 0);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // If we were sleeping when the Main thread called stopRun, we would erroneously add to the queue, so break
            if (!mRunning) break;

            totalInterarrivalTimes += sleepTime;
            int numArrivals = batchSize > 1 ? 1 + rand.nextInt(batchSize) : 1;
            totalArrivals += numArrivals;
            for (int i = 0; i < numArrivals; i++) {
                int priority = assignPriorities ? 1 + rand.nextInt(3) : 1;
                if (assignPriorities)
                {
                    if (priorityQueue.size() < maxSystemSize - 1) {
                        Customer x = new Customer(priority, totalArrivals);
                        x.setArrivalTime(System.nanoTime());
                        priorityQueue.add(x);
                    }
                }
                else {
                    if (nonPriorityQueue.size() < maxSystemSize - 1) {
                        Customer y = new Customer(priority, totalArrivals);
                        y.setArrivalTime(System.nanoTime());
                        nonPriorityQueue.add(y);
                    }
                }
            }
        }
        effectiveLambda = (1000000*ms/(totalInterarrivalTimes/(totalArrivals)));
    }
}
