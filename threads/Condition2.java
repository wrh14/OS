package nachos.threads;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
        this.conditionLock = conditionLock;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        boolean intStatus = Machine.interrupt().disable();
        conditionLock.release();
        WaitQueue.waitForAccess(KThread.currentThread());
        KThread.sleep();
        conditionLock.acquire();
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        
        boolean intStatus = Machine.interrupt().disable();
        KThread next = WaitQueue.nextThread();
        if (next != null)
        {
        	next.ready();
        }
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        
        boolean intStatus = Machine.interrupt().disable();
        KThread next = WaitQueue.nextThread();
        while (next != null)
        {
        	next.ready();
        	next = WaitQueue.nextThread();
        }
        Machine.interrupt().restore(intStatus);
    }

    public static void selfTest(){
        System.out.println(" ");
        System.out.println("This is the test for task 2 : class Condition2");
        // use the same test with class Comminicator, but with Communicator2, which depends on Condition2.


        Communicator2 communicator = new Communicator2();

        KThread kt = new KThread(new Runnable(){
            public void run(){
                for(int i = 0; i < 5; i++){
                    communicator.speak2(i);
                    System.out.println(KThread.currentThread().toString() + " has speak " + i);
                }  
                ThreadedKernel.alarm.waitUntil(20000);
                for(int i = 5; i < 10; i++){
                    communicator.speak2(i);
                    System.out.println(KThread.currentThread().toString() + " has speak " + i);
                }  
            }
        });
        kt.fork();

        KThread kt2 = new KThread(new Runnable(){
            public void run(){
                ThreadedKernel.alarm.waitUntil(20000);
                for(int i = 0; i<10; i++){
                    int word = communicator.listen2();
                    System.out.println(KThread.currentThread().toString() + " has listen " + word);
                }  
            }
        });
        kt2.fork();

        kt.join();
        kt2.join();
    }

    private Lock conditionLock;
    
    /** have problem on initialization     */
    private ThreadQueue WaitQueue=
            ThreadedKernel.scheduler.newThreadQueue(false);
}
