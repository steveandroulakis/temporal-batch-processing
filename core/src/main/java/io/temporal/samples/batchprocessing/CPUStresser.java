package io.temporal.samples.batchprocessing;

public class CPUStresser {
    public void startStressingCPU() {

            int maxIterations = 50; // 500000; // Specify the number of iterations before stopping

            // capture the start time
            long startTime = System.currentTimeMillis();

            int iteration = 0;
            while (iteration < maxIterations) {
                // Perform some computationally intensive task
                double value = Math.random();
                for (int j = 0; j < 1; j++) {
                    value = Math.sin(value);
                    value = Math.cos(value);
                    value = Math.tan(value);
                }

                iteration++;
            }

            // capture the end time
            long endTime = System.currentTimeMillis();

            // calculate the elapsed time
            long elapsedTime = endTime - startTime;
            System.out.println("Stress CPU elapsed time: " + elapsedTime + "ms");

    }
}