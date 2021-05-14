package com.manakov;

import mpi.MPI;

public class App 
{
    public static void main( String[] args ) {

        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        Unit unit = new Unit(rank);

        unit.exec();

        MPI.Finalize();

    }
}
