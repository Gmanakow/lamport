package com.manakov;

import mpi.MPI;

import javax.sound.sampled.FloatControl;

public class App 
{
    public static void main( String[] args ) {

        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();

        Unit unit = new Unit(rank);

        try {
            for (int i = 0; i< 10; i++) {
                unit.lock();
                Thread.sleep(10);
                System.out.println(rank + " act " + i);
                Thread.sleep(10);
                System.out.println(rank + " act " + i);
                Thread.sleep(10);
                System.out.println(rank + " act " + i);
                Thread.sleep(10);
                System.out.println(rank + " act " + i);
                Thread.sleep(10);
                System.out.println(rank + " act " + i);
                Thread.sleep(10);
                System.out.println(rank + " act " + i);
                Thread.sleep(10);
                System.out.println(rank + " act " + i);
                Thread.sleep(10);
                System.out.println(rank + " act " + i);
                unit.unlock();
            }
        } catch (InterruptedException e){
        }

        MPI.COMM_WORLD.Barrier();

        unit.merge();

        MPI.Finalize();

        System.exit(0);
    }
}
