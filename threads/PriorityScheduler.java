package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Iterator;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * We have adopted some ideas (especially the data structure of waitQueue) from https://github.com/viturena/nachos.git
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param transferPriority <tt>true</tt> if this queue should
     *     transfer priority from waiting threads
     *     to the owning thread.
     * @return a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());

        Lib.assertTrue(priority >= priorityMinimum &&
                priority <= priorityMaximum);

        getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMaximum)
            return false;

        setPriority(thread, priority+1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMinimum)
            return false;

        setPriority(thread, priority-1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param thread the thread whose scheduling state to return.
     * @return the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new ThreadState(thread);

        return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
        PriorityQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
        }

        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).waitForAccess(this);
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).acquire(this);
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            if (waitQueue.isEmpty())
             return null;
            KThread next = waitQueue.poll().thread;
            acquire(next);
            // debug may need to return user?
            Lib.assertTrue(user == next);
            return next;
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return the next thread that <tt>nextThread()</tt> would
         *  return.
         */
        protected ThreadState pickNextThread() {
         return waitQueue.peek();
        }

        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());
        }

        /**
         * <tt>true</tt> if this queue should transfer priority from waiting
         * threads to the owning thread.
         */
        public boolean transferPriority;
        
        private KThread user = null;
        //why 8? and I think only one lock will be in the waiting queue of a threadstate
        private java.util.PriorityQueue<ThreadState> waitQueue = new java.util.PriorityQueue<ThreadState>(8,new ThreadStateComparator<ThreadState>(this));
        /**
         * need to implement ThreadStateComparator
         */
        
        protected class ThreadStateComparator<T extends ThreadState> implements Comparator<T> 
        {
         protected ThreadStateComparator(nachos.threads.PriorityScheduler.PriorityQueue priorityqueue) {
    priorityQueue = priorityqueue;
   }
         
         private nachos.threads.PriorityScheduler.PriorityQueue priorityQueue;
         
         public int compare(T thread1, T thread2)
         {
          int effP1 = thread1.getEffectivePriority();
          int effP2 = thread2.getEffectivePriority();
          if (effP1 > effP2)
           return -1;
          else if (effP1 < effP2)
           return 1;
          else
          {
           long  waitTime1 = thread1.waiting.get(priorityQueue), waitTime2 =thread2.waiting.get(priorityQueue);
           if (waitTime1 < waitTime2)
               return -1;
              else if (waitTime1 > waitTime2)
               return 1;
              else
               return 0;
          }
         }
         
        }
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param thread the thread this state belongs to.
         */
        public ThreadState(KThread thread) {
            this.thread = thread;

            setPriority(priorityDefault);
        }

        /**
         * Return the priority of the associated thread.
         *
         * @return the priority of the associated thread.
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Return the effective priority of the associated thread.
         *
         * @return the effective priority of the associated thread.
         */
        public int getEffectivePriority() {
            return effectivePriority;
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param priority the new priority.
         */
        public void setPriority(int priority) {
            if (this.priority == priority)
                return;

            this.priority = priority;

            updateEffectivePriority();
        }
        
        public void updateEffectivePriority()
        {
         for (PriorityQueue waitingqueue : waiting.keySet())
    waitingqueue.waitQueue.remove(this);
         int temp = priority;
         for (PriorityQueue pq : waited)
         {
          if (pq.transferPriority)
          {
           ThreadState largestThread = pq.waitQueue.peek();
           if (largestThread != null)
           {
            if (largestThread.getEffectivePriority() > temp)
             temp = largestThread.getEffectivePriority();
           }
          }
         }
         
         boolean transfer = (effectivePriority != temp);
         effectivePriority = temp;
         /** will this code cause this to be added for more than one time? 
         * should we just modify the priority in the waitQueue?
         * or will the original "this" be replaced?
         */
         for (PriorityQueue waitingqueue : waiting.keySet())
          waitingqueue.waitQueue.add(this);
         
         if (transfer)
         {
          for (PriorityQueue waitingqueue : waiting.keySet())
           if (waitingqueue.transferPriority && waitingqueue.user != null)
            getThreadState(waitingqueue.user).updateEffectivePriority();
         }
        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitQueue</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param waitQueue the queue that the associated thread is
         *    now waiting on.
         *
         * @see nachos.threads.ThreadQueue#waitForAccess
         */
        public void waitForAccess(PriorityQueue waitQueue) {
         if (!waiting.containsKey(waitQueue)) {
          release(waitQueue);
          waiting.put(waitQueue, Machine.timer().getTime());
          // will it cause multiple add? maybe not. One key, one add
          waitQueue.waitQueue.add(this);
          if (waitQueue.user != null)
           getThreadState(waitQueue.user).updateEffectivePriority();
         }
        }

        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @see nachos.threads.ThreadQueue#acquire
         * @see nachos.threads.ThreadQueue#nextThread
         */
        public void acquire(PriorityQueue waitQueue) {
         if (waitQueue.user != null) {
    getThreadState(waitQueue.user).release(waitQueue);
   }
         waitQueue.waitQueue.remove(this);
         waitQueue.user = this.thread;
         waited.add(waitQueue);
         waiting.remove(waitQueue);
            updateEffectivePriority();
        }
        
        private void release(PriorityQueue priorityQueue) {
   if (waited.remove(priorityQueue)) {
    priorityQueue.user = null;
    updateEffectivePriority();
   }
  }

        /** The thread with which this object is associated. */
        protected KThread thread;
        /** The priority of the associated thread. */
        protected int priority;
        protected int effectivePriority;
        
        // The queues of threads which are waiting for this thread to release resources
        private HashSet<nachos.threads.PriorityScheduler.PriorityQueue> waited = new HashSet<nachos.threads.PriorityScheduler.PriorityQueue>();
        
        // The lock this thread is waiting on and its waiting time
        public HashMap<nachos.threads.PriorityScheduler.PriorityQueue,Long> waiting = new HashMap<nachos.threads.PriorityScheduler.PriorityQueue,Long>();
    }


    private static class RunnableLow implements Runnable  {

        RunnableLow(Lock lock, boolean communicate) {
            this.lock = lock;
            this.communicate = communicate;
        }

        public void run() { 
            lock.acquire();
            while (this.communicate == false) {
                System.out.print("Low Priority thread blocked.\n");
                KThread.currentThread().yield();
            }
            this.communicate = false;
            System.out.print("Low thread released\n");
            lock.release();
        }

        Lock lock;
        static public boolean communicate = false;
    } 

    private static class RunnableHigh implements Runnable  {

        RunnableHigh(Lock lock) {
            this.lock = lock;
        }

        public void run() { 
            RunnableLow.communicate = true;

            lock.acquire();
            while (RunnableLow.communicate == true) {
                System.out.print("High Priority thread blocked\n");
                KThread.currentThread().yield();
            }

            RunnableLow.communicate = true;
            System.out.print("High Priority thread released\n");
            lock.release();
        }

        Lock lock;
        static public boolean communicate = false;
    } 

    private static class RunnableMedi implements Runnable  {
        RunnableMedi() {
        }

        public void run() { 
            while(RunnableLow.communicate == false) {
                System.out.print("Medium Priority thread blocked\n");
                KThread.currentThread().yield();
            }

            System.out.print("Medium Priority thread released. good.\n");
            KThread.currentThread().yield();
        }
    }

    /**
     * VAR4: Create a scenario to hit the priority inverse problem.
     * Verify the highest thread is blocked by lower priority thread.
     */

    //This test is adopted from priority test in https://github.com/thinkhy/CS162.git
    public static void selfTest(){
        System.out.println(" ");
        System.out.println("This is the test for task 5 : PriorityScheduler()");

        Lock lock = new Lock();

        // low priority thread closes the door
        KThread low = new KThread(new RunnableLow(lock, false));
        low.fork();
        boolean intStatus = Machine.interrupt().disable();
        ThreadedKernel.scheduler.setPriority(low, 1);
        Machine.interrupt().restore(intStatus);
        KThread.currentThread().yield();

        
        KThread high = new KThread(new RunnableHigh(lock));
        high.fork();
        intStatus = Machine.interrupt().disable();
        ThreadedKernel.scheduler.setPriority(high, 7);
        Machine.interrupt().restore(intStatus);

        
        KThread medium = new KThread(new RunnableMedi());
        medium.fork();
        intStatus = Machine.interrupt().disable();
        ThreadedKernel.scheduler.setPriority(medium, 4);
        Machine.interrupt().restore(intStatus);

        KThread.currentThread().yield();
    }
    
}