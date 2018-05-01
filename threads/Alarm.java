package nachos.threads;

import nachos.machine.*;
import java.util.TreeSet;
import java.util.Comparator;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        boolean status = Machine.interrupt().disable();
        while(threadWaitSet.size() > 0 && threadWaitSet.first().getWakeTime() <= Machine.timer().getTime()){
            threadWaitSet.pollFirst().getkThread().ready();
        }
        KThread.currentThread().yield();
        Machine.interrupt().restore(status);
        // To be verified that if this yield() is needed. 
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
    	// for now, cheat just to get something working (busy waiting is bad)

        boolean status = Machine.interrupt().disable(); 

    	long wakeTime = Machine.timer().getTime() + x;
    	if (wakeTime > Machine.timer().getTime()){
            threadWaitSet.add(new KThreadTime(wakeTime, KThread.currentThread()));
            KThread.currentThread().sleep();
        }
        Machine.interrupt().restore(status); 
    }

    private class KThreadTime {
        public KThreadTime(long wakeTime, KThread thread){
            this.wakeTime = wakeTime;
            this.thread = thread;
        }

        public long getWakeTime(){
            return wakeTime;
        }

        public KThread getkThread(){
            return thread;
        }

        private KThread thread;
        private long wakeTime;
    }

    TreeSet<KThreadTime> threadWaitSet = new TreeSet<KThreadTime>(new Comparator<KThreadTime>() {  
    @Override  
    public int compare(KThreadTime t1, KThreadTime t2) {  
        return t1.getWakeTime()>t2.getWakeTime() ? 1 : -1;  
    }  
    });


    /**
     *This is the test for Alarm.java, which will be execited in selfTest().
     */
    
    public static void selfTest(){
        System.out.println(" ");
        System.out.println("This is the test for task 3 : class Alarm");


        KThread kt = new KThread(new Runnable(){
            public void run(){
                System.out.println(KThread.currentThread().toString() + " will sleep for at least 10000. Now is " + Machine.timer().getTime());
                ThreadedKernel.alarm.waitUntil(10000);
                System.out.println(KThread.currentThread().toString() + " has been wake! Now is " + Machine.timer().getTime());
            }
        });
        kt.fork();

        /*
        KThread kt2 = new KThread(new Runnable(){
            public void run(){
                for(int k = 0; k < 10 ; k++){
                    System.out.println(KThread.currentThread().toString() + "will sleep at least 1500. Now is " + Machine.timer().getTime() );
                    ThreadedKernel.alarm.waitUntil(1500);
                }
            }
        });
        kt2.fork();

        KThread.currentThread().yield();
        for(int k = 0; k < 5 ; k++){
            System.out.println(KThread.currentThread().toString() + "will sleep at least 3A000. Now is " + Machine.timer().getTime() );
            ThreadedKernel.alarm.waitUntil(3000);
        }
        */

        kt.join();
        //kt2.join();
    }


}
