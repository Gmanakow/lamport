package com.manakov;

import mpi.MPI;
import mpi.Status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Unit {

    int state;
    int time;
    int lockTime;

    ArrayList<Doub> queue = new ArrayList<>();

    class Doub{
        public Doub(int rank, int time){
            this.rank = rank;
            this.time = 0;
        }
        public int rank;
        public int time;
    }

    int rank;
    int size;

    Thread thread;

    public Unit(int rank){
        this.state = 0;
        this.time = rank;

        this.size = MPI.COMM_WORLD.Size();
        this.rank = rank;

        Runnable runnable = this::main;
        this.thread = new Thread(runnable);
        this.thread.start();
    }

    public void lock(){
        this.state = 1;
        sendNotifications();
        this.lockTime = time;
        increment("lock");
        getNotifications();
        this.state = 2;
    }

    public void unlock(){
        this.state = 0;
        int[] buffer = new int[1];
        buffer[0] = time;
        for (Doub item : queue) {
            MPI.COMM_WORLD.Ssend(buffer, 0, 1, MPI.INT, item.rank, 0);
            System.out.println(rank + " allows " + item.rank + " on release " + time + " " + item.time );
        }
        queue.clear();
    }

    public void merge(){
        try {
            this.thread.stop();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendNotifications(){ //sending awaited messages to all threads, notifying them of intentions to take control.
        int[] buffer = new int[1];
        for (int i=0; i< size; i++){
            if (i!=rank){
                buffer[0] = this.time;
                MPI.COMM_WORLD.Ssend(buffer, 0, 1, MPI.INT, i, 1);
            }
        }
    }

    public void getNotifications(){ // awaiting messages from threads with permission.
        int[] buffer = new int[1];
        for (int i=0; i < size-1; i++){
            Status status = MPI.COMM_WORLD.Recv(buffer, 0, 1, MPI.INT, MPI.ANY_SOURCE, 0);
            increment("income");
        }
    }

    public void increment(String context){
        synchronized ((Object) time){
            this.time++;
            System.out.println(context + " " +rank);
        }
    }

    public void additem(int rank, int time){
        synchronized ((Object) queue){
            this.queue.add(new Doub(rank, time));
        }
    }

    public void main(){
        while(true){
            int[] buffer = new int[1];
            Status status = MPI.COMM_WORLD.Recv(buffer, 0 , 1, MPI.INT, MPI.ANY_SOURCE, 1);
            int incomeTime = buffer[0];
            if (incomeTime > this.time) this.time = incomeTime + 1;
            if (status.source != rank){
                switch (this.state){
                    case 0:
                        buffer[0] = this.time;
                        MPI.COMM_WORLD.Ssend(buffer, 0, 1, MPI.INT, status.source, 0);
                        System.out.println(rank + " allows " + status.source + " cos free");
                        increment("allow by free");
                        break;
                    case 1:
                        if (this.lockTime < incomeTime) {                                                         // if i locked earlier, he waits
                            additem(status.source, incomeTime);
                            System.out.println(rank + " blocks " + status.source + " on time " + this.lockTime + " " + incomeTime );
                        } else if ( this.lockTime == incomeTime ) {
                            if (rank < status.source){                                                            // if we locked at the same time, but i am lower rank, he waits
                                additem(status.source, incomeTime);
                                System.out.println(rank + " blocks " + status.source + " on rank " + this.lockTime + " " + incomeTime );
                            } else {                                                                              // or i will wait.
                                buffer[0] = this.time;
                                MPI.COMM_WORLD.Ssend(buffer, 0, 1, MPI.INT, status.source, 0);
                                System.out.println(rank + " allows " + status.source + " on rank " + this.lockTime + " " + incomeTime );
                                increment("allow by rank");
                            }
                        } else {                                                                                  // or i will wait
                            buffer[0] = this.time;
                            MPI.COMM_WORLD.Ssend(buffer, 0, 1, MPI.INT, status.source, 0);
                            System.out.println(rank + " allows " + status.source + " on time " + this.lockTime + " " + incomeTime );
                            increment("allow by time");
                        }
                        break;
                    case 2: // if i am working, he waits.
                        additem(status.source, incomeTime);
                        System.out.println(rank + " blocks " + status.source + " on work " + this.lockTime + " " + incomeTime );
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
