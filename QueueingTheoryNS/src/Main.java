// Queueing theory project
// Robert Kaufman
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class Main extends Thread{
    ArrivalProcess arrivals;
    ArrayList<Servicer> servicers = new ArrayList<>();
    PriorityBlockingQueue priorityQueue = new PriorityBlockingQueue();
    LinkedBlockingQueue nonPriorityQueue = new LinkedBlockingQueue();
    long runTimeMs; // Run for this much time
    long pollTime = 5;
    public int timeScale;
    boolean priority;
/**
 * @param runTimeMs How long to run the sim in milliseconds
 * @param priority whether to generate priorties for the customers. They will be [1,3] and ~U
 * @param servers number of servers
 * @param full whether to service full or partial batches
 * @param ms the number of ms in a second, if you use a smaller number, the sim will run faster than realtime
 */
    public Main(int runTimeMs, double mu, double lambda, boolean priority, int servers, int batchServiceSize, int batchArrivalSize, boolean full, int ms, int maxSystemSize) {
        this.runTimeMs = runTimeMs;
        this.timeScale = ms;
        this.priority = priority;
        int server = 1;
        if (priority) {
            arrivals = new ArrivalProcess(maxSystemSize, lambda, priorityQueue, batchArrivalSize, priority, ms);
            for (int i = 0; i < servers; i++) {
                servicers.add(new Servicer(server++, mu, priorityQueue, batchServiceSize, full, ms));
            }
        }
        else {
            arrivals = new ArrivalProcess(maxSystemSize, lambda, nonPriorityQueue, batchArrivalSize, priority, ms);
            for (int i = 0; i < servers; i++) {
                servicers.add(new Servicer(server++, mu, nonPriorityQueue, batchServiceSize, full, ms));
            }
        }
    }

    public void run()
    {
        long startTimeMs = System.currentTimeMillis();
        for (Servicer x : servicers)
        {
            x.start();
        }
        arrivals.start();
        int totalInService = 0;
        int totalInQueue = 0;
        int timesPolled = 0;
        long printTime = 0;
        long timeSinceStart = 0;
        while (System.currentTimeMillis() - startTimeMs < runTimeMs)
        {
            if (System.currentTimeMillis() - printTime > 5000)
            {
                printTime = System.currentTimeMillis();
                //System.out.println("Runtime remaining: " + (runTimeMs-timeSinceStart)/1000 + " s");
                timeSinceStart += 5000;
            }
            try {
                sleep(pollTime);
                timesPolled += 1;
                //printStatistics();
                if (priority) {
                    if (priorityQueue.size() > 1000 && priorityQueue.size() % 100 == 0) {
                        //System.out.println("Queue is backed up " + priorityQueue.size());
                    }
                    totalInQueue += priorityQueue.size();
                    for (Servicer x : servicers) {
                        totalInService += x.numberInService;
                    }
                }
                else {
                    if (nonPriorityQueue.size() > 1000 && nonPriorityQueue.size() % 100 == 0) {
                        //System.out.println("Queue is backed up " + nonPriorityQueue.size());
                    }
                    totalInQueue += nonPriorityQueue.size();
                    for (Servicer x : servicers) {
                        totalInService += x.numberInService;
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            System.out.println("\n************************************************");
            System.out.println(priority ? "Priority Service" : "No priority");
            arrivals.stopRun();
            arrivals.join();
            System.out.println("Effective lambda: " + arrivals.effectiveLambda);
            System.out.println("Total Arrivals: " + arrivals.totalArrivals);
            for (Servicer x : servicers) {
                x.stopRun();
                x.join();
                System.out.println(new StringBuilder().append("Server ").append(x.server).append(" Effective mu: ").append(x.effectiveMu).append(" Customers Serviced: ").append(x.getServicedCustomers().size()).toString());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        printStatistics();
        System.out.println("L = " + (double)(totalInService+totalInQueue)/timesPolled);
        System.out.println("Lq = " + (double)totalInQueue/timesPolled);
        System.out.println("************************************************\n");
    }

    public void printStatistics()
    {
        long numServiced = 0;
        long totalTimeInSystem = 0;
        long totalTimeInQueue = 0;
        for (Servicer x : servicers)
        {
            ArrayList<Customer> servicedCustomers = x.getServicedCustomers();
            numServiced += servicedCustomers.size();
            for (Customer c : servicedCustomers)
            {
                totalTimeInQueue += (c.getServiceStartTime() - c.getArrivalTime());
                totalTimeInSystem += (c.getServiceCompletionTime() - c.getArrivalTime());
            }
        }
        System.out.println("Number of Servers: " + servicers.size());
        System.out.println("Total Customers serviced: "+ numServiced);
        double multiple = 1;
        if (timeScale == 100)
        {
            multiple = 10;
        }
        else if (timeScale == 10)
        {
            multiple = 100;
        }
        else if (timeScale == 10000)
        {
            multiple = .1;
        }
        System.out.println("W = " + multiple*totalTimeInSystem/(1000000*numServiced) + " ms");
        System.out.println("Wq = " + multiple*totalTimeInQueue/(1000000*numServiced) + " ms");
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting Simulation");
        System.out.println("M/M/1");
        Main main = new Main(10000,600, 570, false, 1, 1, 1, true, 1000, 1000);
        main.start();
        main.join();
//        System.out.println("M/M/2");
//        Main main2 = new Main(600000,600, 570, false, 2, 1, 1, true, 1000, 1000);
//        main2.start();
//        main2.join();
//        System.out.println("M/M/3");
//        Main main3 = new Main(600000,600, 570, false, 3, 1, 1, true, 1000, 1000);
//        main3.start();
//        main3.join();
//        System.out.println("M/M/10");
//        Main main4 = new Main(600000,600, 570, false, 10, 1, 3, true, 1000, 1000);
//        main4.start();
//        main4.join();
//        System.out.println("M^3/M/10");
//        Main main5 = new Main(600000,600, 285, false, 10, 1, 3, true, 1000, 1000);
//        main5.start();
//        main5.join();
//        System.out.println("M^3/M/1");
//        Main main6 = new Main(600000,600, 285, false, 1, 1, 3, true, 1000, 1000);
//        main6.start();
//        main6.join();
    }
}