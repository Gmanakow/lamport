package com.manakov;
import mpi.MPI;

import java.util.ArrayList;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class Unit {
    public int rank;
    public int size;

    public Stack<Task> tasks = null;
    public ArrayList<LampartTime> queue = null;

    AtomicIntegerArray ticket;
    AtomicIntegerArray entering;


    public Unit(int rank){
        this.rank = rank;
        this.size = MPI.COMM_WORLD.Size();

        this.tasks = new Stack<>();
        this.queue = new ArrayList<>();

        this.ticket = new AtomicIntegerArray(size);
        this.entering = new AtomicIntegerArray(size);
    }

    public void exec(){
            lock(rank);

            for (int i = 0; i< 10; i++){
                System.out.println("hey " + i + " from rank " + rank);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            };

            unlock(rank);

    }

    public void lock(int rank){
        System.out.println("locked " + rank);
        entering.set(rank, 1);

        int max = 0;
        for (int i = 0; i<size; i++){
            int current = ticket.get(i);
            if (current > max){
                max = current;
            }
        }

        ticket.set(rank, 1+max);
        entering.set(rank, 0);

        for (int i = 0; i< size; ++i){
            if (i != rank){
                while (entering.get(i) == 1) {
                    System.out.println(rank + "yeild");
                    Thread.yield();
                }
                System.out.println(i + " "+ ticket.get(i) + " " +ticket.get(rank) );
                while ((ticket.get(i) != 0) && ((ticket.get(rank) > ticket.get(i)) || ((ticket.get(rank) == ticket.get(i)) && (rank > i))) ){
                    System.out.println(rank + "yeild");
                    Thread.yield();
                }
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    public void unlock(int rank){
        System.out.println("unlocked " + rank);
        ticket.set(rank, 0);
    }


}
