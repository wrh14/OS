package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator2 {
    /**
     * Allocate a new communicator.
     */
    public Communicator2() {
    }


    //These two is for the test of Condition2
    public void speak2(int word) {
        lock.acquire();
        while(isTransfered){
            listenReady2.sleep();
        }
        isTransfered = true;
        this.word = word;
        speakReady2.wake();
        lock.release();
    }

    public int listen2() {
        lock.acquire();
        while(!isTransfered){
            speakReady2.sleep();
        }
        isTransfered = false;
        listenReady2.wake();
        lock.release();
        return word;
    }

    Lock lock = new Lock();

    Condition2 speakReady2 = new Condition2(lock);
    Condition2 listenReady2 = new Condition2(lock);
    int word = 0;
    boolean isTransfered = false;
}
