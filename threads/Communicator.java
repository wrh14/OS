package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        lock.acquire();
        while(isTransfered){
            waitSpeak.sleep();
        }
        isTransfered = true;
        this.word = word;
        speakReady.wake();
        listenReady.sleep();

        lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
        lock.acquire();
        while(!isTransfered){
            speakReady.sleep();
        }
        isTransfered = false;
        listenReady.wake();
        waitSpeak.wake();
        lock.release();
        return word;
    }

    public static void selfTest(){
        System.out.println(" ");
        System.out.println("This is the test for task 4 : class Communicator");


        Communicator communicator = new Communicator();

        KThread kt = new KThread(new Runnable(){
            public void run(){
                for(int i = 0; i < 5; i++){
                    communicator.speak(i);
                    System.out.println(KThread.currentThread().toString() + " has speak " + i);
                }  
                ThreadedKernel.alarm.waitUntil(20000);
                for(int i = 5; i < 10; i++){
                    communicator.speak(i);
                    System.out.println(KThread.currentThread().toString() + " has speak " + i);
                }  
            }
        });
        kt.fork();

        KThread kt2 = new KThread(new Runnable(){
            public void run(){
                ThreadedKernel.alarm.waitUntil(20000);
                for(int i = 0; i<10; i++){
                    int word = communicator.listen();
                    System.out.println(KThread.currentThread().toString() + " has listen " + word);
                }  
            }
        });
        kt2.fork();

        kt.join();
        kt2.join();
    }

    Lock lock = new Lock();
    Condition speakReady = new Condition(lock);
    Condition waitSpeak = new Condition(lock);
    Condition listenReady = new Condition(lock);
    int word = 0;
    boolean isTransfered = false;
}
