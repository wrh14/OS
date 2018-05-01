package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;

    
    static int peopleOnBoat = 0;

    static int adultsOnOahu = 0;
    static int childrenOnOahu = 0;
    static int adultsOnMolokai = 0;
    static int childrenOnMolokai = 0;
    static boolean RealGameOver = false;
    static int Oahu =0;
    static int Molokai=1;
    static int BoatLocation = Oahu;

    static Lock boatLock = new Lock();

    static Lock GameOverLock = new Lock();
    static Condition2 IsGameOver = new Condition2(GameOverLock);
    static Condition2 boatHolder1 = new Condition2(boatLock);
    static Condition2 boatHolder2 = new Condition2(boatLock);

    static Communicator communicator1 = new Communicator();
    static Communicator communicator2 = new Communicator();

    static class Child implements Runnable {
        int location;
        int MemoryPeople;
        Child() 
        {
            location = Oahu;
        }

        public void run() {
            ChildItinerary(this);
        }
    }

    static class Adult implements Runnable {
        int location;
        int MemoryPeople;
        Adult() 
        {
            location = Oahu;
        }

        public void run() {
            AdultItinerary(this);
        }
    }
    public static void selfTest()
    {
        BoatGrader b = new BoatGrader();

        System.out.println("\n This test is for Task 6: Boat");
        
        begin(6, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
        // Store the externally generated autograder in a class
        // variable to be accessible by children.
        bg = b;
        // Instantiate global variables here

        // Create threads here. See section 3.4 of the Nachos for Java
        // Walkthrough linked from the projects page.

        // Create all Adult threads
        // System.out.println("\n The Test for Task 6, adults = " + adults + ", children = " + children);

        // if(children < 2){
        //     System.out.println("The goal can't be achieve. exit !");
        //     return;
        // }

        for (int i = 0; i < adults; i++)
            new KThread(new Adult()).setName("Adult").fork();

        // Create all Children threads
        for (int i = 0; i < children; i++)
            new KThread(new Child()).setName("Child").fork();

        communicator1.listen();
              
        while (childrenOnMolokai + adultsOnMolokai != adults + children)
        {
            // God sleep
            communicator2.speak(1);
            communicator1.listen();
            // Now god is awaken by a child
        }

    }

    //gameover and boat exclusive
    //

    static void AdultItinerary(Adult adult)
    {
        bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
        //DO NOT PUT ANYTHING ABOVE THIS LINE. 

 /* This is where you should put your solutions. Make calls
    to the BoatGrader to show that it is synchronized. For
    example:
        bg.AdultRowToMolokai();
    indicates that an adult has rowed the boat across to Molokai
 */
        adultsOnOahu++;
        //here is a busy waiting
        boatLock.acquire();
        while (childrenOnOahu != 1 || BoatLocation != Oahu || peopleOnBoat != 0)
        {
            boatHolder1.sleep();
        }
        bg.AdultRowToMolokai();
        adultsOnOahu--;
        adultsOnMolokai++;
        BoatLocation = Molokai;
        adult.location = Molokai;

        boatHolder2.wakeAll();
        boatLock.release();
    }

    static void ChildItinerary(Child child)
    {
        bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
        //DO NOT PUT ANYTHING ABOVE THIS LINE. 
        childrenOnOahu++;

        while(true){
            if(child.location == Oahu){
                boatLock.acquire();
                while((childrenOnOahu == 1 && peopleOnBoat == 0) || BoatLocation != Oahu){
                    boatHolder1.sleep();
                }
                if(peopleOnBoat == 0){
                    bg.ChildRowToMolokai();

                    childrenOnOahu--;

                    peopleOnBoat++;

                    childrenOnMolokai++;

                    child.location = Molokai;

                    boatHolder1.wakeAll();

                    boatLock.release();
                }
                else{
                    bg.ChildRideToMolokai();

                    child.MemoryPeople = childrenOnOahu + adultsOnOahu - 1;
                    childrenOnOahu--;

                    childrenOnMolokai++;

                    child.location = Molokai;

                    peopleOnBoat = 0;

                    BoatLocation = Molokai;

                    if(child.MemoryPeople == 0){

                        communicator1.speak(1);
                                                //System.out.println(child.MemoryPeople + " |");
                        
                        communicator2.listen();
                    }

                    boatHolder2.wakeAll();

                    boatLock.release();
                }
            }
            else{
                boatLock.acquire();
                while(BoatLocation != Molokai){
                    boatHolder2.sleep();
                }

                bg.ChildRowToOahu();

                childrenOnMolokai--;

                childrenOnOahu++;

                BoatLocation = Oahu;

                child.location = Oahu;

                boatHolder1.wakeAll();

                boatLock.release();
            }
        }
    }

    static void SampleItinerary()
    {
        // Please note that this isn't a valid solution (you can't fit
        // all of them on the boat). Please also note that you may not
        // have a single thread calculate a solution and then just play
        // it back at the autograder -- you will be caught.
        System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
        bg.AdultRowToMolokai();
        bg.ChildRideToMolokai();
        bg.AdultRideToMolokai();
        bg.ChildRideToMolokai();
    }



}